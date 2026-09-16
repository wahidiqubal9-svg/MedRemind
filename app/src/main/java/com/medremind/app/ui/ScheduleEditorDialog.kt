package com.medremind.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalLayoutApi::class)
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
    var times by remember { mutableStateOf(initial?.times ?: "08:00") }
    var daysMask by remember { mutableStateOf(initial?.daysMask ?: 0) }
    var intervalHours by remember { mutableStateOf((initial?.intervalHours ?: 8).toString()) }
    var startDate by remember {
        mutableStateOf(initial?.startDate?.takeIf { it > 0 }?.let { formatDate(it) } ?: "")
    }
    var endDate by remember {
        mutableStateOf(initial?.endDate?.let { formatDate(it) } ?: "")
    }
    var doseLabel by remember { mutableStateOf(initial?.doseLabel ?: "") }

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

                when (type) {
                    ScheduleType.INTERVAL -> {
                        OutlinedTextField(
                            value = intervalHours,
                            onValueChange = { intervalHours = it.filter { c -> c.isDigit() } },
                            label = { Text("Every N hours") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        OutlinedTextField(
                            value = times,
                            onValueChange = { times = it },
                            label = { Text("Times (HH:mm, comma separated)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                onClick = {
                    val base = initial ?: Schedule(medicineId = 0L)
                    onSave(
                        base.copy(
                            type = type,
                            times = times.trim(),
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
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

private val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
