package com.medremind.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricDao {
    @Insert
    suspend fun insert(metric: Metric): Long

    @androidx.room.Update
    suspend fun update(metric: Metric)

    @Delete
    suspend fun delete(metric: Metric)

    @Query("SELECT * FROM metrics ORDER BY recordedAt DESC")
    fun observeAll(): Flow<List<Metric>>

    @Query("SELECT * FROM metrics WHERE profileId = :profileId ORDER BY recordedAt DESC")
    fun observeAll(profileId: Long): Flow<List<Metric>>

    @Query("SELECT * FROM metrics ORDER BY recordedAt DESC")
    suspend fun getAllOnce(): List<Metric>

    @Query("SELECT * FROM metrics WHERE profileId = :profileId ORDER BY recordedAt DESC")
    suspend fun getAllOnce(profileId: Long): List<Metric>

    @Query("SELECT * FROM metrics WHERE cloudId = :cloudId AND cloudId != '' LIMIT 1")
    suspend fun byCloudId(cloudId: String): Metric?

    @Query("DELETE FROM metrics")
    suspend fun clear()

    @Query("DELETE FROM metrics WHERE profileId = :profileId")
    suspend fun deleteFor(profileId: Long)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(metrics: List<Metric>)
}
