package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medremind.app.R
import com.medremind.app.data.Medicine
import com.medremind.app.data.Metric
import com.medremind.app.data.MetricType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class NavSpec(
    val label: String,
    val icon: Int
)

private val navSpecs = listOf(
    NavSpec("Today", R.drawable.ic_nav_today),
    NavSpec("Med", R.drawable.ic_nav_med),
    NavSpec("Progress", R.drawable.ic_nav_progress),
    NavSpec("Health", R.drawable.ic_nav_health)
)

private val commonDiseases = listOf(
    "Diabetes", "High blood pressure", "High cholesterol", "Asthma", "COPD",
    "Heart disease", "Stroke", "Thyroid disorder", "Arthritis", "Osteoporosis",
    "Depression", "Anxiety", "Epilepsy", "Migraine", "Kidney disease",
    "Liver disease", "Anemia", "Asthma (exercise-induced)", "Sleep apnea", "Obesity",
    "Acid reflux (GERD)", "Peptic ulcer", "Irritable bowel syndrome", "Crohn's disease",
    "Ulcerative colitis", "Celiac disease", "Psoriasis", "Eczema", "Glaucoma",
    "Cataract", "Cancer", "Tuberculosis", "HIV/AIDS", "Dementia", "Parkinson's disease",
    "Pregnancy", "Allergy", "Osteoarthritis"
)

@Composable
fun MainTabs(
    tab: Int,
    onTabChange: (Int) -> Unit,
    medicines: List<Medicine>,
    vm: MedicineViewModel,
    settings: SettingsViewModel,
    onAdd: () -> Unit,
    onEdit: (Medicine) -> Unit,
    onDelete: (Medicine) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMe: () -> Unit
) {
    val schedulesByMedicine by vm.schedulesByMedicine.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = tab != 0) { onTabChange(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            MedBottomBar(
                tab = tab,
                onTabChange = onTabChange,
                onOpenSettings = onOpenSettings
            )
        },
        floatingActionButton = {
            when (tab) {
                0, 1 -> GradientPillButton(
                    text = "Add medicine",
                    icon = Icons.Rounded.Add,
                    onClick = onAdd
                )
                else -> {}
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val forward = targetState > initialState
                val enter = slideInHorizontally(
                    animationSpec = motionTween(MedMotion.Medium)
                ) { width -> if (forward) width / 5 else -width / 5 } +
                    fadeIn(motionTween(MedMotion.Medium, easing = MedMotion.Decelerate))
                val exit = slideOutHorizontally(
                    animationSpec = motionTween(MedMotion.Fast, easing = MedMotion.Accelerate)
                ) { width -> if (forward) -width / 5 else width / 5 } +
                    fadeOut(motionTween(MedMotion.Fast))
                enter togetherWith exit
            },
            label = "tabTransition"
        ) { current ->
            when (current) {
                0 -> TodayContent(
                    modifier = Modifier.padding(padding),
                    medicines = medicines,
                    vm = vm,
                    onAdd = onAdd,
                    onOpenMe = onOpenMe,
                    profilePhoto = settings.profilePhoto,
                    snackbarHostState = snackbarHostState
                )
                1 -> MedContent(
                    modifier = Modifier.padding(padding),
                    medicines = medicines,
                    schedulesByMedicine = schedulesByMedicine,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onAdd = onAdd,
                    onOpenMe = onOpenMe,
                    profilePhoto = settings.profilePhoto
                )
                2 -> HistoryContent(
                    modifier = Modifier.padding(padding),
                    vm = vm,
                    onOpenMe = onOpenMe,
                    profilePhoto = settings.profilePhoto
                )
                else -> HealthScreen(
                    modifier = Modifier.padding(padding),
                    settings = settings,
                    vm = vm,
                    onOpenMe = onOpenMe
                )
            }
        }
    }
}

