package com.medremind.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.medremind.app.data.DoseStatus
import com.medremind.app.data.Medicine
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Date
import java.util.Locale

@Composable
fun TodayContent(
    modifier: Modifier = Modifier,
    medicines: List<Medicine>,
    vm: MedicineViewModel,
    onAdd: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var doses by remember { mutableStateOf<List<TodayDose>>(emptyList()) }
    var dosesLoaded by remember { mutableStateOf(false) }
    var reloadTick by remember { mutableIntStateOf(0) }
    var markedDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedDate, medicines, reloadTick) {
        doses = vm.dosesOn(selectedDate)
        dosesLoaded = true
    }

    LaunchedEffect(medicines) {
        val today = LocalDate.now()
        markedDates = vm.datesWithDoses(today.minusDays(180), today.plusDays(180))
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(1600)
            toast = null
        }
    }

    val doseTimeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val pendingGroups = remember(doses) {
        doses
            .filter { it.status == DoseStatus.PENDING }
            .groupBy { doseTimeFormat.format(Date(it.timeMillis)).lowercase(Locale.getDefault()) }
            .toList()
            .sortedBy { (_, list) -> list.minOf { it.timeMillis } }
    }
    val doneGroups = remember(doses) {
        doses
            .filter { it.status != DoseStatus.PENDING }
            .groupBy { doseTimeFormat.format(Date(it.timeMillis)).lowercase(Locale.getDefault()) }
            .toList()
            .sortedBy { (_, list) -> list.minOf { it.timeMillis } }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        TodayHeader(
            selectedDate = selectedDate,
            expanded = expanded,
            onToggleCalendar = { expanded = !expanded },
            onOpenSettings = onOpenSettings
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(expanded) {
                    var acc = 0f
                    detectVerticalDragGestures { _, amount ->
                        acc += amount
                        if (acc > 40f) {
                            expanded = true
                            acc = 0f
                        } else if (acc < -40f) {
                            expanded = false
                            acc = 0f
                        }
                    }
                }
        ) {
            Column {
                AnimatedVisibility(
                    visible = !expanded,
                    enter = expandVertically(tween(280)) + fadeIn(tween(220)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(140))
                ) {
                    WeekStrip(
                        selectedDate = selectedDate,
                        markedDates = markedDates,
                        onSelect = {
                            selectedDate = it
                            month = YearMonth.from(it)
                        }
                    )
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(tween(300)) + fadeIn(tween(240)),
                    exit = shrinkVertically(tween(220)) + fadeOut(tween(140))
                ) {
                    MonthGrid(
                        month = month,
                        selectedDate = selectedDate,
                        markedDates = markedDates,
                        onPrevMonth = { month = month.minusMonths(1) },
                        onNextMonth = { month = month.plusMonths(1) },
                        onSelectDay = { date ->
                            selectedDate = date
                            month = YearMonth.from(date)
                        }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!dosesLoaded) {
                item(key = "loading") {
                    MedCard(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
            item(key = "summary") {
                SummaryCard(
                    taken = doses.count { it.status == DoseStatus.TAKEN },
                    total = doses.size,
                    missed = doses.count { it.status == DoseStatus.MISSED }
                )
            }
            if (doses.isEmpty()) {
                item(key = "empty") {
                    MedCard(modifier = Modifier.fillMaxWidth()) {
                        MedEmptyState(
                            icon = Icons.Rounded.CalendarMonth,
                            title = "No doses scheduled",
                            message = "Nothing is scheduled for this day. Add a medicine to get started.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            action = {
                                GradientPillButton(
                                    text = "Add medicine",
                                    icon = Icons.Rounded.Add,
                                    onClick = onAdd
                                )
                            }
                        )
                    }
                }
            } else {
                items(
                    items = pendingGroups,
                    key = { "p-${it.first}" },
                    contentType = { "dose_group" }
                ) { (time, list) ->
                    TimeGroupCard(
                        time = time,
                        doses = list,
                        onTake = { dose ->
                            vm.markDose(dose, DoseStatus.TAKEN) { reloadTick++ }
                            toast = "${dose.medicine.name} taken"
                        },
                        onSkip = { dose ->
                            vm.markDose(dose, DoseStatus.SKIPPED) { reloadTick++ }
                            toast = "${dose.medicine.name} skipped"
                        }
                    )
                }

                if (doneGroups.isNotEmpty()) {
                    item(key = "done_header") {
                        SectionHeader(
                            "Today's medicines",
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    items(
                        items = doneGroups,
                        key = { "d-${it.first}" },
                        contentType = { "dose_group" }
                    ) { (time, list) ->
                        TimeGroupCard(
                            time = time,
                            doses = list,
                            onTake = { dose -> vm.markDose(dose, DoseStatus.TAKEN) { reloadTick++ } },
                            onSkip = { dose -> vm.markDose(dose, DoseStatus.SKIPPED) { reloadTick++ } }
                        )
                    }
                }
            }
            }
        }
    }
    AnimatedVisibility(
        visible = toast != null,
        enter = fadeIn(tween(160)) + slideInVertically(tween(240)) { it },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 110.dp, start = 16.dp, end = 16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = toast ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
    }
}

@Composable
private fun SummaryCard(taken: Int, total: Int, missed: Int) {
    val pct = if (total == 0) 0 else taken * 100 / total
    MedHeroCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Daily progress",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "$taken of $total doses taken",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryChip(Color(0xFF7EF0B2), "$taken Taken")
                    SummaryChip(Color(0xFFFFB3C4), "$missed Missed")
                }
            }
            Spacer(Modifier.width(12.dp))
            ProgressRing(percent = pct, modifier = Modifier.size(80.dp)) {
                Text(
                    "$pct%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SummaryChip(dot: Color, text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.18f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dot)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun timeOfDayColor(hour: Int): Color = when {
    hour in 5..11 -> Color(0xFFD97706)
    hour in 12..16 -> Color(0xFFF59E0B)
    hour in 17..20 -> Color(0xFF9333EA)
    else -> Color(0xFF6D5BD0)
}

@Composable
private fun TodayHeader(
    selectedDate: LocalDate,
    expanded: Boolean,
    onToggleCalendar: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isToday = selectedDate == LocalDate.now()
    val title = if (isToday) {
        "Today"
    } else {
        val sameYear = selectedDate.year == LocalDate.now().year
        SimpleDateFormat(
            if (sameYear) "MMMM d" else "MMMM d, yyyy",
            Locale.getDefault()
        ).format(dateToMillis(selectedDate))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        SquareIconButton(
            icon = Icons.Rounded.CalendarMonth,
            contentDescription = if (expanded) "Hide calendar" else "Full calendar",
            onClick = onToggleCalendar
        )
        Spacer(Modifier.width(10.dp))
        SquareIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "Settings",
            onClick = onOpenSettings
        )
    }
}

@Composable
private fun WeekStrip(
    selectedDate: LocalDate,
    markedDates: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val pageCount = 1201
    val center = pageCount / 2
    val pagerState = rememberPagerState(initialPage = center, pageCount = { pageCount })

    LaunchedEffect(pagerState.settledPage) {
        val anchor = today.plusDays(((pagerState.settledPage - center) * 7).toLong())
        if (anchor != selectedDate) onSelect(anchor)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
    ) { page ->
        val start = today.plusDays(((page - center) * 7).toLong()).minusDays(3)
        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                for (i in 0 until 7) {
                    val date = start.plusDays(i.toLong())
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelect(date) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = weekdayLetter(date),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else Color.Transparent
                                )
                                .then(
                                    if (isToday && !isSelected) {
                                        Modifier.border(
                                            1.5.dp,
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        )
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected || isToday) FontWeight.SemiBold
                                else FontWeight.Normal,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.surface
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (date in markedDates) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                        )
                    }
                }
            }
        }
    }


