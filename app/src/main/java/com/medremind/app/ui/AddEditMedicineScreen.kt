package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NoMeals
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.medremind.app.data.IntakeInstruction
import com.medremind.app.data.Medicine
import com.medremind.app.data.MedicineForm
import com.medremind.app.data.PhotoStorage
import com.medremind.app.data.Schedule
import com.medremind.app.data.ScheduleType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val doseUnits = listOf("mg", "g", "ml", "IU")
private val qtyUnits = listOf("tablet", "capsule", "drop", "ml", "puff")
private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
private val dayNamesFull = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val doseOrdinals = listOf(
    "1st dose", "2nd dose", "3rd dose", "4th dose", "5th dose",
    "6th dose", "7th dose", "8th dose", "9th dose", "10th dose"
)

private fun suggestedTimes(count: Int): List<String> = when (count) {
    1 -> listOf("08:00")
    2 -> listOf("08:00", "20:00")
    3 -> listOf("08:00", "14:00", "20:00")
    4 -> listOf("06:00", "12:00", "18:00", "00:00")
    else -> {
        val interval = 12.0 / (count - 1)
        (0 until count).map { i ->
            val hour = 8 + interval * i
            val h = hour.toInt()
            val m = ((hour - h) * 60).toInt()
            String.format("%02d:%02d", h, m)
        }
    }
}

private fun timeOfDayColor(hour: Int): Color = when {
    hour in 5..11 -> Color(0xFFFB923C)
    hour in 12..16 -> Color(0xFFF59E0B)
    hour in 17..20 -> Color(0xFF8B5CF6)
    else -> Color(0xFF3B82F6)
}

