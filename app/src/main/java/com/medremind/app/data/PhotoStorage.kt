package com.medremind.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

object PhotoStorage {

    fun newPhotoFile(context: Context): File {
        val dir = File(context.filesDir, "med_photos")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "med_${System.currentTimeMillis()}.jpg")
    }

    fun copyToInternal(context: Context, uri: Uri): String? {
        return try {
            val file = newPhotoFile(context)
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun delete(path: String?) {
        if (path == null) return
        runCatching { File(path).delete() }
    }

    /**
     * Downscales the photo and returns it as a base64 JPEG string for cloud sync
     * (kept small so it fits comfortably in a Firestore document).
     */
    fun toBase64(path: String?, maxPx: Int = 512, quality: Int = 70): String? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val file = File(path)
            if (!file.exists()) return@runCatching null
            val original = BitmapFactory.decodeFile(path) ?: return@runCatching null
            val longest = maxOf(original.width, original.height).coerceAtLeast(1)
            val bitmap = if (longest > maxPx) {
                val scale = maxPx.toFloat() / longest
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                original
            }
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    /** Writes a base64 JPEG photo to a local file and returns its path. */
    fun saveBase64(context: Context, base64: String): String? {
        return runCatching {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val file = newPhotoFile(context)
            file.outputStream().use { it.write(bytes) }
            file.absolutePath
        }.getOrNull()
    }
}
