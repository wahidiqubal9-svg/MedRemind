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

    @Delete
    suspend fun delete(metric: Metric)

    @Query("SELECT * FROM metrics ORDER BY recordedAt DESC")
    fun observeAll(): Flow<List<Metric>>

    @Query("SELECT * FROM metrics ORDER BY recordedAt DESC")
    suspend fun getAllOnce(): List<Metric>

    @Query("DELETE FROM metrics")
    suspend fun clear()

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(metrics: List<Metric>)
}
