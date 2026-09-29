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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Sms
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.medremind.app.data.CaregiverLink
import com.medremind.app.data.CaregiverPermission
import com.medremind.app.data.CaregiverStatus
import com.medremind.app.data.DoseStatus
import com.medremind.app.data.IntakeInstruction
import com.medremind.app.data.Medicine
import com.medremind.app.data.PairingRequest
import com.medremind.app.data.Patient
import com.medremind.app.data.Schedule
import com.medremind.app.data.ScheduleType
import com.medremind.app.data.caregiver.QrEncoder
import com.medremind.app.ui.GlassIconButton
import com.medremind.app.ui.GradientPillButton
import com.medremind.app.ui.MedCard
import com.medremind.app.ui.MedClickableCard
import com.medremind.app.ui.MedConfirmDialog
import com.medremind.app.ui.MedEmptyState
import com.medremind.app.ui.MedIconSquare
import com.medremind.app.ui.MedSegmentedButtons
import com.medremind.app.ui.ScreenHeader
import com.medremind.app.ui.StatusChip
import com.medremind.app.ui.MedicineViewModel
import com.medremind.app.ui.ProgressRing
import kotlinx.coroutines.launch
import java.io.File
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
                OutlinedButton(
                    onClick = { vm.demoConnectCaregiver() },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Demo: simulate a caregiver (testing)") }
                Text(
                    "For testing without a cloud connection \u2014 connects instantly on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
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
                            val relationLabel = patient?.relation?.takeIf { it.isNotBlank() }
                            Text(
                                when {
                                    link.status != CaregiverStatus.ACTIVE -> "Waiting for approval"
                                    relationLabel != null -> "$relationLabel \u00b7 Tap to open"
                                    else -> "Tap to open dashboard"
                                },
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
            OutlinedButton(
                onClick = { vm.demoConnectPatient() },
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Demo: simulate a patient (testing)") }
            Text(
                "Creates a sample patient with a few medicines so you can test Remind now.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
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
            onAdd = { name, relation, phone ->
                vm.addPatient(name, relation, phone) { }
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
    onAdd: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
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
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } },
                    label = { Text("Phone (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onAdd(name, relation, phone) }
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
    onEditMedicine: (Medicine) -> Unit,
    onViewAs: () -> Unit = {}
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
    val patientObj = patient.firstOrNull { it.id == profileId }
    val patientName = patientObj?.name ?: "Patient"
    val relation = patientObj?.relation?.takeIf { it.isNotBlank() }
    val patientTitle = if (relation != null) "$patientName ($relation)" else patientName
    val todayLabel = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())

    var section by remember { mutableIntStateOf(0) }
    var todayDoses by remember { mutableStateOf<List<PatientDose>>(emptyList()) }
    var progress by remember { mutableStateOf(CaregiverViewModel.Progress(0, 0, 0, 0)) }
    var adherence by remember { mutableStateOf(CaregiverViewModel.Adherence(0, 0, 0)) }
    var reload by remember { mutableIntStateOf(0) }
    var removeMedicine by remember { mutableStateOf<Medicine?>(null) }
    var detail by remember { mutableStateOf<Medicine?>(null) }
    var showRemind by remember { mutableStateOf(false) }
    var editPatient by remember { mutableStateOf(false) }
    var week by remember { mutableStateOf<List<CaregiverViewModel.DayAdherence>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(profileId, reload) {
        todayDoses = vm.dosesOn(profileId, java.time.LocalDate.now())
        progress = vm.todayProgress(profileId)
        adherence = vm.adherence(profileId, 7)
        week = vm.weekAdherence(profileId)
    }

    fun remind(medicine: Medicine) {
        vm.remindNow(profileId, medicine) { message ->
            scope.launch { snackbar.showSnackbar(message) }
        }
        reload++
    }

    val now = System.currentTimeMillis()
    val nextDose = todayDoses
        .filter { it.status == DoseStatus.PENDING && it.scheduledAt > now }
        .minByOrNull { it.scheduledAt }
    val lowStock = medicines.filter {
        it.quantity > 0 && it.refillThreshold > 0 && it.quantity <= it.refillThreshold
    }
    val attentionCount = todayDoses.count {
        it.status == DoseStatus.MISSED || it.status == DoseStatus.PENDING
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = {
                            medicineVm.setActiveProfile(profileId, patientName)
                            onViewAs()
                        },
                        shape = RoundedCornerShape(50)
                    ) { Text("View as") }
                }

                Spacer(Modifier.height(12.dp))
                val phone = patientObj?.phone.orEmpty()
                CaregiverPatientCard(
                    patientName = patientName,
                    patientTitle = patientTitle,
                    todayLabel = todayLabel,
                    avatarPath = patientObj?.avatarPath,
                    profileId = profileId,
                    progress = progress,
                    attentionCount = attentionCount,
                    canContact = phone.isNotBlank(),
                    onRemind = { showRemind = true },
                    onCall = { callNumber(context, phone) },
                    onMessage = { messageNumber(context, phone, patientName) },
                    onEdit = { editPatient = true }
                )

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
                        nextDose = nextDose,
                        adherence = adherence,
                        week = week,
                        lowStock = lowStock,
                        patientName = patientName,
                        onRemind = { remind(it) },
                        onRefresh = { reload++ }
                    )
                    1 -> MedicinesSection(
                        medicines = medicines,
                        schedules = schedules,
                        onRemind = { remind(it) },
                        onEdit = onEditMedicine,
                        onDelete = { removeMedicine = it },
                        onOpenDetail = { detail = it },
                        onAdd = onAddMedicine
                    )
                    else -> ActivitySection(activity)
                }
            }
        }

        detail?.let { medicine ->
            CaregiverMedicineDetail(
                medicine = medicine,
                schedules = schedules.filter { it.medicineId == medicine.id },
                onBack = { detail = null },
                onRemind = { remind(medicine) },
                onEdit = {
                    detail = null
                    onEditMedicine(medicine)
                },
                onRemove = {
                    detail = null
                    removeMedicine = medicine
                }
            )
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

    if (showRemind) {
        RemindPickerSheet(
            patientName = patientName,
            medicines = medicines,
            onDismiss = { showRemind = false },
            onRemind = { medicine ->
                showRemind = false
                remind(medicine)
            }
        )
    }

    if (editPatient) {
        patientObj?.let { p ->
            EditPatientDialog(
                patient = p,
                onDismiss = { editPatient = false },
                onSave = { updated ->
                    vm.updatePatient(updated)
                    editPatient = false
                }
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RemindPickerSheet(
    patientName: String,
    medicines: List<Medicine>,
    onDismiss: () -> Unit,
    onRemind: (Medicine) -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Remind now", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Choose the medicine to remind $patientName about.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            if (medicines.isEmpty()) {
                Text(
                    "No medicines yet. Add one first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    medicines.forEach { medicine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRemind(medicine) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MedIconSquare(
                                label = medicine.name,
                                seed = medicine.id,
                                photoPath = medicine.photoPath
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                listOf(medicine.name, medicine.strength)
                                    .filter { it.isNotBlank() }.joinToString(" "),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditPatientDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (Patient) -> Unit
) {
    var name by remember { mutableStateOf(patient.name) }
    var relation by remember { mutableStateOf(patient.relation) }
    var phone by remember { mutableStateOf(patient.phone) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit person") },
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
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } },
                    label = { Text("Phone (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(patient.copy(name = name.trim(), relation = relation.trim(), phone = phone.trim()))
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun callNumber(context: android.content.Context, phone: String) {
    if (phone.isBlank()) return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + phone.trim()))
        )
    }
}

private fun messageNumber(context: android.content.Context, phone: String, name: String) {
    if (phone.isBlank()) return
    runCatching {
        val intent = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:" + phone.trim()))
        intent.putExtra("sms_body", "Hi $name, just checking in. How are you feeling?")
        context.startActivity(intent)
    }
}

@Composable
private fun CaregiverPatientCard(
    patientName: String,
    patientTitle: String,
    todayLabel: String,
    avatarPath: String?,
    profileId: Long,
    progress: CaregiverViewModel.Progress,
    attentionCount: Int,
    canContact: Boolean,
    onRemind: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onEdit: () -> Unit
) {
    val percent = if (progress.total == 0) 0 else progress.confirmed * 100 / progress.total
    val (statusLabel, statusTint) = when {
        progress.total == 0 -> "No doses today" to MaterialTheme.colorScheme.onSurfaceVariant
        attentionCount > 0 -> "$attentionCount need attention" to Color(0xFFD97706)
        else -> "On track today" to Color(0xFF0A7F4F)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedIconSquare(
                    label = patientName,
                    seed = profileId,
                    size = 58.dp,
                    photoPath = avatarPath
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        patientTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Text(
                        todayLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusTint.copy(alpha = 0.14f),
                        contentColor = statusTint
                    ) {
                        Text(
                            statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                ProgressRing(
                    percent = percent,
                    modifier = Modifier.size(66.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    progressColor = MaterialTheme.colorScheme.primary,
                    strokeWidth = 7.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${progress.confirmed}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "of ${progress.total}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction(
                    label = "Remind",
                    icon = Icons.Rounded.NotificationsActive,
                    tint = MaterialTheme.colorScheme.primary,
                    enabled = true,
                    onClick = onRemind,
                    modifier = Modifier.weight(1f)
                )
                QuickAction(
                    label = "Call",
                    icon = Icons.Rounded.Call,
                    tint = Color(0xFF12B76A),
                    enabled = canContact,
                    onClick = onCall,
                    modifier = Modifier.weight(1f)
                )
                QuickAction(
                    label = "Message",
                    icon = Icons.Rounded.Sms,
                    tint = Color(0xFF2563EB),
                    enabled = canContact,
                    onClick = onMessage,
                    modifier = Modifier.weight(1f)
                )
                QuickAction(
                    label = "Edit",
                    icon = Icons.Rounded.Edit,
                    tint = MaterialTheme.colorScheme.secondary,
                    enabled = true,
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = tint.copy(alpha = if (enabled) 0.14f else 0.06f),
        contentColor = if (enabled) tint else tint.copy(alpha = 0.4f),
        modifier = modifier.height(62.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TodaySection(
    progress: CaregiverViewModel.Progress,
    doses: List<PatientDose>,
    nextDose: PatientDose?,
    adherence: CaregiverViewModel.Adherence,
    week: List<CaregiverViewModel.DayAdherence>,
    lowStock: List<Medicine>,
    patientName: String,
    onRemind: (Medicine) -> Unit,
    onRefresh: () -> Unit
) {
    val attention = doses.filter {
        it.status == DoseStatus.MISSED || it.status == DoseStatus.PENDING
    }

    nextDose?.let { dose ->
        MedCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "NEXT DOSE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedIconSquare(
                    label = dose.medicine.name,
                    seed = dose.medicine.id,
                    photoPath = dose.medicine.photoPath
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        dose.medicine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        timeLabel(dose.scheduledAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
        if (adherence.scheduled > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Last 7 days \u00b7 ${adherence.taken} of ${adherence.scheduled} taken",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        RoundedCornerShape(50)
                    )
            ) {
                val pct = adherence.taken * 100 / adherence.scheduled
                Box(
                    modifier = Modifier
                        .fillMaxWidth((pct / 100f).coerceIn(0f, 1f))
                        .height(8.dp)
                        .background(Color(0xFF0A7F4F), RoundedCornerShape(50))
                )
            }
        }
    }

    WeekStrip(week)

    if (lowStock.isNotEmpty()) {
        Text("Running low", style = MaterialTheme.typography.titleMedium)
        lowStock.forEach { medicine ->
            MedCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MedIconSquare(
                        label = medicine.name,
                        seed = medicine.id,
                        photoPath = medicine.photoPath
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(medicine.name, fontWeight = FontWeight.Bold)
                        Text(
                            "${medicine.quantity} left",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }
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
private fun WeekStrip(week: List<CaregiverViewModel.DayAdherence>) {
    if (week.isEmpty()) return
    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
    MedCard(modifier = Modifier.fillMaxWidth()) {
        Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            week.forEach { day ->
                val fraction = if (day.total == 0) 0f else day.taken.toFloat() / day.total
                val barColor = when {
                    day.total == 0 -> MaterialTheme.colorScheme.surfaceContainerHighest
                    fraction >= 0.7f -> Color(0xFF0A7F4F)
                    fraction >= 0.4f -> Color(0xFFD97706)
                    else -> Color(0xFFD53862)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((56f * fraction).coerceAtLeast(6f).dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(barColor)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        letters.getOrElse(day.dow - 1) { "?" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
    onOpenDetail: (Medicine) -> Unit,
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

        MedClickableCard(
            onClick = { onOpenDetail(medicine) },
            modifier = Modifier.fillMaxWidth()
        ) {
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

// =============================================================================
// MEDICINE DETAIL
// =============================================================================

@Composable
private fun CaregiverMedicineDetail(
    medicine: Medicine,
    schedules: List<Schedule>,
    onBack: () -> Unit,
    onRemind: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    BackHandler { onBack() }
    val isPrn = schedules.any { it.type == ScheduleType.AS_NEEDED }
    val dose = schedules.firstOrNull()?.doseLabel.orEmpty()
    val scheduleLabel = if (isPrn) "As needed"
    else schedules.map { it.times }.firstOrNull()?.replace(",", " \u00b7 ") ?: ""
    val intake = IntakeInstruction.label(medicine.intakeInstruction)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader("Medicine", onBack = onBack)

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val photo = medicine.photoPath
                    if (photo != null) {
                        AsyncImage(
                            model = File(photo),
                            contentDescription = medicine.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }

            Text(
                listOf(medicine.name, medicine.strength)
                    .filter { it.isNotBlank() }.joinToString(" "),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            if (dose.isNotBlank()) {
                Text("Dose \u00b7 $dose", style = MaterialTheme.typography.bodyLarge)
            }
            if (scheduleLabel.isNotBlank()) {
                Text("Schedule \u00b7 $scheduleLabel", style = MaterialTheme.typography.bodyLarge)
            }
            if (intake.isNotBlank()) {
                Text("How to take \u00b7 $intake", style = MaterialTheme.typography.bodyLarge)
            }
            if (isPrn) {
                Text(
                    "Take only when needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (medicine.quantity > 0) {
                MedCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Stock", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${medicine.quantity} remaining",
                        fontWeight = FontWeight.Bold,
                        color = if (medicine.refillThreshold > 0 && medicine.quantity <= medicine.refillThreshold) {
                            Color(0xFFB45309)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            GradientPillButton(
                text = "Remind now",
                icon = Icons.Rounded.NotificationsActive,
                modifier = Modifier.fillMaxWidth(),
                onClick = onRemind
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f)
                ) { Text("Edit") }
                OutlinedButton(
                    onClick = onRemove,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f)
                ) { Text("Remove") }
            }
        }
    }
}
