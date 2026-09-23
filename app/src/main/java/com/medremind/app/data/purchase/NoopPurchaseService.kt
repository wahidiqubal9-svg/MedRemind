package com.medremind.app.data.purchase

/**
 * Placeholder until Google Play Billing is connected. It never pretends a
 * purchase succeeded — the paywall shows the "not available yet" message.
 */
class NoopPurchaseService : PurchaseService {
    override val available: Boolean = false
    override suspend fun purchase(plan: PurchasePlan): PurchaseResult = PurchaseResult.NotAvailable
    override suspend fun restore(): PurchaseResult = PurchaseResult.NotAvailable
}
