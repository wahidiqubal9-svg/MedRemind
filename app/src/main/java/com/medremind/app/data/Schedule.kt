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
    const val AS_NEEDED = "AS_NEEDED"

    /** Every N days (e.g. alternate day = 2). */
    const val EVERY_N_DAYS = "EVERY_N_DAYS"

    /** Only the specific calendar dates in [Schedule.selectedDates]. */
    const val SELECTED_DATES = "SELECTED_DATES"
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
    val enabled: Boolean = true,
    /** For EVERY_N_DAYS: the gap in days. */
    val intervalDays: Int = 0,
    /** For SELECTED_DATES: comma-separated epoch days. */
    val selectedDates: String = ""
)
