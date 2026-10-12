package com.medremind.app.data.caregiver

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.CaregiverDirection
import com.medremind.app.data.CaregiverPermission
import com.medremind.app.data.remote.awaitResult

/**
 * "Remind now" for a cross-device link: writes a reminder to Firestore so the
 * **patient's** phone fires the alarm. Falls back to the local pipeline when the
 * patient is on the same device (no cloud link).
 */
class CloudRemoteReminderService(private val context: Context) : RemoteReminderService {

    private val local = LocalRemoteReminderService(context)
    private val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    private val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    override suspend fun sendReminder(
        patientProfileId: Long,
        medicineId: Long,
        caregiverName: String
    ): ReminderResult {
        val dao = AppDatabase.get(context).caregiverDao()
        val link = dao.linksOnce().firstOrNull {
            it.patientProfileId == patientProfileId &&
                it.direction == CaregiverDirection.OUTGOING &&
                it.cloudId.isNotBlank() &&
                it.peerUid.isNotBlank()
        }
        val database = db
        val uid = auth?.currentUser?.uid
        if (link != null && database != null && uid != null) {
            if (!CaregiverPermission.has(link.permissions, CaregiverPermission.SEND_REMINDERS)) {
                return ReminderResult.Error("You don't have permission to send reminders.")
            }
            val medicine = AppDatabase.get(context).medicineDao().byId(medicineId)
            val name = medicine?.name ?: "Medicine"
            val patientName = AppDatabase.get(context).caregiverDao()
                .patient(link.patientProfileId)?.name ?: "your patient"
            val now = System.currentTimeMillis()
            return runCatching {
                val ref = database.collection("reminders").add(
                    mapOf(
                        "caregiverUid" to uid,
                        "patientUid" to link.peerUid,
                        "medicineName" to name,
                        "caregiverName" to caregiverName,
                        "patientName" to patientName,
                        "status" to "PENDING",
                        "sentAtMillis" to now,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).awaitResult()
                // Alert the caregiver if it isn't delivered (5 min) or expires (30 min).
                com.medremind.app.alarm.ReminderScheduler.scheduleReminderCheck(
                    context, ref.id, now + 5 * 60 * 1000L, "CHECK5"
                )
                com.medremind.app.alarm.ReminderScheduler.scheduleReminderCheck(
                    context, ref.id, now + 30 * 60 * 1000L, "EXPIRE30"
                )
                ReminderResult.Delivered(eventId = 0L, local = false)
            }.getOrElse {
                ReminderResult.Error("Couldn't reach the cloud to send the reminder.")
            }
        }
        // Same-device patient (no cloud link): use the local alarm pipeline.
        return local.sendReminder(patientProfileId, medicineId, caregiverName)
    }

    override suspend fun sendReminderByName(
        patientProfileId: Long,
        medicineName: String,
        caregiverName: String
    ): ReminderResult = local.sendReminderByName(patientProfileId, medicineName, caregiverName)
}
