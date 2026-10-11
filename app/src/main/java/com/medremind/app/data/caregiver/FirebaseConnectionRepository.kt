package com.medremind.app.data.caregiver

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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

    override suspend fun createPatientPairingCode(patientName: String): PairingRequest {
        val request = local.createPatientPairingCode(patientName)
        val u = uid
        val database = db
        if (u != null && database != null) {
            runCatching {
                database.collection("pairings").document(request.code).set(
                    mapOf(
                        "ownerUid" to u,
                        "peerName" to request.peerName,
                        "token" to request.token,
                        "expiresAt" to request.expiresAt,
                        "status" to PairingStatus.OPEN,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).awaitResult()
            }
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
        val snap = if (database != null) {
            runCatching { database.collection("pairings").document(clean).get().awaitResult() }
                .getOrNull()
        } else null

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
                val myUid = uid
                if (database != null && myUid != null) {
                    runCatching {
                        database.collection("pairings").document(clean)
                            .update("status", PairingStatus.CLAIMED)
                        database.collection("links").document().set(
                            mapOf(
                                "caregiverUid" to myUid,
                                "patientUid" to (snap.getString("ownerUid") ?: ""),
                                "caregiverName" to caregiverName.ifBlank { "Caregiver" },
                                "patientName" to (snap.getString("peerName") ?: "Patient"),
                                "status" to "ACTIVE",
                                "permissions" to CaregiverPermission.DEFAULT_MEDICATION,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        ).awaitResult()
                    }
                }
            }
            return result
        }

        // No cloud config / signed out: same-device local pairing.
        return local.requestCaregiverAccess(clean, caregiverName)
    }

    override suspend fun approve(pairingId: Long): Boolean = local.approve(pairingId)
    override suspend fun decline(pairingId: Long): Boolean = local.decline(pairingId)
    override fun pendingRequestsForMe(): Flow<List<CaregiverLink>> = local.pendingRequestsForMe()
    override fun pendingRequestsICreated(): Flow<List<CaregiverLink>> = local.pendingRequestsICreated()
    override suspend fun activeCaregiverForMe(): CaregiverLink? = local.activeCaregiverForMe()
    override suspend fun expireStale() = local.expireStale()
}
