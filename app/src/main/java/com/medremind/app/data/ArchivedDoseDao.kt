package com.medremind.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ArchivedDoseDao {
    @Insert
    suspend fun insertAll(rows: List<ArchivedDose>)

    @Query(
        "SELECT * FROM archived_doses WHERE profileId = :profileId " +
            "AND scheduledAt >= :from AND scheduledAt < :to ORDER BY scheduledAt ASC"
    )
    suspend fun between(profileId: Long, from: Long, to: Long): List<ArchivedDose>

    @Query("SELECT * FROM archived_doses WHERE profileId = :profileId ORDER BY scheduledAt DESC")
    suspend fun getAllOnce(profileId: Long): List<ArchivedDose>

    @Query("SELECT * FROM archived_doses ORDER BY scheduledAt ASC")
    suspend fun getAllOnce(): List<ArchivedDose>

    @Query("DELETE FROM archived_doses WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: Long)

    @Query("DELETE FROM archived_doses")
    suspend fun clear()
}
