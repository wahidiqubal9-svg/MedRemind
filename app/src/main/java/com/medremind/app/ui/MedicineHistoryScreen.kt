package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medremind.app.data.DoseStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adherence history for one specific medicine, opened from Progress.
 */
@Composable
fun MedicineHistoryScreen(
    vm: MedicineViewModel,
    medicineId: Long,
    profileId: Long,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val medicines by vm.medicines.collectAsState()
    val medicine = remember(medicines, medicineId) {
        medicines.firstOrNull { it.id == medicineId }
    }
    var rangeDays by remember { mutableIntStateOf(30) }
    var log by remember { mutableStateOf<List<DoseLogEntry>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()) }

    LaunchedEffect(medicineId, rangeDays, profileId) {
        log = vm.doseLogForMedicine(medicineId, rangeDays, profileId)
            .filter {
                it.status == DoseStatus.TAKEN ||
                    it.status == DoseStatus.MISSED ||
                    it.status == DoseStatus.SKIPPED
            }
        loaded = true
    }

    val taken = log.count { it.status == DoseStatus.TAKEN }
    val missed = log.count { it.status == DoseStatus.MISSED }
    val skipped = log.count { it.status == DoseStatus.SKIPPED }
    val due = taken + missed + skipped
    val percent = if (due == 0) 0 else taken * 100 / due

    val grouped = remember(log) {
        val map = linkedMapOf<String, MutableList<DoseLogEntry>>()
        log.forEach {
            map.getOrPut(dayFormat.format(Date(it.scheduledAt))) { mutableListOf() }.add(it)
        }
        map
    }

    val displayName = medicine?.name ?: log.firstOrNull()?.medicineName ?: "Medicine"
    val displayPhoto = medicine?.photoPath ?: log.firstOrNull()?.photoPath

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
                "Medicine history",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "medicine") {
                MedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MedIconSquare(
                            label = displayName,
                            seed = medicineId,
                            size = 52.dp,
                            photoPath = displayPhoto
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            medicine?.strength?.takeIf { it.isNotBlank() }?.let { strength ->
                                Text(
                                    strength,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (medicine?.pausedAt != null) {
                            Text(
                                "Paused",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item(key = "range") {
                MedSegmentedButtons(
                    options = listOf("7 days", "30 days", "90 days"),
                    selectedIndex = when (rangeDays) {
                        7 -> 0
                        30 -> 1
                        else -> 2
                    },
                    onSelect = { rangeDays = listOf(7, 30, 90)[it] },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(key = "summary") {
                MedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(
                            percent = percent,
                            modifier = Modifier.size(96.dp),
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                            progressColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                "$percent%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.width(18.dp))
                        Column {
                            SummaryStat("Taken", taken, statusTint(DoseStatus.TAKEN))
                            Spacer(Modifier.height(6.dp))
                            SummaryStat("Missed", missed, statusTint(DoseStatus.MISSED))
                            Spacer(Modifier.height(6.dp))
                            SummaryStat("Skipped", skipped, statusTint(DoseStatus.SKIPPED))
                        }
                    }
                }
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
                        message = "Taken, missed and skipped doses for this medicine will appear here.",
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
                        DoseLogRow(entry = entry, timeFormat = timeFormat)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: Int, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tint)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}
