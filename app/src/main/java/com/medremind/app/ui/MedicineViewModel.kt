package com.medremind.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medremind.app.alarm.AlarmNotifier
import com.medremind.app.alarm.ReminderScheduler
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.ArchivedDose
import com.medremind.app.data.Medicine
import com.medremind.app.data.Metric
import com.medremind.app.data.DoseEvent
import com.medremind.app.data.DoseSource
import com.medremind.app.data.DoseStatus
import com.medremind.app.data.PhotoStorage
import com.medremind.app.data.Schedule
import com.medremind.app.data.ScheduleType
import com.medremind.app.widget.NextDoseWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

class MedicineViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val db = AppDatabase.get(application)
    private val prefs = application.getSharedPreferences("medremind_settings", Context.MODE_PRIVATE)

    /** Which patient profile the main tabs show. 0 = the device owner. */
    private val activeProfile = MutableStateFlow(prefs.getLong("active_profile_id", 0L))
    val activeProfileId: StateFlow<Long> = activeProfile

    private val activeProfileNameState =
        MutableStateFlow(prefs.getString("active_profile_name", "") ?: "")
    val activeProfileName: StateFlow<String> = activeProfileNameState

    fun setActiveProfile(profileId: Long, name: String = "") {
        activeProfile.value = profileId
        activeProfileNameState.value = if (profileId == 0L) "" else name
        prefs.edit()
            .putLong("active_profile_id", profileId)
            .putString("active_profile_name", if (profileId == 0L) "" else name)
            .apply()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val medicinesFlow = activeProfile.flatMapLatest { db.medicineDao().observeAll(it) }

    val medicines: StateFlow<List<Medicine>> = medicinesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val loaded: StateFlow<Boolean> = medicinesFlow
        .map { true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val schedulesByMedicine: StateFlow<Map<Long, List<Schedule>>> = db.scheduleDao().observeAll()
        .map { list -> list.groupBy { it.medicineId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Medicines whose schedule is "as needed" rather than fixed times. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val prnMedicines: StateFlow<List<Medicine>> = combine(
        activeProfile.flatMapLatest { db.medicineDao().observeAll(it) },
        db.scheduleDao().observeAll()
    ) { medicines, schedules ->
        val ids = schedules.filter { it.type == ScheduleType.AS_NEEDED }
            .map { it.medicineId }.toSet()
        medicines.filter { it.id in ids && it.pausedAt == null }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val metrics: StateFlow<List<Metric>> = activeProfile
        .flatMapLatest { db.metricDao().observeAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addMetric(
        type: String,
        value: Float,
        value2: Float = 0f,
        context: String = com.medremind.app.data.MetricContext.NONE,
        profileId: Long = activeProfile.value,
        actor: String = "",
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.metricDao().insert(
                    Metric(
                        type = type,
                        value = value,
                        value2 = value2,
                        context = context,
                        profileId = profileId,
                        loggedBy = actor
                    )
                )
                if (profileId != 0L) {
                    com.medremind.app.data.caregiver.HealthDataSync.pushMetrics(app, profileId)
                } else {
                    com.medremind.app.data.caregiver.HealthDataSync.pushMyMetrics(app)
                }
            }
            onDone()
        }
    }

    /** Adds [amount] pills to the medicine's current stock (refill). */
    fun refillStock(medicine: Medicine, amount: Int, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (amount <= 0) return@withContext
                val current = db.medicineDao().byId(medicine.id) ?: return@withContext
                db.medicineDao().update(current.copy(quantity = current.quantity + amount))
            }
            refreshWidget()
            onDone()
        }
    }

    fun deleteMetric(metric: Metric, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.metricDao().delete(metric)
                com.medremind.app.data.caregiver.HealthDataSync.deleteMetric(app, metric)
            }
            onDone()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<List<DoseHistoryItem>> = combine(
        db.doseEventDao().observeAll(),
        activeProfile.flatMapLatest { db.medicineDao().observeAll(it) }
    ) { events, medicines ->
        val byId = medicines.associateBy { it.id }
        events
            .filter { it.scheduleId != 0L }
            .map { event ->
                val medicine = byId[event.medicineId]
                DoseHistoryItem(
                    event = event,
                    medicineName = medicine?.name ?: "Medicine",
                    photoPath = medicine?.photoPath
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markOverdueAsMissed() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.doseEventDao().markMissedBefore(System.currentTimeMillis() - MISSED_AFTER_MILLIS)
            }
        }
    }

    suspend fun schedulesFor(medicineId: Long): List<Schedule> =
        withContext(Dispatchers.IO) { db.scheduleDao().forMedicine(medicineId) }

    /** An existing medicine with the same name in this profile (for a duplicate hint). */
    suspend fun findDuplicate(profileId: Long, name: String, excludeId: Long): Medicine? =
        withContext(Dispatchers.IO) {
            if (name.isBlank()) return@withContext null
            db.medicineDao().getAllOnce(profileId).firstOrNull {
                it.id != excludeId && it.name.equals(name.trim(), ignoreCase = true)
            }
        }

    suspend fun doseLogForRange(
        days: Int,
        profileId: Long = activeProfile.value
    ): List<DoseLogEntry> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val start = today.minusDays((days - 1).toLong())
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val schedules = db.scheduleDao().getAllOnce().filter { it.enabled }
        val medicinesById = db.medicineDao().getAllOnce(profileId).associateBy { it.id }
        // scheduleId == 0 marks the "test alarm" - exclude it.
        val events = db.doseEventDao().between(startMillis, endMillis).filter { it.scheduleId != 0L }
        val now = System.currentTimeMillis()
        val result = mutableListOf<DoseLogEntry>()
        (0 until days).forEach { offset ->
            val date = start.plusDays(offset.toLong())
            schedules.forEach { schedule ->
                val medicine = medicinesById[schedule.medicineId] ?: return@forEach
                val createdDate = Instant.ofEpochMilli(medicine.createdAt).atZone(zone).toLocalDate()
                if (date.isBefore(createdDate)) return@forEach
                val startRef = doseStartRef(medicine.createdAt, schedule.startDate)
                val pausedAt = medicine.pausedAt
                ReminderScheduler.occurrencesOn(schedule, date).forEach { trigger ->
                    if (pausedAt != null && trigger >= pausedAt) return@forEach
                    val future = date.isAfter(today)
                    val event = if (future) null else events.firstOrNull {
                        it.medicineId == schedule.medicineId && abs(it.scheduledAt - trigger) < 90_000L
                    }
                    if (trigger < startRef && event == null) return@forEach
                    val status = if (future) {
                        FUTURE_STATUS
                    } else {
                        when {
                            event != null -> event.status
                            trigger >= now -> DoseStatus.PENDING
                            else -> DoseStatus.MISSED
                        }
                    }
                    result.add(
                        DoseLogEntry(
                            medicineId = medicine.id,
                            medicineName = medicine.name,
                            photoPath = medicine.photoPath,
                            scheduledAt = trigger,
                            status = status
                        )
                    )
                }
            }
        }
        // Doses kept from medicines that were deleted with "keep history".
        db.archivedDoseDao().between(profileId, startMillis, endMillis).forEach { a ->
            result.add(
                DoseLogEntry(
                    medicineId = 0L,
                    medicineName = a.medicineName,
                    photoPath = a.photoPath,
                    scheduledAt = a.scheduledAt,
                    status = a.status
                )
            )
        }
        result.sortedByDescending { it.scheduledAt }
    }

    /** History for one specific medicine (used by the per-medicine page). */
    suspend fun doseLogForMedicine(
        medicineId: Long,
        days: Int,
        profileId: Long = activeProfile.value
    ): List<DoseLogEntry> = doseLogForRange(days, profileId)
        .filter { it.medicineId == medicineId }

    suspend fun datesWithDoses(from: LocalDate, to: LocalDate): Set<LocalDate> =
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val schedules = db.scheduleDao().getAllOnce().filter { it.enabled }
            if (schedules.isEmpty()) return@withContext emptySet()
            val medicinesById = db.medicineDao().getAllOnce(activeProfile.value).associateBy { it.id }
            val rangeStart = from.atStartOfDay(zone).toInstant().toEpochMilli()
            val rangeEnd = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val eventsOnDay = db.doseEventDao().between(rangeStart, rangeEnd)
                .filter { it.scheduleId != 0L }
                .map { it.medicineId to Instant.ofEpochMilli(it.scheduledAt).atZone(zone).toLocalDate() }
                .toSet()
            val result = mutableSetOf<LocalDate>()
            var day = from
            while (!day.isAfter(to)) {
                val hasDose = schedules.any { schedule ->
                    val medicine = medicinesById[schedule.medicineId] ?: return@any false
                    val createdDate = Instant.ofEpochMilli(medicine.createdAt).atZone(zone).toLocalDate()
                    if (day.isBefore(createdDate)) return@any false
                    val startRef = doseStartRef(medicine.createdAt, schedule.startDate)
                    val pausedAt = medicine.pausedAt
                    ReminderScheduler.occurrencesOn(schedule, day).any {
                        it >= startRef && (pausedAt == null || it < pausedAt)
                    } || (schedule.medicineId to day) in eventsOnDay
                }
                if (hasDose) result.add(day)
                day = day.plusDays(1)
            }
            result
        }

    fun saveMedicine(
        medicine: Medicine,
        schedules: List<Schedule>,
        profileId: Long = activeProfile.value,
        actor: String = "",
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // A blank actor means the owner is saving it themselves; a named
                // actor marks a caregiver's change so we can show "Added by ...".
                val withActor = if (actor.isNotBlank()) medicine.copy(addedBy = actor) else medicine
                val ready = withActor.copy(
                    globalId = withActor.globalId.ifBlank {
                        java.util.UUID.randomUUID().toString().replace("-", "").take(20)
                    },
                    version = if (withActor.id == 0L) 1L else withActor.version + 1L
                )
                val medicineId = if (ready.id == 0L) {
                    db.medicineDao().insert(ready.copy(profileId = profileId))
                } else {
                    db.medicineDao().update(ready)
                    ready.id
                }

                val existing = db.scheduleDao().forMedicine(medicineId)
                val keptIds = schedules.filter { it.id != 0L }.map { it.id }.toSet()
                existing.filter { it.id !in keptIds }.forEach {
                    ReminderScheduler.cancel(app, it.id)
                    db.scheduleDao().delete(it)
                }

                schedules.forEach { schedule ->
                    val normalized = if (schedule.type == ScheduleType.INTERVAL && schedule.startDate == 0L) {
                        schedule.copy(startDate = System.currentTimeMillis())
                    } else schedule
                    val toSave = normalized.copy(medicineId = medicineId)
                    val savedId = if (toSave.id == 0L) {
                        db.scheduleDao().insert(toSave)
                    } else {
                        db.scheduleDao().update(toSave)
                        toSave.id
                    }
                    // Only the device owner's medicines ring on this device; a
                    // patient's medicines ring on the patient's own phone.
                    if (profileId == 0L) {
                        ReminderScheduler.schedule(app, toSave.copy(id = savedId))
                    } else {
                        ReminderScheduler.cancel(app, savedId)
                    }
                }
                // Sync medicines across the caregiver link (both directions).
                if (profileId != 0L) {
                    com.medremind.app.data.caregiver.PatientDataSync.pushMedicines(app, profileId)
                } else {
                    com.medremind.app.data.caregiver.PatientDataSync.pushMyMedicines(app)
                }
            }
            refreshWidget()
            onDone()
        }
    }

    fun duplicateMedicine(medicine: Medicine, schedules: List<Schedule>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val copy = medicine.copy(
                    id = 0L,
                    name = medicine.name + " (copy)",
                    createdAt = System.currentTimeMillis()
                )
                val newId = db.medicineDao().insert(copy)
                schedules.forEach { schedule ->
                    val newSchedule = schedule.copy(id = 0L, medicineId = newId)
                    val scheduleId = db.scheduleDao().insert(newSchedule)
                    ReminderScheduler.schedule(app, newSchedule.copy(id = scheduleId))
                }
            }
            refreshWidget()
            onDone()
        }
    }

    /** Pauses/resumes reminders and future doses without losing past history. */
    fun pauseMedicine(medicine: Medicine, paused: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.medicineDao().update(
                    medicine.copy(
                        pausedAt = if (paused) System.currentTimeMillis() else null
                    )
                )
                val schedules = db.scheduleDao().forMedicine(medicine.id)
                if (paused || medicine.profileId != 0L) {
                    schedules.forEach { ReminderScheduler.cancel(app, it.id) }
                } else {
                    schedules.forEach { ReminderScheduler.schedule(app, it) }
                }
                if (paused) {
                    // Never leave a pending dose for a time after the pause.
                    db.doseEventDao().deletePendingAfter(medicine.id, System.currentTimeMillis())
                }
            }
            refreshWidget()
            onDone()
        }
    }

    fun deleteMedicine(
        medicine: Medicine,
        keepLog: Boolean = false,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.scheduleDao().forMedicine(medicine.id).forEach {
                    ReminderScheduler.cancel(app, it.id)
                }
                if (keepLog) {
                    // Snapshot the past doses so Progress/exports keep this medicine's
                    // adherence history even though the medicine itself is removed.
                    val now = System.currentTimeMillis()
                    val days = (((now - medicine.createdAt) / 86_400_000L).toInt() + 1)
                        .coerceIn(1, 3650)
                    val kept = doseLogForRange(days, medicine.profileId)
                        .filter {
                            it.medicineId == medicine.id &&
                                (it.status == DoseStatus.TAKEN ||
                                    it.status == DoseStatus.SKIPPED ||
                                    it.status == DoseStatus.MISSED)
                        }
                        .map {
                            ArchivedDose(
                                profileId = medicine.profileId,
                                medicineName = it.medicineName,
                                photoPath = it.photoPath,
                                scheduledAt = it.scheduledAt,
                                status = it.status
                            )
                        }
                    if (kept.isNotEmpty()) db.archivedDoseDao().insertAll(kept)
                }
                db.doseEventDao().deleteForMedicine(medicine.id)
                db.medicineDao().delete(medicine)
                if (!keepLog) PhotoStorage.delete(medicine.photoPath)
                // Propagate the deletion so the other phone drops it too.
                com.medremind.app.data.caregiver.PatientDataSync.deleteMedicine(app, medicine)
            }
            refreshWidget()
            onDone()
        }
    }

    /** Erases every medicine, schedule, dose, health reading and caregiver link. */
    fun wipeAllData(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.scheduleDao().getAllOnce().forEach { ReminderScheduler.cancel(app, it.id) }
                db.medicineDao().getAllOnce().forEach { PhotoStorage.delete(it.photoPath) }
                db.doseEventDao().clear()
                db.scheduleDao().clear()
                db.medicineDao().clear()
                db.metricDao().clear()
                db.caregiverDao().clearActivity()
                db.caregiverDao().clearLinks()
                db.caregiverDao().clearPatients()
            }
            setActiveProfile(0L)
            refreshWidget()
            onDone()
        }
    }

    suspend fun upcomingAlarms(): List<UpcomingAlarm> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db.scheduleDao().getAllOnce()
            .filter { it.enabled }
            .mapNotNull { schedule ->
                val trigger = ReminderScheduler.nextTrigger(schedule, now) ?: return@mapNotNull null
                val medicine = db.medicineDao().byId(schedule.medicineId)
                if (medicine?.pausedAt != null) return@mapNotNull null
                UpcomingAlarm(medicine?.name ?: "Medicine", trigger)
            }
            .sortedBy { it.triggerAt }
    }

    suspend fun dosesOn(date: LocalDate): List<TodayDose> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = dayStart + 86_400_000L
        val schedules = db.scheduleDao().getAllOnce().filter { it.enabled }
        val medicinesById = db.medicineDao().getAllOnce(activeProfile.value).associateBy { it.id }
        val events = db.doseEventDao().between(dayStart, dayEnd)
        val now = System.currentTimeMillis()
        val result = mutableListOf<TodayDose>()
        schedules.forEach { schedule ->
            val medicine = medicinesById[schedule.medicineId] ?: return@forEach
            val createdDate = Instant.ofEpochMilli(medicine.createdAt).atZone(zone).toLocalDate()
            if (date.isBefore(createdDate)) return@forEach
            // A dose time that already passed before the medicine/schedule was set up
            // never existed, so it must not be counted (e.g. added 11am with an 8am time).
            val startRef = doseStartRef(medicine.createdAt, schedule.startDate)
            val pausedAt = medicine.pausedAt
            ReminderScheduler.occurrencesOn(schedule, date).forEach { trigger ->
                if (pausedAt != null && trigger >= pausedAt) return@forEach
                val event = events.firstOrNull {
                    it.medicineId == schedule.medicineId && abs(it.scheduledAt - trigger) < 90_000L
                }
                // Occurrences before the schedule was set up didn't exist yet, so
                // they're not "missed" - unless a dose was actually recorded for them.
                if (trigger < startRef && event == null) return@forEach
                val status = event?.status ?: if (trigger < now) DoseStatus.MISSED else DoseStatus.PENDING
                result.add(
                    TodayDose(
                        timeMillis = trigger,
                        medicine = medicine,
                        status = status,
                        schedule = schedule,
                        eventId = event?.id,
                        source = event?.source ?: DoseSource.SCHEDULED,
                        actorName = event?.actorName.orEmpty()
                    )
                )
            }
        }
        result.sortedBy { it.timeMillis }
    }

    fun markDose(dose: TodayDose, status: String, onDone: (Long?) -> Unit = {}) {
        viewModelScope.launch {
            var eventId: Long? = null
            withContext(Dispatchers.IO) {
                val dao = db.doseEventDao()
                val existingId = dose.eventId
                if (existingId != null) {
                    val event = dao.byId(existingId)
                    if (event != null) {
                        dao.update(
                            event.copy(
                                status = status,
                                actedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    eventId = existingId
                } else {
                    eventId = dao.insert(
                        DoseEvent(
                            scheduleId = dose.schedule.id,
                            medicineId = dose.medicine.id,
                            scheduledAt = dose.timeMillis,
                            status = status,
                            actedAt = System.currentTimeMillis()
                        )
                    )
                }
                if (status == DoseStatus.TAKEN) {
                    consumeStock(dose)
                    // If this dose came from a caregiver reminder, record it in the
                    // caregiver activity feed so the caregiver can see the confirmation.
                    eventId?.let { id ->
                        val event = dao.byId(id)
                        if (event != null && event.source == com.medremind.app.data.DoseSource.CAREGIVER) {
                            db.caregiverDao().insertActivity(
                                com.medremind.app.data.CaregiverActivity(
                                    patientProfileId = dose.medicine.profileId,
                                    actor = com.medremind.app.data.CaregiverActor.PATIENT,
                                    type = com.medremind.app.data.CaregiverActivityType.DOSE_CONFIRMED,
                                    medicineName = dose.medicine.name,
                                    message = "${dose.medicine.name} confirmed"
                                )
                            )
                        }
                    }
                }
                eventId?.let { com.medremind.app.data.caregiver.DoseDataSync.pushEvent(app, it) }
            }
            refreshWidget()
            onDone(eventId)
        }
    }

    /** Logs an as-needed dose as taken, right now. */
    fun logPrnDose(medicine: Medicine, onDone: (Long?) -> Unit = {}) {
        viewModelScope.launch {
            var eventId: Long? = null
            withContext(Dispatchers.IO) {
                val schedule = db.scheduleDao().forMedicine(medicine.id)
                    .firstOrNull { it.type == ScheduleType.AS_NEEDED } ?: return@withContext
                val now = System.currentTimeMillis()
                eventId = db.doseEventDao().insert(
                    DoseEvent(
                        scheduleId = schedule.id,
                        medicineId = medicine.id,
                        scheduledAt = now,
                        status = DoseStatus.TAKEN,
                        actedAt = now
                    )
                )
                eventId?.let { com.medremind.app.data.caregiver.DoseDataSync.pushEvent(app, it) }
            }
            refreshWidget()
            onDone(eventId)
        }
    }

    /** Decrements pill stock after a dose is taken and warns when supply runs low. */
    private suspend fun refreshWidget() {
        runCatching { NextDoseWidget().updateAll(app) }
    }

    private suspend fun consumeStock(dose: TodayDose) {
        val medicine = db.medicineDao().byId(dose.medicine.id) ?: return
        if (medicine.quantity <= 0) return
        val per = dose.schedule.doseLabel.trim().split(Regex("\\s+"))
            .firstOrNull()?.toFloatOrNull()?.toInt()?.coerceAtLeast(1) ?: 1
        val newQty = (medicine.quantity - per).coerceAtLeast(0)
        db.medicineDao().update(medicine.copy(quantity = newQty))
        val prefs = app.getSharedPreferences("medremind_settings", Context.MODE_PRIVATE)
        val key = "refill_notified_${medicine.id}"
        val last = prefs.getInt(key, -1)
        if (newQty <= medicine.refillThreshold && last != newQty) {
            prefs.edit().putInt(key, newQty).apply()
            AlarmNotifier.showRefill(app, medicine.name, newQty)
        }
    }

    /** Reverts a taken/skipped dose back to pending (used by the Undo snackbar). */
    fun undoDose(eventId: Long?, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (eventId != null) {
                    val dao = db.doseEventDao()
                    dao.byId(eventId)?.let { event ->
                        dao.update(event.copy(status = DoseStatus.PENDING, actedAt = null))
                    }
                    com.medremind.app.data.caregiver.DoseDataSync.pushEvent(app, eventId)
                }
            }
            onDone()
        }
    }

    fun triggerTestAlarm() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val medicine = db.medicineDao().first()
                val eventId = db.doseEventDao().insert(
                    DoseEvent(
                        scheduleId = 0L,
                        medicineId = medicine?.id ?: 0L,
                        scheduledAt = System.currentTimeMillis()
                    )
                )
                ReminderScheduler.scheduleSnooze(
                    app,
                    eventId,
                    System.currentTimeMillis() + 10_000L
                )
            }
        }
    }
}

data class UpcomingAlarm(val medicineName: String, val triggerAt: Long)

data class DoseHistoryItem(
    val event: DoseEvent,
    val medicineName: String,
    val photoPath: String?
)

data class DoseLogEntry(
    val medicineId: Long = 0L,
    val medicineName: String,
    val photoPath: String?,
    val scheduledAt: Long,
    val status: String
)

data class TodayDose(
    val timeMillis: Long,
    val medicine: Medicine,
    val status: String,
    val schedule: Schedule,
    val eventId: Long? = null,
    val source: String = DoseSource.SCHEDULED,
    val actorName: String = ""
)

/**
 * The moment a medicine/schedule started being tracked. Dose times that fall before
 * this point never really existed (e.g. a medicine added at 11:00 with an 08:00 time),
 * so they shouldn't be counted as missed.
 */
internal fun doseStartRef(createdAt: Long, scheduleStart: Long): Long =
    if (scheduleStart > 0L) maxOf(createdAt, scheduleStart) else createdAt

private const val MISSED_AFTER_MILLIS = 2 * 60 * 60 * 1000L
const val FUTURE_STATUS = "FUTURE"