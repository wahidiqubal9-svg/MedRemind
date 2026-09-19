package com.medremind.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object IntakeInstruction {
    const val NONE = ""
    const val BEFORE_MEAL = "BEFORE_MEAL"
    const val WITH_MEAL = "WITH_MEAL"
    const val AFTER_MEAL = "AFTER_MEAL"
    const val EMPTY_STOMACH = "EMPTY_STOMACH"

    fun label(value: String): String = when (value) {
        BEFORE_MEAL -> "Before meal"
        WITH_MEAL -> "With food"
        AFTER_MEAL -> "After meal"
        EMPTY_STOMACH -> "Empty stomach"
        else -> ""
    }
}

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
    val refillThreshold: Int = 0,
    /** How the medicine should be taken, relative to food. */
    val intakeInstruction: String = IntakeInstruction.NONE
)
