package com.medremind.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Local, offline backup and restore. Produces a self-contained .zip with a JSON
 * snapshot of the database plus any medicine photos, so nothing ever leaves the
 * device unless the user explicitly shares it.
 */
object BackupManager {

    private const val DATA_ENTRY = "data.json"
    private const val PHOTO_PREFIX = "photos/"

    data class Summary(val medicines: Int, val schedules: Int, val events: Int, val photos: Int)

    suspend fun export(context: Context, uri: Uri): Summary {
        val db = AppDatabase.get(context)
        val medicines = db.medicineDao().getAllOnce()
        val schedules = db.scheduleDao().getAllOnce()
        val events = db.doseEventDao().getAllOnce()
        val metrics = db.metricDao().getAllOnce()

        val json = JSONObject().apply {
            put("version", 3)
            put("exportedAt", System.currentTimeMillis())
            put("medicines", JSONArray().apply {
                medicines.forEach { m ->
                    put(JSONObject().apply {
                        put("id", m.id)
                        put("name", m.name)
                        put("strength", m.strength)
                        put("notes", m.notes)
                        put("createdAt", m.createdAt)
                        put("quantity", m.quantity)
                        put("refillThreshold", m.refillThreshold)
                        put("intakeInstruction", m.intakeInstruction)
                        put("category", m.category)
                        put("form", m.form)
                        put("prescriber", m.prescriber)
                        put("rxNumber", m.rxNumber)
                        put("refillsLeft", m.refillsLeft)
                        put("packSize", m.packSize)
                        put("autoRefillDate", m.autoRefillDate ?: JSONObject.NULL)
                        put("photoName", m.photoPath?.let { File(it).name })
                    })
                }
            })
            put("schedules", JSONArray().apply {
                schedules.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("medicineId", s.medicineId)
                        put("type", s.type)
                        put("times", s.times)
                        put("daysMask", s.daysMask)
                        put("intervalHours", s.intervalHours)
                        put("startDate", s.startDate)
                        put("endDate", s.endDate ?: JSONObject.NULL)
                        put("doseLabel", s.doseLabel)
                        put("enabled", s.enabled)
                    })
                }
            })
            put("events", JSONArray().apply {
                events.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("scheduleId", e.scheduleId)
                        put("medicineId", e.medicineId)
                        put("scheduledAt", e.scheduledAt)
                        put("status", e.status)
                        put("actedAt", e.actedAt ?: JSONObject.NULL)
                        put("snoozeCount", e.snoozeCount)
                    })
                }
            })
            put("metrics", JSONArray().apply {
                metrics.forEach { m ->
                    put(JSONObject().apply {
                        put("id", m.id)
                        put("type", m.type)
                        put("value", m.value.toDouble())
                        put("value2", m.value2.toDouble())
                        put("recordedAt", m.recordedAt)
                    })
                }
            })
        }

        var photos = 0
        val out = context.contentResolver.openOutputStream(uri, "wt")
            ?: error("Could not open the selected location")
        out.use {
            ZipOutputStream(it).use { zip ->
                zip.putNextEntry(ZipEntry(DATA_ENTRY))
                zip.write(json.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                medicines.forEach { m ->
                    val file = m.photoPath?.let { p -> File(p) }
                    if (file != null && file.exists()) {
                        zip.putNextEntry(ZipEntry(PHOTO_PREFIX + file.name))
                        file.inputStream().use { input -> input.copyTo(zip) }
                        zip.closeEntry()
                        photos++
                    }
                }
            }
        }
        return Summary(medicines.size, schedules.size, events.size, photos)
    }

    suspend fun import(context: Context, uri: Uri): Summary {
        val entries = mutableMapOf<String, ByteArray>()
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Could not open the selected file")
        input.use {
            ZipInputStream(it).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        val raw = entries[DATA_ENTRY]?.toString(Charsets.UTF_8)
            ?: error("This does not look like a MedRemind backup")
        val json = JSONObject(raw)

        val photoDir = File(context.filesDir, "med_photos").apply { mkdirs() }
        val photoPaths = mutableMapOf<String, String>()
        entries.filterKeys { it.startsWith(PHOTO_PREFIX) }.forEach { (name, bytes) ->
            val file = File(photoDir, name.removePrefix(PHOTO_PREFIX))
            file.writeBytes(bytes)
            photoPaths[file.name] = file.absolutePath
        }

        val medicines = mutableListOf<Medicine>()
        val meds = json.optJSONArray("medicines") ?: JSONArray()
        for (i in 0 until meds.length()) {
            val o = meds.getJSONObject(i)
            val photoName = if (o.isNull("photoName")) null else o.optString("photoName")
            medicines.add(
                Medicine(
                    id = o.optLong("id"),
                    name = o.optString("name"),
                    strength = o.optString("strength"),
                    notes = o.optString("notes"),
                    photoPath = photoName?.let { photoPaths[it] },
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    quantity = o.optInt("quantity", 0),
                    refillThreshold = o.optInt("refillThreshold", 0),
                    intakeInstruction = o.optString("intakeInstruction", ""),
                    category = o.optString("category", MedicineCategory.PRESCRIPTION),
                    form = o.optString("form", MedicineForm.TABLET),
                    prescriber = o.optString("prescriber", ""),
                    rxNumber = o.optString("rxNumber", ""),
                    refillsLeft = o.optInt("refillsLeft", 0),
                    packSize = o.optInt("packSize", 0),
                    autoRefillDate = if (o.isNull("autoRefillDate")) null else o.optLong("autoRefillDate")
                )
            )
        }

        val schedules = mutableListOf<Schedule>()
        val scheds = json.optJSONArray("schedules") ?: JSONArray()
        for (i in 0 until scheds.length()) {
            val o = scheds.getJSONObject(i)
            schedules.add(
                Schedule(
                    id = o.optLong("id"),
                    medicineId = o.optLong("medicineId"),
                    type = o.optString("type", ScheduleType.DAILY),
                    times = o.optString("times", "08:00"),
                    daysMask = o.optInt("daysMask", 0),
                    intervalHours = o.optInt("intervalHours", 0),
                    startDate = o.optLong("startDate", 0),
                    endDate = if (o.isNull("endDate")) null else o.optLong("endDate"),
                    doseLabel = o.optString("doseLabel"),
                    enabled = o.optBoolean("enabled", true)
                )
            )
        }

        val events = mutableListOf<DoseEvent>()
        val evs = json.optJSONArray("events") ?: JSONArray()
        for (i in 0 until evs.length()) {
            val o = evs.getJSONObject(i)
            events.add(
                DoseEvent(
                    id = o.optLong("id"),
                    scheduleId = o.optLong("scheduleId"),
                    medicineId = o.optLong("medicineId"),
                    scheduledAt = o.optLong("scheduledAt"),
                    status = o.optString("status", DoseStatus.PENDING),
                    actedAt = if (o.isNull("actedAt")) null else o.optLong("actedAt"),
                    snoozeCount = o.optInt("snoozeCount", 0)
                )
            )
        }

        val metrics = mutableListOf<Metric>()
        val mets = json.optJSONArray("metrics") ?: JSONArray()
        for (i in 0 until mets.length()) {
            val o = mets.getJSONObject(i)
            metrics.add(
                Metric(
                    id = o.optLong("id"),
                    type = o.optString("type"),
                    value = o.optDouble("value", 0.0).toFloat(),
                    value2 = o.optDouble("value2", 0.0).toFloat(),
                    recordedAt = o.optLong("recordedAt", System.currentTimeMillis())
                )
            )
        }

        val db = AppDatabase.get(context)
        db.doseEventDao().clear()
        db.scheduleDao().clear()
        db.medicineDao().clear()
        db.metricDao().clear()
        db.medicineDao().insertAll(medicines)
        db.scheduleDao().insertAll(schedules)
        db.doseEventDao().insertAll(events)
        db.metricDao().insertAll(metrics)

        return Summary(medicines.size, schedules.size, events.size, photoPaths.size)
    }
}
