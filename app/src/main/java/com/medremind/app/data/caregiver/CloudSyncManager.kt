package com.medremind.app.data.caregiver

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.medremind.app.alarm.ReminderScheduler
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.CaregiverDirection
import com.medremind.app.data.CaregiverLink
import com.medremind.app.data.CaregiverPermission
import com.medremind.app.data.Medicine
import com.medremind.app.data.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps the local caregiver links in step with the cloud and delivers reminders
 * on the patient's device.
 *
 * - On the **patient** device: links addressed to me become local incoming links
 *   (so I can see my caregiver and set permissions), and pending reminders fire
 *   the full-screen alarm on THIS phone.
 * - On the **caregiver** device: permission/status changes made by the patient
 *   are applied to my local links.
 */
object CloudSyncManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = mutableListOf<ListenerRegistration>()
    private var started = false
    private var currentUid: String? = null

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull() ?: return
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return
        auth.addAuthStateListener { fa ->
            val uid = fa.currentUser?.uid
            when {
                uid == null -> {
                    clearListeners()
                    currentUid = null
                }
                uid != currentUid -> {
                    clearListeners()
                    currentUid = uid
                    listen(app, db, uid)
                }
            }
        }
    }

    private fun clearListeners() {
        listeners.forEach { runCatching { it.remove() } }
        listeners.clear()
    }

    private fun listen(app: Context, db: FirebaseFirestore, uid: String) {
        val dao = AppDatabase.get(app).caregiverDao()

        // I am the patient: links addressed to me.
        listeners += db.collection("links").whereEqualTo("patientUid", uid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                scope.launch {
                    for (doc in snap.documents) {
                        val caregiverUid = doc.getString("caregiverUid") ?: continue
                        val caregiverName = doc.getString("caregiverName") ?: "Caregiver"
                        val status = doc.getString("status") ?: "PENDING"
                        val permissions = (doc.getLong("permissions")
                            ?: CaregiverPermission.DEFAULT_MEDICATION.toLong()).toInt()
                        val existing = dao.linkByCloudId(doc.id)
                        if (existing == null) {
                            dao.insertLink(
                                CaregiverLink(
                                    patientProfileId = 0L,
                                    caregiverName = caregiverName,
                                    direction = CaregiverDirection.INCOMING,
                                    status = status,
                                    permissions = permissions,
                                    cloudId = doc.id,
                                    peerUid = caregiverUid
                                )
                            )
                        } else if (existing.permissions != permissions || existing.status != status) {
                            dao.updateLink(
                                existing.copy(
                                    status = status,
                                    permissions = permissions,
                                    caregiverName = caregiverName
                                )
                            )
                        }
                    }
                }
            }

        // I am the caregiver: apply the permissions/status the patient set.
        listeners += db.collection("links").whereEqualTo("caregiverUid", uid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                scope.launch {
                    for (doc in snap.documents) {
                        val existing = dao.linkByCloudId(doc.id) ?: continue
                        val status = doc.getString("status") ?: existing.status
                        val permissions = (doc.getLong("permissions")
                            ?: existing.permissions.toLong()).toInt()
                        if (existing.permissions != permissions || existing.status != status) {
                            dao.updateLink(existing.copy(status = status, permissions = permissions))
                        }
                    }
                }
            }

        // I am the patient: pending reminders fire the alarm on THIS device.
        listeners += db.collection("reminders")
            .whereEqualTo("patientUid", uid)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                scope.launch {
                    for (doc in snap.documents) {
                        val caregiverUid = doc.getString("caregiverUid") ?: ""
                        val medicineName = doc.getString("medicineName") ?: continue
                        val caregiverName = doc.getString("caregiverName") ?: "Caregiver"
                        val link = dao.incomingLinkByPeer(caregiverUid)
                        val allowed = link == null ||
                            CaregiverPermission.has(link.permissions, CaregiverPermission.SEND_REMINDERS)
                        if (allowed) {
                            runCatching {
                                CaregiverProvider.reminderService(app)
                                    .sendReminderByName(0L, medicineName, caregiverName)
                            }
                        }
                        runCatching { doc.reference.update("status", "HANDLED") }
                    }
                }
            }

        // I am the patient: mirror the medicines the caregiver set up for me.
        listeners += db.collection("patients").document(uid).collection("medicines")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                scope.launch { upsertMedicines(app, snap.documents) }
            }
    }

    /** Creates/updates local medicines (profile 0) from the cloud documents. */
    private suspend fun upsertMedicines(app: Context, docs: List<DocumentSnapshot>) {
        val db = AppDatabase.get(app)
        val medicineDao = db.medicineDao()
        val scheduleDao = db.scheduleDao()
        for (doc in docs) {
            val name = doc.getString("name") ?: continue
            val strength = doc.getString("strength") ?: ""
            val form = doc.getString("form") ?: "tablet"
            val intake = doc.getString("intake") ?: ""
            val addedBy = doc.getString("addedBy") ?: ""
            val times = doc.getString("times") ?: ""
            val scheduleType = doc.getString("scheduleType") ?: "DAILY"
            val daysMask = (doc.getLong("daysMask") ?: 0L).toInt()
            val doseLabel = doc.getString("doseLabel") ?: ""

            val existing = medicineDao.byCloudId(doc.id)
            val medicine = if (existing == null) {
                val id = medicineDao.insert(
                    Medicine(
                        name = name,
                        strength = strength,
                        form = form,
                        intakeInstruction = intake,
                        addedBy = addedBy,
                        profileId = 0L,
                        cloudId = doc.id
                    )
                )
                medicineDao.byId(id) ?: continue
            } else {
                val updated = existing.copy(
                    name = name,
                    strength = strength,
                    form = form,
                    intakeInstruction = intake,
                    addedBy = addedBy
                )
                medicineDao.update(updated)
                updated
            }

            if (times.isNotBlank()) {
                val schedule = scheduleDao.forMedicine(medicine.id).firstOrNull()
                if (schedule == null) {
                    val sid = scheduleDao.insert(
                        Schedule(
                            medicineId = medicine.id,
                            type = scheduleType,
                            times = times,
                            daysMask = daysMask,
                            doseLabel = doseLabel
                        )
                    )
                    ReminderScheduler.schedule(
                        app,
                        Schedule(
                            id = sid,
                            medicineId = medicine.id,
                            type = scheduleType,
                            times = times,
                            daysMask = daysMask,
                            doseLabel = doseLabel
                        )
                    )
                } else {
                    val updated = schedule.copy(
                        type = scheduleType,
                        times = times,
                        daysMask = daysMask,
                        doseLabel = doseLabel
                    )
                    scheduleDao.update(updated)
                    ReminderScheduler.schedule(app, updated)
                }
            }
        }
    }
}
