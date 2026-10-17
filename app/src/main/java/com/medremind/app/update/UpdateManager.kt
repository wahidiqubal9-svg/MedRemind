package com.medremind.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.medremind.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String = ""
)

/**
 * Checks the small version.json published on the apk branch, downloads the APK
 * and hands it to the Android system installer. This bypasses Obtainium (which
 * HyperOS blocks) while still being one tap.
 */
object UpdateManager {
    private const val VERSION_URL =
        "https://raw.githubusercontent.com/wahidiqubal9-svg/MedRemind/apk/version.json"
    private const val PREFS = "medremind_updates"
    private const val LAST_CHECK = "last_check"
    private const val CHECK_INTERVAL_MS = 5 * 60 * 1000L

    /** Returns the newer version, or null when up to date / offline. */
    suspend fun check(): AppUpdate? = withContext(Dispatchers.IO) {
        runCatching {
            // Cache-buster so a fresh release is seen immediately.
            val url = URL("$VERSION_URL?t=${System.currentTimeMillis()}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                instanceFollowRedirects = true
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val code = json.optInt("versionCode", 0)
                if (code <= BuildConfig.VERSION_CODE) return@runCatching null
                AppUpdate(
                    versionCode = code,
                    versionName = json.optString("versionName", code.toString()),
                    apkUrl = json.optString("apkUrl", ""),
                    notes = json.optString("notes", "")
                ).takeIf { it.apkUrl.isNotBlank() }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    fun shouldAutoCheck(context: Context): Boolean {
        val last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(LAST_CHECK, 0L)
        return System.currentTimeMillis() - last >= CHECK_INTERVAL_MS
    }

    fun markChecked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_CHECK, System.currentTimeMillis())
            .apply()
    }

    suspend fun download(context: Context, update: AppUpdate): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { old ->
                if (old.name != "MedRemind-${update.versionName}.apk") old.delete()
            }
            val out = File(dir, "MedRemind-${update.versionName}.apk")
            if (out.exists() && out.length() > 0) return@withContext out
            val conn = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
            }
            try {
                if (conn.responseCode !in 200..299) {
                    throw IllegalStateException("Download failed (${conn.responseCode})")
                }
                conn.inputStream.use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
            } finally {
                conn.disconnect()
            }
            out
        }

    /** True when MedRemind still needs the user to allow installing unknown apps. */
    fun needsInstallPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    /** Opens the Android installer for a downloaded APK. */
    fun install(context: Context, file: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                clipData = android.content.ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        true
    }.getOrDefault(false)
}
