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
    val createdAt: Long = System.currentTimeMillis()
)
