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
    }
}
