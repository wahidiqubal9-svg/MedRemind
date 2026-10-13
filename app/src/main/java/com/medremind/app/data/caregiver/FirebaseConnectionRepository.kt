package com.medremind.app.data.caregiver

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.CaregiverDirection
import com.medremind.app.data.CaregiverLink
import com.medremind.app.data.CaregiverPermission
import com.medremind.app.data.PairingRequest
import com.medremind.app.data.PairingStatus
import com.medremind.app.data.remote.awaitResult
import kotlinx.coroutines.flow.Flow

/**
 * Pairing backed by Firestore so a caregiver on one device can connect using a
 * code (or QR) generated on the patient's device. Falls back to the local,
 * same-device flow when cloud isn't configured or the user isn't signed in.
 */
class FirebaseConnectionRepository(private val context: Context) : ConnectionRepository {

    private val local = LocalConnectionRepository(context)
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    private val uid: String? get() = auth?.currentUser?.uid

    /** Whether a cloud pairing is possible (signed in + Firestore available). */
    val cloudReady: Boolean get() = uid != null && db != null

    override suspend fun createPatientPairingCode(patientName: String): PairingRequest {
        val request = local.createPatientPairingCode(patientName)
        val u = uid
        val database = db
        if (u != null && database != null) {
            // Publish the code so a caregiver on another device can find it.
            // Fire-and-forget: never block showing the QR/code on the network.
            database.collection("pairings").document(request.code).set(
                mapOf(
                    "ownerUid" to u,
                    "peerName" to request.peerName,
                    "token" to request.token,
                    "expiresAt" to request.expiresAt,
                    "status" to PairingStatus.OPEN,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
        return request
    }

    override suspend fun pairingByCode(code: String): PairingRequest? {
        val clean = code.filter { it.isDigit() }
        if (clean.length != 6) return null
        val database = db
        if (database != null) {
            val snap = runCatching {
                database.collection("pairings").document(clean).get().awaitResult()
            }.getOrNull()
            if (snap != null && snap.exists()) {
                val status = snap.getString("status")
                val expires = snap.getLong("expiresAt") ?: 0L
                if (status == PairingStatus.OPEN && expires > System.currentTimeMillis()) {
                    return PairingRequest(
                        token = snap.getString("token") ?: "",
                        code = clean,
                        direction = CaregiverDirection.INCOMING,
                        peerName = snap.getString("peerName") ?: "Patient",
                        expiresAt = expires,
                        status = PairingStatus.OPEN
                    )
                }
            }
        }
        return local.pairingByCode(clean)
    }

    override suspend fun requestCaregiverAccess(
        code: String,
        caregiverName: String
    ): ConnectionResult {
        val clean = code.filter { it.isDigit() }
        if (clean.length != 6) return ConnectionResult.Error("Enter the 6-digit code.")
        val database = db
        if (database == null) {
            return local.requestCaregiverAccess(clean, caregiverName)
        }

        val read = runCatching {
            database.collection("pairings").document(clean).get().awaitResult()
        }
        if (read.isFailure) {
            return ConnectionResult.Error(
                "Couldn't reach the cloud. Check your internet and that Firestore " +
                    "is enabled for this project."
            )
        }
        val snap = read.getOrNull()

        if (snap != null && snap.exists()) {
            val status = snap.getString("status")
            val expires = snap.getLong("expiresAt") ?: 0L
            if (status != PairingStatus.OPEN) {
                return ConnectionResult.Error("That code has already been used.")
            }
            if (expires < System.currentTimeMillis()) {
                return ConnectionResult.Error("That code has expired. Ask for a new one.")
            }
            // Bring the cloud pairing into the local store, then run the local
            // flow (creates the patient profile and both link rows).
            val myUid = uid ?: return ConnectionResult.Error(
                "Sign in on this phone, then try again to connect to someone on another device."
            )
            local.injectRemotePairing(
                token = snap.getString("token") ?: "",
                code = clean,
                patientName = snap.getString("peerName") ?: "Patient",
                expiresAt = expires
            )
            val result = local.requestCaregiverAccess(clean, caregiverName)
            if (result is ConnectionResult.Pending) {
                // The patient generated the code on purpose, so treat that as
                // consent and activate the link immediately.
                local.approve(result.pairingId)
                val patientUid = snap.getString("ownerUid") ?: ""
                // Remove any earlier links with this patient so reconnects don't
                // leave duplicate caregivers/patients on either phone.
                runCatching {
                    database.collection("links")
                        .whereEqualTo("caregiverUid", myUid)
                        .whereEqualTo("patientUid", patientUid)
                        .get().awaitResult().documents.forEach { it.reference.delete() }
                }
                val linkRef = database.collection("links").document()
                runCatching {
                    database.collection("pairings").document(clean)
                        .update("status", PairingStatus.CLAIMED)
                    linkRef.set(
                        mapOf(
                            "caregiverUid" to myUid,
                            "patientUid" to patientUid,
                            "caregiverName" to caregiverName.ifBlank { "Caregiver" },
                            "patientName" to (snap.getString("peerName") ?: "Patient"),
                            "status" to "ACTIVE",
                            "permissions" to CaregiverPermission.DEFAULT_MEDICATION,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).awaitResult()
                }
                // Grant the caregiver access to the patient's data + photos
                // (checked by the security rules).
                runCatching {
                    database.collection("access").document(patientUid)
                        .collection("caregivers").document(myUid)
                        .set(mapOf("createdAt" to FieldValue.serverTimestamp()))
                }
                // Remember the cloud id + patient's uid locally so that
                // "Remind now" can route to the patient's phone.
                val dao = AppDatabase.get(context).caregiverDao()
                dao.linksOnce().firstOrNull {
                    it.pairingId == result.pairingId &&
                        it.direction == CaregiverDirection.OUTGOING
                }?.let {
                    dao.updateLink(it.copy(cloudId = linkRef.id, peerUid = patientUid))
                }
            }
            return result
        }

        // Not in the cloud: try the same-device local flow, else explain clearly.
        val localResult = local.requestCaregiverAccess(clean, caregiverName)
        if (localResult is ConnectionResult.Error) {
            return if (uid == null) {
                ConnectionResult.Error(
                    "Sign in on this phone, then try again \u2014 cross-device " +
                        "connection needs an account."
                )
            } else {
                ConnectionResult.Error(
                    "That code doesn't match an active request. Make sure the other " +
                        "phone is signed in when it creates the code."
                )
            }
        }
        return localResult
    }

    override suspend fun approve(pairingId: Long): Boolean = local.approve(pairingId)
    override suspend fun decline(pairingId: Long): Boolean = local.decline(pairingId)
    override fun pendingRequestsForMe(): Flow<List<CaregiverLink>> = local.pendingRequestsForMe()
    override fun pendingRequestsICreated(): Flow<List<CaregiverLink>> = local.pendingRequestsICreated()
    override suspend fun activeCaregiverForMe(): CaregiverLink? = local.activeCaregiverForMe()
    override suspend fun expireStale() = local.expireStale()
}
