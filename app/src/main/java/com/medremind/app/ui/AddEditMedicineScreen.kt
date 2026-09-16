package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }
    var showScheduleEditor by remember { mutableStateOf(false) }
    var pendingFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(initial?.id) {
        schedules = if (initial != null) vm.schedulesFor(initial.id) else emptyList()
    }

    BackHandler(enabled = !showScheduleEditor) { onCancel() }

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

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            MedTopAppBar(title = if (initial == null) "Add medicine" else "Edit medicine")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
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
                            Text(
                                "No photo yet",
                                style = MaterialTheme.typography.bodyLarge,
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

            Spacer(Modifier.height(20.dp))

            SectionHeader("Details")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name *") },
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
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            SectionHeader("Reminders")
            Spacer(Modifier.height(8.dp))

            schedules.forEach { schedule ->
                MedClickableCard(
                    onClick = {
                        editingSchedule = schedule
                        showScheduleEditor = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(scheduleSummary(schedule), style = MaterialTheme.typography.bodyLarge)
                    if (!schedule.enabled) {
                        Text(
                            "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    editingSchedule = null
                    showScheduleEditor = true
                },
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add reminder")
            }

            Spacer(Modifier.height(24.dp))

            GradientPillButton(
                text = "Save",
                icon = Icons.Filled.Add,
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
                    Text("Delete medicine")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showScheduleEditor) {
        ScheduleEditorDialog(
            initial = editingSchedule,
            onDismiss = {
                showScheduleEditor = false
                editingSchedule = null
            },
            onSave = { schedule ->
                val existing = editingSchedule
                schedules = if (existing == null) {
                    schedules + schedule
                } else {
                    schedules.map { if (it === existing) schedule else it }
                }
                showScheduleEditor = false
                editingSchedule = null
            },
            onDelete = {
                val existing = editingSchedule
                if (existing != null) schedules = schedules.filter { it !== existing }
                showScheduleEditor = false
                editingSchedule = null
            }
        )
    }
}

private val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

fun scheduleSummary(schedule: Schedule): String = when (schedule.type) {
    ScheduleType.WEEKDAYS -> {
        val days = dayNames.filterIndexed { index, _ -> (schedule.daysMask and (1 shl index)) != 0 }
        val dayText = if (days.isEmpty()) "No days" else days.joinToString(", ")
        "$dayText at ${schedule.times}"
    }
    ScheduleType.INTERVAL -> "Every ${schedule.intervalHours} hours"
    ScheduleType.COURSE -> "${schedule.times} (course)"
    else -> "Daily at ${schedule.times}"
}
