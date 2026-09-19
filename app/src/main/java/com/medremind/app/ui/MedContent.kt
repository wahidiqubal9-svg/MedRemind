package com.medremind.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medremind.app.data.Medicine
import com.medremind.app.data.Schedule
import com.medremind.app.data.ScheduleType
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
    onAdd: () -> Unit,
    onOpenMe: () -> Unit,
    profilePhoto: String? = null
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    val filterOptions = listOf("All", "Daily", "Weekly", "Course")
    val filteredMedicines = medicines.filter { medicine ->
        (query.isBlank() || medicine.name.contains(query, ignoreCase = true)) &&
            when (filter) {
                1 -> schedulesByMedicine[medicine.id].orEmpty().any { it.type == ScheduleType.DAILY }
                2 -> schedulesByMedicine[medicine.id].orEmpty().any { it.type == ScheduleType.WEEKDAYS }
                3 -> schedulesByMedicine[medicine.id].orEmpty().any { it.type == ScheduleType.COURSE }
                else -> true
            }
    }
    if (medicines.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            ScreenHeader("Medicines", modifier = Modifier.padding(horizontal = 16.dp)) {
                SquareIconButton(
                    icon = Icons.Rounded.Person,
                    contentDescription = "Me",
                    onClick = onOpenMe,
                    photoPath = profilePhoto
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(),
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
        }
        return
    }

    var menuMedicine by remember { mutableStateOf<Medicine?>(null) }
    var pendingDelete by remember { mutableStateOf<Medicine?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "med_header") {
            ScreenHeader("Medicines") {
                SquareIconButton(
                    icon = Icons.Rounded.Person,
                    contentDescription = "Me",
                    onClick = onOpenMe,
                    photoPath = profilePhoto
                )
            }
        }
        item(key = "search") {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search medicines…"
            )
        }
        item(key = "filters") {
            FilterChipRow(
                options = filterOptions,
                selectedIndex = filter,
                onSelect = { filter = it }
            )
        }
        items(filteredMedicines, key = { it.id }) { medicine ->
            MedicineCard(
                medicine = medicine,
                schedules = schedulesByMedicine[medicine.id].orEmpty(),
                onClick = { onEdit(medicine) },
                onOpenMenu = { menuMedicine = medicine },
                modifier = Modifier.animateItem()
            )
        }
    }

    MedicineActionOverlay(
        medicine = menuMedicine,
        subtitle = menuMedicine?.let {
            doseLine(it, schedulesByMedicine[it.id].orEmpty())
        } ?: "",
        onEdit = {
            val med = menuMedicine
            menuMedicine = null
            if (med != null) onEdit(med)
        },
        onDelete = {
            val med = menuMedicine
            menuMedicine = null
            if (med != null) pendingDelete = med
        },
        onDismiss = { menuMedicine = null }
    )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        MedConfirmDialog(
            title = "Delete medicine?",
            message = "\"${toDelete.name}\" and all of its schedules will be permanently removed. This cannot be undone.",
            onConfirm = {
                pendingDelete = null
                onDelete(toDelete)
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun MedicineActionOverlay(
    medicine: Medicine?,
    subtitle: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val shown = remember { mutableStateOf<Medicine?>(null) }
    val shownSubtitle = remember { mutableStateOf("") }
    LaunchedEffect(medicine, subtitle) {
        if (medicine != null) {
            shown.value = medicine
            shownSubtitle.value = subtitle
        }
    }

    val visible = medicine != null
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "menuScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = motionTween(MedMotion.Fast),
        label = "menuAlpha"
    )
    if (!visible && alpha <= 0.01f) return

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f * alpha))
                .clickable { onDismiss() }
        )
        val med = shown.value
        if (med != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(28.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = MedElevation.sheet
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MedIconSquare(
                            label = med.name,
                            seed = med.id,
                            size = 56.dp,
                            photoPath = med.photoPath
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                med.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                shownSubtitle.value.ifBlank { "\u2014" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    MenuActionRow(
                        icon = Icons.Rounded.Edit,
                        label = "Edit medicine",
                        onClick = onEdit
                    )
                    Spacer(Modifier.height(10.dp))
                    MenuActionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        label = "Delete medicine",
                        destructive = true,
                        onClick = onDelete
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuActionRow(
    icon: ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val haptics = rememberMedHaptics()
    Surface(
        onClick = {
            if (destructive) haptics.reject() else haptics.tap()
            onClick()
        },
        shape = RoundedCornerShape(18.dp),
        color = if (destructive) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (destructive) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MedicineCard(
    medicine: Medicine,
    schedules: List<Schedule>,
    onClick: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = medicineAccent(medicine.id)
    val haptics = rememberMedHaptics()

    Surface(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = MedElevation.card
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedIconSquare(
                    label = medicine.name,
                    seed = medicine.id,
                    size = 66.dp,
                    photoPath = medicine.photoPath
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = doseLine(medicine, schedules).ifBlank { "—" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    onClick = {
                        haptics.tap()
                        onOpenMenu()
                    },
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

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            val timing = timingText(schedules)
            val patternText = schedules.map { schedulePatternLabel(it) }.distinct().joinToString(", ")
            val stackTags = timing.isNotBlank() && patternText.isNotBlank() &&
                (timing.length + patternText.length) > 16
            val tags: @Composable () -> Unit = {
                if (timing.isNotBlank()) {
                    TagPill(
                        icon = Icons.Rounded.Schedule,
                        text = timing,
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (patternText.isNotBlank()) {
                    TagPill(
                        icon = Icons.Rounded.Repeat,
                        text = patternText,
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                if (medicine.quantity > 0) {
                    val low = medicine.quantity <= medicine.refillThreshold
                    TagPill(
                        icon = Icons.Rounded.Medication,
                        text = if (low) "Refill \u00b7 ${medicine.quantity} left"
                        else "${medicine.quantity} left",
                        container = if (low) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                        content = if (low) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            if (stackTags) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { tags() }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { tags() }
            }
        }
    }
}

@Composable
private fun TagPill(
    icon: ImageVector,
    text: String,
    container: Color,
    content: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
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
    ScheduleType.AS_NEEDED -> "As needed"
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
