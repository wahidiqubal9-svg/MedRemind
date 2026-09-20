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
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureAsPdf
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
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        snackbarHost = {
            Box(modifier = Modifier.padding(bottom = 96.dp)) {
                SnackbarHost(snackbarHostState)
            }
        },
        floatingActionButton = {
            when (tab) {
                0, 1 -> Box(modifier = Modifier.padding(bottom = 96.dp)) {
                    GradientPillButton(
                        text = "Add medicine",
                        icon = Icons.Rounded.Add,
                        onClick = onAdd
                    )
                }
                else -> {}
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            modifier = Modifier.fillMaxSize(),
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
                    greetingName = settings.profileName,
                    snackbarHostState = snackbarHostState
                )
                1 -> MedContent(
                    modifier = Modifier.padding(padding),
                    medicines = medicines,
                    schedulesByMedicine = schedulesByMedicine,
                    settings = settings,
                    vm = vm,
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
        MedBottomBar(
            tab = tab,
            onTabChange = onTabChange,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        }
    }
}

@Composable
private fun MedBottomBar(
    tab: Int,
    onTabChange: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
    val metrics by vm.metrics.collectAsState()
    var showAddGlucose by remember { mutableStateOf(false) }
    var showAddBp by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bpReadings = metrics.filter { it.type == MetricType.BP }.sortedBy { it.recordedAt }
    val glucoseReadings = metrics.filter { it.type == MetricType.GLUCOSE }.sortedBy { it.recordedAt }
    val weightReadings = metrics.filter { it.type == MetricType.WEIGHT }.sortedBy { it.recordedAt }

    val bpControl = if (bpReadings.isEmpty()) null
    else bpReadings.count {
        Vitals.classifyBP(it.value, it.value2) == HealthZone.GREEN
    } * 100 / bpReadings.size
    val cbgControl = if (glucoseReadings.isEmpty()) null
    else glucoseReadings.count {
        Vitals.classifyCBG(it.value, it.context) == HealthZone.GREEN
    } * 100 / glucoseReadings.size
    val bpAvgText = if (bpReadings.isEmpty()) "\u2014"
    else "${bpReadings.map { it.value }.average().toInt()}/" +
        "${bpReadings.map { it.value2 }.average().toInt()} mmHg"
    val cbgAvgText = if (glucoseReadings.isEmpty()) "\u2014"
    else "${glucoseReadings.map { it.value }.average().toInt()} mg/dL"

    fun badgeFor(pct: Int?): String? = when {
        pct == null -> null
        pct >= 70 -> "Well controlled"
        pct >= 50 -> "Needs attention"
        else -> "Out of control"
    }

    fun shareVitals(mime: String, chooser: String, build: () -> File) {
        scope.launch {
            val file = withContext(Dispatchers.IO) { build() }
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooser))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader("Health", modifier = Modifier.padding(horizontal = 16.dp)) {
            SquareIconButton(
                icon = Icons.Rounded.Person,
                contentDescription = "Me",
                onClick = onOpenMe,
                photoPath = settings.profilePhoto
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VitalsStatCard(
                    label = "BP in control",
                    value = if (bpControl != null) "$bpControl%" else "\u2014",
                    badgeText = badgeFor(bpControl),
                    badgeTint = bpControl?.let { Vitals.badgeTint(it) } ?: Vitals.Green,
                    modifier = Modifier.weight(1f)
                )
                VitalsStatCard(
                    label = "CBG in range",
                    value = if (cbgControl != null) "$cbgControl%" else "\u2014",
                    badgeText = badgeFor(cbgControl),
                    badgeTint = cbgControl?.let { Vitals.badgeTint(it) } ?: Vitals.Green,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VitalsStatCard(
                    label = "BP avg",
                    value = bpAvgText,
                    badgeText = null,
                    badgeTint = Vitals.Systolic,
                    modifier = Modifier.weight(1f)
                )
                VitalsStatCard(
                    label = "CBG avg",
                    value = cbgAvgText,
                    badgeText = null,
                    badgeTint = Vitals.Glucose,
                    modifier = Modifier.weight(1f)
                )
            }

            if (glucoseReadings.isNotEmpty()) {
                GlucoseTrendsCard(
                    readings = glucoseReadings,
                    onAdd = { showAddGlucose = true }
                )
                VitalsChartCard(
                    title = "Blood Glucose",
                    subtitle = "CBG (mg/dL) \u00b7 last ${glucoseReadings.size}",
                    currentText = "${glucoseReadings.last().value.toInt()}",
                    series = listOf(
                        VitalSeries(
                            label = "Blood glucose",
                            color = Vitals.Glucose,
                            values = glucoseReadings.map { it.value },
                            zones = glucoseReadings.map {
                                Vitals.classifyCBG(it.value, it.context)
                            }
                        )
                    ),
                    bands = listOf(
                        ZoneBand(180f, 300f, Vitals.Red),
                        ZoneBand(130f, 180f, Vitals.Yellow),
                        ZoneBand(80f, 130f, Vitals.Green),
                        ZoneBand(40f, 70f, Vitals.Red)
                    ),
                    yMin = 40f,
                    yMax = 300f,
                    xLabels = glucoseReadings.map {
                        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(it.recordedAt))
                    },
                    onAdd = { showAddGlucose = true }
                )
            }

            if (bpReadings.isNotEmpty()) {
                BpTrendsCard(
                    readings = bpReadings,
                    onAdd = { showAddBp = true }
                )
            }

            if (weightReadings.isNotEmpty()) {
                MetricChartCard(
                    title = "Weight",
                    currentText = formatMetric(weightReadings.last()),
                    series = listOf(
                        ChartSeries(
                            values = weightReadings.map { it.value },
                            color = MaterialTheme.colorScheme.secondary,
                            label = "Weight"
                        )
                    )
                )
            }

            MedCard(modifier = Modifier.fillMaxWidth()) {
                Text("Recent readings", style = MaterialTheme.typography.titleMedium)
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

            HealthExportCard(
                enabled = metrics.isNotEmpty(),
                onCsv = {
                    shareVitals("text/csv", "Share CSV report") {
                        ReportExporter.exportVitalsCsv(context, metrics)
                    }
                },
                onPdf = {
                    shareVitals("application/pdf", "Share PDF report") {
                        ReportExporter.exportVitalsPdf(context, metrics)
                    }
                }
            )
        }
    }

    if (showAddGlucose) {
        AddGlucoseDialog(
            onDismiss = { showAddGlucose = false },
            onSave = { ctx, value ->
                vm.addMetric(MetricType.GLUCOSE, value, 0f, ctx)
                showAddGlucose = false
            }
        )
    }
    if (showAddBp) {
        AddBpDialog(
            onDismiss = { showAddBp = false },
            onSave = { sys, dia ->
                vm.addMetric(MetricType.BP, sys, dia)
                showAddBp = false
            }
        )
    }
}

