package com.medremind.app.data.purchase

/**
 * Development purchase service: "purchases" always succeed so the upgrade flow
 * (paywall → Pro → continue) can be built and tested end-to-end. Real charging
 * uses a payment gateway + backend (the app is distributed off the Play Store).
 */
class SimulatedPurchaseService : PurchaseService {
    override val available: Boolean = true

    override suspend fun purchase(plan: PurchasePlan): PurchaseResult =
        PurchaseResult.Purchased(plan.productId)

    override suspend fun restore(): PurchaseResult =
        PurchaseResult.Purchased(PurchasePlan.YEARLY.productId)
}
