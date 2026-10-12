package com.medremind.app.data.caregiver

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.CaregiverDirection
import com.medremind.app.data.remote.awaitResult

/** Pushes dose confirmations (taken/skipped/missed) so the other device mirrors them. */
object DoseDataSync {

    /** Push one dose event (after it's created or changed). */
    suspend fun pushEvent(context: Context, eventId: Long) {
        if (eventId <= 0L) return
        val app = context.applicationContext
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return
        val eventDao = AppDatabase.get(app).doseEventDao()
        val event = eventDao.byId(eventId) ?: return
        // Ignore the test alarm and caregiver-reminder placeholders that have no medicine.
        val medicine = AppDatabase.get(app).medicineDao().byId(event.medicineId)
        val profileId = medicine?.profileId ?: return
        val patientUid = resolvePatientUid(context, profileId) ?: return
        val coll = db.collection("patients").document(patientUid).collection("doseEvents")
        val cloudId = event.cloudId.ifBlank { coll.document().id }
        if (event.cloudId.isBlank()) eventDao.update(event.copy(cloudId = cloudId))
        runCatching {
            coll.document(cloudId).set(
                mapOf(
                    "medicineName" to (medicine.name ?: ""),
                    "medicineCloudId" to (medicine.cloudId),
                    "scheduledAt" to event.scheduledAt,
                    "status" to event.status,
                    "actedAt" to event.actedAt,
                    "source" to event.source,
                    "actorName" to event.actorName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitResult()
        }
    }

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
}
