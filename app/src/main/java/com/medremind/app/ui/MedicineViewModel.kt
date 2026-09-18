package com.medremind.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medremind.app.alarm.ReminderScheduler
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.Medicine
import com.medremind.app.data.DoseEvent
import com.medremind.app.data.DoseStatus
import com.medremind.app.data.PhotoStorage
import com.medremind.app.data.Schedule
import com.medremind.app.data.ScheduleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    val medicines: StateFlow<List<Medicine>> = db.medicineDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val loaded: StateFlow<Boolean> = db.medicineDao().getAll()
        .map { true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val schedulesByMedicine: StateFlow<Map<Long, List<Schedule>>> = db.scheduleDao().observeAll()
        .map { list -> list.groupBy { it.medicineId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val history: StateFlow<List<DoseHistoryItem>> = combine(
        db.doseEventDao().observeAll(),
        db.medicineDao().observeAll()
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
        val medicinesById = db.medicineDao().getAllOnce().associateBy { it.id }
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
            val medicinesById = db.medicineDao().getAllOnce().associateBy { it.id }
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

    fun saveMedicine(medicine: Medicine, schedules: List<Schedule>, onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val medicineId = if (medicine.id == 0L) {
                    db.medicineDao().insert(medicine)
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
        val medicinesById = db.medicineDao().getAllOnce().associateBy { it.id }
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

    fun markDose(dose: TodayDose, status: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
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
                } else {
                    dao.insert(
                        DoseEvent(
                            scheduleId = dose.schedule.id,
                            medicineId = dose.medicine.id,
                            scheduledAt = dose.timeMillis,
                            status = status,
                            actedAt = System.currentTimeMillis()
                        )
                    )
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