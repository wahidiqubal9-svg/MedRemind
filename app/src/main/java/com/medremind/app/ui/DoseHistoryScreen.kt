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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medremind.app.data.DoseStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * All dose history, opened from Progress. Tapping a medicine opens that
 * medicine's own adherence page.
 */
@Composable
fun DoseHistoryScreen(
    vm: MedicineViewModel,
    profileId: Long,
    onBack: () -> Unit,
    onOpenMedicine: (Long) -> Unit
) {
    BackHandler(onBack = onBack)
    var rangeDays by remember { mutableIntStateOf(30) }
    var log by remember { mutableStateOf<List<DoseLogEntry>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()) }

    LaunchedEffect(profileId, rangeDays) {
        log = vm.doseLogForRange(rangeDays, profileId)
            .filter {
                it.status == DoseStatus.TAKEN ||
                    it.status == DoseStatus.MISSED ||
                    it.status == DoseStatus.SKIPPED
            }
        loaded = true
    }

    val grouped = remember(log) {
        val map = linkedMapOf<String, MutableList<DoseLogEntry>>()
        log.forEach {
            map.getOrPut(dayFormat.format(Date(it.scheduledAt))) { mutableListOf() }.add(it)
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
                "History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "range") {
                MedSegmentedButtons(
                    options = listOf("7 days", "30 days", "90 days", "All"),
                    selectedIndex = when (rangeDays) {
                        7 -> 0
                        30 -> 1
                        90 -> 2
                        else -> 3
                    },
                    onSelect = { rangeDays = listOf(7, 30, 90, 3650)[it] },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item(key = "hint") {
                Text(
                    "Tap a medicine to see its adherence history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (!loaded) {
                item(key = "loading") {
                    ListSkeleton(modifier = Modifier.padding(top = 4.dp))
                }
            } else if (log.isEmpty()) {
                item(key = "empty") {
                    MedEmptyState(
                        icon = Icons.Rounded.CalendarMonth,
                        title = "No doses in this period",
                        message = "Taken, missed and skipped doses will appear here.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            } else {
                grouped.forEach { (day, entries) ->
                    item(key = "day-$day") {
                        Text(
                            day,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(entries, key = { "$day-${it.scheduledAt}" }) { entry ->
                        DoseLogRow(
                            entry = entry,
                            timeFormat = timeFormat,
                            onClick = if (entry.medicineId != 0L) {
                                { onOpenMedicine(entry.medicineId) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