private fun timeOfDayLabel(hour: Int): String = when {
    hour in 5..11 -> "Morning"
    hour in 12..16 -> "Afternoon"
    hour in 17..20 -> "Evening"
    else -> "Night"
}

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
    var showPhotoSheet by remember { mutableStateOf(false) }
    var unitTarget by remember { mutableStateOf<Int?>(null) }

    var stockAmount by rememberSaveable {
        mutableStateOf((initial?.quantity ?: 0).takeIf { it > 0 }?.toString() ?: "")
    }
    var refillBelow by rememberSaveable {
        mutableStateOf((initial?.refillThreshold ?: 0).takeIf { it > 0 }?.toString() ?: "")
    }
    var intake by rememberSaveable { mutableStateOf(initial?.intakeInstruction ?: "") }
    var form by rememberSaveable {
        mutableStateOf(initial?.form ?: com.medremind.app.data.MedicineForm.TABLET)
    }

    var timesCount by remember { mutableIntStateOf(2) }
    var isCustomCount by remember { mutableStateOf(false) }
    var asNeeded by rememberSaveable { mutableStateOf(false) }
    var specificDaysOnly by remember { mutableStateOf(false) }
    var daysMask by remember { mutableIntStateOf(0b0011111) }
    var durationDays by remember { mutableIntStateOf(0) }
    var times by remember { mutableStateOf(listOf("08:00", "20:00")) }
    var editingTimeIndex by remember { mutableStateOf<Int?>(null) }

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
                timesCount = times.size.coerceIn(1, 10)
                isCustomCount = timesCount > 4
                asNeeded = first.type == ScheduleType.AS_NEEDED
                specificDaysOnly = first.type == ScheduleType.WEEKDAYS
                daysMask = if (first.type == ScheduleType.WEEKDAYS) first.daysMask.let {
                    if (it == 0) 0b0011111 else it
                } else 0b0011111
                durationDays = first.endDate?.let { end ->
                    val days = ((end - System.currentTimeMillis()) / 86_400_000L).toInt()
                    days.coerceAtLeast(1)
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
                stockAmount = ""
                refillBelow = ""
                intake = ""
                form = MedicineForm.TABLET
                timesCount = 2
                isCustomCount = false
                asNeeded = false
                specificDaysOnly = false
                daysMask = 0b0011111
                durationDays = 0
                times = listOf("08:00", "20:00")
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
                        text = "Step ${step + 1} of 4 · ${listOf("Details", "Schedule", "Intake", "Review")[step]}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Stepper(
                step = step,
                total = 4,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 20.dp)
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (slideInHorizontally(tween(320)) { w -> if (forward) w else -w } +
                            fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(tween(260)) { w -> if (forward) -w else w } +
                                fadeOut(tween(180)))
                    },
                    label = "stepTransition"
                ) { stepIndex ->
                    Column {
                        when (stepIndex) {
                    0 -> DetailsStep(
                        name = name,
                        onName = { name = it },
                        doseAmount = doseAmount,
                        onDoseAmount = { doseAmount = it },
                        doseUnit = doseUnit,
                        qtyAmount = qtyAmount,
                        onQtyAmount = { qtyAmount = it },
                        qtyUnit = qtyUnit,
                        photoPath = photoPath,
                        onPhotoClick = { showPhotoSheet = true },
                        onOpenUnit = { target -> unitTarget = target },
                        form = form,
                        onForm = { form = it }
                    )
                    1 -> ScheduleStep(
                        asNeeded = asNeeded,
                        onAsNeeded = { asNeeded = it },
                        timesCount = timesCount,
                        isCustomCount = isCustomCount,
                        onSelectCount = { count, custom ->
                            isCustomCount = custom
                            timesCount = count
                            times = suggestedTimes(count)
                        },
                        specificDaysOnly = specificDaysOnly,
                        onSelectDayType = { specific ->
                            specificDaysOnly = specific
                            if (specific && daysMask == 0) daysMask = 0b0011111
                        },
                        daysMask = daysMask,
                        onToggleDay = { index -> daysMask = daysMask xor (1 shl index) },
                        times = times,
                        onEditTime = { index -> editingTimeIndex = index },
                        durationDays = durationDays,
                        onDuration = { durationDays = it }
                    )
                    2 -> IntakeInventoryStep(
                        intake = intake,
                        onIntake = { intake = it },
                        stockAmount = stockAmount,
                        onStockAmount = { stockAmount = it },
                        refillBelow = refillBelow,
                        onRefillBelow = { refillBelow = it }
                    )
                    else -> ReviewStep(
                        name = name.ifBlank { "Medicine" },
                        strength = "$doseAmount $doseUnit".trim(),
                        doseLabel = "$qtyAmount $qtyUnit".trim(),
                        photoPath = photoPath,
                        onEdit = { step = 0 },
                        formLabel = MedicineForm.label(form),
                        timeLabel = if (asNeeded) "Take when needed"
                        else times.joinToString(" · ") { formatTimeLabel(it) },
                        daysLabel = if (asNeeded) "As needed"
                        else if (specificDaysOnly) daysLabel(daysMask) else "Every day",
                        durationLabel = if (asNeeded) "Ongoing"
                        else if (durationDays == 0) "Continue" else "$durationDays days",
                        intakeLabel = com.medremind.app.data.IntakeInstruction.label(intake),
                        inventoryLabel = buildInventoryLabel(stockAmount, refillBelow)
                    )
                        }
                    }
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
                    if (step == 3 && initial != null && initial.id != 0L) {
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
                            0, 1 -> "Continue"
                            2 -> "Review"
                            else -> "Save medicine"
                        },
                        icon = if (step == 3) Icons.Rounded.Check else Icons.Rounded.ChevronRight,
                        onClick = {
                            if (step < 3) {
                                step++
                            } else {
                                val base = initial ?: Medicine(name = "")
                                val schedule = Schedule(
                                    id = initial?.let { 0L } ?: 0L,
                                    medicineId = base.id,
                                    type = when {
                                        asNeeded -> ScheduleType.AS_NEEDED
                                        specificDaysOnly -> ScheduleType.WEEKDAYS
                                        else -> ScheduleType.DAILY
                                    },
                                    times = if (asNeeded) "" else times.joinToString(","),
                                    daysMask = if (specificDaysOnly && !asNeeded) daysMask else 0,
                                    doseLabel = "$qtyAmount $qtyUnit".trim(),
                                    endDate = if (!asNeeded && durationDays > 0) {
                                        System.currentTimeMillis() + durationDays * 86_400_000L
                                    } else null,
                                    enabled = true
                                )
                                val medicine = base.copy(
                                    name = name.trim(),
                                    strength = "$doseAmount $doseUnit".trim(),
                                    photoPath = photoPath,
                                    quantity = stockAmount.toIntOrNull() ?: 0,
                                    refillThreshold = refillBelow.toIntOrNull() ?: 0,
                                    intakeInstruction = intake,
                                    form = form
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

    if (showPhotoSheet) {
        PhotoSourceSheet(
            onDismiss = { showPhotoSheet = false },
            onCamera = {
                showPhotoSheet = false
                val file = PhotoStorage.newPhotoFile(context)
                pendingFile = file
                val uri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".fileprovider",
                    file
                )
                takePicture.launch(uri)
            },
            onGallery = {
                showPhotoSheet = false
                pickImage.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }

    unitTarget?.let { target ->
        UnitSheet(
            options = if (target == 0) doseUnits else qtyUnits,
            current = if (target == 0) doseUnit else qtyUnit,
            onSelect = { value ->
                if (target == 0) doseUnit = value else qtyUnit = value
                unitTarget = null
            },
            onDismiss = { unitTarget = null }
        )
    }

    editingTimeIndex?.let { index ->
        TimePickerDialog(
            initial = times.getOrElse(index) { "08:00" },
            onDismiss = { editingTimeIndex = null },
            onConfirm = { hour, minute ->
                val newTime = String.format("%02d:%02d", hour, minute)
                times = times.toMutableList().also {
                    if (index in it.indices) it[index] = newTime
                }
                editingTimeIndex = null
            }
        )
    }
}

@Composable
private fun DetailsStep(
    name: String,
    onName: (String) -> Unit,
    doseAmount: String,
    onDoseAmount: (String) -> Unit,
    doseUnit: String,
    qtyAmount: String,
    onQtyAmount: (String) -> Unit,
    qtyUnit: String,
    photoPath: String?,
    onPhotoClick: () -> Unit,
    onOpenUnit: (Int) -> Unit,
    form: String,
    onForm: (String) -> Unit
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
    FieldLabel("Form")
    FilterChipRow(
        options = MedicineForm.all.map { MedicineForm.label(it) },
        selectedIndex = MedicineForm.all.indexOf(form).coerceAtLeast(0),
        onSelect = { onForm(MedicineForm.all[it]) }
    )

    Spacer(Modifier.height(16.dp))
    FieldLabel("Dose", hint = "(amount per intake)")
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = doseAmount,
            onValueChange = { onDoseAmount(it.filter { c -> c.isDigit() || c == '.' }) },
            placeholder = { Text("500") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        UnitButton(value = doseUnit, onClick = { onOpenUnit(0) })
    }

    Spacer(Modifier.height(16.dp))
    FieldLabel("Medicine photo", hint = "(optional)")
    if (photoPath != null) {
        Surface(
            onClick = onPhotoClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = "Medicine photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        DropZone(
            title = "Add photo",
            subtitle = "Tap to take a photo or choose from gallery",
            onClick = onPhotoClick
        )
    }

    Spacer(Modifier.height(16.dp))
    FieldLabel("How many at each time?")
    Row(verticalAlignment = Alignment.CenterVertically) {
        QuantityStepper(
            value = qtyAmount,
            onValueChange = onQtyAmount,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        UnitButton(value = qtyUnit, onClick = { onOpenUnit(1) })
    }
}

@Composable
private fun QuantityStepper(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberMedHaptics()
    val current = value.toIntOrNull() ?: 1
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepperButton(
                icon = Icons.Rounded.Remove,
                enabled = current > 1,
                onClick = {
                    haptics.tick()
                    onValueChange((current - 1).coerceAtLeast(1).toString())
                }
            )
            Text(
                text = current.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            StepperButton(
                icon = Icons.Rounded.Add,
                enabled = current < 30,
                onClick = {
                    haptics.tick()
                    onValueChange((current + 1).coerceAtMost(30).toString())
                }
            )
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ScheduleStep(
    asNeeded: Boolean,
    onAsNeeded: (Boolean) -> Unit,
    timesCount: Int,
    isCustomCount: Boolean,
    onSelectCount: (Int, Boolean) -> Unit,
    specificDaysOnly: Boolean,
    onSelectDayType: (Boolean) -> Unit,
    daysMask: Int,
    onToggleDay: (Int) -> Unit,
    times: List<String>,
    onEditTime: (Int) -> Unit,
    durationDays: Int,
    onDuration: (Int) -> Unit
) {
    var customCountText by remember { mutableStateOf(if (isCustomCount) timesCount.toString() else "5") }
    var customDurationText by remember { mutableStateOf(if (durationDays > 0) durationDays.toString() else "14") }
    val durationIsCustom = durationDays > 0 && durationDays != 7 && durationDays != 30

    Text(
        "How often?",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "Set the daily frequency, days, and duration.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(16.dp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("As needed", style = MaterialTheme.typography.titleMedium)
                Text(
                    "No fixed times \u2014 log a dose whenever you take it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = asNeeded, onCheckedChange = onAsNeeded)
        }
    }

    if (!asNeeded) {
    Spacer(Modifier.height(18.dp))

    FieldLabel("1. Times per day")
    MedSegmentedButtons(
        options = listOf("1x", "2x", "3x", "4x", "Custom"),
        selectedIndex = if (isCustomCount || timesCount !in 1..4) 4 else timesCount - 1,
        onSelect = { index ->
            if (index == 4) {
                onSelectCount(customCountText.toIntOrNull()?.coerceIn(5, 10) ?: 5, true)
            } else {
                onSelectCount(index + 1, false)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
    AnimatedVisibility(visible = isCustomCount) {
        Column {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "How many times?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = customCountText,
                    onValueChange = { value ->
                        val filtered = value.filter { it.isDigit() }.take(2)
                        customCountText = filtered
                        val n = filtered.toIntOrNull()
                        if (n != null && n in 5..10) onSelectCount(n, true)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(96.dp)
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))
    FieldLabel("2. Which days?")
    MedSegmentedButtons(
        options = listOf("Every day", "Specific days"),
        selectedIndex = if (specificDaysOnly) 1 else 0,
        onSelect = { onSelectDayType(it == 1) },
        modifier = Modifier.fillMaxWidth()
    )
    AnimatedVisibility(visible = specificDaysOnly) {
        Column {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dayLabels.forEachIndexed { index, label ->
                    DayPill(
                        label = label,
                        selected = (daysMask and (1 shl index)) != 0,
                        onClick = { onToggleDay(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (daysMask == 0) {
                    "Select at least one day."
                } else {
                    "Selected: " + dayNamesFull
                        .filterIndexed { index, _ -> (daysMask and (1 shl index)) != 0 }
                        .joinToString(", ")
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (daysMask == 0) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(Modifier.height(20.dp))
    FieldLabel("3. Set times")
    Box {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 22.dp, bottom = 22.dp)
                .offset(x = 7.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            key(times.joinToString(",")) {
                times.forEachIndexed { index, time ->
                    AlarmRow(index = index, time = time, onClick = { onEditTime(index) })
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Tap any time to adjust it.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(20.dp))
    FieldLabel("4. For how long?", hint = "(optional)")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(0 to "Continue", 7 to "7 days", 30 to "30 days", -1 to "Custom").forEach { (days, label) ->
            val selected = if (days == -1) durationIsCustom else durationDays == days
            DurationChip(
                label = label,
                selected = selected,
                onClick = {
                    if (days == -1) {
                        onDuration(customDurationText.toIntOrNull()?.coerceIn(1, 365) ?: 14)
                    } else {
                        onDuration(days)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
    if (durationIsCustom) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "For",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = customDurationText,
                onValueChange = { value ->
                    val filtered = value.filter { it.isDigit() }.take(3)
                    customDurationText = filtered
                    val n = filtered.toIntOrNull()
                    if (n != null && n in 1..365) onDuration(n)
                },
                singleLine = true,
                modifier = Modifier.width(96.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "days",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    }
}

@Composable
private fun AlarmRow(
    index: Int,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(index, time) {
        delay(index * 70L)
        appeared = true
    }
    val hour = time.substringBefore(':').toIntOrNull() ?: 8
    val accent = timeOfDayColor(hour)
    AnimatedVisibility(
        visible = appeared,
        modifier = modifier,
        enter = slideInHorizontally(tween(420)) { it } +
            fadeIn(tween(320)) +
            scaleIn(tween(420), initialScale = 0.95f)
    ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(Modifier.width(14.dp))
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            color = accent.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatTimeLabel(time),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (hour < 12) "AM" else "PM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = timeOfDayLabel(hour),
                        style = MaterialTheme.typography.titleSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = doseOrdinals.getOrElse(index) { "Dose ${index + 1}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val parts = initial.split(':')
    val startHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val startMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val state = rememberTimePickerState(
        initialHour = startHour,
        initialMinute = startMinute,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Set") }
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
private fun IntakeInventoryStep(
    intake: String,
    onIntake: (String) -> Unit,
    stockAmount: String,
    onStockAmount: (String) -> Unit,
    refillBelow: String,
    onRefillBelow: (String) -> Unit
) {
    Text(
        "Intake & inventory",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "How should it be taken, and how much do you have?",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(18.dp))

    Text("Intake instructions", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    Text(
        "Optional \u2014 tap to choose.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(10.dp))
    val intakeOptions = listOf(
        Triple(IntakeInstruction.BEFORE_MEAL, "Before meal", Icons.Rounded.Schedule),
        Triple(IntakeInstruction.WITH_MEAL, "With food", Icons.Rounded.Restaurant),
        Triple(IntakeInstruction.AFTER_MEAL, "After meal", Icons.Rounded.RestaurantMenu),
        Triple(IntakeInstruction.EMPTY_STOMACH, "Empty stomach", Icons.Rounded.NoMeals)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        intakeOptions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (value, label, icon) ->
                    IntakeOptionCard(
                        label = label,
                        icon = icon,
                        selected = intake == value,
                        onClick = { onIntake(if (intake == value) "" else value) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    MedCard(modifier = Modifier.fillMaxWidth()) {
        Text("Inventory & refill", style = MaterialTheme.typography.titleMedium)
        Text(
            "Optional \u2014 count pills and get a low-supply reminder.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        FieldLabel("Pills remaining")
        QuantityStepper(value = stockAmount, onValueChange = onStockAmount)
        Spacer(Modifier.height(12.dp))
        FieldLabel("Remind me when below")
        QuantityStepper(value = refillBelow, onValueChange = onRefillBelow)
    }
}

@Composable
private fun ReviewStep(
    name: String,
    strength: String,
    doseLabel: String,
    photoPath: String?,
    onEdit: () -> Unit,
    formLabel: String,
    timeLabel: String,
    daysLabel: String,
    durationLabel: String,
    intakeLabel: String,
    inventoryLabel: String
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
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
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
                    listOf(strength, doseLabel).filter { it.isNotBlank() }.joinToString(" \u00b7 "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        if (formLabel.isNotBlank()) {
            ReviewRow(Icons.Rounded.Medication, "Form", formLabel)
            Spacer(Modifier.height(10.dp))
        }
        ReviewRow(Icons.Rounded.Schedule, "Time", timeLabel)
        Spacer(Modifier.height(10.dp))
        ReviewRow(Icons.Rounded.Schedule, "Days", daysLabel)
        Spacer(Modifier.height(10.dp))
        ReviewRow(Icons.Rounded.Schedule, "Duration", durationLabel)
        if (intakeLabel.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            ReviewRow(Icons.Rounded.Restaurant, "Intake", intakeLabel)
        }
        if (inventoryLabel.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            ReviewRow(Icons.Rounded.Medication, "Inventory", inventoryLabel)
        }
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
private fun IntakeOptionCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberMedHaptics()
    val bg by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        label = "intakeBg"
    )
    Surface(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 1.6.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = if (selected) MedElevation.card else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MealMascot(icon = icon, selected = selected)
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun MealMascot(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean
) {
    val accent = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    val face = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
    Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val w = size.width
            val h = size.height
            // Capsule body.
            drawRoundRect(
                color = accent,
                topLeft = Offset(w * 0.08f, h * 0.26f),
                size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.44f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.22f, h * 0.22f)
            )
            // Eyes.
            drawCircle(face, radius = w * 0.045f, center = Offset(w * 0.36f, h * 0.46f))
            drawCircle(face, radius = w * 0.045f, center = Offset(w * 0.64f, h * 0.46f))
            // Smile.
            drawArc(
                color = face,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.40f, h * 0.40f),
                size = androidx.compose.ui.geometry.Size(w * 0.20f, h * 0.16f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = w * 0.03f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
        Surface(
            shape = CircleShape,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp))
            }
        }
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
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(320)) +
            slideInVertically(tween(440)) { it / 6 } +
            scaleIn(tween(440), initialScale = 0.94f)
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
private fun UnitButton(value: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceSheet(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Medicine photo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
            SheetOption(Icons.Rounded.PhotoCamera, "Take photo", onCamera)
            SheetOption(Icons.Rounded.PhotoLibrary, "Choose from gallery", onGallery)
            Spacer(Modifier.height(8.dp))
            SheetCancel(onDismiss)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitSheet(
    options: List<String>,
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Select unit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
            options.forEach { option ->
                val selected = option == current
                Surface(
                    onClick = { onSelect(option) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SheetCancel(onDismiss)
        }
    }
}

@Composable
private fun SheetOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SheetCancel(onDismiss: () -> Unit) {
    Surface(
        onClick = onDismiss,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Cancel", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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

private fun buildInventoryLabel(stockAmount: String, refillBelow: String): String {
    val parts = mutableListOf<String>()
    stockAmount.toIntOrNull()?.takeIf { it > 0 }?.let { parts.add("$it in stock") }
    refillBelow.toIntOrNull()?.takeIf { it > 0 }?.let { parts.add("remind below $it") }
    return parts.joinToString(" \u00b7 ")
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

private fun daysLabel(mask: Int): String = when {
    mask == 0b1111111 -> "Every day"
    mask == 0b0011111 -> "Mon – Fri"
    mask == 0 -> "No days"
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
