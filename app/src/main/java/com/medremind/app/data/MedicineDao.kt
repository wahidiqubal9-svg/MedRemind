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

    @Query("SELECT * FROM medicines ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Medicine>>

    /** Medicines belonging to one profile (patient). */
    @Query("SELECT * FROM medicines WHERE profileId = :profileId ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(profileId: Long): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE profileId = :profileId ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOnce(profileId: Long): List<Medicine>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun byId(id: Long): Medicine?

    @Query("SELECT * FROM medicines ORDER BY id ASC LIMIT 1")
    suspend fun first(): Medicine?

    @Query("SELECT * FROM medicines ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<Medicine>

    @Insert
    suspend fun insert(medicine: Medicine): Long

    @Update
    suspend fun update(medicine: Medicine)

    @Delete
    suspend fun delete(medicine: Medicine)

    @Query("DELETE FROM medicines")
    suspend fun clear()

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(medicines: List<Medicine>)
}
