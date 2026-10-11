package com.medremind.app.data.caregiver

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.remote.awaitResult

/**
 * Pushes a patient's medicines (and their schedules) to Firestore under
 * `patients/{patientUid}/medicines`, so the patient's own phone can mirror them.
 * No-op when the profile isn't a cross-device patient.
 */
object PatientDataSync {

    suspend fun pushMedicines(context: Context, profileId: Long) {
        if (profileId == 0L) return
        val app = context.applicationContext
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return
        val dao = AppDatabase.get(app).caregiverDao()
        val link = dao.outgoingCloudLink(profileId) ?: return
        val patientUid = link.peerUid
        if (patientUid.isBlank()) return

        val medicineDao = AppDatabase.get(app).medicineDao()
        val scheduleDao = AppDatabase.get(app).scheduleDao()
        val coll = db.collection("patients").document(patientUid).collection("medicines")

        val medicines = medicineDao.getAllOnce(profileId)
        val keep = mutableSetOf<String>()
        for (m in medicines) {
            val cloudId = m.cloudId.ifBlank { coll.document().id }
            keep += cloudId
            val schedule = scheduleDao.forMedicine(m.id).firstOrNull()
            runCatching {
                coll.document(cloudId).set(
                    mapOf(
                        "name" to m.name,
                        "strength" to m.strength,
                        "form" to m.form,
                        "intake" to m.intakeInstruction,
                        "addedBy" to m.addedBy,
                        "doseLabel" to (schedule?.doseLabel ?: ""),
                        "times" to (schedule?.times ?: ""),
                        "scheduleType" to (schedule?.type ?: "DAILY"),
                        "daysMask" to (schedule?.daysMask ?: 0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitResult()
            }
            if (m.cloudId.isBlank()) medicineDao.update(m.copy(cloudId = cloudId))
        }

        // Drop cloud medicines that were removed locally.
        runCatching {
            coll.get().awaitResult().documents.forEach {
                if (it.id !in keep) it.reference.delete()
            }
        }
    }
}
