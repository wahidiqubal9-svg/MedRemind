package com.medremind.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object DoseStatus {
    const val PENDING = "PENDING"
    const val TAKEN = "TAKEN"
    const val SKIPPED = "SKIPPED"
    const val MISSED = "MISSED"
}

@Entity(
    tableName = "dose_events",
    indices = [Index("scheduleId"), Index("medicineId"), Index("scheduledAt")]
)
data class DoseEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val scheduleId: Long,
    val medicineId: Long,
    val scheduledAt: Long,
    val status: String = DoseStatus.PENDING,
    val actedAt: Long? = null,
    val snoozeCount: Int = 0
)
