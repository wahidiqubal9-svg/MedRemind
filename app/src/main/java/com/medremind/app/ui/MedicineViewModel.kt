package com.medremind.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medremind.app.alarm.ReminderScheduler
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.Medicine
import com.medremind.app.data.DoseEvent
import com.medremind.app.data.PhotoStorage
import com.medremind.app.data.Schedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicineViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val db = AppDatabase.get(application)

    val medicines: StateFlow<List<Medicine>> = db.medicineDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun schedulesFor(medicineId: Long): List<Schedule> =
        withContext(Dispatchers.IO) { db.scheduleDao().forMedicine(medicineId) }

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
                    val toSave = schedule.copy(medicineId = medicineId)
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
