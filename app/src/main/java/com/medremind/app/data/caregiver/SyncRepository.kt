package com.medremind.app.data.caregiver

/**
 * Placeholder for future two-way synchronisation.
 *
 * There is no cloud yet, so this reports that sync is unavailable rather than
 * pretending data was uploaded or downloaded.
 */
interface SyncRepository {
    val isCloudAvailable: Boolean
    suspend fun requestSync(): Boolean
}

class LocalSyncRepository : SyncRepository {
    override val isCloudAvailable: Boolean = false
    override suspend fun requestSync(): Boolean = false
}

/**
 * Registration hook for push messaging. Intentionally a no-op until a backend
 * exists, so the app never claims remote notifications were set up.
 */
interface PushRegistrar {
    val isAvailable: Boolean
    suspend fun register(): Boolean
}

class NoopPushRegistrar : PushRegistrar {
    override val isAvailable: Boolean = false
    override suspend fun register(): Boolean = false
}
