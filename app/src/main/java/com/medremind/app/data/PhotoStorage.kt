package com.medremind.app.data

import android.content.Context
import android.net.Uri
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
}
