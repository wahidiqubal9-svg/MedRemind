package com.medremind.app.ui

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds a shareable CSV adherence report for a doctor or caregiver. */
object ReportExporter {

    fun exportCsv(context: Context, log: List<DoseLogEntry>): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
        val file = File(dir, "medremind-adherence-$stamp.csv")
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val time = SimpleDateFormat("HH:mm", Locale.getDefault())
        file.bufferedWriter().use { writer ->
            writer.write("Date,Time,Medicine,Status\n")
            log.sortedByDescending { it.scheduledAt }.forEach { entry ->
                val d = Date(entry.scheduledAt)
                writer.write(
                    "${date.format(d)},${time.format(d)},${csv(entry.medicineName)},${entry.status}\n"
                )
            }
        }
        return file
    }

    private fun csv(value: String): String =
        if (value.contains(',') || value.contains('"')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value
}