@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    markedDates: Set<LocalDate>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    .format(dateToMillis(month.atDay(1))),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month")
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                Text(
                    text = label,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        val leading = month.atDay(1).dayOfWeek.value % 7
        val totalCells = leading + month.lengthOfMonth()
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayNumber = row * 7 + col - leading + 1
                    if (dayNumber in 1..month.lengthOfMonth()) {
                        val date = month.atDay(dayNumber)
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.onSurface
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onSelectDay(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.surface
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected || isToday) FontWeight.SemiBold
                                    else FontWeight.Normal
                                )
                            }
                            if (date in markedDates) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 3.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f).height(42.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeGroupCard(
    time: String,
    doses: List<TodayDose>,
    onTake: (TodayDose) -> Unit,
    onSkip: (TodayDose) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column {
            val hour = remember(doses) {
                java.util.Calendar.getInstance().apply { timeInMillis = doses.first().timeMillis }
                    .get(java.util.Calendar.HOUR_OF_DAY)
            }
            val allDone = doses.all { it.status != DoseStatus.PENDING }
            val accentColor = timeOfDayColor(hour)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${doses.size} medicine" + if (doses.size == 1) "" else "s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!allDone) {
                    StatusChip(status = DoseStatus.PENDING, label = "Upcoming")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                doses.forEachIndexed { index, dose ->
                    DoseRow(dose = dose, onTake = onTake, onSkip = onSkip)
                    if (index != doses.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 66.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun DoseRow(
    dose: TodayDose,
    onTake: (TodayDose) -> Unit,
    onSkip: (TodayDose) -> Unit
) {
    val completed = dose.status != DoseStatus.PENDING
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MedAvatar(
            name = dose.medicine.name,
            photoPath = dose.medicine.photoPath,
            size = 46.dp,
            accent = medicineAccent(dose.medicine.id)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dose.medicine.name,
                style = MaterialTheme.typography.titleMedium,
                textDecoration = if (completed) TextDecoration.LineThrough else null,
                color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = doseSubtitle(dose),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        if (completed) {
            StatusChip(status = dose.status)
        } else {
            DoseActionButton(
                text = "Take",
                icon = Icons.Rounded.CheckCircle,
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { onTake(dose) }
            )
            Spacer(Modifier.width(6.dp))
            DoseActionButton(
                text = "Skip",
                icon = Icons.Rounded.Close,
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onSkip(dose) }
            )
        }
    }
}

@Composable
private fun DoseActionButton(
    text: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun doseSubtitle(dose: TodayDose): String {
    val doseText = dose.schedule.doseLabel.takeIf { it.isNotBlank() }
        ?: dose.medicine.strength.takeIf { it.isNotBlank() }
        ?: "1 dose"
    val pattern = schedulePatternLabel(dose.schedule)
    return "$doseText  |  $pattern"
}

private fun weekdayLetter(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault())



private fun dateToMillis(date: LocalDate): Long =
    date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
