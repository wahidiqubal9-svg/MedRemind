package com.medremind.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medremind.app.alarm.AlarmNotifier
import com.medremind.app.alarm.ReminderScheduler
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.Medicine
import com.medremind.app.data.Metric
import com.medremind.app.data.DoseEvent
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
        medicines.filter { it.id in ids }
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
                        profileId = profileId
                    )
                )
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
            withContext(Dispatchers.IO) { db.metricDao().delete(metric) }
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

    suspend fun doseLogForRange(days: Int): List<DoseLogEntry> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val start = today.minusDays((days - 1).toLong())
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val schedules = db.scheduleDao().getAllOnce().filter { it.enabled }
        val medicinesById = db.medicineDao().getAllOnce(activeProfile.value).associateBy { it.id }
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
                ReminderScheduler.occurrencesOn(schedule, date).forEach { trigger ->
                    val status = if (date.isAfter(today)) {
                        FUTURE_STATUS
                    } else {
                        val event = events.firstOrNull {
                            it.medicineId == schedule.medicineId && abs(it.scheduledAt - trigger) < 90_000L
                        }
                        when {
                            event != null -> event.status
                            trigger >= now -> DoseStatus.PENDING
                            else -> DoseStatus.MISSED
                        }
                    }
                    result.add(
                        DoseLogEntry(
                            medicineName = medicine.name,
                            photoPath = medicine.photoPath,
                            scheduledAt = trigger,
                            status = status
                        )
                    )
                }
            }
        }
        result.sortedByDescending { it.scheduledAt }
    }

    suspend fun datesWithDoses(from: LocalDate, to: LocalDate): Set<LocalDate> =
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val schedules = db.scheduleDao().getAllOnce().filter { it.enabled }
            if (schedules.isEmpty()) return@withContext emptySet()
            val medicinesById = db.medicineDao().getAllOnce(activeProfile.value).associateBy { it.id }
            val result = mutableSetOf<LocalDate>()
            var day = from
            while (!day.isAfter(to)) {
                val hasDose = schedules.any { schedule ->
                    val medicine = medicinesById[schedule.medicineId] ?: return@any false
                    val createdDate = Instant.ofEpochMilli(medicine.createdAt).atZone(zone).toLocalDate()
                    !day.isBefore(createdDate) &&
                        ReminderScheduler.occurrencesOn(schedule, day).isNotEmpty()
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
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val medicineId = if (medicine.id == 0L) {
                    db.medicineDao().insert(medicine.copy(profileId = profileId))
                } else {
                    db.medicineDao().update(medicine)
                    medicine.id
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
                    if (toSave.id == 0L) {
                        val newId = db.scheduleDao().insert(toSave)
                        ReminderScheduler.schedule(app, toSave.copy(id = newId))
                    } else {
                        db.scheduleDao().update(toSave)
                        ReminderScheduler.schedule(app, toSave)
                    }
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

    fun deleteMedicine(medicine: Medicine, onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.scheduleDao().forMedicine(medicine.id).forEach {
                    ReminderScheduler.cancel(app, it.id)
                }
                db.medicineDao().delete(medicine)
                PhotoStorage.delete(medicine.photoPath)
            }
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
            ReminderScheduler.occurrencesOn(schedule, date).forEach { trigger ->
                val event = events.firstOrNull {
                    it.medicineId == schedule.medicineId && abs(it.scheduledAt - trigger) < 90_000L
                }
                val status = event?.status ?: if (trigger < now) DoseStatus.MISSED else DoseStatus.PENDING
                result.add(TodayDose(trigger, medicine, status, schedule, event?.id))
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
    val eventId: Long? = null
)

private const val MISSED_AFTER_MILLIS = 2 * 60 * 60 * 1000L
const val FUTURE_STATUS = "FUTURE"