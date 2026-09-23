package com.medremind.app.data.entitlement

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Offline, preferences-backed entitlement cache. */
class LocalEntitlementRepository(context: Context) : EntitlementRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val state = MutableStateFlow(read())

    private fun read(): Entitlement = Entitlement(
        isPro = prefs.getBoolean("is_pro", false),
        plan = prefs.getString("plan", null),
        expiry = prefs.getLong("expiry", 0L).takeIf { it > 0L },
        lastVerified = prefs.getLong("last_verified", 0L),
        source = prefs.getString("source", Entitlement.SOURCE_NONE)
            ?: Entitlement.SOURCE_NONE
    )

    override fun observe(): Flow<Entitlement> = state.asStateFlow()

    override suspend fun current(): Entitlement = state.value

    override suspend fun set(entitlement: Entitlement) {
        prefs.edit()
            .putBoolean("is_pro", entitlement.isPro)
            .putString("plan", entitlement.plan)
            .putLong("expiry", entitlement.expiry ?: 0L)
            .putLong("last_verified", entitlement.lastVerified)
            .putString("source", entitlement.source)
            .apply()
        state.value = entitlement
    }

    override suspend fun clear() {
        prefs.edit().clear().apply()
        state.value = Entitlement()
    }

    companion object {
        private const val PREFS = "medremind_entitlement"
    }
}
