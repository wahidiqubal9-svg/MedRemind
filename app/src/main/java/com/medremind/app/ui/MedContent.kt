package com.medremind.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.medremind.app.data.Medicine
import com.medremind.app.data.Schedule
import com.medremind.app.data.ScheduleType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MedContent(
    modifier: Modifier = Modifier,
    medicines: List<Medicine>,
    schedulesByMedicine: Map<Long, List<Schedule>>,
    onEdit: (Medicine) -> Unit,
    onDelete: (Medicine) -> Unit,
    onAdd: () -> Unit
) {
    if (medicines.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            MedEmptyState(
                icon = Icons.Outlined.Medication,
                title = "No medicines yet",
                message = "Add your first medicine to start tracking doses.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                action = {
                    GradientPillButton(
                        text = "Add medicine",
                        icon = Icons.Rounded.Add,
                        onClick = onAdd
                    )
                }
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(medicines, key = { it.id }) { medicine ->
            MedicineCard(
                medicine = medicine,
                schedules = schedulesByMedicine[medicine.id].orEmpty(),
                onClick = { onEdit(medicine) },
                onEdit = { onEdit(medicine) },
                onDelete = { onDelete(medicine) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun MedicineCard(
    medicine: Medicine,
    schedules: List<Schedule>,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = medicineAccent(medicine.id)
    var actionsExpanded by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column {
            val photo = medicine.photoPath
            if (photo != null) {
                AsyncImage(
                    model = File(photo),
                    contentDescription = medicine.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(accent, accent.copy(alpha = 0.55f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Medication,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(14.dp)
                                    .size(36.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = medicine.name.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = medicine.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        val dose = doseLine(medicine, schedules)
                        if (dose.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = dose,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val timing = timingText(schedules)
                        if (timing.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Repeat,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = timing,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    InlineMedicineActions(
                        expanded = actionsExpanded,
                        onToggle = { actionsExpanded = !actionsExpanded },
                        onEdit = {
                            actionsExpanded = false
                            onEdit()
                        },
                        onDelete = {
                            actionsExpanded = false
                            showDelete = true
                        }
                    )
                }

                val patterns = schedules.map { schedulePatternLabel(it) }.distinct()
                if (patterns.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        patterns.forEach { pattern ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = accent.copy(alpha = 0.14f),
                                contentColor = accent
                            ) {
                                Text(
                                    text = pattern,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDelete) {
        MedConfirmDialog(
            title = "Delete medicine?",
            message = "\"${medicine.name}\" and all of its schedules will be permanently removed. This cannot be undone.",
            onConfirm = {
                showDelete = false
                onDelete()
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun InlineMedicineActions(
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(visible = expanded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onEdit,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = onDelete,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
        }

        Surface(
            onClick = onToggle,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More options",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun doseLine(medicine: Medicine, schedules: List<Schedule>): String {
    val doseLabel = schedules.firstNotNullOfOrNull { s -> s.doseLabel.takeIf { it.isNotBlank() } }
    return listOf(medicine.strength, doseLabel)
        .filterNotNull()
        .filter { it.isNotBlank() }
        .joinToString(" \u00b7 ")
}

private fun timingText(schedules: List<Schedule>): String {
    if (schedules.isEmpty()) return ""
    val clockTimes = mutableListOf<String>()
    val intervals = mutableListOf<Int>()
    schedules.forEach { s ->
        if (s.type == ScheduleType.INTERVAL) {
            if (s.intervalHours > 0) intervals.add(s.intervalHours)
        } else {
            s.times.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { clockTimes.add(it) }
        }
    }
    val uniqueTimes = clockTimes.distinct()
    val timesStr = when {
        uniqueTimes.size > 3 ->
            uniqueTimes.take(2).joinToString(" \u00b7 ") { to12Hour(it) } +
                " \u00b7 +${uniqueTimes.size - 2} more"
        uniqueTimes.isNotEmpty() -> uniqueTimes.joinToString(" \u00b7 ") { to12Hour(it) }
        else -> null
    }
    val intervalStr = if (intervals.isNotEmpty()) {
        intervals.distinct().joinToString(", ") { "Every ${it} h" }
    } else null
    return when {
        timesStr != null && intervalStr != null -> "$timesStr \u00b7 $intervalStr"
        timesStr != null -> timesStr
        intervalStr != null -> intervalStr
        else -> ""
    }
}

internal fun schedulePatternLabel(schedule: Schedule): String = when (schedule.type) {
    ScheduleType.DAILY -> "Daily"
    ScheduleType.WEEKDAYS -> {
        val days = weekdayShort.filterIndexed { index, _ ->
            (schedule.daysMask and (1 shl index)) != 0
        }
        if (days.isEmpty()) "No days" else days.joinToString(", ")
    }
    ScheduleType.INTERVAL -> "Every ${schedule.intervalHours} h"
    ScheduleType.COURSE -> {
        if (schedule.endDate != null) {
            "Course until " + SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(schedule.endDate))
        } else {
            "Course"
        }
    }
    else -> "Daily"
}

private fun to12Hour(time: String): String {
    val parts = time.trim().split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return time.trim()
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val suffix = if (hour < 12) "AM" else "PM"
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", h, minute, suffix)
}

private val weekdayShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
