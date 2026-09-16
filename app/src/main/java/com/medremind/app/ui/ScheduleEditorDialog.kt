package com.medremind.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medremind.app.data.Schedule
import com.medremind.app.data.ScheduleType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorDialog(
    initial: Schedule?,
    onDismiss: () -> Unit,
    onSave: (Schedule) -> Unit,
    onDelete: () -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }

    fun formatDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toString()

    fun parseDate(text: String): Long? = runCatching {
        LocalDate.parse(text.trim()).atStartOfDay(zone).toInstant().toEpochMilli()
    }.getOrNull()

    var type by remember { mutableStateOf(initial?.type ?: ScheduleType.DAILY) }
    var times by remember {
        mutableStateOf(
            (initial?.times ?: "").split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableList()
        )
    }
    var daysMask by remember { mutableStateOf(initial?.daysMask ?: 0) }
    var intervalHours by remember { mutableStateOf((initial?.intervalHours ?: 8).toString()) }
    var startDate by remember {
        mutableStateOf(initial?.startDate?.takeIf { it > 0 }?.let { formatDate(it) } ?: "")
    }
    var endDate by remember {
        mutableStateOf(initial?.endDate?.let { formatDate(it) } ?: "")
    }
    var doseLabel by remember { mutableStateOf(initial?.doseLabel ?: "") }

    var showTimePicker by remember { mutableStateOf(false) }
    var editingTimeIndex by remember { mutableIntStateOf(-1) }

    val needsTimes = type != ScheduleType.INTERVAL
    val canSave = !needsTimes || times.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add reminder" else "Edit reminder") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Type")
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TypeChip("Daily", type == ScheduleType.DAILY) { type = ScheduleType.DAILY }
                    TypeChip("Days", type == ScheduleType.WEEKDAYS) { type = ScheduleType.WEEKDAYS }
                    TypeChip("Interval", type == ScheduleType.INTERVAL) { type = ScheduleType.INTERVAL }
                    TypeChip("Course", type == ScheduleType.COURSE) { type = ScheduleType.COURSE }
                }

                Spacer(Modifier.height(12.dp))

                if (type == ScheduleType.INTERVAL) {
                    OutlinedTextField(
                        value = intervalHours,
                        onValueChange = { intervalHours = it.filter { c -> c.isDigit() } },
                        label = { Text("Every N hours") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Times")
                    Spacer(Modifier.height(4.dp))
                    if (times.isEmpty()) {
                        Text(
                            "No times yet — tap \"Add time\".",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        times.forEachIndexed { index, time ->
                            FilterChip(
                                selected = true,
                                onClick = {
                                    editingTimeIndex = index
                                    showTimePicker = true
                                },
                                label = { Text("${to12Hour(time)}  \u2715") }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            editingTimeIndex = -1
                            showTimePicker = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add time")
                    }
                    Text(
                        "Tap a time to change or remove it.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }

                if (type == ScheduleType.WEEKDAYS) {
                    Spacer(Modifier.height(12.dp))
                    Text("Days")
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        dayLabels.forEachIndexed { index, label ->
                            val selected = (daysMask and (1 shl index)) != 0
                            TypeChip(label, selected) {
                                daysMask = if (selected) {
                                    daysMask and (1 shl index).inv()
                                } else {
                                    daysMask or (1 shl index)
                                }
                            }
                        }
                    }
                }

                if (type == ScheduleType.COURSE) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = doseLabel,
                    onValueChange = { doseLabel = it },
                    label = { Text("Dose note (e.g. 1 tablet)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val base = initial ?: Schedule(medicineId = 0L)
                    onSave(
                        base.copy(
                            type = type,
                            times = times.joinToString(","),
                            daysMask = daysMask,
                            intervalHours = intervalHours.toIntOrNull() ?: 8,
                            startDate = parseDate(startDate) ?: 0L,
                            endDate = parseDate(endDate),
                            doseLabel = doseLabel.trim(),
                            enabled = true
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            if (initial != null) {
                TextButton(onClick = onDelete) { Text("Remove") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )

    if (showTimePicker) {
        val current = times.getOrNull(editingTimeIndex)
        val parts = current?.split(':')
        val now = java.time.LocalTime.now()
        val initHour = parts?.getOrNull(0)?.toIntOrNull() ?: now.hour
        val initMinute = parts?.getOrNull(1)?.toIntOrNull() ?: now.minute
        TimePickerDialog(
            initialHour = initHour,
            initialMinute = initMinute,
            allowRemove = editingTimeIndex in times.indices && times.size > 1,
            onConfirm = { hour, minute ->
                val formatted = String.format("%02d:%02d", hour, minute)
                if (editingTimeIndex in times.indices) {
                    times = times.toMutableList().also { it[editingTimeIndex] = formatted }
                } else {
                    times = (times + formatted).distinct().sorted().toMutableList()
                }
                showTimePicker = false
            },
            onRemove = {
                if (editingTimeIndex in times.indices) {
                    times = times.toMutableList().also { it.removeAt(editingTimeIndex) }
                }
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    allowRemove: Boolean,
    onConfirm: (Int, Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select time") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (allowRemove) {
                    TextButton(onClick = onRemove) { Text("Remove") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

private val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun to12Hour(time: String): String {
    val parts = time.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return time
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val suffix = if (hour < 12) "AM" else "PM"
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%d:%02d %s", h, minute, suffix)
}
