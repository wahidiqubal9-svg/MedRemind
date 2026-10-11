package com.medremind.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medremind.app.data.entitlement.Entitlement
import com.medremind.app.data.entitlement.EntitlementManager
import com.medremind.app.data.entitlement.LocalEntitlementRepository
import com.medremind.app.data.purchase.PurchasePlan
import com.medremind.app.data.purchase.PurchaseResult
import com.medremind.app.data.purchase.SimulatedPurchaseService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Compose-facing wrapper around [EntitlementManager]. Shared across the activity
 * so every screen sees the same Pro state.
 */
class EntitlementViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = EntitlementManager(
        LocalEntitlementRepository(application),
        viewModelScope
    )
    private val purchaseService = SimulatedPurchaseService()

    val entitlement: StateFlow<Entitlement> = manager.entitlement
    val isPro: StateFlow<Boolean> = manager.isPro

    /** Whether the paywall is currently shown. Drives AppRoot's routing. */
    var showPaywall by mutableStateOf(false)
        private set

    /** Why the paywall was opened (e.g. "caregiver") — tailors the copy. */
    var paywallReason by mutableStateOf<String?>(null)
        private set

    /** Action to run once the user becomes Pro (the reason they hit the paywall). */
    private var afterUnlock: (() -> Unit)? = null

    fun openPaywall(reason: String? = null, afterUnlock: (() -> Unit)? = null) {
        this.afterUnlock = afterUnlock
        paywallReason = reason
        showPaywall = true
    }

    fun closePaywall() {
        showPaywall = false
        paywallReason = null
        afterUnlock = null
    }

    /** Called once Pro is active: closes the paywall and continues the action. */
    fun completeUnlock() {
        val action = afterUnlock
        showPaywall = false
        paywallReason = null
        afterUnlock = null
        action?.invoke()
    }

    fun setSimulatedPro(enabled: Boolean) = manager.setSimulatedPro(enabled)

    fun subscribe(plan: PurchasePlan, onResult: (PurchaseResult) -> Unit) {
        viewModelScope.launch {
            val result = purchaseService.purchase(plan)
            if (result is PurchaseResult.Purchased) {
                manager.grant(result.plan)
                onResult(result)
                completeUnlock()
            } else {
                onResult(result)
            }
        }
    }

    fun restore(onResult: (PurchaseResult) -> Unit) {
        viewModelScope.launch {
            onResult(purchaseService.restore())
        }
    }
}
