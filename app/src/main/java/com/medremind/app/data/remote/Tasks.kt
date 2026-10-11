package com.medremind.app.data.remote

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Awaits a Google/Firebase [Task] in a coroutine. */
internal suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) cont.resume(task.result as T)
        else cont.resumeWithException(task.exception ?: RuntimeException("Task failed"))
    }
}
