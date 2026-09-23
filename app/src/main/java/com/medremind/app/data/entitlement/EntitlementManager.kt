package com.medremind.app.data.entitlement

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The single source of truth for "is this user Pro?". Every paid feature should
 * ask this object rather than reading preferences directly.
 */
class EntitlementManager(
    private val repository: EntitlementRepository,
    private val scope: CoroutineScope
) {

    val entitlement: StateFlow<Entitlement> = repository.observe()
        .stateIn(scope, SharingStarted.Eagerly, Entitlement())

    val isPro: StateFlow<Boolean> = entitlement
        .map { it.isPro }
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** Used by the developer toggle to preview both free and Pro states. */
    fun setSimulatedPro(enabled: Boolean) {
        scope.launch {
            if (enabled) {
                repository.set(
                    Entitlement(
                        isPro = true,
                        plan = "simulated",
                        expiry = null,
                        lastVerified = System.currentTimeMillis(),
                        source = Entitlement.SOURCE_SIMULATED
                    )
                )
            } else {
                repository.clear()
            }
        }
    }

    /** Grants Pro after a successful purchase. */
    fun grant(plan: String) {
        scope.launch {
            repository.set(
                Entitlement(
                    isPro = true,
                    plan = plan,
                    expiry = null,
                    lastVerified = System.currentTimeMillis(),
                    source = Entitlement.SOURCE_PLAY
                )
            )
        }
    }
}
