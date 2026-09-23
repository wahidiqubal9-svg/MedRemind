package com.medremind.app.data.caregiver

import android.content.Context
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.CaregiverActor
import com.medremind.app.data.CaregiverDirection
import com.medremind.app.data.CaregiverLink
import com.medremind.app.data.CaregiverPermission
import com.medremind.app.data.CaregiverStatus
import com.medremind.app.data.PairingRequest
import com.medremind.app.data.PairingStatus
import com.medremind.app.data.Patient
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Offline pairing. The token is validated locally; nothing leaves the device. */
class LocalConnectionRepository(private val context: Context) : ConnectionRepository {

    private val dao = AppDatabase.get(context).caregiverDao()

    override suspend fun createPatientPairingCode(patientName: String): PairingRequest {
        expireStale()
        val request = PairingRequest(
            token = UUID.randomUUID().toString().replace("-", "").take(24),
            code = (100000..999999).random().toString(),
            direction = CaregiverDirection.INCOMING,
            peerName = patientName.ifBlank { "Patient" },
            expiresAt = System.currentTimeMillis() + CODE_TTL_MILLIS,
            status = PairingStatus.OPEN
        )
        val id = dao.insertPairing(request)
        return request.copy(id = id)
    }

    override suspend fun pairingByCode(code: String): PairingRequest? {
        val clean = code.filter { it.isDigit() }
        if (clean.length != 6) return null
        val request = dao.findByCode(clean) ?: return null
        return if (request.expiresAt < System.currentTimeMillis()) {
            dao.updatePairing(request.copy(status = PairingStatus.EXPIRED))
            null
        } else request
    }

    override suspend fun requestCaregiverAccess(
        code: String,
        caregiverName: String
    ): ConnectionResult {
        val request = pairingByCode(code)
            ?: return ConnectionResult.Error("That code doesn't match an active request.")
        val patientName = request.peerName.ifBlank { "Patient" }

        val existingPatients = dao.patientsOnce()
        val patientId = dao.insertPatient(
            Patient(
                name = patientName,
                relation = "",
                sortOrder = existingPatients.size,
                createdAt = System.currentTimeMillis()
            )
        )
        val now = System.currentTimeMillis()
        dao.insertLink(
            CaregiverLink(
                patientProfileId = patientId,
                caregiverName = caregiverName.ifBlank { "Caregiver" },
                direction = CaregiverDirection.OUTGOING,
                status = CaregiverStatus.PENDING,
                permissions = CaregiverPermission.DEFAULT_MEDICATION,
                pairingId = request.id,
                createdAt = now,
                updatedAt = now
            )
        )
        val incomingId = dao.insertLink(
            CaregiverLink(
                patientProfileId = 0L,
                caregiverName = caregiverName.ifBlank { "Caregiver" },
                direction = CaregiverDirection.INCOMING,
                status = CaregiverStatus.PENDING,
                permissions = CaregiverPermission.DEFAULT_MEDICATION,
                pairingId = request.id,
                createdAt = now,
                updatedAt = now
            )
        )
        dao.updatePairing(request.copy(status = PairingStatus.CLAIMED))
        return ConnectionResult.Pending(
            incomingLinkId = incomingId,
            pairingId = request.id,
            patientName = patientName
        )
    }

    override suspend fun approve(pairingId: Long): Boolean {
        val links = dao.linksForPairing(pairingId)
        if (links.isEmpty()) return false
        links.forEach {
            dao.updateLink(
                it.copy(status = CaregiverStatus.ACTIVE, updatedAt = System.currentTimeMillis())
            )
        }
        dao.pairing(pairingId)?.let { dao.updatePairing(it.copy(status = PairingStatus.APPROVED)) }
        dao.insertActivity(
            com.medremind.app.data.CaregiverActivity(
                linkId = links.first().id,
                patientProfileId = 0L,
                actor = CaregiverActor.PATIENT,
                type = com.medremind.app.data.CaregiverActivityType.CONNECTED,
                message = "Caregiver connected"
            )
        )
        return true
    }

    override suspend fun decline(pairingId: Long): Boolean {
        val links = dao.linksForPairing(pairingId)
        if (links.isEmpty()) return false
        links.forEach { dao.deleteLink(it.id) }
        dao.pairing(pairingId)?.let { dao.updatePairing(it.copy(status = PairingStatus.DECLINED)) }
        return true
    }

    override fun pendingRequestsForMe(): Flow<List<CaregiverLink>> =
        dao.observePending(CaregiverDirection.INCOMING)

    override fun pendingRequestsICreated(): Flow<List<CaregiverLink>> =
        dao.observePending(CaregiverDirection.OUTGOING)

    override suspend fun activeCaregiverForMe(): CaregiverLink? =
        dao.activeLink(0L, CaregiverDirection.INCOMING)

    override suspend fun expireStale() {
        dao.openPairings()
            .filter { it.expiresAt < System.currentTimeMillis() }
            .forEach { dao.updatePairing(it.copy(status = PairingStatus.EXPIRED)) }
    }

    companion object {
        /** Pairing codes are short-lived by design. */
        const val CODE_TTL_MILLIS = 5 * 60 * 1000L
    }
}