@Composable
private fun AddGlucoseDialog(
    onDismiss: () -> Unit,
    onSave: (context: String, value: Float) -> Unit
) {
    val options = com.medremind.app.data.MetricContext.glucoseOptions
    var context by remember { mutableStateOf(options.first()) }
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add glucose reading") },
        text = {
            Column {
                Text(
                    "When was this reading taken?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                FilterChipRow(
                    options = options.map { com.medremind.app.data.MetricContext.label(it) },
                    selectedIndex = options.indexOf(context).coerceAtLeast(0),
                    onSelect = { context = options[it] }
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = { Text("Glucose") },
                    suffix = { Text("mg/dL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.toFloatOrNull() != null,
                onClick = { onSave(context, value.toFloatOrNull() ?: 0f) }
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddBpDialog(
    onDismiss: () -> Unit,
    onSave: (sys: Float, dia: Float) -> Unit
) {
    var sys by remember { mutableStateOf("") }
    var dia by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add blood pressure") },
        text = {
            Column {
                Text(
                    "Systolic / Diastolic (mmHg)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sys,
                        onValueChange = { sys = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Systolic") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dia,
                        onValueChange = { dia = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Diastolic") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = sys.toFloatOrNull() != null && dia.toFloatOrNull() != null,
                onClick = { onSave(sys.toFloatOrNull() ?: 0f, dia.toFloatOrNull() ?: 0f) }
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun HealthExportCard(
    enabled: Boolean,
    onCsv: () -> Unit,
    onPdf: () -> Unit
) {
    MedCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MedGradients.heroHorizontal()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Export report", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Share your health readings as CSV or PDF.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onCsv,
                enabled = enabled,
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Rounded.Description,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("CSV")
            }
            OutlinedButton(
                onClick = onPdf,
                enabled = enabled,
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("PDF")
            }
        }
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
