package com.medremind.app.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds shareable adherence reports (CSV and PDF) for a doctor or caregiver. */
object ReportExporter {

    fun exportCsv(context: Context, log: List<DoseLogEntry>): File {
        val file = File(exportsDir(context), "medremind-adherence-${stamp()}.csv")
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

    fun exportPdf(
        context: Context,
        log: List<DoseLogEntry>,
        rangeDays: Int
    ): File {
        val ordered = log.sortedByDescending { it.scheduledAt }
        val taken = ordered.count { it.status == "TAKEN" }
        val missed = ordered.count { it.status == "MISSED" }
        val skipped = ordered.count { it.status == "SKIPPED" }
        val due = taken + missed + skipped
        val percent = if (due == 0) 0 else taken * 100 / due

        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f

        val titlePaint = Paint().apply {
            color = Color.rgb(49, 46, 129)
            textSize = 20f
            isFakeBoldText = true
        }
        val subPaint = Paint().apply {
            color = Color.rgb(107, 114, 128)
            textSize = 11f
        }
        val labelPaint = Paint().apply {
            color = Color.rgb(17, 24, 39)
            textSize = 12f
        }
        val boldPaint = Paint().apply {
            color = Color.rgb(17, 24, 39)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(229, 231, 235)
            strokeWidth = 1f
        }

        val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val time = SimpleDateFormat("HH:mm", Locale.getDefault())

        var pageNumber = 1
        var page = doc.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        var canvas = page.canvas
        var y = margin + 10f

        canvas.drawText("MedRemind adherence report", margin, y, titlePaint)
        y += 18f
        canvas.drawText("Last $rangeDays days \u00b7 generated ${stamp()}", margin, y, subPaint)
        y += 30f
        canvas.drawText("Taken: $taken", margin, y, labelPaint)
        canvas.drawText("Missed: $missed", margin + 130f, y, labelPaint)
        canvas.drawText("Skipped: $skipped", margin + 260f, y, labelPaint)
        canvas.drawText("Adherence: $percent%", margin + 390f, y, boldPaint)
        y += 26f

        canvas.drawText("Date", margin, y, boldPaint)
        canvas.drawText("Time", margin + 110f, y, boldPaint)
        canvas.drawText("Medicine", margin + 175f, y, boldPaint)
        canvas.drawText("Status", margin + 420f, y, boldPaint)
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 16f

        fun newPage() {
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            canvas = page.canvas
            y = margin
        }

        ordered.forEach { entry ->
            if (y > pageHeight - margin - 20f) newPage()
            val d = Date(entry.scheduledAt)
            canvas.drawText(date.format(d), margin, y, labelPaint)
            canvas.drawText(time.format(d), margin + 110f, y, labelPaint)
            canvas.drawText(entry.medicineName.take(38), margin + 175f, y, labelPaint)
            canvas.drawText(entry.status, margin + 420f, y, labelPaint)
            y += 18f
        }

        if (ordered.isEmpty()) {
            canvas.drawText("No doses in this period.", margin, y, subPaint)
        }

        doc.finishPage(page)
        val file = File(exportsDir(context), "medremind-report-${stamp()}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun exportsDir(context: Context): File =
        File(context.cacheDir, "exports").apply { mkdirs() }

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())

    private fun csv(value: String): String =
        if (value.contains(',') || value.contains('"')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value
}
