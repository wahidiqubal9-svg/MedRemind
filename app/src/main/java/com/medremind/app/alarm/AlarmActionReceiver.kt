package com.medremind.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.DoseStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles the Taken / Snooze / Skip buttons on the reminder notification so the
 * user can act without unlocking the phone or opening the app.
 */
class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_DOSE_EVENT_ID, -1L)
        val action = intent.getStringExtra(EXTRA_ACTION)
        if (eventId <= 0L || action == null) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context).doseEventDao()
                val event = dao.byId(eventId)
                val now = System.currentTimeMillis()
                when (action) {
                    ACTION_TAKE -> event?.let {
                        dao.update(it.copy(status = DoseStatus.TAKEN, actedAt = now))
                    }
                    ACTION_SKIP -> event?.let {
                        dao.update(it.copy(status = DoseStatus.SKIPPED, actedAt = now))
                    }
                    ACTION_SNOOZE -> {
                        event?.let { dao.update(it.copy(snoozeCount = it.snoozeCount + 1)) }
                        val minutes = context
                            .getSharedPreferences("medremind_settings", Context.MODE_PRIVATE)
                            .getInt("snooze_minutes", 5)
                        ReminderScheduler.scheduleSnooze(
                            context,
                            eventId,
                            now + minutes * 60_000L
                        )
                    }
                }
                AlarmNotifier.cancel(context, eventId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TAKE = "com.medremind.app.NOTIF_TAKE"
        const val ACTION_SKIP = "com.medremind.app.NOTIF_SKIP"
        const val ACTION_SNOOZE = "com.medremind.app.NOTIF_SNOOZE"
        const val EXTRA_ACTION = "notifAction"
        const val EXTRA_DOSE_EVENT_ID = "doseEventId"
    }
}
