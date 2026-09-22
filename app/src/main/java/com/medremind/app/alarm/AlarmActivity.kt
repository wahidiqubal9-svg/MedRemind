package com.medremind.app.alarm

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.DoseStatus
import com.medremind.app.ui.MedRemindTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmActivity : ComponentActivity() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var snoozeMinutes: Int = 5
    private var acted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        snoozeMinutes = getSharedPreferences("medremind_settings", MODE_PRIVATE)
            .getInt("snooze_minutes", 5)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val doseEventId = intent.getLongExtra("doseEventId", -1L)
        startSoundAndVibration()
        enableEdgeToEdge()
        setContent {
            MedRemindTheme {
                AlarmScreen(
                    doseEventId = doseEventId,
                    snoozeMinutes = snoozeMinutes,
                    onAction = { action -> handleAction(doseEventId, action) }
                )
            }
        }
    }

    private fun startSoundAndVibration() {
        val soundKey = getSharedPreferences("medremind_settings", MODE_PRIVATE)
            .getString("alarm_sound", "alarm") ?: "alarm"
        if (soundKey != "none") {
            runCatching {
                val uri = when (soundKey) {
                    "ringtone" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    "notification" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
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

    private fun bringBack() {
        if (acted) return
        runCatching {
            startActivity(
                Intent(this, AlarmActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra("doseEventId", intent.getLongExtra("doseEventId", -1L))
            )
        }
    }

    // The alarm cannot be dismissed by Home or Recents; it comes back until the
    // user taps Taken / Skipped / Snooze.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!acted) bringBack()
    }

    override fun onStop() {
        super.onStop()
        if (!acted) {
            Handler(Looper.getMainLooper()).postDelayed({ bringBack() }, 350)
        }
    }

    private fun handleAction(doseEventId: Long, action: String) {
        acted = true
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
        val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
        ReminderScheduler.scheduleSnooze(context, doseEventId, triggerAt)
    }

    override fun onDestroy() {
        stopSoundAndVibration()
        super.onDestroy()
    }
}
