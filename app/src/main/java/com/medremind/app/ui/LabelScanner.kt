package com.medremind.app.ui

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.medremind.app.data.MedicineForm
import java.util.Calendar
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

data class OcrLine(val text: String, val top: Int, val height: Int)

data class OcrResult(val text: String = "", val lines: List<OcrLine> = emptyList())

data class ScannedLabel(
    val name: String = "",
    val strength: String = "",
    val form: String = MedicineForm.TABLET,
    val packSize: Int = 0,
    val batchNumber: String = "",
    val expiryDate: Long? = null,
    val rawText: String = ""
)

/**
 * On-device OCR for medicine packs/labels. Reads the printed text with ML Kit
 * (offline), then heuristically extracts the useful fields. Everything is a
 * suggestion the user reviews before saving.
 */
object LabelScanner {

    private val strengthRegex =
        Regex("(\\d+(?:\\.\\d+)?)\\s*(MG|MCG|G|ML|IU|%)", RegexOption.IGNORE_CASE)

    suspend fun recognize(context: Context, uri: Uri): OcrResult =
        suspendCancellableCoroutine { cont ->
            val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
            if (image == null) {
                cont.resume(OcrResult())
                return@suspendCancellableCoroutine
            }
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    recognizer.close()
                    val lines = mutableListOf<OcrLine>()
                    result.textBlocks.forEach { block ->
                        block.lines.forEach { line ->
                            val box = line.boundingBox
                            lines.add(
                                OcrLine(
                                    text = line.text.trim(),
                                    top = box?.top ?: 0,
                                    height = box?.height() ?: 0
                                )
                            )
                        }
                    }
                    cont.resume(OcrResult(result.text, lines))
                }
                .addOnFailureListener {
                    recognizer.close()
                    cont.resume(OcrResult())
                }
        }

    fun parse(result: OcrResult): ScannedLabel {
        val lines = if (result.lines.isNotEmpty()) {
            result.lines.map { it.text }.filter { it.isNotEmpty() }
        } else {
            result.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        }
        if (lines.isEmpty()) return ScannedLabel(rawText = result.text)

        val strengthMatch = lines.asSequence().mapNotNull { strengthRegex.find(it) }.firstOrNull()
        val strength = strengthMatch?.let {
            "${it.groupValues[1]} ${it.groupValues[2].lowercase()}"
        } ?: ""

        val form = when {
            lines.any { it.contains("softgel", true) } -> MedicineForm.SOFTGEL
            lines.any { it.contains("capsule", true) } -> MedicineForm.CAPSULE
            lines.any {
                it.contains("syrup", true) || it.contains("suspension", true) ||
                    it.contains("solution", true) || it.contains("drops", true)
            } -> MedicineForm.LIQUID
            lines.any { it.contains("injection", true) } -> MedicineForm.INJECTION
            else -> MedicineForm.TABLET
        }

        val packRegex = Regex(
            "(\\d{1,4})\\s*(TABLETS?|CAPSULES?|SOFTGELS?|TABS?|CAPS?|PIECES?|SACHETS?)",
            RegexOption.IGNORE_CASE
        )
        val packSize = lines.asSequence()
            .mapNotNull { packRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() }
            .firstOrNull() ?: 0

        val batchRegex = Regex(
            "(?:B\\.?\\s*NO\\.?|BATCH(?:\\s*NO\\.?)?|LOT(?:\\s*NO\\.?)?)\\s*[:#]?\\s*([A-Z0-9][A-Z0-9\\-/]{2,})",
            RegexOption.IGNORE_CASE
        )
        val batch = lines.asSequence()
            .mapNotNull { batchRegex.find(it)?.groupValues?.get(1)?.trim() }
            .firstOrNull() ?: ""

        val expiryRegex = Regex(
            "(?:EXP(?:\\.|IRY|IRES)?|USE\\s*BEFORE|VALID\\s*(?:TILL|UP\\s*TO|UPTO))\\s*[:.]?\\s*([A-Za-z0-9/\\- ]{4,12})",
            RegexOption.IGNORE_CASE
        )
        val expiryRaw = lines.asSequence()
            .mapNotNull { expiryRegex.find(it)?.groupValues?.get(1)?.trim() }
            .firstOrNull()
        val expiry = parseExpiry(expiryRaw)

        val name = extractName(result)

        return ScannedLabel(
            name = name,
            strength = strength,
            form = form,
            packSize = packSize,
            batchNumber = batch,
            expiryDate = expiry,
            rawText = result.text
        )
    }

    private val nameKeywords = listOf(
        "BATCH", "LOT ", "LOT#", "EXP", "MFG", "MADE", "STORE", "KEEP", "DOSAGE",
        "DIRECTIONS", "WARNING", "PRESCRIPTION", "SCHEDULE", "ADDRESS", "PHONE",
        "MRP", "PACK", "TABLET", "CAPSULE", "SOFTGEL", "SYRUP", "INJECTION",
        "MEDICINE", "GENERIC", "NET ", "CONTENT", "COMPOSITION", "STORAGE",
        "DOSE", "STRIP", "BLISTER", "PHARMA", "LABS", "LABORATOR", "LTD", "PVT",
        "LIMITED", "MANUFACTUR", "MARKET", "IMPORT", "EXPORT", "USP", "IP ",
        "TAKE", "ONCE", "DAILY", "BEFORE", "AFTER", "FOOD", "MEAL", "WATER",
        "TABLETS", "CAPSULES", "MG ", " ML", "MCG", "MMHG"
    )

    private fun extractName(result: OcrResult): String {
        val candidates = if (result.lines.isNotEmpty()) {
            result.lines
        } else {
            result.text.lines().mapIndexed { index, s ->
                OcrLine(s.trim(), top = index, height = 1)
            }
        }.filter { it.text.isNotEmpty() }

        val scored = candidates.filter { line ->
            val t = line.text.trim()
            val letters = t.count { it.isLetter() }
            val digits = t.count { it.isDigit() }
            val upper = t.uppercase()
            letters >= 3 &&
                t.length in 3..42 &&
                digits <= letters &&
                !t.contains('@') &&
                !upper.contains("WWW") &&
                nameKeywords.none { kw -> upper.contains(kw.trim()) }
        }

        val best = scored.maxWithOrNull(
            compareBy({ it.height }, { -it.top })
        )?.text?.trim() ?: scored.firstOrNull()?.text?.trim() ?: ""

        return best
            .replace(strengthRegex, "")
            .trim()
            .trim('-', ':', '|', ',', '*')
            .trim()
    }

    fun parseExpiry(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace('.', ' ').replace(',', ' ').trim().uppercase()

        Regex("(20\\d{2})[/\\- ](\\d{1,2})").find(cleaned)?.let { m ->
            val year = m.groupValues[1].toIntOrNull() ?: return null
            val month = m.groupValues[2].toIntOrNull() ?: return null
            return endOfMonth(year, month)
        }
        Regex("(\\d{1,2})[/\\- ](20\\d{2}|\\d{2})").find(cleaned)?.let { m ->
            val month = m.groupValues[1].toIntOrNull() ?: return null
            val yRaw = m.groupValues[2]
            val year = if (yRaw.length == 2) 2000 + (yRaw.toIntOrNull() ?: 0)
            else yRaw.toIntOrNull() ?: return null
            return endOfMonth(year, month)
        }
        val months = mapOf(
            "JAN" to 1, "FEB" to 2, "MAR" to 3, "APR" to 4, "MAY" to 5, "JUN" to 6,
            "JUL" to 7, "AUG" to 8, "SEP" to 9, "OCT" to 10, "NOV" to 11, "DEC" to 12
        )
        Regex("([A-Z]{3})[A-Z]*\\s*(\\d{2,4})").find(cleaned)?.let { m ->
            val month = months[m.groupValues[1]] ?: return null
            val yRaw = m.groupValues[2]
            val year = if (yRaw.length == 2) 2000 + (yRaw.toIntOrNull() ?: 0)
            else yRaw.toIntOrNull() ?: return null
            return endOfMonth(year, month)
        }
        return null
    }

    private fun endOfMonth(year: Int, month: Int): Long {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, (month - 1).coerceIn(0, 11), 1)
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        return cal.timeInMillis
    }
}
