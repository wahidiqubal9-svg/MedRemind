package com.medremind.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.DoseEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(ReminderScheduler.EXTRA_SCHEDULE_ID, -1L)
        val snoozeEventId = intent.getLongExtra(ReminderScheduler.EXTRA_SNOOZE_EVENT_ID, -1L)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(context)
                if (snoozeEventId > 0L) {
                    AlarmNotifier.show(context, snoozeEventId)
                } else if (scheduleId > 0L) {
                    val schedule = db.scheduleDao().byId(scheduleId)
                    if (schedule != null && schedule.enabled) {
                        val eventId = db.doseEventDao().insert(
                            DoseEvent(
                                scheduleId = scheduleId,
                                medicineId = schedule.medicineId,
                                scheduledAt = System.currentTimeMillis()
                            )
                        )
                        AlarmNotifier.show(context, eventId)
                        ReminderScheduler.schedule(context, schedule)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
