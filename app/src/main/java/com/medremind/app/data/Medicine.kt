package com.medremind.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val strength: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Pills currently on hand. 0 means refill tracking is off. */
    val quantity: Int = 0,
    /** Show a refill reminder when [quantity] drops to or below this. */
    val refillThreshold: Int = 0
)
