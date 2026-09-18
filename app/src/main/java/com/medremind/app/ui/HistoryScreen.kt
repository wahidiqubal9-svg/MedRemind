package com.medremind.app.ui

import androidx.compose.animation.core.animateIntAsState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.medremind.app.data.DoseStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryContent(
    modifier: Modifier = Modifier,
    vm: MedicineViewModel,
    onOpenMe: () -> Unit,
    profilePhoto: String? = null
) {
    val history by vm.history.collectAsState()

    LaunchedEffect(Unit) { vm.markOverdueAsMissed() }

    var rangeDays by remember { mutableIntStateOf(7) }
    var log by remember { mutableStateOf<List<DoseLogEntry>>(emptyList()) }
    var logLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(rangeDays, history) {
        log = vm.doseLogForRange(rangeDays)
        logLoaded = true
    }

    val taken = log.count { it.status == DoseStatus.TAKEN }
    val missed = log.count { it.status == DoseStatus.MISSED }
    val skipped = log.count { it.status == DoseStatus.SKIPPED }
    val pending = log.count { it.status == DoseStatus.PENDING }
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
                    icon = Icons.Rounded.Person,
                    contentDescription = "Me",
                    onClick = onOpenMe,
                    photoPath = profilePhoto
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
                rangeDays = rangeDays,
                taken = taken,
                missed = missed,
                skipped = skipped,
                pending = pending,
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
                if (!logLoaded) {
                    ListSkeleton(modifier = Modifier.padding(top = 4.dp))
                } else {
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
    rangeDays: Int,
    taken: Int,
    missed: Int,
    skipped: Int,
    pending: Int,
    percent: Int
) {
    val due = taken + missed + skipped
    val animatedPercent by animateIntAsState(
        targetValue = percent,
        animationSpec = motionTween(MedMotion.Slow, easing = MedMotion.Emphasized),
        label = "adherencePct"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        shadowElevation = MedElevation.raised
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
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ProgressRing(percent = animatedPercent, modifier = Modifier.size(124.dp)) {
                            Text(
                                text = "$animatedPercent%",
                                style = MaterialTheme.typography.headlineSmall,
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

            }
        }
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
