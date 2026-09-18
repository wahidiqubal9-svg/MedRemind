package com.medremind.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object MetricType {
    const val BP = "BP"
    const val GLUCOSE = "GLUCOSE"
    const val WEIGHT = "WEIGHT"

    fun label(type: String): String = when (type) {
        BP -> "Blood pressure"
        GLUCOSE -> "Blood glucose"
        WEIGHT -> "Weight"
        else -> "Reading"
    }

    fun unit(type: String): String = when (type) {
        BP -> "mmHg"
        GLUCOSE -> "mg/dL"
        WEIGHT -> "kg"
        else -> ""
    }
}

@Entity(tableName = "metrics")
data class Metric(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val value: Float,
    /** Second value, used for blood pressure (diastolic). */
    val value2: Float = 0f,
    val recordedAt: Long = System.currentTimeMillis()
)
