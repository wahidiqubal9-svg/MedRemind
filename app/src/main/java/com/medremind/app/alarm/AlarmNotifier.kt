package com.medremind.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.medremind.app.R

object AlarmNotifier {

    const val CHANNEL_ID = "med_alarm"
    private const val PREFS = "medremind_settings"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun style(context: Context): String =
        prefs(context).getString("alarm_style", "fullscreen") ?: "fullscreen"

    fun soundKey(context: Context): String =
        prefs(context).getString("alarm_sound", "alarm") ?: "alarm"

    fun soundUri(context: Context): Uri? = when (soundKey(context)) {
        "ringtone" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        "notification" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        "none" -> null
        else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }

    private fun channelId(context: Context): String = CHANNEL_ID + "_" + soundKey(context)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val id = channelId(context)
        if (nm.getNotificationChannel(id) != null) return
        val channel = NotificationChannel(
            id,
            "Medicine alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Full-screen reminders to take medicine"
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            val uri = soundUri(context)
            if (uri != null) {
                setSound(
                    uri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            } else {
                setSound(null, null)
            }
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
    }

    private fun actionIntent(
        context: Context,
        doseEventId: Long,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, AlarmActionReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmActionReceiver.EXTRA_DOSE_EVENT_ID, doseEventId)
            putExtra(AlarmActionReceiver.EXTRA_ACTION, action)
        }
        return PendingIntent.getBroadcast(
            context,
            (doseEventId.toInt() * 10) + requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun show(context: Context, doseEventId: Long, medicineName: String? = null) {
        ensureChannel(context)
        val intent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("doseEventId", doseEventId)
        }
        val pi = PendingIntent.getActivity(
            context,
            doseEventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fullScreen = style(context) == "fullscreen"
        val builder = NotificationCompat.Builder(context, channelId(context))
            .setSmallIcon(R.drawable.ic_stat_pill)
            .setContentTitle(medicineName ?: "Medicine time")
            .setContentText("Time to take your medicine")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pi)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(
                0,
                "Taken",
                actionIntent(context, doseEventId, AlarmActionReceiver.ACTION_TAKE, 1)
            )
            .addAction(
                0,
                "Snooze",
                actionIntent(context, doseEventId, AlarmActionReceiver.ACTION_SNOOZE, 2)
            )
            .addAction(
                0,
                "Skip",
                actionIntent(context, doseEventId, AlarmActionReceiver.ACTION_SKIP, 3)
            )
        if (fullScreen) {
            builder.setFullScreenIntent(pi, true)
        }
        val notification = builder.build()

        runCatching {
            NotificationManagerCompat.from(context).notify(doseEventId.toInt(), notification)
        }
        if (fullScreen) {
            runCatching { context.startActivity(intent) }
        }
    }

    fun cancel(context: Context, doseEventId: Long) {
        runCatching { NotificationManagerCompat.from(context).cancel(doseEventId.toInt()) }
    }
}
