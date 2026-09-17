package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class FreqOption(val title: String, val sub: String, val times: List<String>)

private val freqOptions = listOf(
    FreqOption("Every day", "1 time daily at 8:00 AM", listOf("08:00")),
    FreqOption("Twice a day", "8:00 AM & 8:00 PM", listOf("08:00", "20:00")),
    FreqOption("Three times a day", "8:00 AM · 2:00 PM · 8:00 PM", listOf("08:00", "14:00", "20:00")),
    FreqOption("Specific days", "Choose which days below", listOf("08:00"))
)

private val doseUnits = listOf("mg", "g", "ml", "IU")
private val qtyUnits = listOf("tablet", "capsule", "drop", "ml", "puff")
private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
private val dayNamesFull = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun AddEditMedicineScreen(
    initial: Medicine?,
    vm: MedicineViewModel,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val initialStrength = remember(initial?.id) { splitAmountUnit(initial?.strength ?: "", "mg") }

    var step by remember { mutableIntStateOf(0) }
    var saved by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var doseAmount by rememberSaveable { mutableStateOf(if (initial == null) "" else initialStrength.first) }
    var doseUnit by rememberSaveable { mutableStateOf(if (initial == null) "mg" else initialStrength.second) }
    var qtyAmount by rememberSaveable { mutableStateOf("1") }
    var qtyUnit by rememberSaveable { mutableStateOf("tablet") }
    var photoPath by rememberSaveable { mutableStateOf(initial?.photoPath) }
    var pendingFile by remember { mutableStateOf<File?>(null) }

    var frequency by remember { mutableIntStateOf(0) }
    var daysMask by remember { mutableIntStateOf(0b0011111) }
    var durationDays by remember { mutableIntStateOf(0) }
    var times by remember { mutableStateOf(listOf("08:00")) }

    LaunchedEffect(initial?.id) {
        if (initial != null) {
            val existing = vm.schedulesFor(initial.id)
            val first = existing.firstOrNull()
            if (first != null) {
                val (qty, unit) = splitAmountUnit(first.doseLabel, "tablet")
                if (qty.isNotBlank()) {
                    qtyAmount = qty
                    qtyUnit = unit
                }
                times = first.times.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    .ifEmpty { listOf("08:00") }
                daysMask = if (first.type == ScheduleType.WEEKDAYS) first.daysMask.let {
                    if (it == 0) 0b0011111 else it
                } else 0b0011111
                frequency = when {
                    first.type == ScheduleType.WEEKDAYS -> 3
                    times == listOf("08:00") -> 0
                    times == listOf("08:00", "20:00") -> 1
                    times == listOf("08:00", "14:00", "20:00") -> 2
                    else -> 0
                }
                durationDays = first.endDate?.let { end ->
                    val days = ((end - System.currentTimeMillis()) / 86_400_000L).toInt()
                    listOf(7, 30, 90).minByOrNull { kotlin.math.abs(it - days) }?.takeIf { days > 0 } ?: 0
                } ?: 0
            }
        }
    }

    BackHandler {
        when {
            saved -> onDone()
            step > 0 -> step--
            else -> onCancel()
        }
    }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) photoPath = pendingFile?.absolutePath }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) photoPath = PhotoStorage.copyToInternal(context, uri) }

    if (saved) {
        SuccessScreen(
            name = name.ifBlank { "Medicine" },
            strength = "$doseAmount $doseUnit".trim(),
            timeLabel = firstDoseLabel(times.firstOrNull() ?: "08:00"),
            onDone = onDone,
            onAddAnother = {
                name = ""
                doseAmount = ""
                doseUnit = "mg"
                qtyAmount = "1"
                qtyUnit = "tablet"
                photoPath = null
                frequency = 0
                daysMask = 0b0011111
                durationDays = 0
                times = listOf("08:00")
                step = 0
                saved = false
            }
        )
        return
    }

    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (step > 0) step-- else onCancel() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = "Step ${step + 1} of 3 · ${listOf("Details", "Schedule", "Review")[step]}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (step + 1) / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 20.dp)
            ) {
                when (step) {
                    0 -> DetailsStep(
                        name = name,
                        onName = { name = it },
                        doseAmount = doseAmount,
                        onDoseAmount = { doseAmount = it },
                        doseUnit = doseUnit,
                        onDoseUnit = { doseUnit = it },
                        qtyAmount = qtyAmount,
                        onQtyAmount = { qtyAmount = it },
                        qtyUnit = qtyUnit,
                        onQtyUnit = { qtyUnit = it },
                        photoPath = photoPath,
                        onPickPhoto = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onTakePhoto = {
                            val file = PhotoStorage.newPhotoFile(context)
                            pendingFile = file
                            val uri = FileProvider.getUriForFile(
                                context,
                                context.packageName + ".fileprovider",
                                file
                            )
                            takePicture.launch(uri)
                        }
                    )
                    1 -> ScheduleStep(
                        frequency = frequency,
                        onFrequency = { index ->
                            frequency = index
                            if (index != 3) times = freqOptions[index].times
                        },
                        daysMask = daysMask,
                        onToggleDay = { index ->
                            daysMask = daysMask xor (1 shl index)
                        },
                        durationDays = durationDays,
                        onDuration = { durationDays = it }
                    )
                    else -> ReviewStep(
                        name = name.ifBlank { "Medicine" },
                        strength = "$doseAmount $doseUnit".trim(),
                        doseLabel = "$qtyAmount $qtyUnit".trim(),
                        photoPath = photoPath,
                        onEdit = { step = 0 },
                        timeLabel = times.joinToString(" · ") { formatTimeLabel(it) },
                        daysLabel = daysLabel(daysMask, frequency),
                        durationLabel = if (durationDays == 0) "Ongoing" else "$durationDays days"
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    if (step == 2 && initial != null) {
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
                        Spacer(Modifier.height(10.dp))
                    }
                    GradientPillButton(
                        text = when (step) {
                            0 -> "Continue"
                            1 -> "Review"
                            else -> "Save medicine"
                        },
                        icon = if (step == 2) Icons.Rounded.Check else Icons.Rounded.ChevronRight,
                        onClick = {
                            if (step < 2) {
                                step++
                            } else {
                                val base = initial ?: Medicine(name = "")
                                val schedule = Schedule(
                                    id = initial?.let { 0L } ?: 0L,
                                    medicineId = base.id,
                                    type = if (frequency == 3) ScheduleType.WEEKDAYS else ScheduleType.DAILY,
                                    times = times.joinToString(","),
                                    daysMask = if (frequency == 3) daysMask else 0,
                                    doseLabel = "$qtyAmount $qtyUnit".trim(),
                                    endDate = if (durationDays > 0) {
                                        System.currentTimeMillis() + durationDays * 86_400_000L
                                    } else null,
                                    enabled = true
                                )
                                val medicine = base.copy(
                                    name = name.trim(),
                                    strength = "$doseAmount $doseUnit".trim(),
                                    photoPath = photoPath
                                )
                                vm.saveMedicine(medicine, listOf(schedule)) { saved = true }
                            }
                        },
                        enabled = step != 0 || name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsStep(
    name: String,
    onName: (String) -> Unit,
    doseAmount: String,
    onDoseAmount: (String) -> Unit,
    doseUnit: String,
    onDoseUnit: (String) -> Unit,
    qtyAmount: String,
    onQtyAmount: (String) -> Unit,
    qtyUnit: String,
    onQtyUnit: (String) -> Unit,
    photoPath: String?,
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit
) {
    Text(
        "Add medicine",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "Enter the details from the medicine label. You can change these anytime.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(18.dp))

    FieldLabel("Medicine name")
    OutlinedTextField(
        value = name,
        onValueChange = onName,
        placeholder = { Text("e.g. Metformin") },
        leadingIcon = { Icon(Icons.Rounded.Medication, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(16.dp))
    FieldLabel("Dose", hint = "(amount per intake)")
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = doseAmount,
            onValueChange = { onDoseAmount(it.filter { c -> c.isDigit() || c == '.' }) },
            placeholder = { Text("500") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        UnitDropdown(value = doseUnit, options = doseUnits, onSelect = onDoseUnit)
    }

    Spacer(Modifier.height(16.dp))
    FieldLabel("Medicine photo", hint = "(optional)")
    Surface(
        onClick = onPickPhoto,
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (photoPath != null) {
                AsyncImage(
                    model = File(photoPath),
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
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Add photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onTakePhoto,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Take a photo")
    }

    Spacer(Modifier.height(16.dp))
    FieldLabel("How many at each time?")
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = qtyAmount,
            onValueChange = { onQtyAmount(it.filter { c -> c.isDigit() || c == '.' }) },
            placeholder = { Text("1") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        UnitDropdown(value = qtyUnit, options = qtyUnits, onSelect = onQtyUnit)
    }
}

@Composable
private fun ScheduleStep(
    frequency: Int,
    onFrequency: (Int) -> Unit,
    daysMask: Int,
    onToggleDay: (Int) -> Unit,
    durationDays: Int,
    onDuration: (Int) -> Unit
) {
    Text(
        "How often?",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "Choose how frequently you take this medicine. We'll remind you at the right times.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(18.dp))

    FieldLabel("Frequency")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        freqOptions.forEachIndexed { index, option ->
            FrequencyOptionRow(
                title = option.title,
                subtitle = option.sub,
                selected = frequency == index,
                onClick = { onFrequency(index) }
            )
        }
    }

    Spacer(Modifier.height(18.dp))
    FieldLabel("Days", hint = "(tap to toggle)")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dayLabels.forEachIndexed { index, label ->
            val selected = (daysMask and (1 shl index)) != 0
            DayPill(
                label = label,
                selected = selected,
                onClick = { onToggleDay(index) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(Modifier.height(18.dp))
    FieldLabel("For how long?", hint = "(optional)")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(0 to "Ongoing", 7 to "7 days", 30 to "30 days", 90 to "90 days").forEach { (days, label) ->
            DurationChip(
                label = label,
                selected = durationDays == days,
                onClick = { onDuration(days) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReviewStep(
    name: String,
    strength: String,
    doseLabel: String,
    photoPath: String?,
    onEdit: () -> Unit,
    timeLabel: String,
    daysLabel: String,
    durationLabel: String
) {
    Text(
        "Check the details",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "Make sure everything looks right before saving.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(18.dp))

    MedCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (photoPath != null) {
                AsyncImage(
                    model = File(photoPath),
                    contentDescription = name,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    listOf(strength, doseLabel).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        ReviewRow(Icons.Rounded.Schedule, "Time", timeLabel)
        Spacer(Modifier.height(10.dp))
        ReviewRow(Icons.Rounded.Schedule, "Days", daysLabel)
        Spacer(Modifier.height(10.dp))
        ReviewRow(Icons.Rounded.Schedule, "Duration", durationLabel)
    }

    Spacer(Modifier.height(14.dp))
    OutlinedButton(
        onClick = onEdit,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Edit details")
    }
}

@Composable
private fun SuccessScreen(
    name: String,
    strength: String,
    timeLabel: String,
    onDone: () -> Unit,
    onAddAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(22.dp)
                    .size(44.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Medicine saved!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "$name is now scheduled. We'll remind you before each dose.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(22.dp))
        MedCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "First dose",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(timeLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(
                listOf(name, strength).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(22.dp))
        GradientPillButton(
            text = "Back to Today",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onAddAnother,
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add another")
        }
    }
}

@Composable
private fun FieldLabel(text: String, hint: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        if (hint != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UnitDropdown(value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FrequencyOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(modifier = Modifier.height(46.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DurationChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(modifier = Modifier.height(42.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ReviewRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun splitAmountUnit(value: String, defaultUnit: String): Pair<String, String> {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return "" to defaultUnit
    val match = Regex("^([0-9.]+)\\s*(.*)$").find(trimmed)
    return if (match != null) {
        val amount = match.groupValues[1]
        val unit = match.groupValues[2].ifBlank { defaultUnit }
        amount to unit
    } else {
        "" to defaultUnit
    }
}

private fun daysLabel(mask: Int, frequency: Int): String = when {
    frequency != 3 -> "Every day"
    mask == 0b1111111 -> "Every day"
    mask == 0b0011111 -> "Mon – Fri"
    else -> dayNamesFull.filterIndexed { index, _ -> (mask and (1 shl index)) != 0 }
        .joinToString(", ")
        .ifBlank { "No days" }
}

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

private fun firstDoseLabel(time: String): String {
    val parts = time.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }
    val prefix = if (cal.timeInMillis > System.currentTimeMillis()) "Today" else "Tomorrow"
    val fmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(cal.timeInMillis))
    return "$prefix · $fmt"
}
