package com.medremind.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.medremind.app.data.DoseStatus
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

@Composable
fun HistoryContent(
    modifier: Modifier = Modifier,
    vm: MedicineViewModel,
    onOpenSettings: () -> Unit
) {
    val history by vm.history.collectAsState()

    LaunchedEffect(Unit) { vm.markOverdueAsMissed() }

    var rangeDays by remember { mutableIntStateOf(7) }
    var log by remember { mutableStateOf<List<DoseLogEntry>>(emptyList()) }

    LaunchedEffect(rangeDays, history) {
        log = vm.doseLogForRange(rangeDays)
    }

    val zone = remember { ZoneId.systemDefault() }
    val dayStats = remember(log, rangeDays, zone) {
        ((rangeDays - 1) downTo 0).map { offset ->
            val date = LocalDate.now().minusDays(offset.toLong())
            val statuses = log
                .filter { Instant.ofEpochMilli(it.scheduledAt).atZone(zone).toLocalDate() == date }
                .map { it.status }
            DayDoseStat(date, statuses)
        }
    }

    val taken = log.count { it.status == DoseStatus.TAKEN }
    val missed = log.count { it.status == DoseStatus.MISSED }
    val skipped = log.count { it.status == DoseStatus.SKIPPED }
    val pending = log.count { it.status == DoseStatus.PENDING }
    val future = log.count { it.status == FUTURE_STATUS }
    val due = taken + missed + skipped
    val percent = if (due == 0) 0 else taken * 100 / due

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val grouped = remember(log) {
        val map = linkedMapOf<String, MutableList<DoseLogEntry>>()
        log.forEach { entry ->
            map.getOrPut(dateKey(entry.scheduledAt)) { mutableListOf() }.add(entry)
        }
        map
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
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
            AdherenceChartCard(
                stats = dayStats,
                rangeDays = rangeDays,
                taken = taken,
                missed = missed,
                skipped = skipped,
                pending = pending,
                future = future,
                percent = percent
            )
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

        if (log.isEmpty()) {
            item(key = "empty") {
                MedEmptyState(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "No doses in this period",
                    message = "Add a medicine and its scheduled doses will appear here.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }

        grouped.forEach { (day, entries) ->
            item(key = "day-$day") {
                Text(
                    day,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            itemsIndexed(
                items = entries,
                key = { index, entry -> "$day-${entry.scheduledAt}-$index" }
            ) { _, entry ->
                DoseLogRow(entry = entry, timeFormat = timeFormat)
            }
        }
    }
}

@Composable
private fun AdherenceChartCard(
    stats: List<DayDoseStat>,
    rangeDays: Int,
    taken: Int,
    missed: Int,
    skipped: Int,
    pending: Int,
    future: Int,
    percent: Int
) {
    val visibleDays = stats.takeLast(minOf(rangeDays, 7))
    val due = taken + missed + skipped

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        shadowElevation = 3.dp
    ) {
        Box(modifier = Modifier.background(MedGradients.hero())) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ADHERENCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Last $rangeDays days",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HeroChip("$taken of $due taken")
                            HeroChip("$missed missed")
                            if (skipped > 0) HeroChip("$skipped skipped")
                            if (pending > 0) HeroChip("$pending pending")
                            if (future > 0) HeroChip("$future upcoming")
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ProgressRing(percent = percent, modifier = Modifier.size(100.dp)) {
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "of due doses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                if (rangeDays > 7) {
                    Text(
                        text = "Chart shows the last 7 days · totals cover $rangeDays days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    visibleDays.forEach { day ->
                        DayColumn(
                            date = day.date,
                            statuses = day.statuses,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendSwatch(Color(0xFF2FBF8F), {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(7.dp)
                        )
                    }) { "Taken" }
                    LegendSwatch(Color(0xFFE4664C), {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(7.dp)
                        )
                    }) { "Missed" }
                    LegendSwatch(Color.White, {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(1.5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF4338CA))
                        )
                    }) { "Skipped" }
                }
            }
        }
    }
}

@Composable
private fun DayColumn(
    date: LocalDate,
    statuses: List<String>,
    modifier: Modifier = Modifier
) {
    val isToday = date == LocalDate.now()
    val n = statuses.size
    val takenN = statuses.count { it == DoseStatus.TAKEN }
    val anyMissed = statuses.contains(DoseStatus.MISSED)
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                statuses.forEach { status -> SegBlock(status) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (n == 0) "—" else "$takenN/$n",
            style = MaterialTheme.typography.labelMedium,
            color = if (anyMissed) Color(0xFFFFC9B8) else onPrimary,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(3.dp))
        if (isToday) {
            Surface(shape = RoundedCornerShape(50), color = onPrimary) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        } else {
            Text(
                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = onPrimary.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SegBlock(status: String) {
    val base = Modifier
        .width(22.dp)
        .height(16.dp)
        .clip(RoundedCornerShape(50))
    when (status) {
        DoseStatus.TAKEN -> Box(
            modifier = base.background(
                Brush.verticalGradient(listOf(Color(0xFF43D19E), Color(0xFF1EA478)))
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
        }
        DoseStatus.MISSED -> Box(
            modifier = base.background(
                Brush.verticalGradient(listOf(Color(0xFFF07B5F), Color(0xFFC9482F)))
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
        DoseStatus.SKIPPED -> Box(
            modifier = base.background(Color.White.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF4338CA))
            )
        }
        FUTURE_STATUS -> Box(
            modifier = base
                .border(1.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.06f))
        )
        else -> Box(
            modifier = base
                .border(1.5.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.15f))
        )
    }
}

@Composable
private fun HeroChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.18f),
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun LegendSwatch(
    color: Color,
    symbol: (@Composable () -> Unit)? = null,
    label: @Composable () -> String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            if (symbol != null) {
                symbol()
            }
        }
        Spacer(Modifier.width(7.dp))
        Text(
            text = label(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DoseLogRow(
    entry: DoseLogEntry,
    timeFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    MedCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val photo = entry.photoPath
            if (photo != null) {
                AsyncImage(
                    model = File(photo),
                    contentDescription = entry.medicineName,
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
                        text = entry.medicineName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.medicineName, style = MaterialTheme.typography.titleSmall)
                Text(
                    timeFormat.format(Date(entry.scheduledAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(status = entry.status)
        }
    }
}

private fun dateKey(millis: Long): String =
    SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(Date(millis))
