package com.medremind.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medremind.app.data.DoseStatus
import com.medremind.app.data.Medicine
import com.medremind.app.data.MedicineCategory
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
    var menuMedicine by remember { mutableStateOf<Medicine?>(null) }
    var pendingDelete by remember { mutableStateOf<Medicine?>(null) }
    var showPharmacyDialog by remember { mutableStateOf(false) }
    var bannerDismissed by remember { mutableStateOf(false) }

    val prescriptionCount = medicines.count { it.category == MedicineCategory.PRESCRIPTION }
    val supplementCount = medicines.count {
        it.category == MedicineCategory.SUPPLEMENT || it.category == MedicineCategory.OTC
    }
    val lowMedicines = medicines.filter { it.quantity > 0 && it.quantity <= it.refillThreshold }
    val filterOptions = listOf(
        "All (${medicines.size})",
        "Prescriptions ($prescriptionCount)",
        "Supplements ($supplementCount)",
        "Needs refill (${lowMedicines.size})"
    )
    val filteredMedicines = medicines.filter { medicine ->
        val matchesQuery = query.isBlank() || medicine.name.contains(query, ignoreCase = true)
        val matchesFilter = when (filter) {
            1 -> medicine.category == MedicineCategory.PRESCRIPTION
            2 -> medicine.category == MedicineCategory.SUPPLEMENT ||
                medicine.category == MedicineCategory.OTC
            3 -> medicine.quantity > 0 && medicine.quantity <= medicine.refillThreshold
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
                            if (phone.isNotBlank()) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    )
                                }
                            } else {
                                onEdit(low)
                            }
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
                    onOpenMenu = { menuMedicine = medicine },
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

    if (showPharmacyDialog) {
        PharmacyDialog(
            settings = settings,
            onDismiss = { showPharmacyDialog = false }
        )
    }
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
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberMedHaptics()
    val low = medicine.quantity > 0 && medicine.quantity <= medicine.refillThreshold
    val accent = when {
        low -> MaterialTheme.colorScheme.error
        medicine.category == MedicineCategory.SUPPLEMENT -> MaterialTheme.colorScheme.tertiary
        medicine.category == MedicineCategory.OTC -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    val icon = when (medicine.category) {
        MedicineCategory.SUPPLEMENT -> Icons.Rounded.WbSunny
        else -> Icons.Rounded.Medication
    }
    val pattern = schedules.firstOrNull()?.let { schedulePatternLabel(it) }.orEmpty()
    val intake = com.medremind.app.data.IntakeInstruction.label(medicine.intakeInstruction)

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
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (low) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (low) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = if (low) "Refill due" else MedicineCategory.label(medicine.category),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    if (medicine.rxNumber.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = "Rx #${medicine.rxNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (!low && medicine.refillsLeft > 0) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "${medicine.refillsLeft} refills left",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        onClick = {
                            haptics.tap()
                            onOpenMenu()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More options",
                                modifier = Modifier.size(18.dp)
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
                        if (medicine.prescriber.isNotBlank()) {
                            Text(
                                text = "Prescribed by ${medicine.prescriber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                            imageVector = icon,
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        )
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "LOW SUPPLY WARNING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text(
                            "$daysLeft day" + (if (daysLeft == 1) "" else "s") + " left",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${medicine.name} has only ${medicine.quantity} " +
                        unitLabel(medicine.form, medicine.quantity != 1) + " remaining.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onRefill,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.LocalPharmacy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Refill",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Surface(
                        onClick = onLater,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            "Later",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
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
                    label = { Text("Phone") },
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

private fun doseLine(medicine: Medicine, schedules: List<Schedule>): String {
    val doseLabel = schedules.firstNotNullOfOrNull { s -> s.doseLabel.takeIf { it.isNotBlank() } }
    val intake = com.medremind.app.data.IntakeInstruction.label(medicine.intakeInstruction)
    return listOf(medicine.strength, doseLabel, intake)
        .filterNotNull()
        .filter { it.isNotBlank() }
        .joinToString(" \u00b7 ")
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
