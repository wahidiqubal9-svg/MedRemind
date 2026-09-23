package com.medremind.app.data.caregiver

import android.content.Context
import com.medremind.app.alarm.ReminderScheduler
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.CaregiverActor
import com.medremind.app.data.CaregiverActivityType
import com.medremind.app.data.DoseEvent
import com.medremind.app.data.DoseSource
import com.medremind.app.data.DoseStatus
import com.medremind.app.data.ScheduleType

/**
 * Delivers a "Remind now" through the existing alarm system on this device.
 *
 * This is not a fake network call: because the caregiver and the patient share
 * this device in the offline build, the real full-screen medication alarm is
 * launched. A real backend/push implementation will replace this class later.
 */
class LocalRemoteReminderService(private val context: Context) : RemoteReminderService {

    override suspend fun sendReminder(
        patientProfileId: Long,
        medicineId: Long,
        caregiverName: String
    ): ReminderResult {
        val db = AppDatabase.get(context)
        val medicine = db.medicineDao().byId(medicineId)
            ?: return ReminderResult.Error("Medicine not found")

        // Prefer an as-needed schedule so a PRN reminder is far from other times,
        // otherwise fall back to any schedule for this medicine.
        val schedules = db.scheduleDao().forMedicine(medicineId).filter { it.enabled }
        val schedule = schedules.firstOrNull { it.type == ScheduleType.AS_NEEDED }
            ?: schedules.firstOrNull()

        val now = System.currentTimeMillis()
        val eventId = db.doseEventDao().insert(
            DoseEvent(
                scheduleId = schedule?.id ?: 0L,
                medicineId = medicineId,
                scheduledAt = now,
                status = DoseStatus.PENDING,
                source = DoseSource.CAREGIVER
            )
        )

        db.caregiverDao().insertActivity(
            com.medremind.app.data.CaregiverActivity(
                patientProfileId = patientProfileId,
                actor = CaregiverActor.CAREGIVER,
                type = CaregiverActivityType.REMIND_SENT,
                medicineName = medicine.name,
                message = "Reminder sent for ${medicine.name}"
            )
        )

        // Launch the existing alarm almost immediately.
        ReminderScheduler.scheduleSnooze(context, eventId, now + 1500L)
        return ReminderResult.Delivered(eventId = eventId)
    }
}
