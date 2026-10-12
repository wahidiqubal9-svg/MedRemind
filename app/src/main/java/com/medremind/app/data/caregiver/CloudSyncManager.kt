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
import com.medremind.app.data.DoseStatus
import com.medremind.app.data.Medicine
import com.medremind.app.data.Metric
import com.medremind.app.data.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps caregiver links, medicines, health readings and dose confirmations in
 * step with the cloud (both directions), and turns remote reminders into alarms
 * on the patient's device.
 */
object CloudSyncManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = mutableListOf<ListenerRegistration>()
    private val childListeners = mutableMapOf<String, ListenerRegistration>()
    private var started = false
    private var currentUid: String? = null

    /** Reminders we've already written back, keyed "docId:status" to avoid repeats. */
    private val handledReminders = mutableSetOf<String>()

    private const val REMINDER_EXPIRY_MS = 30 * 60 * 1000L
    private const val TAKEN_RECENT_MS = 3 * 60 * 60 * 1000L

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
        childListeners.clear()
        handledReminders.clear()
    }

    private fun listen(app: Context, db: FirebaseFirestore, uid: String) {
        val dao = AppDatabase.get(app).caregiverDao()

        // ---- I am the patient: links addressed to me -------------------------
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
                            dao.updateLink(existing.copy(status = status, permissions = permissions, caregiverName = caregiverName))
                        }
                    }
                    val present = snap.documents.map { it.id }.toSet()
                    dao.linksOnce()
                        .filter { it.direction == CaregiverDirection.INCOMING && it.cloudId.isNotBlank() && it.cloudId !in present }
                        .forEach { dao.deleteLink(it.id) }
                }
            }

        // ---- I am the caregiver: apply the patient's choices -----------------
        listeners += db.collection("links").whereEqualTo("caregiverUid", uid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                scope.launch {
                    for (doc in snap.documents) {
                        var existing = dao.linkByCloudId(doc.id)
                        if (existing == null) {
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
                        val permissions = (doc.getLong("permissions") ?: link.permissions.toLong()).toInt()
                        if (link.permissions != permissions || link.status != status) {
                            dao.updateLink(link.copy(status = status, permissions = permissions))
                        }
                    }
                    val present = snap.documents.map { it.id }.toSet()
                    dao.linksOnce()
                        .filter { it.direction == CaregiverDirection.OUTGOING && it.cloudId.isNotBlank() && it.cloudId !in present }
                        .forEach {
                            val profileId = it.patientProfileId
                            dao.deleteLink(it.id)
                            if (profileId != 0L) {
                                val adb = AppDatabase.get(app)
                                adb.medicineDao().getAllOnce(profileId).forEach { m -> adb.medicineDao().delete(m) }
                                dao.patient(profileId)?.let { p -> dao.deletePatient(p) }
                            }
                        }
                    ensureChildListeners(app, db)
                }
            }

        // ---- I am the patient: pending reminders -----------------------------
        listeners += db.collection("reminders")
            .whereEqualTo("patientUid", uid)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                scope.launch {
                    for (doc in snap.documents) handleReminder(app, doc)
                }
            }

        // ---- I am the patient: mirror my caregiver's data --------------------
        listeners += db.collection("patients").document(uid).collection("medicines")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                scope.launch { mirrorMedicines(app, 0L, snap.documents) }
            }
        listeners += db.collection("patients").document(uid).collection("metrics")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                scope.launch { mirrorMetrics(app, 0L, snap.documents) }
            }
        listeners += db.collection("patients").document(uid).collection("doseEvents")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                scope.launch { mirrorDoses(app, 0L, snap.documents) }
            }

        ensureChildListeners(app, db)
    }

    /** For every linked patient, mirror their cloud data into their local profile. */
    private fun ensureChildListeners(app: Context, db: FirebaseFirestore) {
        val dao = AppDatabase.get(app).caregiverDao()
        scope.launch {
            val links = dao.linksOnce().filter {
                it.direction == CaregiverDirection.OUTGOING &&
                    it.cloudId.isNotBlank() && it.peerUid.isNotBlank() && it.patientProfileId != 0L
            }
            for (link in links) {
                val peer = link.peerUid
                val profileId = link.patientProfileId
                sub("med:$peer", db, peer, profileId, "medicines", app, ::mirrorMedicines)
                sub("met:$peer", db, peer, profileId, "metrics", app, ::mirrorMetrics)
                sub("dose:$peer", db, peer, profileId, "doseEvents", app, ::mirrorDoses)
            }
        }
    }

    private fun sub(
        key: String,
        db: FirebaseFirestore,
        peer: String,
        profileId: Long,
        collection: String,
        app: Context,
        mirror: suspend (Context, Long, List<DocumentSnapshot>) -> Unit
    ) {
        if (childListeners[key] != null) return
        val reg = db.collection("patients").document(peer).collection(collection)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                scope.launch { mirror(app, profileId, snap.documents) }
            }
        childListeners[key] = reg
        listeners += reg
    }

    // ---------------------------------------------------------------- reminders

    private suspend fun handleReminder(app: Context, doc: DocumentSnapshot) {
        val sentAt = doc.getLong("sentAtMillis") ?: 0L
        val now = System.currentTimeMillis()
        val key = doc.id
        if (sentAt > 0 && now - sentAt > REMINDER_EXPIRY_MS) {
            if (handledReminders.add("$key:EXPIRED")) {
                runCatching { doc.reference.update("status", "EXPIRED") }
            }
            return
        }
        val medicineName = doc.getString("medicineName") ?: return
        val caregiverName = doc.getString("caregiverName") ?: "Caregiver"
        val caregiverUid = doc.getString("caregiverUid") ?: ""
        val dao = AppDatabase.get(app).caregiverDao()
        val link = dao.incomingLinkByPeer(caregiverUid)
        val allowed = link == null ||
            CaregiverPermission.has(link.permissions, CaregiverPermission.SEND_REMINDERS)
        if (!allowed) {
            runCatching { doc.reference.update("status", "BLOCKED") }
            return
        }
        if (recentlyTaken(app, medicineName)) {
            if (handledReminders.add("$key:ALREADY_TAKEN")) {
                runCatching {
                    doc.reference.update(mapOf("status" to "ALREADY_TAKEN", "resolvedAt" to now))
                }
            }
        } else {
            runCatching {
                CaregiverProvider.reminderService(app)
                    .sendReminderByName(0L, medicineName, caregiverName)
            }
            runCatching {
                doc.reference.update(mapOf("status" to "DELIVERED", "deliveredAt" to now))
            }
        }
    }

    private suspend fun recentlyTaken(app: Context, medicineName: String): Boolean {
        val db = AppDatabase.get(app)
        val medicine = db.medicineDao().getAllOnce(0L)
            .firstOrNull { it.name.equals(medicineName.trim(), ignoreCase = true) } ?: return false
        val cutoff = System.currentTimeMillis() - TAKEN_RECENT_MS
        return db.doseEventDao().between(cutoff, System.currentTimeMillis() + 86_400_000L)
            .any { it.medicineId == medicine.id && it.status == DoseStatus.TAKEN && (it.actedAt ?: 0) >= cutoff }
    }

    // ----------------------------------------------------------------- mirrors

    private suspend fun mirrorMedicines(app: Context, profileId: Long, docs: List<DocumentSnapshot>) {
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
                ?: medicineDao.getAllOnce(profileId).firstOrNull {
                    it.cloudId.isBlank() && it.name.equals(name, ignoreCase = true)
                }
            val medicine = if (existing == null) {
                val id = medicineDao.insert(
                    Medicine(
                        name = name, strength = strength, form = form,
                        intakeInstruction = intake, addedBy = addedBy,
                        profileId = profileId, cloudId = doc.id
                    )
                )
                medicineDao.byId(id) ?: continue
            } else {
                val updated = existing.copy(
                    name = name, strength = strength, form = form,
                    intakeInstruction = intake, addedBy = addedBy, cloudId = doc.id
                )
                medicineDao.update(updated)
                updated
            }

            // Safety net: drop leftover duplicates of the same medicine.
            medicineDao.getAllOnce(profileId)
                .filter { it.id != medicine.id && it.name.equals(name, ignoreCase = true) }
                .forEach { dup ->
                    scheduleDao.forMedicine(dup.id).forEach { ReminderScheduler.cancel(app, it.id) }
                    medicineDao.delete(dup)
                }

            val photoBase64 = doc.getString("photoBase64")
            if (!photoBase64.isNullOrBlank() && medicine.photoPath.isNullOrBlank()) {
                val newPath = com.medremind.app.data.PhotoStorage.saveBase64(app, photoBase64)
                if (newPath != null) medicineDao.update(medicine.copy(photoPath = newPath))
            }

            if (times.isNotBlank()) {
                val schedule = scheduleDao.forMedicine(medicine.id).firstOrNull()
                val saved = if (schedule == null) {
                    // Anchor to arrival time so past times today aren't counted.
                    val created = Schedule(
                        medicineId = medicine.id, type = scheduleType, times = times,
                        daysMask = daysMask, doseLabel = doseLabel,
                        startDate = System.currentTimeMillis()
                    )
                    created.copy(id = scheduleDao.insert(created))
                } else {
                    val updated = schedule.copy(
                        type = scheduleType, times = times, daysMask = daysMask, doseLabel = doseLabel
                    )
                    scheduleDao.update(updated)
                    updated
                }
                // Only the device owner's medicines ring on this device.
                if (profileId == 0L) ReminderScheduler.schedule(app, saved)
                else ReminderScheduler.cancel(app, saved.id)
            }
        }

        medicineDao.getAllOnce(profileId)
            .filter { it.cloudId.isNotBlank() && it.cloudId !in present }
            .forEach { stale ->
                scheduleDao.forMedicine(stale.id).forEach { ReminderScheduler.cancel(app, it.id) }
                medicineDao.delete(stale)
            }
    }

    private suspend fun mirrorMetrics(app: Context, profileId: Long, docs: List<DocumentSnapshot>) {
        val metricDao = AppDatabase.get(app).metricDao()
        val present = docs.map { it.id }.toSet()
        for (doc in docs) {
            val type = doc.getString("type") ?: continue
            val value = (doc.getDouble("value") ?: 0.0).toFloat()
            val value2 = (doc.getDouble("value2") ?: 0.0).toFloat()
            val ctx = doc.getString("context") ?: ""
            val recordedAt = doc.getLong("recordedAt") ?: System.currentTimeMillis()
            val loggedBy = doc.getString("loggedBy") ?: ""
            val existing = metricDao.byCloudId(doc.id)
                ?: metricDao.getAllOnce(profileId).firstOrNull {
                    it.cloudId.isBlank() && it.type == type &&
                        kotlin.math.abs(it.recordedAt - recordedAt) < 60_000L
                }
            if (existing == null) {
                metricDao.insert(
                    Metric(
                        type = type, value = value, value2 = value2, context = ctx,
                        recordedAt = recordedAt, profileId = profileId, loggedBy = loggedBy,
                        cloudId = doc.id
                    )
                )
            } else {
                metricDao.update(
                    existing.copy(
                        type = type, value = value, value2 = value2, context = ctx,
                        recordedAt = recordedAt, loggedBy = loggedBy, cloudId = doc.id
                    )
                )
            }
        }
        metricDao.getAllOnce(profileId)
            .filter { it.cloudId.isNotBlank() && it.cloudId !in present }
            .forEach { metricDao.delete(it) }
    }

    private suspend fun mirrorDoses(app: Context, profileId: Long, docs: List<DocumentSnapshot>) {
        val db = AppDatabase.get(app)
        val eventDao = db.doseEventDao()
        val medicineDao = db.medicineDao()
        val medicines = medicineDao.getAllOnce(profileId)
        for (doc in docs) {
            val name = doc.getString("medicineName") ?: continue
            val medicineCloudId = doc.getString("medicineCloudId") ?: ""
            val scheduledAt = doc.getLong("scheduledAt") ?: System.currentTimeMillis()
            val status = doc.getString("status") ?: DoseStatus.PENDING
            val actedAt = doc.getLong("actedAt")
            val source = doc.getString("source") ?: "SCHEDULED"
            val actorName = doc.getString("actorName") ?: ""
            // Map to the local medicine (by cloud id, then by name).
            val medicine = medicines.firstOrNull { it.cloudId.isNotBlank() && it.cloudId == medicineCloudId }
                ?: medicines.firstOrNull { it.name.equals(name, ignoreCase = true) }
            val medicineId = medicine?.id ?: 0L
            val existing = eventDao.byCloudId(doc.id)
            if (existing == null) {
                eventDao.insert(
                    com.medremind.app.data.DoseEvent(
                        scheduleId = 0L, medicineId = medicineId, scheduledAt = scheduledAt,
                        status = status, actedAt = actedAt, source = source,
                        actorName = actorName, cloudId = doc.id
                    )
                )
            } else {
                eventDao.update(
                    existing.copy(
                        medicineId = if (medicineId != 0L) medicineId else existing.medicineId,
                        scheduledAt = scheduledAt, status = status, actedAt = actedAt,
                        source = source, actorName = actorName
                    )
                )
            }
        }
        // (Dose events are updated in place; we don't delete synced ones.)
    }
}
