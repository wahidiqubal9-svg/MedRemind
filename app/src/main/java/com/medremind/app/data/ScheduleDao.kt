package com.medremind.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY id ASC")
    suspend fun getAllOnce(): List<Schedule>

    @Query("SELECT * FROM schedules WHERE medicineId = :medicineId ORDER BY id ASC")
    suspend fun forMedicine(medicineId: Long): List<Schedule>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun byId(id: Long): Schedule?

    @Insert
    suspend fun insert(schedule: Schedule): Long

    @Update
    suspend fun update(schedule: Schedule)

    @Delete
    suspend fun delete(schedule: Schedule)

    @Query("SELECT * FROM schedules WHERE medicineId = :medicineId ORDER BY id ASC")
    fun observeForMedicine(medicineId: Long): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules ORDER BY id ASC")
    fun observeAll(): Flow<List<Schedule>>

    @Query("DELETE FROM schedules")
    suspend fun clear()

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<Schedule>)
}