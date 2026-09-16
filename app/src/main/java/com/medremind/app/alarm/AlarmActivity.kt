package com.medremind.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.DoseStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmActivity : ComponentActivity() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        val doseEventId = intent.getLongExtra("doseEventId", -1L)
        startSoundAndVibration()
        setContent {
            MaterialTheme {
                AlarmScreen(
                    doseEventId = doseEventId,
                    onAction = { action -> handleAction(doseEventId, action) }
                )
            }
        }
    }

    private fun startSoundAndVibration() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(this, uri)
            mp.isLooping = true
            mp.prepare()
            mp.start()
            player = mp
        }
        runCatching {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 800), 1))
            vibrator = vib
        }
    }

    private fun stopSoundAndVibration() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private fun handleAction(doseEventId: Long, action: String) {
        val appContext = applicationContext
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val dao = AppDatabase.get(appContext).doseEventDao()
                val event = dao.byId(doseEventId)
                when (action) {
                    "TAKEN" -> event?.let {
                        dao.update(it.copy(status = DoseStatus.TAKEN, actedAt = System.currentTimeMillis()))
                    }
                    "SKIPPED" -> event?.let {
                        dao.update(it.copy(status = DoseStatus.SKIPPED, actedAt = System.currentTimeMillis()))
                    }
                    "SNOOZE" -> {
                        event?.let { dao.update(it.copy(snoozeCount = it.snoozeCount + 1)) }
                        scheduleSnooze(appContext, doseEventId)
                    }
                }
            }
            AlarmNotifier.cancel(appContext, doseEventId)
            stopSoundAndVibration()
            finish()
        }
    }

    private fun scheduleSnooze(context: Context, doseEventId: Long) {
        val triggerAt = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
        ReminderScheduler.scheduleSnooze(context, doseEventId, triggerAt)
    }

    override fun onDestroy() {
        stopSoundAndVibration()
        super.onDestroy()
    }

    companion object {
        const val SNOOZE_MINUTES = 5L
    }
}
