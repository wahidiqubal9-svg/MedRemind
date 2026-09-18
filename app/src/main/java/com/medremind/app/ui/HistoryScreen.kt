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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
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
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

private val chartTakenStart = Color(0xFF43D19E)
private val chartTakenEnd = Color(0xFF1EA478)
private val chartMissStart = Color(0xFFF07B5F)
private val chartMissEnd = Color(0xFFC9482F)
private val chartAmber = Color(0xFFF0B35C)
private val chartInk2 = Color(0xFF9DB5AA)
private val chartInk3 = Color(0x8C9DB5AA)

@Composable
fun HistoryContent(
    modifier: Modifier = Modifier,
    vm: MedicineViewModel,
    onOpenSettings: () -> Unit
) {
    val history by vm.history.collectAsState()

    LaunchedEffect(Unit) { vm.markOverdueAsMissed() }

    var rangeDays by remember { mutableIntStateOf(7) }
    var stats by remember { mutableStateOf<List<DayDoseStat>>(emptyList()) }

    LaunchedEffect(rangeDays, history) {
        stats = vm.doseStatsForRange(rangeDays)
    }

    val allStatuses = stats.flatMap { it.statuses }
    val taken = allStatuses.count { it == DoseStatus.TAKEN }
    val missed = allStatuses.count { it == DoseStatus.MISSED }
    val skipped = allStatuses.count { it == DoseStatus.SKIPPED }
    val pending = allStatuses.count { it == DoseStatus.PENDING }
    val future = allStatuses.count { it == FUTURE_STATUS }
    val due = taken + missed + skipped
    val percent = if (due == 0) 0 else taken * 100 / due

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val grouped = remember(history) { history.groupBy { dateKey(it.event.scheduledAt) } }

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
                stats = stats,
                rangeDays = rangeDays,
                taken = taken,
                missed = missed,
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

        if (history.isEmpty()) {
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
private fun AdherenceChartCard(
    stats: List<DayDoseStat>,
    rangeDays: Int,
    taken: Int,
    missed: Int,
    pending: Int,
    future: Int,
    percent: Int
) {
    val days = stats.takeLast(minOf(rangeDays, 7))
    val due = taken + missed
    val shape = RoundedCornerShape(26.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = Color(0xFF0C2019),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(listOf(Color(0xFF122B24), Color(0xFF0C2019)))
            )
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MEDIREMIND · ADHERENCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = chartAmber,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Last $rangeDays days",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            ChartChip(chartTakenStart, "$taken of $due due taken")
                            ChartChip(chartMissStart, "$missed missed")
                        }
                        Spacer(Modifier.height(7.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            ChartChip(chartAmber, "$pending pending")
                            ChartChip(null, "$future upcoming")
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "OF DUE DOSES",
                            style = MaterialTheme.typography.labelSmall,
                            color = chartInk2,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "1 block = 1 dose",
                            style = MaterialTheme.typography.labelSmall,
                            color = chartInk3
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    days.forEach { day -> DayColumn(day = day, modifier = Modifier.weight(1f)) }
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendSwatch(chartTakenStart) { "Taken" }
                    LegendSwatch(chartAmber) { "Pending" }
                    LegendSwatch(chartMissStart) { "Missed" }
                    LegendSwatch(null) { "Upcoming" }
                }
            }
        }
    }
}

@Composable
private fun DayColumn(day: DayDoseStat, modifier: Modifier = Modifier) {
    val isToday = day.date == LocalDate.now()
    val n = day.statuses.size
    val takenN = day.statuses.count { it == DoseStatus.TAKEN }
    val allFuture = n > 0 && day.statuses.all { it == FUTURE_STATUS }
    val fracColor = when {
        allFuture -> chartInk3
        day.statuses.contains(DoseStatus.MISSED) -> chartMissStart
        day.statuses.contains(DoseStatus.PENDING) -> chartAmber
        else -> chartTakenStart
    }

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
                day.statuses.forEach { status -> SegBlock(status) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (allFuture || n == 0) "—" else "$takenN/$n",
            style = MaterialTheme.typography.labelMedium,
            color = fracColor,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(3.dp))
        if (isToday) {
            Surface(
                shape = RoundedCornerShape(50),
                color = chartTakenEnd
            ) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF06120E),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        } else {
            Text(
                text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = chartInk2,
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
                Brush.verticalGradient(listOf(chartTakenStart, chartTakenEnd))
            )
        )
        DoseStatus.MISSED -> Box(
            modifier = base.background(
                Brush.verticalGradient(listOf(chartMissStart, chartMissEnd))
            ),
            contentAlignment = Alignment.Center
        ) {
            Text("!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        DoseStatus.SKIPPED -> Box(
            modifier = base.background(
                Brush.verticalGradient(listOf(Color(0xFF94A3B8), Color(0xFF64748B)))
            )
        )
        FUTURE_STATUS -> Box(
            modifier = base
                .border(1.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.04f))
        )
        else -> Box(
            modifier = base
                .border(1.5.dp, Color(0xFF2FBF8F).copy(alpha = 0.75f), RoundedCornerShape(50))
                .background(Color(0xFF2FBF8F).copy(alpha = 0.15f))
        )
    }
}

@Composable
private fun ChartChip(dot: Color?, text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dot != null) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dot)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = chartInk2,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LegendSwatch(color: Color?, label: @Composable () -> String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (color != null) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        } else {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
            )
        }
        Spacer(Modifier.width(7.dp))
        Text(
            text = label(),
            style = MaterialTheme.typography.labelSmall,
            color = chartInk2,
            fontWeight = FontWeight.Bold
        )
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

private fun dateKey(millis: Long): String =
    SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(Date(millis))
