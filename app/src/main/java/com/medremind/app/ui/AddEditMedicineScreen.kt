package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.medremind.app.data.Medicine
import com.medremind.app.data.PhotoStorage
import com.medremind.app.data.Schedule
import com.medremind.app.data.ScheduleType
import java.io.File

@Composable
fun AddEditMedicineScreen(
    initial: Medicine?,
    vm: MedicineViewModel,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var strength by rememberSaveable { mutableStateOf(initial?.strength ?: "") }
    var notes by rememberSaveable { mutableStateOf(initial?.notes ?: "") }
    var photoPath by rememberSaveable { mutableStateOf(initial?.photoPath) }
    var schedules by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var pendingFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(initial?.id) {
        schedules = if (initial != null) {
            vm.schedulesFor(initial.id).ifEmpty { listOf(defaultSchedule()) }
        } else {
            listOf(defaultSchedule())
        }
    }

    BackHandler { onCancel() }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) photoPath = pendingFile?.absolutePath
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photoPath = PhotoStorage.copyToInternal(context, uri)
    }

    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            ScreenHeader(
                title = if (initial == null) "Add medicine" else "Edit medicine",
                onBack = onCancel,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            StepCard(
                step = 1,
                icon = Icons.Rounded.PhotoCamera,
                title = "Photo",
                subtitle = "A clear photo helps you recognise it"
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val photo = photoPath
                        if (photo != null) {
                            AsyncImage(
                                model = File(photo),
                                contentDescription = "Medicine photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoCamera,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No photo yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    GradientPillButton(
                        text = "Take photo",
                        icon = Icons.Rounded.PhotoCamera,
                        onClick = {
                            val file = PhotoStorage.newPhotoFile(context)
                            pendingFile = file
                            val uri = FileProvider.getUriForFile(
                                context,
                                context.packageName + ".fileprovider",
                                file
                            )
                            takePicture.launch(uri)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Choose")
                    }
                }
            }

            StepCard(
                step = 2,
                icon = Icons.Rounded.Medication,
                title = "Details",
                subtitle = "Name, strength and notes"
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Medication, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = strength,
                    onValueChange = { strength = it },
                    label = { Text("Strength / dose (e.g. 500 mg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Notes, contentDescription = null)
                    },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            StepCard(
                step = 3,
                icon = Icons.Rounded.Schedule,
                title = "Schedule",
                subtitle = "When should we remind you?"
            ) {
                schedules.forEachIndexed { index, schedule ->
                    InlineScheduleEditor(
                        schedule = schedule,
                        canRemove = schedules.size > 1,
                        onChange = { updated ->
                            schedules = schedules.toMutableList().also { it[index] = updated }
                        },
                        onRemove = {
                            schedules = schedules.filterIndexed { i, _ -> i != index }
                        }
                    )
                    if (index != schedules.lastIndex) {
                        Spacer(Modifier.height(10.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { schedules = schedules + defaultSchedule() },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Add another schedule")
                }
            }

            Spacer(Modifier.height(20.dp))

            GradientPillButton(
                text = "Save",
                icon = Icons.Rounded.Add,
                onClick = {
                    val base = initial ?: Medicine(name = "")
                    val medicine = base.copy(
                        name = name.trim(),
                        strength = strength.trim(),
                        notes = notes.trim(),
                        photoPath = photoPath
                    )
                    vm.saveMedicine(medicine, schedules) { onDone() }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            if (initial != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { vm.deleteMedicine(initial) { onDone() } },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Delete medicine")
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    step: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    MedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(9.dp)
                        .size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Step $step · $title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun InlineScheduleEditor(
    schedule: Schedule,
    canRemove: Boolean,
    onChange: (Schedule) -> Unit,
    onRemove: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypeChip(
                    icon = Icons.Rounded.Schedule,
                    label = "Daily",
                    selected = schedule.type == ScheduleType.DAILY
                ) { onChange(schedule.copy(type = ScheduleType.DAILY)) }
                TypeChip(
                    icon = Icons.Rounded.CalendarMonth,
                    label = "Weekdays",
                    selected = schedule.type == ScheduleType.WEEKDAYS
                ) { onChange(schedule.copy(type = ScheduleType.WEEKDAYS)) }
                TypeChip(
                    icon = Icons.Rounded.Repeat,
                    label = "Interval",
                    selected = schedule.type == ScheduleType.INTERVAL
                ) { onChange(schedule.copy(type = ScheduleType.INTERVAL)) }
                TypeChip(
                    icon = Icons.Rounded.CalendarMonth,
                    label = "Course",
                    selected = schedule.type == ScheduleType.COURSE
                ) { onChange(schedule.copy(type = ScheduleType.COURSE)) }

                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remove schedule",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (schedule.type == ScheduleType.INTERVAL) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Take every",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(4, 6, 8, 12, 24).forEach { hours ->
                        PillToggle(
                            label = "${hours}h",
                            selected = schedule.intervalHours == hours
                        ) { onChange(schedule.copy(intervalHours = hours)) }
                    }
                }
            } else {
                if (schedule.type == ScheduleType.WEEKDAYS) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "On days",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val labels = listOf("M", "T", "W", "T", "F", "S", "S")
                        labels.forEachIndexed { index, label ->
                            val selected = (schedule.daysMask and (1 shl index)) != 0
                            DayToggle(
                                label = label,
                                selected = selected,
                                onClick = {
                                    val newMask = if (selected) {
                                        schedule.daysMask and (1 shl index).inv()
                                    } else {
                                        schedule.daysMask or (1 shl index)
                                    }
                                    onChange(schedule.copy(daysMask = newMask))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Times",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    parseTimes(schedule.times).forEach { time ->
                        TimeChip(
                            text = formatTimeLabel(time),
                            onRemove = {
                                onChange(schedule.copy(times = removeTime(schedule.times, time)))
                            }
                        )
                    }
                    Surface(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Add time", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = schedule.doseLabel,
                onValueChange = { onChange(schedule.copy(doseLabel = it)) },
                label = { Text("Dose label (e.g. 1 tablet)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                onChange(schedule.copy(times = addTime(schedule.times, hour, minute)))
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val state = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        }
    )
}

@Composable
private fun TypeChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PillToggle(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun DayToggle(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(modifier = Modifier.height(38.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TimeChip(text: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove time",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun defaultSchedule() = Schedule(
    medicineId = 0L,
    type = ScheduleType.DAILY,
    times = "08:00",
    enabled = true
)

private fun parseTimes(times: String): List<String> =
    times.split(',').map { it.trim() }.filter { it.isNotEmpty() }

private fun addTime(times: String, hour: Int, minute: Int): String {
    val newTime = String.format("%02d:%02d", hour, minute)
    val existing = parseTimes(times).toMutableList()
    if (!existing.contains(newTime)) existing.add(newTime)
    return existing.sorted().joinToString(",")
}

private fun removeTime(times: String, time: String): String =
    parseTimes(times).filter { it != time }.joinToString(",")

private fun formatTimeLabel(time: String): String {
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
