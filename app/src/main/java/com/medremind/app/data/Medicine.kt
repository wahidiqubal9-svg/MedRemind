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

object MedicineCategory {
    const val PRESCRIPTION = "PRESCRIPTION"
    const val SUPPLEMENT = "SUPPLEMENT"
    const val OTC = "OTC"

    fun label(value: String): String = when (value) {
        SUPPLEMENT -> "Supplement"
        OTC -> "OTC"
        else -> "Prescription"
    }
}

object MedicineForm {
    const val TABLET = "tablet"
    const val CAPSULE = "capsule"
    const val SYRUP = "syrup"
    const val DROP = "drop"
    const val INJECTION = "injection"
    const val OINTMENT = "ointment"
    const val OTHER = "other"

    val all = listOf(TABLET, CAPSULE, SYRUP, DROP, INJECTION, OINTMENT, OTHER)

    fun label(value: String): String = when (value) {
        TABLET -> "Tablet"
        CAPSULE, "softgel" -> "Capsule"
        SYRUP, "liquid" -> "Syrup"
        DROP -> "Drop"
        INJECTION -> "Injection"
        OINTMENT -> "Ointment"
        else -> "Others"
    }

    /** Maps legacy values to the current set. */
    fun normalize(value: String): String = when (value) {
        "softgel" -> CAPSULE
        "liquid" -> SYRUP
        else -> value
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
    val intakeInstruction: String = IntakeInstruction.NONE,
    val category: String = MedicineCategory.PRESCRIPTION,
    val form: String = MedicineForm.TABLET,
    val prescriber: String = "",
    val rxNumber: String = "",
    val refillsLeft: Int = 0,
    /** Total units in a full pack; 0 means unknown. Powers the stock gauge. */
    val packSize: Int = 0,
    val autoRefillDate: Long? = null,
    /** Printed batch/lot number. */
    val batchNumber: String = "",
    /** Printed expiry date, as epoch millis. */
    val expiryDate: Long? = null
)
