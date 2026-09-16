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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
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
    vm: MedicineViewModel
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
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30, 90).forEach { days ->
                    FilterChip(
                        selected = rangeDays == days,
                        onClick = { rangeDays = days },
                        label = { Text("$days days") }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(180.dp)) {
                            val stroke = 20.dp.toPx()
                            val diameter = size.minDimension - stroke
                            val topLeft = Offset(
                                (size.width - diameter) / 2f,
                                (size.height - diameter) / 2f
                            )
                            drawArc(
                                color = trackColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = primary,
                                startAngle = -90f,
                                sweepAngle = 360f * (percent / 100f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$percent%",
                                style = MaterialTheme.typography.displaySmall
                            )
                            Text("adherence", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Last $rangeDays days",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Taken",
                    value = taken,
                    color = Color(0xFF2E7D32)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Missed",
                    value = missed,
                    color = Color(0xFFC62828)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Skipped",
                    value = skipped,
                    color = Color(0xFF757575)
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Adherence trend", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Green = taken, red = missed",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.height(12.dp))
                    val maxTotal = chartDays.maxOfOrNull { it.taken + it.missed }?.coerceAtLeast(1) ?: 1
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        val count = chartDays.size
                        if (count == 0) return@Canvas
                        val gap = if (count > 30) 1.dp.toPx() else 3.dp.toPx()
                        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1f)
                        chartDays.forEachIndexed { index, day ->
                            val x = index * (barWidth + gap)
                            val total = day.taken + day.missed
                            val totalHeight = size.height * total / maxTotal
                            val takenHeight = size.height * day.taken / maxTotal
                            val missedHeight = totalHeight - takenHeight
                            val baseY = size.height
                            if (takenHeight > 0f) {
                                drawRect(
                                    color = primary,
                                    topLeft = Offset(x, baseY - takenHeight),
                                    size = Size(barWidth, takenHeight)
                                )
                            }
                            if (missedHeight > 0f) {
                                drawRect(
                                    color = errorColor,
                                    topLeft = Offset(x, baseY - takenHeight - missedHeight),
                                    size = Size(barWidth, missedHeight)
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
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                last?.let { fmt.format(dateToMillis(it)) } ?: "",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        if (rangeItems.isEmpty()) {
            item {
                Text(
                    "No doses recorded in this period.",
                    style = MaterialTheme.typography.bodyMedium,
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

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    color: Color
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = color
            )
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

private fun dateToMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

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
