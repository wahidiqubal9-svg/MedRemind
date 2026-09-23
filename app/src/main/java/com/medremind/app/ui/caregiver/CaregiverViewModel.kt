package com.medremind.app.ui.caregiver

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medremind.app.alarm.ReminderScheduler
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.CaregiverActivity
import com.medremind.app.data.CaregiverActor
import com.medremind.app.data.CaregiverDirection
import com.medremind.app.data.CaregiverLink
import com.medremind.app.data.DoseStatus
import com.medremind.app.data.Medicine
import com.medremind.app.data.PairingRequest
import com.medremind.app.data.Patient
import com.medremind.app.data.caregiver.CaregiverProvider
import com.medremind.app.data.caregiver.ConnectionResult
import com.medremind.app.data.caregiver.ReminderResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/** A dose occurrence for a specific patient profile. */
data class PatientDose(
    val scheduledAt: Long,
    val medicine: Medicine,
    val status: String,
    val eventId: Long?
)

object NotificationType {
    const val ACTIVITY = "ACTIVITY"
    const val MISSED = "MISSED"
    const val LOW_STOCK = "LOW_STOCK"
}

/** One row in the caregiver notification centre. */
data class CaregiverNotification(
    val at: Long,
    val type: String,
    val title: String,
    val subtitle: String,
    val patientProfileId: Long,
    val medicineId: Long?
)

class CaregiverViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val db = AppDatabase.get(app)
    private val caregiverRepo = CaregiverProvider.caregiverRepository(app)
    private val connectionRepo = CaregiverProvider.connectionRepository(app)
    private val reminderService = CaregiverProvider.reminderService(app)

    val myName: String =
        app.getSharedPreferences("medremind_settings", Context.MODE_PRIVATE)
            .getString("profile_name", "")?.ifBlank { null } ?: "You"

    val patients: StateFlow<List<Patient>> = caregiverRepo.patients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val caregiversForMe: StateFlow<List<CaregiverLink>> = caregiverRepo.caregiversForMe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val peopleICareFor: StateFlow<List<CaregiverLink>> = caregiverRepo.peopleICareFor()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingForMe: StateFlow<List<CaregiverLink>> = connectionRepo.pendingRequestsForMe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingICreated: StateFlow<List<CaregiverLink>> = connectionRepo.pendingRequestsICreated()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val prefs = app.getSharedPreferences("medremind_settings", Context.MODE_PRIVATE)
    private val seenAt = kotlinx.coroutines.flow.MutableStateFlow(
        prefs.getLong("caregiver_seen_at", 0L)
    )

    /** Badge count: new caregiver activity since the notification centre was last opened. */
    val unreadCount: StateFlow<Int> = caregiverRepo.activity(null)
        .map { list -> list.count { it.at > seenAt.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun markNotificationsSeen() {
        val now = System.currentTimeMillis()
        seenAt.value = now
        prefs.edit().putLong("caregiver_seen_at", now).apply()
    }

    /** A merged feed of caregiver activity, missed doses and low stock. */
    val notifications: StateFlow<List<CaregiverNotification>> = kotlinx.coroutines.flow.combine(
        caregiverRepo.activity(null),
        caregiverRepo.patients(),
        db.medicineDao().observeAll(),
        db.scheduleDao().observeAll(),
        db.doseEventDao().observeAll()
    ) { activity, patients, medicines, schedules, events ->
        buildNotifications(activity, patients, medicines, schedules, events)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun buildNotifications(
        activity: List<CaregiverActivity>,
        patients: List<Patient>,
        medicines: List<Medicine>,
        schedules: List<com.medremind.app.data.Schedule>,
        events: List<com.medremind.app.data.DoseEvent>
    ): List<CaregiverNotification> {
        val now = System.currentTimeMillis()
        val items = mutableListOf<CaregiverNotification>()
        fun patientName(id: Long): String =
            if (id == 0L) "You" else patients.firstOrNull { it.id == id }?.name ?: "Patient"

        activity.forEach {
            items.add(
                CaregiverNotification(
                    at = it.at,
                    type = NotificationType.ACTIVITY,
                    title = it.type.replace('_', ' ').lowercase()
                        .replaceFirstChar { c -> c.uppercase() },
                    subtitle = it.message.ifBlank { it.medicineName },
                    patientProfileId = it.patientProfileId,
                    medicineId = null
                )
            )
        }

        medicines.filter { it.quantity > 0 && it.refillThreshold > 0 && it.quantity <= it.refillThreshold }
            .forEach {
                items.add(
                    CaregiverNotification(
                        at = now,
                        type = NotificationType.LOW_STOCK,
                        title = "${it.name} running low",
                        subtitle = "${patientName(it.profileId)} \u00b7 ${it.quantity} left",
                        patientProfileId = it.profileId,
                        medicineId = it.id
                    )
                )
            }

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val byId = medicines.associateBy { it.id }
        schedules.filter { it.enabled && it.medicineId in byId }.forEach { schedule ->
            val medicine = byId[schedule.medicineId] ?: return@forEach
            ReminderScheduler.occurrencesOn(schedule, today)
                .filter { it <= now }
                .forEach { trigger ->
                    val event = events.firstOrNull {
                        it.medicineId == schedule.medicineId &&
                            abs(it.scheduledAt - trigger) < 90_000L
                    }
                    if (event == null || event.status == DoseStatus.MISSED) {
                        val time = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                            .format(java.util.Date(trigger))
                        items.add(
                            CaregiverNotification(
                                at = trigger,
                                type = NotificationType.MISSED,
                                title = "Missed dose",
                                subtitle = "${patientName(medicine.profileId)} \u00b7 ${medicine.name} \u00b7 $time",
                                patientProfileId = medicine.profileId,
                                medicineId = medicine.id
                            )
                        )
                    }
                }
        }
        return items.sortedByDescending { it.at }
    }

    data class Adherence(val taken: Int, val scheduled: Int, val missed: Int)

    /** Adherence for a profile over the last [days] days (past doses only). */
    suspend fun adherence(profileId: Long, days: Int): Adherence = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val start = today.minusDays((days - 1).toLong())
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val medicines = db.medicineDao().getAllOnce(profileId).associateBy { it.id }
        if (medicines.isEmpty()) return@withContext Adherence(0, 0, 0)
        val schedules = db.scheduleDao().getAllOnce().filter { it.enabled && it.medicineId in medicines }
        val events = db.doseEventDao().between(startMillis, endMillis)
        val now = System.currentTimeMillis()
        var taken = 0
        var missed = 0
        var scheduled = 0
        var day = start
        while (!day.isAfter(today)) {
            schedules.forEach { schedule ->
                val medicine = medicines[schedule.medicineId] ?: return@forEach
                val createdDate = Instant.ofEpochMilli(medicine.createdAt).atZone(zone).toLocalDate()
                if (day.isBefore(createdDate)) return@forEach
                ReminderScheduler.occurrencesOn(schedule, day).forEach { trigger ->
                    if (trigger > now) return@forEach
                    scheduled++
                    val event = events.firstOrNull {
                        it.medicineId == schedule.medicineId && abs(it.scheduledAt - trigger) < 90_000L
                    }
                    when (event?.status) {
                        DoseStatus.TAKEN -> taken++
                        DoseStatus.SKIPPED -> {}
                        else -> missed++
                    }
                }
            }
            day = day.plusDays(1)
        }
        Adherence(taken, scheduled, missed)
    }

    fun medicinesFor(profileId: Long): Flow<List<Medicine>> =
        db.medicineDao().observeAll(profileId)

    fun schedulesFor(medicineId: Long): Flow<List<com.medremind.app.data.Schedule>> =
        db.scheduleDao().observeForMedicine(medicineId)

    fun allSchedules(): Flow<List<com.medremind.app.data.Schedule>> =
        db.scheduleDao().observeAll()

    fun activityFor(profileId: Long?): Flow<List<CaregiverActivity>> =
        caregiverRepo.activity(profileId)

    suspend fun patientOnce(profileId: Long): Patient? = withContext(Dispatchers.IO) {
        db.caregiverDao().patient(profileId)
    }

    // ---- Pairing -------------------------------------------------------------

    suspend fun createPairingCode(): PairingRequest = withContext(Dispatchers.IO) {
        connectionRepo.createPatientPairingCode(myName)
    }

    suspend fun requestAccess(code: String, onError: (String) -> Unit) {
        val result = withContext(Dispatchers.IO) {
            connectionRepo.requestCaregiverAccess(code, myName)
        }
        if (result is ConnectionResult.Error) onError(result.message)
    }

    fun approve(link: CaregiverLink) {
        viewModelScope.launch(Dispatchers.IO) { connectionRepo.approve(link.pairingId) }
    }

    fun decline(link: CaregiverLink) {
        viewModelScope.launch(Dispatchers.IO) { connectionRepo.decline(link.pairingId) }
    }

    fun removeCaregiver(link: CaregiverLink) {
        viewModelScope.launch(Dispatchers.IO) {
            caregiverRepo.removeLink(link.id)
            caregiverRepo.logActivity(
                profileId = 0L,
                actor = CaregiverActor.PATIENT,
                type = com.medremind.app.data.CaregiverActivityType.REMOVED,
                message = "Caregiver removed"
            )
        }
    }

    fun removePatient(profileId: Long) {
        viewModelScope.launch(Dispatchers.IO) { caregiverRepo.removePatient(profileId) }
    }

    fun savePermissions(link: CaregiverLink, permissions: Int) {
        viewModelScope.launch(Dispatchers.IO) { caregiverRepo.updatePermissions(link.id, permissions) }
    }

    // ---- Local demo (no cloud) ----------------------------------------------

    /** Instantly connects a demo caregiver for testing on this device. */
    fun demoConnectCaregiver(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                com.medremind.app.data.caregiver.CaregiverDemo
                    .connectCaregiverForMe(app, "Wahid (demo)")
            }
            onDone()
        }
    }

    /** Instantly adds a demo patient (with sample medicines) for testing. */
    fun demoConnectPatient(onAdded: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) {
                com.medremind.app.data.caregiver.CaregiverDemo
                    .connectDemoPatient(app, "Mom (demo)", myName)
            }
            onAdded(id)
        }
    }

    fun addPatient(name: String, relation: String, onAdded: (Long) -> Unit) {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { caregiverRepo.addPatient(name, relation) }
            onAdded(id)
        }
    }

    // ---- Remind now ----------------------------------------------------------

    fun remindNow(
        profileId: Long,
        medicine: Medicine,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                reminderService.sendReminder(profileId, medicine.id, myName)
            }
            val message = when (result) {
                is ReminderResult.Delivered -> "Reminder sent for ${medicine.name}"
                is ReminderResult.Error -> result.message
            }
            onResult(message)
        }
    }

    // ---- Per-patient dose data -----------------------------------------------

    suspend fun dosesOn(profileId: Long, date: LocalDate): List<PatientDose> =
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = dayStart + 86_400_000L
            val medicines = db.medicineDao().getAllOnce(profileId)
            if (medicines.isEmpty()) return@withContext emptyList()
            val byId = medicines.associateBy { it.id }
            val schedules = db.scheduleDao().getAllOnce()
                .filter { it.enabled && it.medicineId in byId }
            val events = db.doseEventDao().between(dayStart, dayEnd)
            val now = System.currentTimeMillis()
            val result = mutableListOf<PatientDose>()
            schedules.forEach { schedule ->
                val medicine = byId[schedule.medicineId] ?: return@forEach
                val createdDate = Instant.ofEpochMilli(medicine.createdAt).atZone(zone).toLocalDate()
                if (date.isBefore(createdDate)) return@forEach
                ReminderScheduler.occurrencesOn(schedule, date).forEach { trigger ->
                    val event = events.firstOrNull {
                        it.medicineId == schedule.medicineId && abs(it.scheduledAt - trigger) < 90_000L
                    }
                    val status = event?.status ?: if (trigger < now) DoseStatus.MISSED else DoseStatus.PENDING
                    result.add(PatientDose(trigger, medicine, status, event?.id))
                }
            }
            result.sortedBy { it.scheduledAt }
        }

    data class Progress(val confirmed: Int, val total: Int, val missed: Int, val upcoming: Int)

    suspend fun todayProgress(profileId: Long): Progress = withContext(Dispatchers.IO) {
        val doses = dosesOn(profileId, LocalDate.now())
        Progress(
            confirmed = doses.count { it.status == DoseStatus.TAKEN },
            total = doses.size,
            missed = doses.count { it.status == DoseStatus.MISSED },
            upcoming = doses.count { it.status == DoseStatus.PENDING }
        )
    }

    suspend fun needsAttention(profileId: Long): List<PatientDose> = withContext(Dispatchers.IO) {
        dosesOn(profileId, LocalDate.now()).filter {
            it.status == DoseStatus.MISSED || it.status == DoseStatus.PENDING
        }
    }

    fun profileName(profile: Patient?, fallback: String): String =
        profile?.name?.ifBlank { fallback } ?: fallback
}
