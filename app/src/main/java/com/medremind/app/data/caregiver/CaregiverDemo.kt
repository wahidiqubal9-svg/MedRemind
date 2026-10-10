package com.medremind.app.data.caregiver

import android.content.Context
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.CaregiverActivity
import com.medremind.app.data.CaregiverActor
import com.medremind.app.data.CaregiverActivityType
import com.medremind.app.data.CaregiverDirection
import com.medremind.app.data.CaregiverLink
import com.medremind.app.data.CaregiverPermission
import com.medremind.app.data.CaregiverStatus
import com.medremind.app.data.IntakeInstruction
import com.medremind.app.data.Medicine
import com.medremind.app.data.MedicineForm
import com.medremind.app.data.Metric
import com.medremind.app.data.MetricContext
import com.medremind.app.data.MetricType
import com.medremind.app.data.Patient
import com.medremind.app.data.Schedule
import com.medremind.app.data.ScheduleType

/**
 * Local-only helpers for testing the caregiver workflow on a single device.
 *
 * These simulate the two sides of a connection without any cloud. They are
 * clearly labelled "Demo" in the UI and never claim to be a real remote link.
 */
object CaregiverDemo {

    /** Creates an instantly-active caregiver for the device owner. */
    suspend fun connectCaregiverForMe(context: Context, caregiverName: String): Long {
        val dao = AppDatabase.get(context).caregiverDao()
        val name = caregiverName.ifBlank { "Demo caregiver" }
        val id = dao.insertLink(
            CaregiverLink(
                patientProfileId = 0L,
                caregiverName = name,
                direction = CaregiverDirection.INCOMING,
                status = CaregiverStatus.ACTIVE,
                permissions = CaregiverPermission.DEFAULT_MEDICATION
            )
        )
        dao.insertActivity(
            CaregiverActivity(
                linkId = id,
                patientProfileId = 0L,
                actor = CaregiverActor.PATIENT,
                type = CaregiverActivityType.CONNECTED,
                message = "$name connected (demo)"
            )
        )
        return id
    }

    /**
     * Creates an instantly-active patient for the caregiver side, plus a couple of
     * sample medicines so the dashboard, progress and "Remind now" are testable.
     */
    suspend fun connectDemoPatient(
        context: Context,
        patientName: String,
        caregiverName: String
    ): Long {
        val db = AppDatabase.get(context)
        val dao = db.caregiverDao()
        val existing = dao.patientsOnce()
        val name = patientName.ifBlank { "Mom" }
        val actor = caregiverName.ifBlank { "You" }
        val patientId = dao.insertPatient(
            Patient(name = name, relation = "Demo", sortOrder = existing.size)
        )
        dao.insertLink(
            CaregiverLink(
                patientProfileId = patientId,
                caregiverName = actor,
                direction = CaregiverDirection.OUTGOING,
                status = CaregiverStatus.ACTIVE,
                // Demo grants the opt-in health permissions so the caregiver can
                // see the patient's readings (BP, glucose, weight).
                permissions = CaregiverPermission.DEFAULT_MEDICATION or
                    CaregiverPermission.HEALTH_BP or
                    CaregiverPermission.HEALTH_GLUCOSE or
                    CaregiverPermission.HEALTH_WEIGHT
            )
        )

        seedMedicine(
            db, patientId, actor,
            name = "Metformin", strength = "500 mg",
            doseLabel = "1 tablet", intake = IntakeInstruction.WITH_MEAL,
            quantity = 30, refillThreshold = 5,
            schedule = Schedule(medicineId = 0L, type = ScheduleType.DAILY, times = "08:00,20:00")
        )
        seedMedicine(
            db, patientId, actor,
            name = "Amlodipine", strength = "5 mg",
            doseLabel = "1 tablet", intake = IntakeInstruction.AFTER_MEAL,
            quantity = 20, refillThreshold = 4,
            schedule = Schedule(medicineId = 0L, type = ScheduleType.DAILY, times = "09:00")
        )
        seedMedicine(
            db, patientId, actor,
            name = "Paracetamol", strength = "500 mg",
            doseLabel = "1 tablet", intake = IntakeInstruction.NONE,
            quantity = 0, refillThreshold = 0,
            schedule = Schedule(medicineId = 0L, type = ScheduleType.AS_NEEDED, times = "08:00")
        )

        // A few sample readings so the caregiver's Health tab has content. They
        // were logged by the patient themselves (no caregiver footnote).
        val now = System.currentTimeMillis()
        db.metricDao().insert(
            Metric(
                type = MetricType.BP, value = 128f, value2 = 82f,
                profileId = patientId, recordedAt = now - 86_400_000L
            )
        )
        db.metricDao().insert(
            Metric(
                type = MetricType.GLUCOSE, value = 110f, context = MetricContext.PRE_MEAL,
                profileId = patientId, recordedAt = now - 43_200_000L
            )
        )
        db.metricDao().insert(
            Metric(
                type = MetricType.WEIGHT, value = 68f,
                profileId = patientId, recordedAt = now - 3_600_000L
            )
        )

        dao.insertActivity(
            CaregiverActivity(
                patientProfileId = patientId,
                actor = CaregiverActor.CAREGIVER,
                type = CaregiverActivityType.CONNECTED,
                message = "$name added (demo)"
            )
        )
        return patientId
    }

    private suspend fun seedMedicine(
        db: AppDatabase,
        profileId: Long,
        addedBy: String,
        name: String,
        strength: String,
        doseLabel: String,
        intake: String,
        quantity: Int,
        refillThreshold: Int,
        schedule: Schedule
    ) {
        val medicineId = db.medicineDao().insert(
            Medicine(
                name = name,
                strength = strength,
                form = MedicineForm.TABLET,
                intakeInstruction = intake,
                quantity = quantity,
                refillThreshold = refillThreshold,
                profileId = profileId,
                addedBy = addedBy
            )
        )
        db.scheduleDao().insert(schedule.copy(medicineId = medicineId, doseLabel = doseLabel))
    }
}
