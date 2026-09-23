package com.medremind.app.data.entitlement

import kotlinx.coroutines.flow.Flow

/**
 * Storage for the current [Entitlement]. The local implementation caches it so
 * paid local features (export, backup) keep working offline; a cloud
 * implementation can replace it later without touching the UI.
 */
interface EntitlementRepository {
    fun observe(): Flow<Entitlement>
    suspend fun current(): Entitlement
    suspend fun set(entitlement: Entitlement)
    suspend fun clear()
}
