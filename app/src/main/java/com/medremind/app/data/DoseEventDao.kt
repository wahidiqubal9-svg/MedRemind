package com.medremind.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseEventDao {
    @Insert
    suspend fun insert(event: DoseEvent): Long

    @Update
    suspend fun update(event: DoseEvent)

    @Query("SELECT * FROM dose_events WHERE id = :id")
    suspend fun byId(id: Long): DoseEvent?

    @Query("SELECT * FROM dose_events ORDER BY scheduledAt DESC")
    fun observeAll(): Flow<List<DoseEvent>>

    @Query("SELECT * FROM dose_events WHERE scheduledAt >= :from AND scheduledAt < :to ORDER BY scheduledAt ASC")
    suspend fun between(from: Long, to: Long): List<DoseEvent>

    @Query("UPDATE dose_events SET status = 'MISSED' WHERE status = 'PENDING' AND scheduledAt < :cutoff")
    suspend fun markMissedBefore(cutoff: Long): Int
}
