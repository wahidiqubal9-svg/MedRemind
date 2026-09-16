package com.medremind.app.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.medremind.app.data.DoseStatus
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

private data class DayStat(val date: LocalDate, val taken: Int, val missed: Int)

@Composable
fun HistoryContent(
    modifier: Modifier = Modifier,
    vm: MedicineViewModel,
    onOpenSettings: () -> Unit
) {
    val history by vm.history.collectAsState()

    LaunchedEffect(Unit) { vm.markOverdueAsMissed() }

    var rangeDays by remember { mutableIntStateOf(7) }

    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now()

    val rangeItems = remember(history, rangeDays) {
        val start = today.minusDays((rangeDays - 1).toLong())
            .atStartOfDay(zone).toInstant().toEpochMilli()
        history.filter { it.event.scheduledAt >= start }
    }

    val taken = rangeItems.count { it.event.status == DoseStatus.TAKEN }
    val missed = rangeItems.count { it.event.status == DoseStatus.MISSED }
    val skipped = rangeItems.count { it.event.status == DoseStatus.SKIPPED }
    val actionable = taken + missed + skipped
    val percent = if (actionable == 0) 0 else taken * 100 / actionable

    val chartDays = remember(history, rangeDays) {
        (rangeDays - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = dayStart + 86_400_000L
            val items = history.filter { it.event.scheduledAt in dayStart until dayEnd }
            DayStat(
                date = date,
                taken = items.count { it.event.status == DoseStatus.TAKEN },
                missed = items.count { it.event.status == DoseStatus.MISSED }
            )
        }
    }

    val grouped = remember(rangeItems) {
        rangeItems.groupBy { dateKey(it.event.scheduledAt) }
    }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val primary = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "progress_header") {
            ScreenHeader("Progress") {
                SquareIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    onClick = onOpenSettings
                )
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
                onSelect = { index -> rangeDays = listOf(7, 30, 90)[index] },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item(key = "adherence") {
            MedHeroCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Adherence",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Last $rangeDays days",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$taken taken \u00b7 $missed missed \u00b7 $skipped skipped",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    ProgressRing(percent = percent, modifier = Modifier.size(96.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$percent%",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }
        }

        item(key = "stats") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Taken",
                    value = taken,
                    tint = statusTint(DoseStatus.TAKEN)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Missed",
                    value = missed,
                    tint = statusTint(DoseStatus.MISSED)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Skipped",
                    value = skipped,
                    tint = statusTint(DoseStatus.SKIPPED)
                )
            }
        }

        item(key = "trend") {
            MedCard(modifier = Modifier.fillMaxWidth()) {
                Text("Adherence trend", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Taken vs missed per day",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                val maxTotal = chartDays.maxOfOrNull { it.taken + it.missed }?.coerceAtLeast(1) ?: 1
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val count = chartDays.size
                    if (count == 0) return@Canvas
                    val gap = if (count > 30) 1.dp.toPx() else 4.dp.toPx()
                    val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1f)
                    val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    chartDays.forEachIndexed { index, day ->
                        val x = index * (barWidth + gap)
                        val total = day.taken + day.missed
                        val totalHeight = size.height * total / maxTotal
                        val takenHeight = size.height * day.taken / maxTotal
                        val missedHeight = totalHeight - takenHeight
                        val baseY = size.height
                        if (takenHeight > 0f) {
                            drawRoundRect(
                                color = primary,
                                topLeft = Offset(x, baseY - takenHeight),
                                size = Size(barWidth, takenHeight),
                                cornerRadius = radius
                            )
                        }
                        if (missedHeight > 0f) {
                            drawRoundRect(
                                color = errorColor.copy(alpha = 0.75f),
                                topLeft = Offset(x, baseY - takenHeight - missedHeight),
                                size = Size(barWidth, missedHeight),
                                cornerRadius = radius
                            )
                        }
                    }
                }
                if (rangeDays > 7) {
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val first = chartDays.firstOrNull()?.date
                        val last = chartDays.lastOrNull()?.date
                        val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
                        Text(
                            first?.let { fmt.format(dateToMillis(it)) } ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            last?.let { fmt.format(dateToMillis(it)) } ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (rangeItems.isEmpty()) {
            item(key = "empty") {
                MedEmptyState(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "No activity yet",
                    message = "No doses recorded in this period.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }

        grouped.forEach { (day, dayItems) ->
            item(key = "day-$day") {
                Text(
                    day,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(dayItems, key = { it.event.id }) { item ->
                HistoryRow(item = item, timeFormat = timeFormat, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: DoseHistoryItem,
    timeFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    MedCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val photo = item.photoPath
            if (photo != null) {
                AsyncImage(
                    model = File(photo),
                    contentDescription = item.medicineName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.medicineName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.medicineName, style = MaterialTheme.typography.titleSmall)
                Text(
                    timeFormat.format(Date(item.event.scheduledAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(status = item.event.status)
        }
    }
}

private fun dateToMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun dateKey(millis: Long): String =
    SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(Date(millis))