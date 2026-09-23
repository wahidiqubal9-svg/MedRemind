package com.medremind.app.data.caregiver

/**
 * Sends a medication reminder to a patient.
 *
 * The current [LocalRemoteReminderService] delivers the reminder on this device
 * through the existing alarm pipeline. A future cloud implementation will send a
 * push signal to the patient's device instead, without changing this interface.
 */
interface RemoteReminderService {
    suspend fun sendReminder(
        patientProfileId: Long,
        medicineId: Long,
        caregiverName: String
    ): ReminderResult
}

sealed class ReminderResult {
    /** The reminder was delivered on this device by the existing alarm system. */
    data class Delivered(val eventId: Long, val local: Boolean = true) : ReminderResult()
    data class Error(val message: String) : ReminderResult()
}
