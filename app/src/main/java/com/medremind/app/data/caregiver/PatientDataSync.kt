package com.medremind.app.data.caregiver

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.CaregiverDirection
import com.medremind.app.data.remote.awaitResult

/**
 * Pushes a person's medicines (and schedules) to Firestore so the other side of
 * a caregiver link mirrors them:
 *  - caregiver device: a patient's medicines -> `patients/{patientUid}/medicines`
 *  - patient device: my own medicines      -> `patients/{myUid}/medicines`
 */
object PatientDataSync {

    /** Caregiver side: push the medicines of a patient profile. */
    suspend fun pushMedicines(context: Context, profileId: Long) {
        if (profileId == 0L) return
        val app = context.applicationContext
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return
        val dao = AppDatabase.get(app).caregiverDao()
        val link = dao.outgoingCloudLink(profileId) ?: return
        val patientUid = link.peerUid
        if (patientUid.isBlank()) return
        push(db.collection("patients").document(patientUid).collection("medicines"), app, profileId)
    }

    /** Patient side: push my own medicines (only when a caregiver is linked). */
    suspend fun pushMyMedicines(context: Context) {
        val app = context.applicationContext
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return
        val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull() ?: return
        val dao = AppDatabase.get(app).caregiverDao()
        val linked = dao.linksOnce().any {
            it.direction == CaregiverDirection.INCOMING && it.cloudId.isNotBlank()
        }
        if (!linked) return
        push(db.collection("patients").document(uid).collection("medicines"), app, 0L)
    }

    private suspend fun push(coll: CollectionReference, app: Context, profileId: Long) {
        val medicineDao = AppDatabase.get(app).medicineDao()
        val scheduleDao = AppDatabase.get(app).scheduleDao()
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
        // Remove cloud medicines that were deleted locally.
        runCatching {
            coll.get().awaitResult().documents.forEach {
                if (it.id !in keep) it.reference.delete()
            }
        }
    }
}
