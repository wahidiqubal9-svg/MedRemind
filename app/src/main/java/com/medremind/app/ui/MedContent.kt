package com.medremind.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    onEdit: (Medicine) -> Unit,
    onDelete: (Medicine) -> Unit,
    onAdd: () -> Unit,
    onOpenMe: () -> Unit,
    profilePhoto: String? = null
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var showPharmacyDialog by remember { mutableStateOf(false) }
    var bannerDismissed by remember { mutableStateOf(false) }
    var refillMedicine by remember { mutableStateOf<Medicine?>(null) }
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

    if (medicines.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            MedHeader(count = 0, onOpenMe = onOpenMe, profilePhoto = profilePhoto)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "cabinet_header") {
                MedHeader(
                    count = medicines.size,
                    onOpenMe = onOpenMe,
                    profilePhoto = profilePhoto
                )
            }
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
                    val low = lowMedicines.first()
                    LowSupplyBanner(
                        medicine = low,
                        schedules = schedulesByMedicine[low.id].orEmpty(),
                        onRefill = {
                            val phone = settings.pharmacyPhone
                            if (phone.isNotBlank()) refillMedicine = low else onEdit(low)
                        },
                        onLater = { bannerDismissed = true }
                    )
                }
            }
            items(filteredMedicines, key = { it.id }) { medicine ->
                MedicineCard(
                    medicine = medicine,
                    schedules = schedulesByMedicine[medicine.id].orEmpty(),
                    onClick = { onEdit(medicine) },
                    modifier = Modifier.animateItem()
                )
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

    if (showPharmacyDialog) {
        PharmacyDialog(
            settings = settings,
            onDismiss = { showPharmacyDialog = false }
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
    count: Int,
    onOpenMe: () -> Unit,
    profilePhoto: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp),
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
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(10.dp))
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberMedHaptics()
    val low = medicine.quantity > 0 && medicine.quantity <= medicine.refillThreshold
    val accent = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val pattern = schedules.firstOrNull()?.let { schedulePatternLabel(it) }.orEmpty()
    val intake = com.medremind.app.data.IntakeInstruction.label(medicine.intakeInstruction)
    val supplyStatus = when {
        low -> "Needs refill"
        medicine.quantity > 0 -> "Well-stocked"
        schedules.isEmpty() -> "No schedule"
        else -> pattern.ifBlank { "Scheduled" }
    }

    Surface(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            if (low) 1.4.dp else 1.dp,
            if (low) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = if (low) MedElevation.raised else MedElevation.card
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (low) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ) {
                            Text(
                                "Refill due",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (!low && medicine.refillsLeft > 0) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                "${medicine.refillsLeft} refills left",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = listOf(medicine.name, medicine.strength)
                                .filter { it.isNotBlank() }.joinToString(" "),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                accent.copy(alpha = 0.14f),
                                RoundedCornerShape(15.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Medication,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pattern.isNotBlank()) {
                        BadgeChip(pattern, Icons.Rounded.Schedule)
                    }
                    if (intake.isNotBlank()) {
                        BadgeChip(intake, Icons.Rounded.Restaurant)
                    }
                }

                if (medicine.quantity > 0) {
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Spacer(Modifier.height(10.dp))
                    StockGauge(medicine = medicine, schedules = schedules)
                }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = supplyStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        onClick = {
                            haptics.tap()
                            onClick()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Text(
                            "Edit dose",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
    }
}

@Composable
private fun BadgeChip(text: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StockGauge(medicine: Medicine, schedules: List<Schedule>) {
    val percent = if (medicine.packSize > 0) {
        (medicine.quantity * 100 / medicine.packSize).coerceIn(0, 100)
    } else null
    val daily = dailyDose(schedules)
    val daysLeft = if (daily > 0f) (medicine.quantity / daily).toInt() else null
    val low = medicine.quantity <= medicine.refillThreshold
    val gaugeColor = when {
        low -> MaterialTheme.colorScheme.error
        percent != null && percent <= 25 -> MaterialTheme.colorScheme.error
        percent != null && percent <= 50 -> Color(0xFFB45309)
        else -> MaterialTheme.colorScheme.primary
    }
    val fraction = when {
        percent != null -> percent / 100f
        else -> 1f
    }
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = motionTween(MedMotion.Slow, easing = MedMotion.Emphasized),
        label = "stock-${medicine.id}"
    )

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val amountText = buildString {
                append(medicine.quantity)
                if (medicine.packSize > 0) append(" / ${medicine.packSize}")
                append(" ")
                append(unitLabel(medicine.form, medicine.quantity != 1))
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (low) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
            if (daysLeft != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "($daysLeft day" + (if (daysLeft == 1) "" else "s") + " left)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (percent != null) "$percent% left" else "tracked",
                style = MaterialTheme.typography.labelSmall,
                color = gaugeColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(50)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .background(gaugeColor, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun LowSupplyBanner(
    medicine: Medicine,
    schedules: List<Schedule>,
    onRefill: () -> Unit,
    onLater: () -> Unit
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
        val weekdayFactor = if (s.type == ScheduleType.WEEKDAYS) {
            Integer.bitCount(s.daysMask) / 7f
        } else 1f
        total += times * perTake * weekdayFactor
    }
    return total
}

private fun unitLabel(form: String, plural: Boolean): String {
    val base = when (form) {
        MedicineForm.CAPSULE -> "capsule"
        MedicineForm.SOFTGEL -> "softgel"
        MedicineForm.LIQUID -> "dose"
        MedicineForm.INJECTION -> "dose"
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
