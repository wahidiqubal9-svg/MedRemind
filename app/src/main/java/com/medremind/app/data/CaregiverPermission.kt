package com.medremind.app.data

/**
 * Caregiver permissions, stored as a bitmask on [CaregiverLink].
 *
 * Medication access is the default. Health measurements are opt-in and never
 * granted automatically, so a caregiver cannot read vitals without consent.
 */
object CaregiverPermission {
    const val VIEW_MEDICINES = 1 shl 0
    const val ADD_MEDICINES = 1 shl 1
    const val EDIT_MEDICINES = 1 shl 2
    const val REMOVE_MEDICINES = 1 shl 3
    const val MANAGE_SCHEDULES = 1 shl 4
    const val SEND_REMINDERS = 1 shl 5
    const val VIEW_CONFIRMATIONS = 1 shl 6
    const val VIEW_HISTORY = 1 shl 7
    const val VIEW_PHOTOS = 1 shl 8
    const val HEALTH_BP = 1 shl 9
    const val HEALTH_GLUCOSE = 1 shl 10
    const val HEALTH_WEIGHT = 1 shl 11

    val MEDICATION_FLAGS = listOf(
        VIEW_MEDICINES,
        ADD_MEDICINES,
        EDIT_MEDICINES,
        REMOVE_MEDICINES,
        MANAGE_SCHEDULES,
        SEND_REMINDERS,
        VIEW_CONFIRMATIONS,
        VIEW_HISTORY,
        VIEW_PHOTOS
    )

    val HEALTH_FLAGS = listOf(HEALTH_BP, HEALTH_GLUCOSE, HEALTH_WEIGHT)

    const val DEFAULT_MEDICATION: Int =
        VIEW_MEDICINES or ADD_MEDICINES or EDIT_MEDICINES or REMOVE_MEDICINES or
            MANAGE_SCHEDULES or SEND_REMINDERS or VIEW_CONFIRMATIONS or VIEW_HISTORY or
            VIEW_PHOTOS

    fun has(permissions: Int, flag: Int): Boolean = (permissions and flag) != 0
}
