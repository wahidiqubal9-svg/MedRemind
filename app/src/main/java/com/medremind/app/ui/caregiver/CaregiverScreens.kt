package com.medremind.app.ui.caregiver

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medremind.app.data.CaregiverLink
import com.medremind.app.data.CaregiverPermission
import com.medremind.app.data.CaregiverStatus
import com.medremind.app.data.DoseStatus
import com.medremind.app.data.Medicine
import com.medremind.app.data.PairingRequest
import com.medremind.app.data.Patient
import com.medremind.app.data.Schedule
import com.medremind.app.data.ScheduleType
import com.medremind.app.data.caregiver.QrEncoder
import com.medremind.app.ui.GradientPillButton
import com.medremind.app.ui.MedCard
import com.medremind.app.ui.MedConfirmDialog
import com.medremind.app.ui.MedEmptyState
import com.medremind.app.ui.MedIconSquare
import com.medremind.app.ui.MedSegmentedButtons
import com.medremind.app.ui.ScreenHeader
import com.medremind.app.ui.StatusChip
import com.medremind.app.ui.MedicineViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun timeLabel(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

// =============================================================================
// PATIENT SIDE — Me -> Caregiver
// =============================================================================

@Composable
fun CaregiverHomeScreen(vm: CaregiverViewModel, onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val caregivers by vm.caregiversForMe.collectAsState()
    val pending by vm.pendingForMe.collectAsState()
    val activity by remember { vm.activityFor(0L) }.collectAsState(initial = emptyList())

    var pairing by remember { mutableStateOf<PairingRequest?>(null) }
    var permissionLink by remember { mutableStateOf<CaregiverLink?>(null) }
    var removeLink by remember { mutableStateOf<CaregiverLink?>(null) }
    var busy by remember { mutableStateOf(false) }

    permissionLink?.let { link ->
        CaregiverPermissionScreen(
            link = link,
            onBack = { permissionLink = null },
            onSave = { perms ->
                vm.savePermissions(link, perms)
                permissionLink = null
            }
        )
        return
    }

    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader("Caregiver", onBack = onBack)

            Text(
                "Someone you trust can help you manage your medicines and send reminders.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Pending requests for this patient.
            pending.forEach { link ->
                MedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Shield, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Caregiver request", fontWeight = FontWeight.Bold)
                            Text(
                                "${link.caregiverName} wants to become your caregiver.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradientPillButton(
                            text = "Accept",
                            icon = Icons.Rounded.Check,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.approve(link) }
                        )
                        OutlinedButton(
                            onClick = { vm.decline(link) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) { Text("Decline") }
                    }
                }
            }

            val active = caregivers.filter { it.status == CaregiverStatus.ACTIVE }
            if (active.isEmpty() && pending.isEmpty()) {
                MedEmptyState(
                    icon = Icons.Rounded.Favorite,
                    title = "No caregiver connected",
                    message = "Connect someone you trust to help with your medicines.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }

            active.forEach { link ->
                MedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFE11D48)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Connected caregiver", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(link.caregiverName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Rounded.Verified, contentDescription = null, tint = Color(0xFF0A7F4F))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { permissionLink = link },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) { Text("Manage access") }
                        OutlinedButton(
                            onClick = { removeLink = link },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) { Text("Remove") }
                    }
                }
            }

            if (active.isEmpty()) {
                GradientPillButton(
                    text = "Connect a caregiver",
                    icon = Icons.Rounded.QrCode,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        busy = true
                        scope.launch {
                            pairing = vm.createPairingCode()
                            busy = false
                        }
                    }
                )
            }

            if (activity.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Recent caregiver activity", style = MaterialTheme.typography.titleMedium)
                activity.take(8).forEach { item ->
                    MedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(timeLabel(item.at), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            item.message.ifBlank { item.type.replace('_', ' ').lowercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    pairing?.let { request ->
        PairingQrDialog(
            request = request,
            onShare = {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Connect to my MedRemind with code ${request.code}"
                    )
                }
                runCatching {
                    context.startActivity(Intent.createChooser(share, "Share connection code"))
                }
            },
            onDismiss = { pairing = null }
        )
    }

    removeLink?.let { link ->
        MedConfirmDialog(
            title = "Remove caregiver?",
            message = "${link.caregiverName} will no longer be able to manage your medicines or send reminders.",
            confirmText = "Remove",
            onConfirm = {
                vm.removeCaregiver(link)
                removeLink = null
            },
            onDismiss = { removeLink = null }
        )
    }
}

@Composable
private fun PairingQrDialog(
    request: PairingRequest,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val bitmap = remember(request.token) { QrEncoder.encode(request.token, 560) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect a caregiver") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Ask your caregiver to scan this QR code or enter the connection code.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Pairing QR code",
                        modifier = Modifier
                            .size(210.dp)
                            .padding(12.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text("Temporary connection code", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    request.code,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "This code is temporary and expires shortly. It only pairs devices \u2014 it never contains your medicine information.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onShare) { Text("Share code") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// =============================================================================
// CAREGIVER SIDE — Me -> Caregiving
// =============================================================================

@Composable
fun CaregivingHomeScreen(
    vm: CaregiverViewModel,
    onBack: () -> Unit,
    onOpenPatient: (Long) -> Unit
) {
    BackHandler { onBack() }
    val links by vm.peopleICareFor.collectAsState()
    val patients by vm.patients.collectAsState()
    val pending by vm.pendingICreated.collectAsState()
    val patientById = remember(patients) { patients.associateBy { it.id } }

    var showConnect by remember { mutableStateOf(false) }
    var showAddPatient by remember { mutableStateOf(false) }
    var codeError by remember { mutableStateOf<String?>(null) }

    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader("Caregiving", onBack = onBack)

            Text("People I care for", style = MaterialTheme.typography.titleMedium)

            if (links.isEmpty() && pending.isEmpty()) {
                MedEmptyState(
                    icon = Icons.Rounded.Person,
                    title = "No one connected yet",
                    message = "Connect a family member to help manage their medicines.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                )
            }

            links.forEach { link ->
                val patient = patientById[link.patientProfileId]
                MedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = link.status == CaregiverStatus.ACTIVE) {
                            onOpenPatient(link.patientProfileId)
                        }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MedIconSquare(
                            label = patient?.name ?: "Patient",
                            seed = link.patientProfileId,
                            photoPath = patient?.avatarPath
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                patient?.name ?: "Patient",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (link.status == CaregiverStatus.ACTIVE) "Tap to open dashboard"
                                else "Waiting for approval",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (link.status == CaregiverStatus.ACTIVE) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = Color(0xFF0A7F4F))
                        } else {
                            StatusChip(status = DoseStatus.PENDING, label = "Pending")
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            GradientPillButton(
                text = "Connect someone",
                icon = Icons.Rounded.QrCodeScanner,
                modifier = Modifier.fillMaxWidth(),
                onClick = { showConnect = true }
            )
            OutlinedButton(
                onClick = { showAddPatient = true },
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add someone manually") }
        }
    }

    if (showConnect) {
        ConnectOptionsDialog(
            vm = vm,
            onDismiss = { showConnect = false; codeError = null }
        )
    }
    if (showAddPatient) {
        AddPatientDialog(
            onDismiss = { showAddPatient = false },
            onAdd = { name, relation ->
                vm.addPatient(name, relation) { }
                showAddPatient = false
            }
        )
    }
    codeError?.let { message ->
        AlertDialog(
            onDismissRequest = { codeError = null },
            title = { Text("Connection") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { codeError = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun ConnectOptionsDialog(
    vm: CaregiverViewModel,
    onDismiss: () -> Unit
) {
    var showCode by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (showCode) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Enter connection code") },
            text = {
                Column {
                    Text(
                        "Ask the person you care for to open Me \u2192 Caregiver and read out the 6-digit code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { input ->
                            code = input.filter { it.isDigit() }.take(6)
                            error = null
                        },
                        label = { Text("6-digit code") },
                        singleLine = true,
                        isError = error != null,
                        supportingText = { error?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = code.length == 6,
                    onClick = {
                        scope.launch {
                            vm.requestAccess(code) { message -> error = message }
                        }
                    }
                ) { Text("Send request") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect someone") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OptionRow(
                    icon = Icons.Rounded.QrCodeScanner,
                    title = "Scan QR code",
                    subtitle = "Available once cloud pairing is added."
                ) { }
                OptionRow(
                    icon = Icons.Rounded.Keyboard,
                    title = "Enter connection code",
                    subtitle = "Use the 6-digit code from their app."
                ) { showCode = true }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun OptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddPatientDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add someone") },
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
                    value = relation,
                    onValueChange = { relation = it },
                    label = { Text("Relation (e.g. Mom)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onAdd(name, relation) }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// =============================================================================
// DASHBOARD
// =============================================================================

@Composable
fun CaregiverDashboardScreen(
    vm: CaregiverViewModel,
    medicineVm: MedicineViewModel,
    profileId: Long,
    onBack: () -> Unit,
    onAddMedicine: () -> Unit,
    onEditMedicine: (Medicine) -> Unit
) {
    BackHandler { onBack() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val medicines by remember(profileId) { vm.medicinesFor(profileId) }
        .collectAsState(initial = emptyList())
    val schedules by remember { vm.allSchedules() }
        .collectAsState(initial = emptyList())
    val activity by remember(profileId) { vm.activityFor(profileId) }
        .collectAsState(initial = emptyList())
    val patient by remember(profileId) { vm.patients }
        .collectAsState(initial = emptyList())
    val patientName = patient.firstOrNull { it.id == profileId }?.name ?: "Patient"

    var section by remember { mutableIntStateOf(0) }
    var todayDoses by remember { mutableStateOf<List<PatientDose>>(emptyList()) }
    var progress by remember { mutableStateOf(CaregiverViewModel.Progress(0, 0, 0, 0)) }
    var reload by remember { mutableIntStateOf(0) }
    var removeMedicine by remember { mutableStateOf<Medicine?>(null) }

    LaunchedEffect(profileId, reload) {
        todayDoses = vm.dosesOn(profileId, java.time.LocalDate.now())
        progress = vm.todayProgress(profileId)
    }

    fun remind(medicine: Medicine) {
        vm.remindNow(profileId, medicine) { message ->
            scope.launch { snackbar.showSnackbar(message) }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader(patientName, onBack = onBack)

            MedSegmentedButtons(
                options = listOf("Today", "Medicines", "Activity"),
                selectedIndex = section,
                onSelect = { section = it },
                modifier = Modifier.fillMaxWidth()
            )

            when (section) {
                0 -> TodaySection(
                    progress = progress,
                    doses = todayDoses,
                    onRemind = { remind(it) },
                    onRefresh = { reload++ }
                )
                1 -> MedicinesSection(
                    medicines = medicines,
                    schedules = schedules,
                    onRemind = { remind(it) },
                    onEdit = onEditMedicine,
                    onDelete = { removeMedicine = it },
                    onAdd = onAddMedicine
                )
                else -> ActivitySection(activity)
            }
        }
    }

    removeMedicine?.let { medicine ->
        MedConfirmDialog(
            title = "Remove medicine?",
            message = "${medicine.name} will be removed from $patientName's medication list.",
            confirmText = "Remove",
            onConfirm = {
                medicineVm.deleteMedicine(medicine) { reload++ }
                removeMedicine = null
            },
            onDismiss = { removeMedicine = null }
        )
    }
}

@Composable
private fun TodaySection(
    progress: CaregiverViewModel.Progress,
    doses: List<PatientDose>,
    onRemind: (Medicine) -> Unit,
    onRefresh: () -> Unit
) {
    val attention = doses.filter {
        it.status == DoseStatus.MISSED || it.status == DoseStatus.PENDING
    }
    MedCard(modifier = Modifier.fillMaxWidth()) {
        Text("Today's medication", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "${progress.confirmed} / ${progress.total} confirmed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(Color(0xFF0A7F4F), "Taken ${progress.confirmed}")
            LegendDot(Color(0xFF2563EB), "Upcoming ${progress.upcoming}")
            LegendDot(Color(0xFFD53862), "Missed ${progress.missed}")
        }
        if (progress.total == 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "No doses scheduled today.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (attention.isEmpty() && progress.total > 0) {
        MedCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF0A7F4F))
                Spacer(Modifier.width(10.dp))
                Text("On track", fontWeight = FontWeight.Bold)
            }
        }
    } else if (attention.isNotEmpty()) {
        Text("Needs attention", style = MaterialTheme.typography.titleMedium)
        attention.forEach { dose ->
            MedCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status = dose.status)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            dose.medicine.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Scheduled for ${timeLabel(dose.scheduledAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                GradientPillButton(
                    text = "Remind now",
                    icon = Icons.Rounded.NotificationsActive,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onRemind(dose.medicine) }
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(50))
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MedicinesSection(
    medicines: List<Medicine>,
    schedules: List<Schedule>,
    onRemind: (Medicine) -> Unit,
    onEdit: (Medicine) -> Unit,
    onDelete: (Medicine) -> Unit,
    onAdd: () -> Unit
) {
    GradientPillButton(
        text = "Add medicine",
        modifier = Modifier.fillMaxWidth(),
        onClick = onAdd
    )
    if (medicines.isEmpty()) {
        MedEmptyState(
            icon = Icons.Rounded.Medication,
            title = "No medicines yet",
            message = "Add a medicine to start sending reminders.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        )
    }
    medicines.forEach { medicine ->
        val medSchedules = schedules.filter { it.medicineId == medicine.id }
        val isPrn = medSchedules.any { it.type == ScheduleType.AS_NEEDED }
        val dose = medSchedules.firstOrNull()?.doseLabel.orEmpty()
        val scheduleLabel = if (isPrn) "As needed"
        else medSchedules.map { it.times }.firstOrNull()?.replace(",", " \u00b7 ") ?: ""

        MedCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedIconSquare(
                    label = medicine.name,
                    seed = medicine.id,
                    photoPath = medicine.photoPath
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        listOf(medicine.name, medicine.strength)
                            .filter { it.isNotBlank() }.joinToString(" "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val detail = listOf(dose, scheduleLabel)
                        .filter { it.isNotBlank() }.joinToString(" \u00b7 ")
                    if (detail.isNotBlank()) {
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onEdit(medicine) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f)
                ) { Text("Edit") }
                OutlinedButton(
                    onClick = { onDelete(medicine) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f)
                ) { Text("Remove") }
            }
            Spacer(Modifier.height(8.dp))
            GradientPillButton(
                text = "Remind now",
                icon = Icons.Rounded.NotificationsActive,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onRemind(medicine) }
            )
        }
    }
}

@Composable
private fun ActivitySection(activity: List<com.medremind.app.data.CaregiverActivity>) {
    if (activity.isEmpty()) {
        MedEmptyState(
            icon = Icons.Rounded.CameraAlt,
            title = "No activity yet",
            message = "Caregiver actions will appear here.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        )
        return
    }
    activity.forEach { item ->
        MedCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                timeLabel(item.at),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                item.message.ifBlank { item.type.replace('_', ' ').lowercase() },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// =============================================================================
// PERMISSIONS
// =============================================================================

@Composable
fun CaregiverPermissionScreen(
    link: CaregiverLink,
    onBack: () -> Unit,
    onSave: (Int) -> Unit
) {
    BackHandler { onBack() }
    var permissions by remember(link.id) { mutableIntStateOf(link.permissions) }

    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader("Caregiver access", onBack = onBack)
            Text(
                link.caregiverName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            MedCard(modifier = Modifier.fillMaxWidth()) {
                Text("Medication", style = MaterialTheme.typography.titleMedium)
                medicationOptions().forEach { (flag, label) ->
                    PermissionToggle(
                        label = label,
                        checked = CaregiverPermission.has(permissions, flag),
                        onCheckedChange = { on ->
                            permissions = toggle(permissions, flag, on)
                        }
                    )
                }
            }
            MedCard(modifier = Modifier.fillMaxWidth()) {
                Text("Health", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Health measurements are off unless you allow them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                healthOptions().forEach { (flag, label) ->
                    PermissionToggle(
                        label = label,
                        checked = CaregiverPermission.has(permissions, flag),
                        onCheckedChange = { on ->
                            permissions = toggle(permissions, flag, on)
                        }
                    )
                }
            }

            GradientPillButton(
                text = "Save",
                icon = Icons.Rounded.Check,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSave(permissions) }
            )
        }
    }
}

@Composable
private fun PermissionToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        androidx.compose.material3.Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun medicationOptions(): List<Pair<Int, String>> = listOf(
    CaregiverPermission.VIEW_MEDICINES to "Medicines",
    CaregiverPermission.MANAGE_SCHEDULES to "Medication schedules",
    CaregiverPermission.SEND_REMINDERS to "Send reminders",
    CaregiverPermission.VIEW_CONFIRMATIONS to "Dose confirmations",
    CaregiverPermission.VIEW_HISTORY to "Medication history",
    CaregiverPermission.VIEW_PHOTOS to "Medicine photos"
)

private fun healthOptions(): List<Pair<Int, String>> = listOf(
    CaregiverPermission.HEALTH_BP to "Blood pressure",
    CaregiverPermission.HEALTH_GLUCOSE to "Blood glucose",
    CaregiverPermission.HEALTH_WEIGHT to "Weight"
)

private fun toggle(current: Int, flag: Int, on: Boolean): Int =
    if (on) current or flag else current and flag.inv()
