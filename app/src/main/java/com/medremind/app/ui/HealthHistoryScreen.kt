package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medremind.app.data.Metric
import com.medremind.app.data.MetricType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Every health reading, opened from the Health tab. */
@Composable
fun HealthHistoryScreen(
    vm: MedicineViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val metrics by vm.metrics.collectAsState()
    var filterIndex by remember { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<Metric?>(null) }

    val types = listOf<String?>(null, MetricType.BP, MetricType.GLUCOSE, MetricType.WEIGHT)
    val typeFilter = types[filterIndex.coerceIn(0, types.lastIndex)]
    val filtered = remember(metrics, typeFilter) {
        metrics.filter { typeFilter == null || it.type == typeFilter }
            .sortedByDescending { it.recordedAt }
    }
    val dayFormat = remember { SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()) }
    val grouped = remember(filtered) {
        val map = linkedMapOf<String, MutableList<Metric>>()
        filtered.forEach { metric ->
            map.getOrPut(dayFormat.format(Date(metric.recordedAt))) { mutableListOf() }.add(metric)
        }
        map
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Health history",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "filter") {
                MedSegmentedButtons(
                    options = listOf("All", "BP", "Glucose", "Weight"),
                    selectedIndex = filterIndex,
                    onSelect = { filterIndex = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (filtered.isEmpty()) {
                item(key = "empty") {
                    MedEmptyState(
                        icon = Icons.Rounded.MonitorHeart,
                        title = "No readings yet",
                        message = "Blood pressure, glucose and weight readings will appear here.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            } else {
                grouped.forEach { (day, readings) ->
                    item(key = "day-$day") {
                        Text(
                            day,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(readings, key = { "${day}-${it.id}" }) { metric ->
                        MetricRow(metric = metric, onDelete = { pendingDelete = metric })
                    }
                }
            }
        }
    }

    pendingDelete?.let { metric ->
        MedConfirmDialog(
            title = "Delete reading?",
            message = "${MetricType.label(metric.type)} reading will be removed.",
            confirmText = "Delete",
            onConfirm = {
                vm.deleteMetric(metric)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}
