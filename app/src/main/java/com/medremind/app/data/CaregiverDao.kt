package com.medremind.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CaregiverDao {

    // ---- Patients / profiles -------------------------------------------------

    @Query("SELECT * FROM patients ORDER BY sortOrder ASC, createdAt ASC")
    fun observePatients(): Flow<List<Patient>>

    @Query("SELECT * FROM patients ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun patientsOnce(): List<Patient>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun patient(id: Long): Patient?

    @Insert
    suspend fun insertPatient(patient: Patient): Long

    @Update
    suspend fun updatePatient(patient: Patient)

    @Delete
    suspend fun deletePatient(patient: Patient)

    // ---- Links ---------------------------------------------------------------

    @Query("SELECT * FROM caregiver_links WHERE status != 'REMOVED' ORDER BY createdAt ASC")
    fun observeLinks(): Flow<List<CaregiverLink>>

    @Query("SELECT * FROM caregiver_links WHERE status != 'REMOVED' ORDER BY createdAt ASC")
    suspend fun linksOnce(): List<CaregiverLink>

    @Query("SELECT * FROM caregiver_links WHERE direction = :direction AND status != 'REMOVED' ORDER BY createdAt ASC")
    fun observeLinks(direction: String): Flow<List<CaregiverLink>>

    @Query("SELECT * FROM caregiver_links WHERE direction = :direction AND status != 'REMOVED' ORDER BY createdAt ASC")
    suspend fun linksOnce(direction: String): List<CaregiverLink>

    @Query("SELECT * FROM caregiver_links WHERE id = :id")
    suspend fun link(id: Long): CaregiverLink?

    @Query("SELECT * FROM caregiver_links WHERE patientProfileId = :profileId AND direction = :direction AND status = 'ACTIVE' LIMIT 1")
    suspend fun activeLink(profileId: Long, direction: String): CaregiverLink?

    @Query("SELECT * FROM caregiver_links WHERE pairingId = :pairingId AND status != 'REMOVED'")
    suspend fun linksForPairing(pairingId: Long): List<CaregiverLink>

    @Query("SELECT * FROM caregiver_links WHERE direction = :direction AND status = 'PENDING' ORDER BY createdAt ASC")
    fun observePending(direction: String): Flow<List<CaregiverLink>>

    @Insert
    suspend fun insertLink(link: CaregiverLink): Long

    @Update
    suspend fun updateLink(link: CaregiverLink)

    @Query("DELETE FROM caregiver_links WHERE id = :id")
    suspend fun deleteLink(id: Long)

    // ---- Pairing -------------------------------------------------------------

    @Query("SELECT * FROM pairing_requests WHERE status = 'OPEN' ORDER BY createdAt DESC")
    suspend fun openPairings(): List<PairingRequest>

    @Query("SELECT * FROM pairing_requests WHERE code = :code AND status = 'OPEN' ORDER BY createdAt DESC LIMIT 1")
    suspend fun findByCode(code: String): PairingRequest?

    @Query("SELECT * FROM pairing_requests WHERE token = :token LIMIT 1")
    suspend fun findByToken(token: String): PairingRequest?

    @Query("SELECT * FROM pairing_requests WHERE id = :id")
    suspend fun pairing(id: Long): PairingRequest?

    @Insert
    suspend fun insertPairing(request: PairingRequest): Long

    @Update
    suspend fun updatePairing(request: PairingRequest)

    @Query("DELETE FROM pairing_requests")
    suspend fun clearPairings()

    // ---- Activity ------------------------------------------------------------

    @Query("SELECT * FROM caregiver_activity ORDER BY at DESC LIMIT 100")
    fun observeActivity(): Flow<List<CaregiverActivity>>

    @Query("SELECT * FROM caregiver_activity WHERE patientProfileId = :profileId ORDER BY at DESC LIMIT 100")
    fun observeActivity(profileId: Long): Flow<List<CaregiverActivity>>

    @Insert
    suspend fun insertActivity(activity: CaregiverActivity): Long

    // ---- Backup --------------------------------------------------------------

    @Query("SELECT * FROM patients")
    suspend fun patientsForBackup(): List<Patient>

    @Query("SELECT * FROM caregiver_links")
    suspend fun linksForBackup(): List<CaregiverLink>

    @Query("SELECT * FROM caregiver_activity ORDER BY at ASC")
    suspend fun activityForBackup(): List<CaregiverActivity>

    @Query("DELETE FROM patients")
    suspend fun clearPatients()

    @Query("DELETE FROM caregiver_links")
    suspend fun clearLinks()

    @Query("DELETE FROM caregiver_activity")
    suspend fun clearActivity()

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertPatients(patients: List<Patient>)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<CaregiverLink>)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activities: List<CaregiverActivity>)
}
