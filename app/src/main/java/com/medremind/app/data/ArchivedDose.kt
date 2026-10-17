package com.medremind.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A snapshot of a single dose belonging to a medicine that was deleted while
 * "keep history" was chosen. It has no foreign key to medicines, so it survives
 * the medicine's removal and can still be shown in Progress and exports.
 */
@Entity(
    tableName = "archived_doses",
    indices = [Index("profileId"), Index("scheduledAt")]
)
data class ArchivedDose(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val profileId: Long = 0L,
    val medicineName: String,
    val photoPath: String? = null,
    val scheduledAt: Long,
    val status: String
)
