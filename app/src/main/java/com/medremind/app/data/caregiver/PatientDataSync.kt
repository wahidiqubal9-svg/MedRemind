package com.medremind.app.data.caregiver

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.CaregiverDirection
import com.medremind.app.data.Medicine
import com.medremind.app.data.PhotoStorage
import com.medremind.app.data.remote.awaitResult
import java.io.File
import java.util.UUID

/** Pushes medicines to Firestore (id = globalId). The photo is uploaded to
 *  Storage only when it changes, so reminders reuse the cached local image. */
object PatientDataSync {

    suspend fun pushMedicines(context: Context, profileId: Long) {
        if (profileId == 0L) return
        val app = context.applicationContext
        val patientUid = resolvePatientUid(context, profileId) ?: return
        push(app, patientUid, profileId)
    }

    suspend fun pushMyMedicines(context: Context) {
        val app = context.applicationContext
        val patientUid = resolvePatientUid(context, 0L) ?: return
        push(app, patientUid, 0L)
    }

    suspend fun deleteMedicine(context: Context, medicine: Medicine) {
        val globalId = medicine.globalId.ifBlank { medicine.cloudId }
        if (globalId.isBlank()) return
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return
        val patientUid = resolvePatientUid(context, medicine.profileId) ?: return
        runCatching {
            db.collection("patients").document(patientUid).collection("medicines")
                .document(globalId).delete().awaitResult()
        }
        runCatching {
            FirebaseStorage.getInstance()
                .reference.child("patients/$patientUid/photos/$globalId.jpg").delete().awaitResult()
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

    private suspend fun push(app: Context, patientUid: String, profileId: Long) {
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return
        val medicineDao = AppDatabase.get(app).medicineDao()
        val scheduleDao = AppDatabase.get(app).scheduleDao()
        val coll = db.collection("patients").document(patientUid).collection("medicines")

        for (m0 in medicineDao.getAllOnce(profileId)) {
            var m = m0
            if (m.globalId.isBlank()) {
                m = m.copy(globalId = UUID.randomUUID().toString().replace("-", "").take(20))
                medicineDao.update(m)
            }
            val schedule = scheduleDao.forMedicine(m.id).firstOrNull()
            val hash = PhotoStorage.hash(m.photoPath)
            val shouldUpload = !m.photoPath.isNullOrBlank() && hash != null && hash != m.photoHash

            val data = mutableMapOf<String, Any?>(
                "name" to m.name,
                "strength" to m.strength,
                "form" to m.form,
                "intake" to m.intakeInstruction,
                "addedBy" to m.addedBy,
                "doseLabel" to (schedule?.doseLabel ?: ""),
                "times" to (schedule?.times ?: ""),
                "scheduleType" to (schedule?.type ?: "DAILY"),
                "daysMask" to (schedule?.daysMask ?: 0),
                "version" to m.version,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (shouldUpload) {
                val url = uploadPhoto(patientUid, m.globalId, m.photoPath!!)
                if (url != null) {
                    data["photoUrl"] = url
                    data["photoHash"] = hash
                }
            }
            if (m.photoPath.isNullOrBlank()) {
                // Photo removed.
                data["photoUrl"] = null
                data["photoHash"] = null
            }

            runCatching { coll.document(m.globalId).set(data).awaitResult() }

            val newPhotoHash = if (shouldUpload && hash != null) hash else m.photoHash
            if (m.photoHash != newPhotoHash || m.cloudId != m.globalId) {
                medicineDao.update(m.copy(photoHash = newPhotoHash, cloudId = m.globalId))
            }
        }
    }

    private suspend fun uploadPhoto(patientUid: String, globalId: String, path: String): String? =
        runCatching {
            val ref = FirebaseStorage.getInstance()
                .reference.child("patients/$patientUid/photos/$globalId.jpg")
            ref.putFile(Uri.fromFile(File(path))).awaitResult()
            ref.downloadUrl.awaitResult().toString()
        }.getOrNull()
}
