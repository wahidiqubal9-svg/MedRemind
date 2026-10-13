package com.medremind.app.alarm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.medremind.app.data.caregiver.CloudSyncManager

/**
 * Periodically checks for caregiver reminders addressed to this (patient) device
 * so one still fires even if the app was closed when it was sent.
 */
class ReminderPollWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        runCatching { CloudSyncManager.processPendingReminders(applicationContext) }
        return Result.success()
    }
}
