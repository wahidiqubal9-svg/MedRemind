package com.medremind.app.data.entitlement

/**
 * Whether this user has paid access ("Pro").
 *
 * Kept intentionally simple so a future Play Billing / cloud implementation can
 * produce the same shape. [source] records where the entitlement came from.
 */
data class Entitlement(
    val isPro: Boolean = false,
    val plan: String? = null,
    val expiry: Long? = null,
    val lastVerified: Long = 0L,
    val source: String = SOURCE_NONE
) {
    companion object {
        const val SOURCE_NONE = "NONE"
        const val SOURCE_SIMULATED = "SIMULATED"
        const val SOURCE_PLAY = "PLAY"
    }
}
