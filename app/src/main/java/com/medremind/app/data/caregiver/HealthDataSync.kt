package com.medremind.app.data.caregiver

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.CaregiverDirection
import com.medremind.app.data.Metric
import com.medremind.app.data.remote.awaitResult

/** Pushes health readings to Firestore so the other device of the link mirrors them. */
object HealthDataSync {

    suspend fun pushMetrics(context: Context, profileId: Long) {
        if (profileId == 0L) return
        val app = context.applicationContext
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return
        val patientUid = resolvePatientUid(context, profileId) ?: return
        push(db.collection("patients").document(patientUid).collection("metrics"), app, profileId)
    }

    suspend fun pushMyMetrics(context: Context) {
        val app = context.applicationContext
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return
        val patientUid = resolvePatientUid(context, 0L) ?: return
        push(db.collection("patients").document(patientUid).collection("metrics"), app, 0L)
    }

    suspend fun deleteMetric(context: Context, metric: Metric) {
        if (metric.cloudId.isBlank()) return
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return
        val patientUid = resolvePatientUid(context, metric.profileId) ?: return
        runCatching {
            db.collection("patients").document(patientUid)
                .collection("metrics").document(metric.cloudId).delete().awaitResult()
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

    private suspend fun push(
        coll: com.google.firebase.firestore.CollectionReference,
        app: Context,
        profileId: Long
    ) {
        val metricDao = AppDatabase.get(app).metricDao()
        for (m in metricDao.getAllOnce(profileId)) {
            val cloudId = m.cloudId.ifBlank { coll.document().id }
            if (m.cloudId.isBlank()) metricDao.update(m.copy(cloudId = cloudId))
            runCatching {
                coll.document(cloudId).set(
                    mapOf(
                        "type" to m.type,
                        "value" to m.value.toDouble(),
                        "value2" to m.value2.toDouble(),
                        "context" to m.context,
                        "recordedAt" to m.recordedAt,
                        "loggedBy" to m.loggedBy
                    )
                ).awaitResult()
            }
        }
    }
}
