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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
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
    private val currentDoseEventId = mutableStateOf(-1L)
    private var medicineName: String? = null

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
        currentDoseEventId.value = intent.getLongExtra("doseEventId", -1L)
        // Remove the status-bar notification/heads-up now that the full screen is up.
        AlarmNotifier.cancel(applicationContext, currentDoseEventId.value)
        startSoundAndVibration()
        enableEdgeToEdge()
        loadMedicineName(currentDoseEventId.value)
        setContent {
            MedRemindTheme {
                key(currentDoseEventId.value) {
                    AlarmScreen(
                        doseEventId = currentDoseEventId.value,
                        snoozeMinutes = snoozeMinutes,
                        onAction = { action -> handleAction(currentDoseEventId.value, action) }
                    )
                }
            }
        }
    }

    private fun loadMedicineName(id: Long) {
        lifecycleScope.launch {
            medicineName = withContext(Dispatchers.IO) {
                val db = AppDatabase.get(applicationContext)
                val event = db.doseEventDao().byId(id)
                event?.let { db.medicineDao().byId(it.medicineId)?.name }
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
        val id = currentDoseEventId.value
        // 1) Directly bring the alarm activity forward.
        runCatching {
            startActivity(
                Intent(this, AlarmActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra("doseEventId", id)
            )
        }
        // 2) Fall back to the sanctioned full-screen-intent path (works even when
        //    background activity starts are restricted). Re-posting triggers it.
        runCatching { AlarmNotifier.show(this, id, medicineName, silent = true) }
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching { AlarmNotifier.cancel(applicationContext, id) }
        }, 900)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        runCatching { AlarmNotifier.cancel(applicationContext, currentDoseEventId.value) }
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
            var nextId: Long? = null
            withContext(Dispatchers.IO) {
                val db = AppDatabase.get(appContext)
                val dao = db.doseEventDao()
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
                // If other medicines were scheduled for the same time, line them up
                // so the patient is prompted for each one in turn.
                if (event != null && event.scheduleId != 0L) {
                    val profileId = db.medicineDao().byId(event.medicineId)?.profileId ?: 0L
                    nextId = dao.pendingBetween(event.scheduledAt - 60_000L, event.scheduledAt + 60_000L)
                        .firstOrNull { candidate ->
                            candidate.id != event.id &&
                                (db.medicineDao().byId(candidate.medicineId)?.profileId ?: 0L) == profileId
                        }?.id
                }
            }
            AlarmNotifier.cancel(appContext, doseEventId)
            val next = nextId
            if (next != null) {
                AlarmNotifier.cancel(appContext, next)
                currentDoseEventId.value = next
                loadMedicineName(next)
                acted = false
            } else {
                stopSoundAndVibration()
                finish()
            }
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
