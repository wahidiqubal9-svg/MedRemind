package com.medremind.app.data.caregiver

import android.content.Context
import com.medremind.app.alarm.ReminderScheduler
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.CaregiverActivity
import com.medremind.app.data.CaregiverActor
import com.medremind.app.data.CaregiverLink
import com.medremind.app.data.CaregiverStatus
import com.medremind.app.data.Patient
import com.medremind.app.data.PhotoStorage
import kotlinx.coroutines.flow.Flow

/** Offline, Room-backed implementation. */
class LocalCaregiverRepository(private val context: Context) : CaregiverRepository {

    private val db = AppDatabase.get(context)
    private val dao = db.caregiverDao()

    override fun patients(): Flow<List<Patient>> = dao.observePatients()

    override fun caregiversForMe(): Flow<List<CaregiverLink>> =
        dao.observeLinks(com.medremind.app.data.CaregiverDirection.INCOMING)

    override fun peopleICareFor(): Flow<List<CaregiverLink>> =
        dao.observeLinks(com.medremind.app.data.CaregiverDirection.OUTGOING)

    override fun activity(profileId: Long?): Flow<List<CaregiverActivity>> =
        if (profileId == null) dao.observeActivity() else dao.observeActivity(profileId)

    override suspend fun link(id: Long): CaregiverLink? = dao.link(id)

    override suspend fun addPatient(name: String, relation: String): Long {
        val existing = dao.patientsOnce()
        return dao.insertPatient(
            Patient(
                name = name.trim(),
                relation = relation.trim(),
                sortOrder = existing.size
            )
        )
    }

    override suspend fun removePatient(profileId: Long) {
        if (profileId == 0L) return
        // Delete this patient's medicines (cascades schedules) and photos.
        db.medicineDao().getAllOnce(profileId).forEach { medicine ->
            db.scheduleDao().forMedicine(medicine.id).forEach {
                ReminderScheduler.cancel(context, it.id)
            }
            db.medicineDao().delete(medicine)
            PhotoStorage.delete(medicine.photoPath)
        }
        dao.linksOnce().filter { it.patientProfileId == profileId }.forEach {
            dao.deleteLink(it.id)
        }
        dao.patient(profileId)?.let { dao.deletePatient(it) }
    }

    override suspend fun updateLink(link: CaregiverLink) {
        dao.updateLink(link.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun updatePermissions(linkId: Long, permissions: Int) {
        dao.link(linkId)?.let {
            dao.updateLink(
                it.copy(permissions = permissions, updatedAt = System.currentTimeMillis())
            )
        }
    }

    override suspend fun removeLink(linkId: Long) {
        dao.link(linkId)?.let {
            dao.updateLink(it.copy(status = CaregiverStatus.REMOVED, updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun setLinkStatus(linkId: Long, status: String) {
        dao.link(linkId)?.let {
            dao.updateLink(it.copy(status = status, updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun logActivity(
        profileId: Long,
        actor: String,
        type: String,
        medicineName: String,
        message: String
    ) {
        dao.insertActivity(
            CaregiverActivity(
                patientProfileId = profileId,
                actor = actor,
                type = type,
                medicineName = medicineName,
                message = message
            )
        )
    }

    companion object {
        const val DEFAULT_ACTOR = CaregiverActor.CAREGIVER
    }
}
