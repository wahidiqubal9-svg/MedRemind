package com.medremind.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<Medicine>>

    @Insert
    suspend fun insert(medicine: Medicine): Long

    @Update
    suspend fun update(medicine: Medicine)

    @Delete
    suspend fun delete(medicine: Medicine)
}
