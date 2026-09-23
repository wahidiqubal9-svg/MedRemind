package com.medremind.app.data.purchase

/** The subscription products the paywall offers. Prices are set in Play Console. */
enum class PurchasePlan(val productId: String, val label: String, val blurb: String) {
    MONTHLY("pro_monthly", "Monthly", "Billed every month"),
    YEARLY("pro_yearly", "Yearly", "Billed once a year \u00b7 best value")
}

sealed class PurchaseResult {
    data class Purchased(val plan: String) : PurchaseResult()
    data class Failed(val message: String) : PurchaseResult()

    /** Billing is not connected yet (test builds). */
    object NotAvailable : PurchaseResult()
}

/**
 * Purchase abstraction. [NoopPurchaseService] is used until Google Play Billing
 * is wired up; the UI does not change when the real implementation lands.
 */
interface PurchaseService {
    val available: Boolean
    suspend fun purchase(plan: PurchasePlan): PurchaseResult
    suspend fun restore(): PurchaseResult
}
