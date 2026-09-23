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
import com.medremind.app.data.purchase.NoopPurchaseService
import com.medremind.app.data.purchase.PurchasePlan
import com.medremind.app.data.purchase.PurchaseResult
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
    private val purchaseService = NoopPurchaseService()

    val entitlement: StateFlow<Entitlement> = manager.entitlement
    val isPro: StateFlow<Boolean> = manager.isPro

    /** Whether the paywall is currently shown. Drives AppRoot's routing. */
    var showPaywall by mutableStateOf(false)
        private set

    fun openPaywall() { showPaywall = true }
    fun closePaywall() { showPaywall = false }

    fun setSimulatedPro(enabled: Boolean) = manager.setSimulatedPro(enabled)

    fun subscribe(plan: PurchasePlan, onResult: (PurchaseResult) -> Unit) {
        viewModelScope.launch {
            val result = purchaseService.purchase(plan)
            if (result is PurchaseResult.Purchased) manager.grant(result.plan)
            onResult(result)
        }
    }

    fun restore(onResult: (PurchaseResult) -> Unit) {
        viewModelScope.launch {
            onResult(purchaseService.restore())
        }
    }
}