@Composable
private fun MedBottomBar(
    tab: Int,
    onTabChange: (Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 14.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                navSpecs.forEachIndexed { index, spec ->
                    val selected = tab == index
                    BottomNavCell(
                        selected = selected,
                        label = spec.label,
                        onClick = { onTabChange(index) }
                    ) {
                        Icon(
                            painter = painterResource(spec.icon),
                            contentDescription = spec.label,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(if (selected) 28.dp else 25.dp)
                        )
                    }
                }
                BottomNavCell(
                    selected = false,
                    label = "Settings",
                    onClick = onOpenSettings
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomNavCell(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val haptics = rememberMedHaptics()
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                haptics.tap()
                onClick()
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 30.dp)
                .clip(RoundedCornerShape(50))
                .then(
                    if (selected) {
                        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun HealthScreen(
    modifier: Modifier = Modifier,
    settings: SettingsViewModel,
    vm: MedicineViewModel,
    onOpenMe: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = settings.profileDiseases
    val atMax = selected.size >= MAX_DISEASES
    val metrics by vm.metrics.collectAsState()
    var showAddMetric by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("Health") {
            SquareIconButton(
                icon = Icons.Rounded.Person,
                contentDescription = "Me",
                onClick = onOpenMe,
                photoPath = settings.profilePhoto
            )
        }

        MedCard(modifier = Modifier.fillMaxWidth()) {
            Text("Your conditions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Add the conditions you have (up to $MAX_DISEASES).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = !atMax,
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Add disease")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    commonDiseases.forEach { disease ->
                        val already = selected.contains(disease)
                        DropdownMenuItem(
                            text = { Text(disease) },
                            enabled = !already,
                            onClick = {
                                settings.addDisease(disease)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (selected.isEmpty()) {
                Text(
                    "No conditions added yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selected.forEach { disease ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = disease,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { settings.removeDisease(disease) }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Remove $disease",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (atMax) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Maximum of $MAX_DISEASES conditions reached.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        MedCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Health log",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showAddMetric = true }) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Add reading")
                }
            }
            if (metrics.isEmpty()) {
                Text(
                    "Log blood pressure, glucose or weight to keep a simple history here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    metrics.take(10).forEach { metric ->
                        MetricRow(metric = metric, onDelete = { vm.deleteMetric(metric) })
                    }
                }
            }
        }
    }

    if (showAddMetric) {
        AddMetricDialog(
            onDismiss = { showAddMetric = false },
            onSave = { type, v1, v2 ->
                vm.addMetric(type, v1, v2)
                showAddMetric = false
            }
        )
    }
}

@Composable
private fun MetricRow(metric: Metric, onDelete: () -> Unit) {
    val format = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.MonitorHeart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                MetricType.label(metric.type),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                format.format(Date(metric.recordedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            formatMetric(metric),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = "Delete reading",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatMetric(metric: Metric): String {
    val unit = MetricType.unit(metric.type)
    return when (metric.type) {
        MetricType.BP -> "${metric.value.toInt()}/${metric.value2.toInt()} $unit"
        else -> {
            val number = if (metric.value % 1f == 0f) metric.value.toInt().toString()
            else String.format(Locale.getDefault(), "%.1f", metric.value)
            "$number $unit"
        }
    }
}

@Composable
private fun AddMetricDialog(
    onDismiss: () -> Unit,
    onSave: (String, Float, Float) -> Unit
) {
    var typeIndex by remember { mutableIntStateOf(0) }
    val types = listOf(MetricType.BP, MetricType.GLUCOSE, MetricType.WEIGHT)
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val type = types[typeIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add reading") },
        text = {
            Column {
                MedSegmentedButtons(
                    options = listOf("BP", "Glucose", "Weight"),
                    selectedIndex = typeIndex,
                    onSelect = {
                        typeIndex = it
                        first = ""
                        second = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                if (type == MetricType.BP) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = first,
                            onValueChange = { first = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Systolic") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = second,
                            onValueChange = { second = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Diastolic") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = first,
                        onValueChange = {
                            first = it.filter { c -> c.isDigit() || c == '.' }.take(6)
                        },
                        label = { Text(MetricType.label(type)) },
                        suffix = { Text(MetricType.unit(type)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            val valid = first.toFloatOrNull() != null &&
                (type != MetricType.BP || second.toFloatOrNull() != null)
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(type, first.toFloatOrNull() ?: 0f, second.toFloatOrNull() ?: 0f)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
