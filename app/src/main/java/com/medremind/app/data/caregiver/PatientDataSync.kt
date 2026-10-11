package com.medremind.app.data.caregiver

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.CaregiverDirection
import com.medremind.app.data.Medicine
import com.medremind.app.data.remote.awaitResult

/**
 * Pushes a person's medicines to Firestore so the other side of a caregiver link
 * mirrors them. Additive only (never removes other docs) so the two sides can
 * both contribute; deletions are propagated explicitly via [deleteMedicine].
 */
object PatientDataSync {

    /** Caregiver side: push the medicines of a patient profile. */
    suspend fun pushMedicines(context: Context, profileId: Long) {
        if (profileId == 0L) return
        val app = context.applicationContext
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return
        val patientUid = resolvePatientUid(context, profileId) ?: return
        push(db.collection("patients").document(patientUid).collection("medicines"), app, profileId)
    }

    /** Patient side: push my own medicines (only when a caregiver is linked). */
    suspend fun pushMyMedicines(context: Context) {
        val app = context.applicationContext
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return
        val patientUid = resolvePatientUid(context, 0L) ?: return
        push(db.collection("patients").document(patientUid).collection("medicines"), app, 0L)
    }

    /** Remove a medicine's cloud document so the other phone drops it. */
    suspend fun deleteMedicine(context: Context, medicine: Medicine) {
        val cloudId = medicine.cloudId
        if (cloudId.isBlank()) return
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return
        val patientUid = resolvePatientUid(context, medicine.profileId) ?: return
        runCatching {
            db.collection("patients").document(patientUid)
                .collection("medicines").document(cloudId).delete().awaitResult()
        }
    }

    /** Which account owns the shared medicine list for this profile. */
    private suspend fun resolvePatientUid(context: Context, profileId: Long): String? {
        val app = context.applicationContext
        val dao = AppDatabase.get(app).caregiverDao()
        return if (profileId != 0L) {
            dao.outgoingCloudLink(profileId)?.peerUid?.ifBlank { null }
        } else {
            val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
            val linked = dao.linksOnce().any {
                it.direction == CaregiverDirection.INCOMING && it.cloudId.isNotBlank()
            }
            if (uid != null && linked) uid else null
        }
    }

    private suspend fun push(coll: CollectionReference, app: Context, profileId: Long) {
        val medicineDao = AppDatabase.get(app).medicineDao()
        val scheduleDao = AppDatabase.get(app).scheduleDao()
        for (m in medicineDao.getAllOnce(profileId)) {
            val cloudId = m.cloudId.ifBlank { coll.document().id }
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
    }
}
