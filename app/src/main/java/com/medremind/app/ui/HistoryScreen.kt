package com.medremind.app.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

private data class DayStat(val label: String, val taken: Int, val missed: Int)

@Composable
fun HistoryContent(
    modifier: Modifier = Modifier,
    vm: MedicineViewModel
) {
    val history by vm.history.collectAsState()

    LaunchedEffect(Unit) { vm.markOverdueAsMissed() }

    val zone = remember { ZoneId.systemDefault() }
    val todayStart = remember {
        LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
    }
    val todayItems = history.filter { it.event.scheduledAt >= todayStart }
    val takenToday = todayItems.count { it.event.status == DoseStatus.TAKEN }
    val skippedToday = todayItems.count { it.event.status == DoseStatus.SKIPPED }
    val missedToday = todayItems.count { it.event.status == DoseStatus.MISSED }
    val pendingToday = todayItems.count { it.event.status == DoseStatus.PENDING }

    val last7 = remember(history) {
        (6 downTo 0).map { offset ->
            val date = LocalDate.now().minusDays(offset.toLong())
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = dayStart + 24L * 60 * 60 * 1000
            val items = history.filter { it.event.scheduledAt in dayStart until dayEnd }
            DayStat(
                label = date.dayOfWeek.name.take(1) + date.dayOfWeek.name.drop(1).take(2).lowercase(),
                taken = items.count { it.event.status == DoseStatus.TAKEN },
                missed = items.count { it.event.status == DoseStatus.MISSED }
            )
        }
    }

    val weekTaken = last7.sumOf { it.taken }
    val weekMissed = last7.sumOf { it.missed }
    val weekTotal = weekTaken + weekMissed
    val adherence = if (weekTotal == 0) 0 else (weekTaken * 100) / weekTotal

    val grouped = remember(history) {
        history.groupBy { dateKey(it.event.scheduledAt) }
    }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("TODAY", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Taken: $takenToday    Missed: $missedToday",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Skipped: $skippedToday    Pending: $pendingToday",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Adherence: $adherence%  ($weekTaken taken, $weekMissed missed)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    val maxTotal = last7.maxOfOrNull { it.taken + it.missed }?.coerceAtLeast(1) ?: 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        last7.forEach { day ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Box(
                                        modifier = Modifier
                                            .width(10.dp)
                                            .height((64f * day.taken / maxTotal).dp)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(10.dp)
                                            .height((64f * day.missed / maxTotal).dp)
                                            .background(MaterialTheme.colorScheme.error)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(day.label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        if (history.isEmpty()) {
            item {
                Text(
                    "No doses recorded yet. When a reminder fires and you mark it, it will appear here.",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        grouped.forEach { (day, dayItems) ->
            item {
                Text(day, style = MaterialTheme.typography.titleMedium)
            }
            items(dayItems, key = { it.event.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val photo = item.photoPath
                        if (photo != null) {
                            AsyncImage(
                                model = File(photo),
                                contentDescription = item.medicineName,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("?")
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.medicineName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                timeFormat.format(Date(item.event.scheduledAt)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = statusLabel(item.event.status),
                            color = statusColor(item.event.status),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

private fun dateKey(millis: Long): String =
    SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(Date(millis))

private fun statusLabel(status: String): String = when (status) {
    DoseStatus.TAKEN -> "Taken"
    DoseStatus.SKIPPED -> "Skipped"
    DoseStatus.MISSED -> "Missed"
    else -> "Pending"
}

private fun statusColor(status: String): Color = when (status) {
    DoseStatus.TAKEN -> Color(0xFF2E7D32)
    DoseStatus.SKIPPED -> Color(0xFF757575)
    DoseStatus.MISSED -> Color(0xFFC62828)
    else -> Color(0xFF1565C0)
}
