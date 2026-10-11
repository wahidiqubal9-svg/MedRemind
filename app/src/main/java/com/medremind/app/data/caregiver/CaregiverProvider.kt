package com.medremind.app.data.caregiver

import android.content.Context

/**
 * Single place that decides which caregiver implementations are used. Swap the
 * local implementations here to plug in a cloud backend later.
 */
object CaregiverProvider {

    @Volatile private var caregiver: CaregiverRepository? = null
    @Volatile private var connection: ConnectionRepository? = null
    @Volatile private var reminders: RemoteReminderService? = null

    val syncRepository: SyncRepository = LocalSyncRepository()
    val pushRegistrar: PushRegistrar = NoopPushRegistrar()

    fun caregiverRepository(context: Context): CaregiverRepository {
        val app = context.applicationContext
        return caregiver ?: synchronized(this) {
            caregiver ?: LocalCaregiverRepository(app).also { caregiver = it }
        }
    }

    fun connectionRepository(context: Context): ConnectionRepository {
        val app = context.applicationContext
        return connection ?: synchronized(this) {
            // Cloud-backed pairing; degrades to local when signed out/unconfigured.
            connection ?: FirebaseConnectionRepository(app).also { connection = it }
        }
    }

    fun reminderService(context: Context): RemoteReminderService {
        val app = context.applicationContext
        return reminders ?: synchronized(this) {
            // Cloud-first: reaches the patient's phone when linked cross-device,
            // otherwise falls back to the local alarm pipeline.
            reminders ?: CloudRemoteReminderService(app).also { reminders = it }
        }
    }
}
