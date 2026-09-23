package com.medremind.app.data.caregiver

import com.medremind.app.data.CaregiverLink
import com.medremind.app.data.PairingRequest
import kotlinx.coroutines.flow.Flow

/**
 * Pairing and consent workflow.
 *
 * The QR code / temporary code carry only a short-lived token. In this offline
 * build validation happens locally; a future cloud implementation validates the
 * token server-side and requires explicit patient approval. No medical data is
 * ever encoded in the token.
 */
interface ConnectionRepository {

    /** Patient side: generate a short-lived code + token to show as a QR. */
    suspend fun createPatientPairingCode(patientName: String): PairingRequest

    /** Look up a still-valid pairing by the 6-digit code. */
    suspend fun pairingByCode(code: String): PairingRequest?

    /** Caregiver side: request access using a code. Creates a pending request. */
    suspend fun requestCaregiverAccess(code: String, caregiverName: String): ConnectionResult

    /** Patient side: approve a pending caregiver request. */
    suspend fun approve(pairingId: Long): Boolean

    /** Patient side: decline a pending caregiver request. */
    suspend fun decline(pairingId: Long): Boolean

    /** Pending requests waiting for this device (owner is the patient). */
    fun pendingRequestsForMe(): Flow<List<CaregiverLink>>

    /** Requests this device created and is waiting to be approved (owner is the caregiver). */
    fun pendingRequestsICreated(): Flow<List<CaregiverLink>>

    suspend fun activeCaregiverForMe(): CaregiverLink?

    /** Marks stale OPEN codes as expired. */
    suspend fun expireStale()
}

sealed class ConnectionResult {
    data class Pending(val incomingLinkId: Long, val pairingId: Long, val patientName: String) :
        ConnectionResult()
    data class Error(val message: String) : ConnectionResult()
}
