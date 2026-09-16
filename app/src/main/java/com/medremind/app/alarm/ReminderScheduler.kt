package com.medremind.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.medremind.app.MainActivity
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.Schedule
import com.medremind.app.data.ScheduleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun parseTimes(times: String): List<LocalTime> =
        times.split(',').mapNotNull { part ->
            val t = part.trim()
            if (t.isEmpty()) null
            else runCatching {
                val hm = t.split(':')
                LocalTime.of(hm[0].trim().toInt(), hm.getOrElse(1) { "0" }.trim().toInt())
            }.getOrNull()
        }

    private fun toMillis(date: LocalDate, time: LocalTime): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    private fun dateOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun nextTrigger(schedule: Schedule, fromMillis: Long): Long? {
        if (!schedule.enabled) return null
        val from = Instant.ofEpochMilli(fromMillis).atZone(zone)
        val startDate = if (schedule.startDate > 0) dateOf(schedule.startDate) else null
        val endDate = schedule.endDate?.let { dateOf(it) }

        if (schedule.type == ScheduleType.INTERVAL) {
            val hours = schedule.intervalHours.coerceAtLeast(1)
            val anchor = if (schedule.startDate > 0) schedule.startDate else fromMillis
            val step = hours * 3_600_000L
            var next = anchor
            if (next <= fromMillis) {
                val k = ((fromMillis - anchor) / step) + 1
                next = anchor + k * step
            }
            if (endDate != null && dateOf(next).isAfter(endDate)) return null
            return next
        }

        val times = parseTimes(schedule.times).sorted()
        if (times.isEmpty()) return null

        var day = from.toLocalDate()
        var i = 0
        while (i < 8) {
            val withinRange = (startDate == null || !day.isBefore(startDate)) &&
                (endDate == null || !day.isAfter(endDate))
            if (withinRange) {
                val dayOk = if (schedule.type == ScheduleType.WEEKDAYS) {
                    val bit = 1 shl (day.dayOfWeek.value - 1)
                    (schedule.daysMask and bit) != 0
                } else true
                if (dayOk) {
                    for (t in times) {
                        val ms = toMillis(day, t)
                        if (ms > fromMillis) return ms
                    }
                }
            }
            day = day.plusDays(1)
            i++
        }
        return null
    }

    fun occurrencesOn(schedule: Schedule, date: LocalDate): List<Long> {
        if (!schedule.enabled) return emptyList()
        val startDate = if (schedule.startDate > 0) dateOf(schedule.startDate) else null
        val endDate = schedule.endDate?.let { dateOf(it) }
        if (startDate != null && date.isBefore(startDate)) return emptyList()
        if (endDate != null && date.isAfter(endDate)) return emptyList()

        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = dayStart + 86_400_000L

        return when (schedule.type) {
            ScheduleType.INTERVAL -> {
                val anchor = schedule.startDate
                if (anchor <= 0L) return emptyList()
                val step = schedule.intervalHours.coerceAtLeast(1) * 3_600_000L
                val result = mutableListOf<Long>()
                var k = if (dayStart <= anchor) 0L else (dayStart - anchor) / step
                var t = anchor + k * step
                var guard = 0
                while (t < dayEnd && guard < 200) {
                    if (t >= dayStart) result.add(t)
                    k++
                    t = anchor + k * step
                    guard++
                }
                result
            }
            ScheduleType.WEEKDAYS -> {
                val bit = 1 shl (date.dayOfWeek.value - 1)
                if ((schedule.daysMask and bit) == 0) emptyList()
                else parseTimes(schedule.times).map { toMillis(date, it) }
            }
            else -> parseTimes(schedule.times).map { toMillis(date, it) }
        }
    }

    private fun showAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun pendingIntent(context: Context, scheduleId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
        }
        return PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context, schedule: Schedule) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context, schedule.id)
        val next = nextTrigger(schedule, System.currentTimeMillis())
        if (next == null) {
            am.cancel(pi)
            return
        }
        if (canScheduleExact(am)) {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(next, showAppIntent(context)), pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        }
    }

    fun scheduleSnooze(context: Context, doseEventId: Long, triggerAt: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_SNOOZE_EVENT_ID, doseEventId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            (doseEventId + 100_000L).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (canScheduleExact(am)) {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showAppIntent(context)), pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context, scheduleId: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pendingIntent(context, scheduleId))
    }

    fun canScheduleExact(am: AlarmManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true

    suspend fun rescheduleAll(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(context)
        db.scheduleDao().getAllOnce().forEach { schedule(context, it) }
    }

    const val ACTION_ALARM = "com.medremind.app.ALARM"
    const val ACTION_SNOOZE = "com.medremind.app.SNOOZE"
    const val EXTRA_SCHEDULE_ID = "scheduleId"
    const val EXTRA_SNOOZE_EVENT_ID = "snoozeEventId"
}
