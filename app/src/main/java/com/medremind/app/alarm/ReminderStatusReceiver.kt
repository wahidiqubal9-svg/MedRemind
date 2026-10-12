package com.medremind.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.firestore.FirebaseFirestore
import com.medremind.app.data.remote.awaitResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Wakes the caregiver's phone to check whether a sent "Remind now" reached the
 * patient. Fires ~5 min (not delivered yet) and ~30 min (expired) after sending.
 */
class ReminderStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return
        val kind = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_KIND) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val snap = runCatching {
                    db.collection("reminders").document(id).get().awaitResult()
                }.getOrNull() ?: return@launch
                if (snap.getString("status") != "PENDING") return@launch
                val name = snap.getString("medicineName") ?: "medicine"
                val patient = snap.getString("patientName") ?: "your patient"
                if (kind == "CHECK5") {
                    AlarmNotifier.showCaregiverAlert(
                        context,
                        ("r5_$id").hashCode(),
                        "Reminder not delivered yet",
                        "$name \u2014 $patient may be offline."
                    )
                } else {
                    runCatching { snap.reference.update("status", "EXPIRED") }
                    AlarmNotifier.showCaregiverAlert(
                        context,
                        ("r30_$id").hashCode(),
                        "Reminder expired",
                        "$name \u2014 not delivered to $patient."
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}
