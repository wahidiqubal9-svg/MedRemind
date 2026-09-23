package com.medremind.app.data.caregiver

import com.medremind.app.data.CaregiverActivity
import com.medremind.app.data.CaregiverLink
import com.medremind.app.data.Patient
import kotlinx.coroutines.flow.Flow

/**
 * Caregiver relationship management.
 *
 * This interface is intentionally free of any networking so the current, fully
 * offline [LocalCaregiverRepository] can later be swapped for a cloud-backed
 * implementation without touching the UI.
 */
interface CaregiverRepository {

    /** People whose medicines are tracked on this device (excludes the owner). */
    fun patients(): Flow<List<Patient>>

    /** Caregivers who help the owner of this device (direction INCOMING). */
    fun caregiversForMe(): Flow<List<CaregiverLink>>

    /** People this device's owner cares for (direction OUTGOING). */
    fun peopleICareFor(): Flow<List<CaregiverLink>>

    fun activity(profileId: Long?): Flow<List<CaregiverActivity>>

    suspend fun link(id: Long): CaregiverLink?

    suspend fun addPatient(name: String, relation: String): Long

    /** Removes a patient, their medicines, schedules and photos. */
    suspend fun removePatient(profileId: Long)

    suspend fun updateLink(link: CaregiverLink)

    suspend fun updatePermissions(linkId: Long, permissions: Int)

    suspend fun removeLink(linkId: Long)

    suspend fun setLinkStatus(linkId: Long, status: String)

    suspend fun logActivity(
        profileId: Long,
        actor: String,
        type: String,
        medicineName: String = "",
        message: String = ""
    )
}
