package com.medremind.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object ScheduleType {
    const val DAILY = "DAILY"
    const val WEEKDAYS = "WEEKDAYS"
    const val INTERVAL = "INTERVAL"
    const val COURSE = "COURSE"
}

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = Medicine::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicineId")]
)
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val medicineId: Long,
    val type: String = ScheduleType.DAILY,
    val times: String = "08:00",
    val daysMask: Int = 0,
    val intervalHours: Int = 0,
    val startDate: Long = 0L,
    val endDate: Long? = null,
    val doseLabel: String = "",
    val enabled: Boolean = true
)
