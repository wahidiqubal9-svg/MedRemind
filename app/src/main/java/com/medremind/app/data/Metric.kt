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

object MetricContext {
    const val NONE = ""
    const val PRE_MEAL = "PRE_MEAL"
    const val AFTER_MEAL = "AFTER_MEAL"
    const val DINNER = "DINNER"
    const val SNACKS = "SNACKS"

    val glucoseOptions = listOf(PRE_MEAL, AFTER_MEAL, DINNER, SNACKS)

    fun label(value: String): String = when (value) {
        PRE_MEAL -> "Pre meal"
        AFTER_MEAL -> "After meal"
        DINNER -> "Dinner"
        SNACKS -> "Snacks"
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
    val recordedAt: Long = System.currentTimeMillis(),
    /** Meal context for glucose readings. */
    val context: String = MetricContext.NONE,
    /** Profile (patient) this reading belongs to. 0 = the device owner. */
    val profileId: Long = 0L
)
