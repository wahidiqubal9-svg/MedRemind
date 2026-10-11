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
 * Keeps caregiver links and shared medicines in step with the cloud.
 *
 * - Patient device: links addressed to me become local incoming links (so I see
 *   my caregiver and set permissions); reminders fire the alarm HERE; medicines
 *   the caregiver set up are mirrored into my own list.
 * - Caregiver device: permission/status changes come back from the patient; the
 *   patient's medicine set is mirrored into their local profile.
 *
 * Mirrors never write back to the cloud, so there are no echo loops: only the
 * user's own edits (via the app) push to Firestore.
 */
object CloudSyncManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = mutableListOf<ListenerRegistration>()
    private val medicineListeners = mutableMapOf<String, ListenerRegistration>()
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
        medicineListeners.clear()
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
                            // Only add one link per caregiver (avoid duplicates).
                            val dup = dao.incomingLinkByPeer(caregiverUid)
                            if (dup == null) {
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
                            } else {
                                dao.updateLink(dup.copy(cloudId = doc.id, status = status, permissions = permissions))
                            }
                        } else {
                            dao.updateLink(
                                existing.copy(
                                    status = status,
                                    permissions = permissions,
                                    caregiverName = caregiverName
                                )
                            )
                        }
                    }
                    val present = snap.documents.map { it.id }.toSet()
                    dao.linksOnce()
                        .filter {
                            it.direction == CaregiverDirection.INCOMING &&
                                it.cloudId.isNotBlank() && it.cloudId !in present
                        }
                        .forEach { dao.deleteLink(it.id) }
                }
            }

        // I am the caregiver: apply what the patient set, mirror their medicines.
        listeners += db.collection("links").whereEqualTo("caregiverUid", uid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                scope.launch {
                    for (doc in snap.documents) {
                        var existing = dao.linkByCloudId(doc.id)
                        if (existing == null) {
                            // Heal an older link that predates cloud ids: attach it to
                            // this cloud link so existing connections keep working
                            // across updates without a manual reconnect.
                            val orphans = dao.linksOnce().filter {
                                it.direction == CaregiverDirection.OUTGOING && it.cloudId.isBlank()
                            }
                            if (orphans.size == 1) {
                                existing = orphans.first().copy(
                                    cloudId = doc.id,
                                    peerUid = doc.getString("patientUid") ?: ""
                                )
                                dao.updateLink(existing)
                            }
                        }
                        val link = existing ?: continue
                        val status = doc.getString("status") ?: link.status
                        val permissions = (doc.getLong("permissions")
                            ?: link.permissions.toLong()).toInt()
                        if (link.permissions != permissions || link.status != status) {
                            dao.updateLink(link.copy(status = status, permissions = permissions))
                        }
                    }
                    // Ensure a medicine mirror per patient (from local links).
                    ensureMedicineListeners(app, db)
                    val present = snap.documents.map { it.id }.toSet()
                    dao.linksOnce()
                        .filter {
                            it.direction == CaregiverDirection.OUTGOING &&
                                it.cloudId.isNotBlank() && it.cloudId !in present
                        }
                        .forEach {
                            val profileId = it.patientProfileId
                            dao.deleteLink(it.id)
                            if (profileId != 0L) {
                                val db2 = AppDatabase.get(app)
                                db2.medicineDao().getAllOnce(profileId)
                                    .forEach { m -> db2.medicineDao().delete(m) }
                                dao.patient(profileId)?.let { p -> dao.deletePatient(p) }
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
                scope.launch { mirrorMedicines(app, 0L, snap.documents) }
            }

        // I am the caregiver: make sure each linked patient is mirrored too.
        ensureMedicineListeners(app, db)
    }

    /** For every linked patient, mirror their cloud medicines into their profile. */
    private fun ensureMedicineListeners(app: Context, db: FirebaseFirestore) {
        val dao = AppDatabase.get(app).caregiverDao()
        scope.launch {
            val links = dao.linksOnce().filter {
                it.direction == CaregiverDirection.OUTGOING &&
                    it.cloudId.isNotBlank() &&
                    it.peerUid.isNotBlank() &&
                    it.patientProfileId != 0L
            }
            for (link in links) {
                val peer = link.peerUid
                val profileId = link.patientProfileId
                if (medicineListeners[peer] == null) {
                    val reg = db.collection("patients").document(peer)
                        .collection("medicines")
                        .addSnapshotListener { msnap, _ ->
                            if (msnap == null) return@addSnapshotListener
                            scope.launch { mirrorMedicines(app, profileId, msnap.documents) }
                        }
                    medicineListeners[peer] = reg
                    listeners += reg
                }
            }
        }
    }

    /** Creates/updates local medicines for [profileId] from cloud docs, and
     *  removes local ones whose cloud document disappeared. */
    private suspend fun mirrorMedicines(
        app: Context,
        profileId: Long,
        docs: List<DocumentSnapshot>
    ) {
        val db = AppDatabase.get(app)
        val medicineDao = db.medicineDao()
        val scheduleDao = db.scheduleDao()
        val present = docs.map { it.id }.toSet()

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
                        profileId = profileId,
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

        // Delete local cloud-linked medicines that no longer exist in the cloud.
        medicineDao.getAllOnce(profileId)
            .filter { it.cloudId.isNotBlank() && it.cloudId !in present }
            .forEach { stale ->
                scheduleDao.forMedicine(stale.id).forEach { ReminderScheduler.cancel(app, it.id) }
                medicineDao.delete(stale)
            }
    }
}
