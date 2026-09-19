package com.medremind.app.ui

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.medremind.app.data.DoseStatus
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val periodDays = listOf(7, 30, 90, 365)
private val periodLabels = listOf("Week", "Month", "90 days", "Year")

@Composable
fun HistoryContent(
    modifier: Modifier = Modifier,
    vm: MedicineViewModel,
    onOpenMe: () -> Unit,
    profilePhoto: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rangeDays by remember { mutableIntStateOf(7) }
    var log by remember { mutableStateOf<List<DoseLogEntry>>(emptyList()) }
    var previous by remember { mutableStateOf<List<DoseLogEntry>>(emptyList()) }
    var logLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.markOverdueAsMissed() }

    LaunchedEffect(rangeDays) {
        val full = vm.doseLogForRange(rangeDays * 2)
        val cutoff = System.currentTimeMillis() - rangeDays * 86_400_000L
        previous = full.filter { it.scheduledAt < cutoff }
        log = full.filter { it.scheduledAt >= cutoff }
        logLoaded = true
    }

    // Current-period totals.
    val taken = log.count { it.status == DoseStatus.TAKEN }
    val missed = log.count { it.status == DoseStatus.MISSED }
    val skipped = log.count { it.status == DoseStatus.SKIPPED }
    val due = taken + missed + skipped
    val percent = if (due == 0) 0 else taken * 100 / due

    // Previous-period adherence for the trend arrow.
    val prevTaken = previous.count { it.status == DoseStatus.TAKEN }
    val prevDue = prevTaken + previous.count { it.status == DoseStatus.MISSED } +
        previous.count { it.status == DoseStatus.SKIPPED }
    val prevPercent = if (prevDue == 0) null else prevTaken * 100 / prevDue

    val zone = ZoneId.systemDefault()
    fun dayOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    // Per-day adherence across the current period.
    val byDay = remember(log) {
        log.groupBy { dayOf(it.scheduledAt) }.mapValues { (_, entries) ->
            val dueCount = entries.count { it.status != FUTURE_STATUS && it.status != DoseStatus.PENDING }
            val takenCount = entries.count { it.status == DoseStatus.TAKEN }
            dueCount to takenCount
        }
    }

    // Streak: consecutive fully-taken days ending today (or yesterday if today is open).
    val today = LocalDate.now()
    val streak = run {
        var count = 0
        var day = today
        val todayOk = byDay[day]?.let { it.first > 0 && it.second == it.first } == true
        if (!todayOk) day = day.minusDays(1)
        while (true) {
            val d = byDay[day]
            if (d != null && d.first > 0 && d.second == d.first) {
                count++
                day = day.minusDays(1)
            } else break
        }
        count
    }

    val weekDays = (6 downTo 0).map { today.minusDays(it.toLong()) }

    // Time-of-day reliability.
    val buckets = remember(log) {
        val defs = listOf(
            Triple("Morning", 5..11, Icons.Rounded.WbSunny),
            Triple("Afternoon", 12..16, Icons.Rounded.WbTwilight),
            Triple("Evening", 17..20, Icons.Rounded.Schedule),
            Triple("Night", 21..23, Icons.Rounded.Bedtime)
        )
        defs.map { (label, hours, icon) ->
            val entries = log.filter {
                val h = Instant.ofEpochMilli(it.scheduledAt).atZone(zone).hour
                h in hours || (label == "Night" && h in 0..4)
            }
            val d = entries.count { it.status != FUTURE_STATUS && it.status != DoseStatus.PENDING }
            val t = entries.count { it.status == DoseStatus.TAKEN }
            BucketAdherence(label, icon, d, t)
        }
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val grouped = remember(log) {
        val map = linkedMapOf<String, MutableList<DoseLogEntry>>()
        log.forEach { entry ->
            map.getOrPut(dateKey(entry.scheduledAt)) { mutableListOf() }.add(entry)
        }
        map
    }

    fun shareReport(mime: String, chooserTitle: String, build: () -> File) {
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
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                options = periodLabels,
                selectedIndex = periodDays.indexOf(rangeDays).coerceAtLeast(0),
                onSelect = { rangeDays = periodDays[it] },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item(key = "adherence") {
            AdherenceHeroCard(
                percent = percent,
                prevPercent = prevPercent,
                taken = taken,
                due = due,
                rangeDays = rangeDays
            )
        }

        item(key = "stats") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniMetricCard(
                    label = "Current streak",
                    value = if (streak == 1) "1 day" else "$streak days",
                    sub = if (streak > 0) "Keep it going!" else "Start a streak today",
                    icon = Icons.Rounded.LocalFireDepartment,
                    accent = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                MiniMetricCard(
                    label = "Doses taken",
                    value = "$taken",
                    sub = "of $due due",
                    icon = Icons.Rounded.TaskAlt,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "week_strip") {
            WeeklyStripCard(days = weekDays, byDay = byDay, today = today)
        }

        item(key = "reliability") {
            ReliabilityCard(buckets = buckets)
        }

        if (logLoaded && due > 0) {
            item(key = "insight") {
                InsightCard(buckets = buckets, percent = percent)
            }
        }

        item(key = "export") {
            ExportCard(
                enabled = log.isNotEmpty(),
                onCsv = {
                    shareReport("text/csv", "Share CSV report") {
                        ReportExporter.exportCsv(context, log)
                    }
                },
                onPdf = {
                    shareReport("application/pdf", "Share PDF report") {
                        ReportExporter.exportPdf(context, log, rangeDays)
                    }
                }
            )
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
        } else {
            item(key = "log_header") {
                SectionHeader(
                    "History",
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            grouped.forEach { (day, entries) ->
                item(key = "day-$day") {
                    Text(
                        day,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
}

private data class BucketAdherence(
    val label: String,
    val icon: ImageVector,
    val due: Int,
    val taken: Int
) {
    val percent: Int get() = if (due == 0) 0 else taken * 100 / due
}

@Composable
private fun AdherenceHeroCard(
    percent: Int,
    prevPercent: Int?,
    taken: Int,
    due: Int,
    rangeDays: Int
) {
    val animatedPercent by animateIntAsState(
        targetValue = percent,
        animationSpec = motionTween(MedMotion.Slow, easing = MedMotion.Emphasized),
        label = "adherencePct"
    )
    val status = when {
        percent >= 95 -> "EXCELLENT"
        percent >= 85 -> "GREAT"
        percent >= 70 -> "GOOD"
        else -> "NEEDS WORK"
    }
    val delta = prevPercent?.let { percent - it }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = MedElevation.raised
    ) {
        Box(modifier = Modifier.background(MedGradients.hero())) {
            Row(
                modifier = Modifier.padding(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OVERALL ADHERENCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$animatedPercent%",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (delta != null && delta != 0) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.18f),
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = (if (delta > 0) "+$delta%" else "$delta%"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Last $rangeDays days \u00b7 $taken of $due doses taken",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ProgressRing(percent = animatedPercent, modifier = Modifier.size(108.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.TaskAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMetricCard(
    label: String,
    value: String,
    sub: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = MedElevation.card
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.14f),
                    contentColor = accent
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyStripCard(
    days: List<LocalDate>,
    byDay: Map<LocalDate, Pair<Int, Int>>,
    today: LocalDate
) {
    MedCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Weekly streak",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Last 7 days",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            days.forEach { date ->
                val info = byDay[date]
                val due = info?.first ?: 0
                val taken = info?.second ?: 0
                val isToday = date == today
                DayCell(
                    date = date,
                    due = due,
                    taken = taken,
                    isToday = isToday,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    due: Int,
    taken: Int,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val complete = due > 0 && taken == due
    val partial = due > 0 && taken in 1 until due
    val missed = due > 0 && taken == 0
    val container = when {
        isToday -> MaterialTheme.colorScheme.primary
        complete -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
        partial -> Color(0xFFFFF4E0)
        missed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        complete -> MaterialTheme.colorScheme.tertiary
        partial -> Color(0xFF9A5B00)
        missed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when {
        isToday -> "TODAY"
        complete -> "100%"
        due > 0 -> "${taken * 100 / due}%"
        else -> "\u2014"
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.dayOfWeek.name.take(1),
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(container),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    complete || isToday -> Icons.Rounded.Check
                    partial -> Icons.Rounded.Schedule
                    else -> Icons.Rounded.TaskAlt
                },
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = content,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun ReliabilityCard(buckets: List<BucketAdherence>) {
    MedCard(modifier = Modifier.fillMaxWidth()) {
        Text("Time-of-day reliability", style = MaterialTheme.typography.titleMedium)
        Text(
            "How consistently you take doses across the day",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        buckets.forEachIndexed { index, bucket ->
            ReliabilityRow(bucket)
            if (index != buckets.lastIndex) Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun ReliabilityRow(bucket: BucketAdherence) {
    val fraction by animateFloatAsState(
        targetValue = bucket.percent / 100f,
        animationSpec = motionTween(MedMotion.Slow, easing = MedMotion.Emphasized),
        label = "rail-${bucket.label}"
    )
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Icon(
                    imageVector = bucket.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${bucket.label} doses",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (bucket.due == 0) "No doses scheduled"
                    else "${bucket.taken}/${bucket.due} on time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (bucket.due == 0) "\u2014" else "${bucket.percent}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (bucket.due == 0) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
private fun InsightCard(buckets: List<BucketAdherence>, percent: Int) {
    val weakest = buckets.filter { it.due >= 2 }.minByOrNull { it.percent }
    val message = when {
        weakest == null -> null
        weakest.percent < 90 ->
            "Tip: your ${weakest.label.lowercase()} doses are your weakest link at " +
                "${weakest.percent}%. A slightly earlier reminder could help."
        percent >= 95 ->
            "Great consistency! You're at ${percent}% adherence. Keep your reminders " +
                "exactly where they are."
        else ->
            "Steady progress. Try taking doses within 30 minutes of the scheduled time " +
                "to lift your average."
    } ?: return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(9.dp)
                        .size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "SMART INSIGHT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ExportCard(
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
                Text("Doctor visit report", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Share your adherence report as CSV or PDF.",
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
