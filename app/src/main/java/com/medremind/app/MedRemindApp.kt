package com.medremind.app

import android.app.Application
import com.medremind.app.alarm.AlarmNotifier
import com.medremind.app.alarm.ReminderScheduler
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
    }
}
