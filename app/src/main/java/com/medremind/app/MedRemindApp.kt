package com.medremind.app

import android.app.Application
import com.medremind.app.alarm.AlarmNotifier
import com.medremind.app.alarm.ReminderScheduler
import com.medremind.app.data.caregiver.CloudSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedRemindApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AlarmNotifier.ensureChannel(this)
        CoroutineScope(Dispatchers.IO).launch {
            ReminderScheduler.rescheduleAll(this@MedRemindApp)
        }
        // Keep caregiver links + incoming reminders in step with the cloud.
        CloudSyncManager.start(this)

        // Also poll periodically so a reminder still fires if the app was closed.
        runCatching {
            val request = androidx.work.PeriodicWorkRequestBuilder<
                com.medremind.app.alarm.ReminderPollWorker
                >(15, java.util.concurrent.TimeUnit.MINUTES).build()
            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "reminder_poll",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
