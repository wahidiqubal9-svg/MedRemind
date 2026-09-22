package com.medremind.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medremind.app.data.DoseStatus
import com.medremind.app.data.Medicine
import com.medremind.app.data.MedicineForm
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
    settings: SettingsViewModel,
    vm: MedicineViewModel,
    onEdit: (Medicine) -> Unit,
    onDelete: (Medicine) -> Unit,
    onAdd: () -> Unit,
    onOpenMe: () -> Unit,
    profilePhoto: String? = null
) {
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<Medicine?>(null) }
    var refillIndex by remember { mutableIntStateOf(0) }

    fun shareMedicine(medicine: Medicine, schedules: List<Schedule>) {
        val text = buildString {
            append(medicine.name)
            if (medicine.strength.isNotBlank()) append(" ${medicine.strength}")
            append("\nTime: ").append(timesLabel(schedules))
            append("\nFrequency: ").append(frequencyLabel(schedules))
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Share medicine")) }
    }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var showPharmacyDialog by remember { mutableStateOf(false) }
    var bannerDismissed by remember { mutableStateOf(false) }
    var refillMedicine by remember { mutableStateOf<Medicine?>(null) }
    var stockMedicine by remember { mutableStateOf<Medicine?>(null) }
    val filterOptions = listOf("All", "Daily", "Weekly", "Course")

    val lowMedicines = medicines.filter { it.quantity > 0 && it.quantity <= it.refillThreshold }
    val filteredMedicines = medicines.filter { medicine ->
        val matchesQuery = query.isBlank() || medicine.name.contains(query, ignoreCase = true)
        val schedules = schedulesByMedicine[medicine.id].orEmpty()
        val matchesFilter = when (filter) {
            1 -> schedules.any { it.type == ScheduleType.DAILY }
            2 -> schedules.any { it.type == ScheduleType.WEEKDAYS }
            3 -> schedules.any { it.type == ScheduleType.COURSE }
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            MedHeader(
                onOpenMe = onOpenMe,
                profilePhoto = profilePhoto
            )
            CabinetSummaryRow(
                allCount = medicines.size,
                lowCount = lowMedicines.size
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 10.dp, bottom = 200.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "search") {
                    SearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Search prescriptions or vitamins\u2026"
                    )
                }
                item(key = "filters") {
                    FilterChipRow(
                        options = filterOptions,
                        selectedIndex = filter,
                        onSelect = { filter = it }
                    )
                }
                if (lowMedicines.isNotEmpty() && !bannerDismissed) {
                    item(key = "low_supply") {
                        val index = refillIndex.coerceIn(0, lowMedicines.lastIndex)
                        val low = lowMedicines[index]
                        LowSupplyBanner(
                            medicine = low,
                            schedules = schedulesByMedicine[low.id].orEmpty(),
                            moreCount = lowMedicines.size - 1,
                            onRefill = {
                                val phone = settings.pharmacyPhone
                                if (phone.isNotBlank()) refillMedicine = low else onEdit(low)
                            },
                            onLater = { bannerDismissed = true },
                            onNext = {
                                refillIndex = (index + 1) % lowMedicines.size
                            }
                        )
                    }
                }
                if (medicines.isEmpty()) {
                    item(key = "empty") {
                        MedCard(modifier = Modifier.fillMaxWidth()) {
                            MedEmptyState(
                                icon = Icons.Rounded.CalendarMonth,
                                title = "No doses scheduled",
                                message = "Nothing is scheduled for this day. Add a medicine to get started.",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp)
                            )
                        }
                    }
                } else if (filteredMedicines.isEmpty()) {
                    item(key = "no_results") {
                        MedEmptyState(
                            icon = Icons.Outlined.Medication,
                            title = "No matches",
                            message = "No medicines match your search or filter.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(filteredMedicines, key = { it.id }) { medicine ->
                        val schedules = schedulesByMedicine[medicine.id].orEmpty()
                MedicineCard(
                    medicine = medicine,
                    schedules = schedules,
                    onEdit = { onEdit(medicine) },
                    onDuplicate = { vm.duplicateMedicine(medicine, schedules) },
                    onShare = { shareMedicine(medicine, schedules) },
                    onRefill = { stockMedicine = medicine },
                    onDelete = { pendingDelete = medicine },
                    modifier = Modifier.animateItem()
                )
                    }
                }
                item(key = "pharmacy") {
                    PharmacyCard(
                        settings = settings,
                        onEdit = { showPharmacyDialog = true },
                        onCall = {
                            val phone = settings.pharmacyPhone
                            if (phone.isNotBlank()) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
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

    if (showPharmacyDialog) {
        PharmacyDialog(
            settings = settings,
            onDismiss = { showPharmacyDialog = false }
        )
    }

    val stockTarget = stockMedicine
    if (stockTarget != null) {
        StockRefillSheet(
            medicine = stockTarget,
            onDismiss = { stockMedicine = null },
            onSave = { amount ->
                vm.refillStock(stockTarget, amount)
                stockMedicine = null
            }
        )
    }

    val refillTarget = refillMedicine
    if (refillTarget != null) {
        RefillDaysDialog(
            medicine = refillTarget,
            suggestedDays = dailyDose(schedulesByMedicine[refillTarget.id].orEmpty())
                .let { d ->
                    if (d > 0f) (refillTarget.quantity / d).toInt().coerceIn(1, 365) else 30
                },
            onDismiss = { refillMedicine = null },
            onConfirm = { days ->
                openWhatsAppRefill(
                    context = context,
                    phone = settings.pharmacyPhone,
                    medicine = refillTarget,
                    schedules = schedulesByMedicine[refillTarget.id].orEmpty(),
                    days = days
                )
                refillMedicine = null
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockRefillSheet(
    medicine: Medicine,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var added by remember { mutableStateOf("30") }
    val addValue = added.toIntOrNull() ?: 0
    val newTotal = medicine.quantity + addValue

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                "Refill stock",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                listOf(medicine.name, medicine.strength)
                    .filter { it.isNotBlank() }.joinToString(" "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = added,
                onValueChange = { added = it.filter { c -> c.isDigit() }.take(5) },
                label = { Text("Pills to add") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Current: ${medicine.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "New stock: $newTotal",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(20.dp))
            GradientPillButton(
                text = "Add to stock",
                icon = Icons.Rounded.Inventory2,
                onClick = { onSave(addValue) },
                enabled = addValue > 0,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RefillDaysDialog(
    medicine: Medicine,
    suggestedDays: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var days by remember { mutableStateOf(suggestedDays.coerceIn(1, 365).toString()) }
    val quick = listOf(7, 14, 30, 90)
    val current = days.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Refill request") },
        text = {
            Column {
                Text(
                    "How many days of ${medicine.name} do you need?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quick.forEach { value ->
                        val selected = current == value
                        Surface(
                            onClick = { days = value.toString() },
                            shape = RoundedCornerShape(50),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                "${value}d",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Days") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = current != null && current > 0,
                onClick = { onConfirm((current ?: suggestedDays).coerceIn(1, 365)) }
            ) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun MedHeader(
    onOpenMe: () -> Unit,
    profilePhoto: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "INVENTORY & SCHEDULE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                "Medicine cabinet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        SquareIconButton(
            icon = Icons.Rounded.Person,
            contentDescription = "Me",
            onClick = onOpenMe,
            photoPath = profilePhoto
        )
    }
}

@Composable
private fun MedicineCard(
    medicine: Medicine,
    schedules: List<Schedule>,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onShare: () -> Unit,
    onRefill: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberMedHaptics()
    var menuExpanded by remember { mutableStateOf(false) }
    val line = MaterialTheme.colorScheme.outlineVariant
    val soft = MaterialTheme.colorScheme.surfaceContainerLow
    val doseLabel = schedules.firstNotNullOfOrNull { s -> s.doseLabel.takeIf { it.isNotBlank() } }.orEmpty()
    val sub = listOf(medicine.strength, doseLabel)
        .filter { it.isNotBlank() }.joinToString(" \u00b7 ")
    val daysSchedule = schedules.firstOrNull { it.type == ScheduleType.WEEKDAYS }
    val tracked = medicine.quantity > 0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, line),
        shadowElevation = MedElevation.raised
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedIconSquare(
                    label = medicine.name,
                    seed = medicine.id,
                    size = 56.dp,
                    photoPath = medicine.photoPath
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    if (sub.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box {
                    Surface(
                        onClick = {
                            haptics.tap()
                            menuExpanded = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = soft,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, line)
                    ) {
                        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More options",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit medicine") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Refill stock") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Inventory2, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onRefill()
                            }
                        )
                        HorizontalDivider(color = line)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete medicine",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailItem(
                    icon = Icons.Rounded.Medication,
                    label = "Form",
                    value = MedicineForm.label(medicine.form),
                    modifier = Modifier.weight(1f)
                )
                DetailItem(
                    icon = Icons.Rounded.Restaurant,
                    label = "Intake",
                    value = com.medremind.app.data.IntakeInstruction
                        .label(medicine.intakeInstruction).ifBlank { "\u2014" },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            DetailItem(
                icon = Icons.Rounded.AccessTime,
                label = "Time",
                value = timesLabel(schedules),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailItem(
                    icon = Icons.Rounded.HourglassEmpty,
                    label = "Duration",
                    value = durationLabel(schedules),
                    modifier = Modifier.weight(1f)
                )
                DetailItem(
                    icon = Icons.Rounded.Repeat,
                    label = "Frequency",
                    value = frequencyLabel(schedules),
                    modifier = Modifier.weight(1f)
                )
            }

            if (daysSchedule != null) {
                Spacer(Modifier.height(10.dp))
                DaysPanel(
                    daysMask = daysSchedule.daysMask,
                    note = offDaysLabel(daysSchedule.daysMask)
                )
            }

            if (tracked) {
                Spacer(Modifier.height(12.dp))
                StockPanel(medicine = medicine, schedules = schedules)
            }
        }
    }
}

@Composable
private fun CabinetSummaryRow(allCount: Int, lowCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryBox(
            icon = Icons.Rounded.Medication,
            label = "All medicines",
            value = allCount,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        SummaryBox(
            icon = Icons.Rounded.Warning,
            label = "Low stock",
            value = lowCount,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryBox(
    icon: ImageVector,
    label: String,
    value: Int,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = MedElevation.card
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    value.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, line, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, line, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DaysPanel(daysMask: Int, note: String) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, line, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(
            "DAYS OF THE WEEK",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(7) { index ->
                val on = (daysMask and (1 shl index)) != 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (on) MedGradients.heroHorizontal()
                            else SolidColor(MaterialTheme.colorScheme.surface)
                        )
                        .then(
                            if (on) Modifier
                            else Modifier.border(1.dp, line, RoundedCornerShape(11.dp))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        weekdayShort[index].take(1),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (on) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (note.isNotBlank()) {
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StockPanel(medicine: Medicine, schedules: List<Schedule>) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val daily = dailyDose(schedules)
    val daysLeft = if (daily > 0f) (medicine.quantity / daily).toInt() else null
    val low = medicine.quantity <= medicine.refillThreshold
    val fraction = if (daysLeft != null) (daysLeft / 30f).coerceIn(0.05f, 1f) else 1f
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = motionTween(MedMotion.Slow, easing = MedMotion.Emphasized),
        label = "stock-${medicine.id}"
    )
    val statusColor = if (low) MaterialTheme.colorScheme.error else Color(0xFF12B76A)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, line, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${medicine.quantity} in stock",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            if (medicine.refillThreshold > 0) {
                Text(
                    "  \u00b7  remind below ${medicine.refillThreshold}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    "TRACKED",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE3E6F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .fillMaxHeight()
                    .background(
                        if (low) SolidColor(MaterialTheme.colorScheme.error)
                        else MedGradients.heroHorizontal()
                    )
            )
        }
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (low) "Needs refill" else "Well-stocked",
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun timesLabel(schedules: List<Schedule>): String {
    if (schedules.isNotEmpty() && schedules.all { it.type == ScheduleType.AS_NEEDED }) {
        return "As needed"
    }
    val times = schedules
        .filter { it.type != ScheduleType.AS_NEEDED }
        .flatMap { it.times.split(',').map { t -> t.trim() }.filter { it.isNotEmpty() } }
        .distinct()
    return if (times.isEmpty()) "As needed" else times.joinToString("  \u00b7  ") { to12Hour(it) }
}

private fun frequencyLabel(schedules: List<Schedule>): String {
    val s = schedules.firstOrNull() ?: return "\u2014"
    return when (s.type) {
        ScheduleType.DAILY -> "Every day"
        ScheduleType.WEEKDAYS -> "${Integer.bitCount(s.daysMask)} days / week"
        ScheduleType.INTERVAL -> "Every ${s.intervalHours} h"
        ScheduleType.EVERY_N_DAYS -> "Every ${s.intervalDays.coerceAtLeast(2)} days"
        ScheduleType.CYCLE -> "${s.cycleOnDays} days on \u00b7 ${s.cycleOffDays} rest"
        ScheduleType.SELECTED_DATES -> {
            val count = s.selectedDates.split(',').count { it.trim().isNotEmpty() }
            "$count date" + (if (count == 1) "" else "s")
        }
        ScheduleType.AS_NEEDED -> "As needed"
        ScheduleType.COURSE -> "Course"
        else -> "Every day"
    }
}

private fun durationLabel(schedules: List<Schedule>): String {
    val s = schedules.firstOrNull() ?: return "\u2014"
    if (s.type == ScheduleType.AS_NEEDED) return "As needed"
    val end = s.endDate ?: return "Ongoing"
    return "Until " + SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(end))
}

private fun offDaysLabel(mask: Int): String {
    val off = weekdayShort.filterIndexed { index, _ -> (mask and (1 shl index)) == 0 }
    return if (off.isEmpty()) "" else "Skipped on " + off.joinToString(", ")
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

@Composable
private fun LowSupplyBanner(
    medicine: Medicine,
    schedules: List<Schedule>,
    moreCount: Int = 0,
    onRefill: () -> Unit,
    onLater: () -> Unit,
    onNext: () -> Unit = {}
) {
    val daily = dailyDose(schedules)
    val daysLeft = if (daily > 0f) (medicine.quantity / daily).toInt() else 0
    val unit = unitLabel(medicine.form, medicine.quantity != 1)
    val rose = Color(0xFFE8466B)
    val ink = Color(0xFF101828)
    val bodyColor = Color(0xFF7A4453)
    val body = androidx.compose.ui.text.buildAnnotatedString {
        withStyle(
            androidx.compose.ui.text.SpanStyle(color = ink, fontWeight = FontWeight.Bold)
        ) { append(medicine.name) }
        append(" has only ")
        withStyle(
            androidx.compose.ui.text.SpanStyle(color = ink, fontWeight = FontWeight.Bold)
        ) { append("${medicine.quantity} $unit") }
        append(" remaining. Tap below to request a refill.")
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF6C9D5)),
        shadowElevation = MedElevation.card
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(listOf(Color(0xFFFFF4F7), Color(0xFFFDEAEF)))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFF0567A), rose)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PriorityHigh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "LOW SUPPLY WARNING",
                                style = MaterialTheme.typography.labelSmall,
                                color = rose,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = rose,
                                contentColor = Color.White
                            ) {
                                Text(
                                    "$daysLeft day" + (if (daysLeft == 1) "" else "s") + " left",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.5.sp,
                                lineHeight = 21.sp
                            ),
                            color = bodyColor
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        onClick = onRefill,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        shadowElevation = MedElevation.card
                    ) {
                        Row(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFF0567A), rose)
                                    )
                                )
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                "Refill Now",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        onClick = onLater,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        contentColor = ink,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFE7EAF2)
                        )
                    ) {
                        Text(
                            "Remind Later",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                        )
                    }
                }
                if (moreCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = onNext,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            "$moreCount more medicine" + (if (moreCount == 1) "" else "s") +
                                " need a refill \u00b7 Show next",
                            style = MaterialTheme.typography.labelMedium,
                            color = rose,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PharmacyCard(
    settings: SettingsViewModel,
    onEdit: () -> Unit,
    onCall: () -> Unit
) {
    val hasPharmacy = settings.pharmacyName.isNotBlank()
    MedCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.LocalPharmacy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "PRIMARY PHARMACY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (hasPharmacy) settings.pharmacyName else "No pharmacy added",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (hasPharmacy) {
                TextButton(onClick = onCall) { Text("Call") }
            }
        }
        if (hasPharmacy && settings.pharmacyAddress.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = listOf(settings.pharmacyAddress, settings.pharmacyHours)
                    .filter { it.isNotBlank() }.joinToString(" \u00b7 "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            onClick = onEdit,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasPharmacy) "Edit pharmacy details" else "Add your pharmacy",
                    style = MaterialTheme.typography.labelMedium,
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
}

@Composable
private fun PharmacyDialog(
    settings: SettingsViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(settings.pharmacyName) }
    var phone by remember { mutableStateOf(settings.pharmacyPhone) }
    var address by remember { mutableStateOf(settings.pharmacyAddress) }
    var hours by remember { mutableStateOf(settings.pharmacyHours) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Primary pharmacy") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("WhatsApp number") },
                    placeholder = { Text("e.g. +1 555 123 4567") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Hours") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                settings.updatePharmacy(name.trim(), phone.trim(), address.trim(), hours.trim())
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Estimated units consumed per day across all enabled schedules. */
private fun dailyDose(schedules: List<Schedule>): Float {
    var total = 0f
    schedules.filter { it.enabled }.forEach { s ->
        val times = when (s.type) {
            ScheduleType.AS_NEEDED -> 0f
            ScheduleType.INTERVAL -> 24f / s.intervalHours.coerceAtLeast(1)
            else -> s.times.split(',').count { it.isNotBlank() }.toFloat()
        }
        val perTake = s.doseLabel.trim().split(Regex("\\s+"))
            .firstOrNull()?.toFloatOrNull()?.coerceAtLeast(0.5f) ?: 1f
        val factor = when (s.type) {
            ScheduleType.WEEKDAYS -> Integer.bitCount(s.daysMask) / 7f
            ScheduleType.EVERY_N_DAYS -> 1f / s.intervalDays.coerceAtLeast(2)
            ScheduleType.CYCLE -> {
                val on = s.cycleOnDays.coerceAtLeast(1)
                val off = s.cycleOffDays.coerceAtLeast(0)
                on / (on + off).toFloat()
            }
            ScheduleType.SELECTED_DATES -> {
                val today = java.time.LocalDate.now().toEpochDay()
                val recent = s.selectedDates.split(',').mapNotNull { it.trim().toLongOrNull() }
                    .count { it in (today - 29)..today }
                recent / 30f
            }
            else -> 1f
        }
        total += times * perTake * factor
    }
    return total
}

private fun unitLabel(form: String, plural: Boolean): String {
    val base = when (form) {
        MedicineForm.CAPSULE -> "capsule"
        MedicineForm.SYRUP -> "ml"
        MedicineForm.DROP -> "drop"
        MedicineForm.INJECTION -> "dose"
        MedicineForm.OINTMENT -> "application"
        MedicineForm.OTHER -> "unit"
        else -> "tablet"
    }
    return if (plural) base + "s" else base
}

private fun openWhatsAppRefill(
    context: android.content.Context,
    phone: String,
    medicine: Medicine,
    schedules: List<Schedule>,
    days: Int
) {
    val digits = phone.filter { it.isDigit() }
    if (digits.isBlank()) return
    val name = listOf(medicine.name, medicine.strength)
        .filter { it.isNotBlank() }.joinToString(" ")
    val daily = dailyDose(schedules)
    val qty = if (daily > 0f) kotlin.math.ceil(days * daily).toInt() else 0
    val qtyText = if (qty > 0) {
        " Please arrange about $qty ${unitLabel(medicine.form, qty != 1)}."
    } else ""
    val message = "Hello, I would like to refill $name for $days days." +
        qtyText + " Thank you."
    val url = "https://wa.me/$digits?text=" +
        java.net.URLEncoder.encode(message, "UTF-8")
    val whatsapp = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        setPackage("com.whatsapp")
    }
    val opened = runCatching { context.startActivity(whatsapp) }.isSuccess
    if (!opened) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
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
    ScheduleType.EVERY_N_DAYS -> "Every ${schedule.intervalDays.coerceAtLeast(2)} days"
    ScheduleType.CYCLE -> "${schedule.cycleOnDays} days on, ${schedule.cycleOffDays} rest"
    ScheduleType.SELECTED_DATES -> {
        val count = schedule.selectedDates.split(',').count { it.trim().isNotEmpty() }
        "$count selected date" + (if (count == 1) "" else "s")
    }
    ScheduleType.AS_NEEDED -> "As needed"
    ScheduleType.COURSE -> {
        if (schedule.endDate != null) {
            "Course until " + SimpleDateFormat("d MMM", Locale.getDefault())
                .format(Date(schedule.endDate))
        } else {
            "Course"
        }
    }
    else -> "Daily"
}

private val weekdayShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
