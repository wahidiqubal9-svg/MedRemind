package com.medremind.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.medremind.app.data.DoseStatus
import com.medremind.app.data.Medicine
import kotlinx.coroutines.launch
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
    onOpenMe: () -> Unit,
    profilePhoto: String? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var doses by remember { mutableStateOf<List<TodayDose>>(emptyList()) }
    var dosesLoaded by remember { mutableStateOf(false) }
    var reloadTick by remember { mutableIntStateOf(0) }
    var markedDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    val scope = rememberCoroutineScope()
    val haptics = rememberMedHaptics()
    val prnMeds by vm.prnMedicines.collectAsState()

    LaunchedEffect(selectedDate, medicines, reloadTick) {
        doses = vm.dosesOn(selectedDate)
        dosesLoaded = true
    }

    LaunchedEffect(medicines) {
        val today = LocalDate.now()
        markedDates = vm.datesWithDoses(today.minusDays(180), today.plusDays(180))
    }

    fun record(dose: TodayDose, status: String) {
        if (status == DoseStatus.TAKEN) haptics.confirm() else haptics.reject()
        val verb = if (status == DoseStatus.TAKEN) "taken" else "skipped"
        vm.markDose(dose, status) { eventId ->
            reloadTick++
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "${dose.medicine.name} $verb",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    haptics.tick()
                    vm.undoDose(eventId) { reloadTick++ }
                }
            }
        }
    }

    val listState = rememberLazyListState()
    // When the calendar collapses, bring the list back to the top so the whole
    // progress card is visible; further upward scrolling then behaves normally.
    LaunchedEffect(expanded) {
        if (!expanded) {
            listState.scrollToItem(0)
        }
    }

    // Pull the list down (like pull-to-refresh) to open the calendar; drag up to collapse.
    val pullAmount = remember { floatArrayOf(0f) }
    val suppressUntil = remember { longArrayOf(0L) }
    val pullToCalendar = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!expanded && available.y > 0f) {
                    pullAmount[0] += available.y
                    if (pullAmount[0] >= 140f) {
                        expanded = true
                        pullAmount[0] = 0f
                        suppressUntil[0] = System.currentTimeMillis() + 450
                    }
                    return Offset(0f, available.y)
                }
                // Swallow the remainder of the opening gesture so the list doesn't scroll.
                if (System.currentTimeMillis() < suppressUntil[0] && available.y > 0f) {
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (expanded && available.y < 0f) {
                    pullAmount[0] += -available.y
                    if (pullAmount[0] >= 110f) {
                        expanded = false
                        pullAmount[0] = 0f
                        suppressUntil[0] = System.currentTimeMillis() + 450
                    }
                    return Offset(0f, available.y)
                }
                // Swallow the remainder of the collapse gesture so the list stays put.
                if (System.currentTimeMillis() < suppressUntil[0] && available.y < 0f) {
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Stop any fling from scrolling the list right after opening/collapsing.
                return if (expanded || System.currentTimeMillis() < suppressUntil[0]) {
                    available
                } else {
                    Velocity.Zero
                }
            }
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
            onOpenMe = onOpenMe,
            profilePhoto = profilePhoto
        )

        CalendarHandle(
            expanded = expanded,
            onToggle = {
                haptics.tap()
                expanded = !expanded
            }
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
                // Kept composed (only the height animates) so the selected date / pager
                // state survives collapsing the calendar.
                val stripHeight by animateDpAsState(
                    targetValue = if (expanded) 0.dp else 78.dp,
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                    label = "stripHeight"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(stripHeight)
                        .clipToBounds()
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
                    enter = expandVertically(tween(320, easing = FastOutSlowInEasing)) +
                        fadeIn(tween(240)),
                    exit = shrinkVertically(tween(320, easing = FastOutSlowInEasing)) +
                        fadeOut(tween(160))
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
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(pullToCalendar),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!dosesLoaded) {
                item(key = "loading") {
                    TodaySkeleton()
                }
            } else {
            if (doses.isNotEmpty()) {
                item(key = "summary") {
                    SummaryCard(
                        taken = doses.count { it.status == DoseStatus.TAKEN },
                        total = doses.size,
                        missed = doses.count { it.status == DoseStatus.MISSED }
                    )
                }
            }
            if (doses.isEmpty() && prnMeds.isEmpty()) {
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
                        onTake = { dose -> record(dose, DoseStatus.TAKEN) },
                        onSkip = { dose -> record(dose, DoseStatus.SKIPPED) }
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
                            onTake = { dose -> record(dose, DoseStatus.TAKEN) },
                            onSkip = { dose -> record(dose, DoseStatus.SKIPPED) }
                        )
                    }
                }

                if (prnMeds.isNotEmpty()) {
                    item(key = "prn_header") {
                        SectionHeader("As needed", modifier = Modifier.padding(top = 10.dp))
                    }
                    items(prnMeds, key = { "prn-${it.id}" }) { med ->
                        PrnCard(
                            medicine = med,
                            onLog = {
                                haptics.confirm()
                                vm.logPrnDose(med) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "${med.name} logged",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
            }
        }
    }
    }
}

@Composable
private fun PrnCard(medicine: Medicine, onLog: () -> Unit) {
    MedCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MedAvatar(
                name = medicine.name,
                photoPath = medicine.photoPath,
                size = 46.dp,
                accent = medicineAccent(medicine.id)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(medicine.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Take when needed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                onClick = onLog,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    "Log dose",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarHandle(expanded: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 5.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = if (expanded) "Hide calendar" else "Swipe down for calendar",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SummaryCard(taken: Int, total: Int, missed: Int) {
    val targetPct = if (total == 0) 0 else taken * 100 / total
    val pct by animateIntAsState(
        targetValue = targetPct,
        animationSpec = motionTween(MedMotion.Slow, easing = MedMotion.Emphasized),
        label = "summaryPct"
    )
    val takenAnim by animateIntAsState(
        targetValue = taken,
        animationSpec = motionTween(MedMotion.Slow, easing = MedMotion.Emphasized),
        label = "summaryTaken"
    )
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
                    "$takenAnim of $total doses taken",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryChip(Color(0xFF7EF0B2), "$takenAnim Taken")
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
    onOpenMe: () -> Unit,
    profilePhoto: String? = null
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
            icon = Icons.Rounded.Person,
            contentDescription = "Me",
            onClick = onOpenMe,
            photoPath = profilePhoto
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
    val todayWeekStart = weekStartOf(today)
    val pageCount = 1201
    val center = pageCount / 2
    val pagerState = rememberPagerState(
        initialPage = (center + weeksBetween(todayWeekStart, selectedDate)).coerceIn(0, pageCount - 1),
        pageCount = { pageCount }
    )

    // Keep the strip on the week that contains the selected date (so after picking
    // a day in the full calendar, the strip shows that date's week).
    LaunchedEffect(selectedDate, todayWeekStart) {
        val target = (center + weeksBetween(todayWeekStart, selectedDate)).coerceIn(0, pageCount - 1)
        if (target != pagerState.currentPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(target)
        }
    }

    // When the user swipes to another week, keep the same weekday selected.
    LaunchedEffect(pagerState.settledPage) {
        val weekStart = todayWeekStart.plusDays(((pagerState.settledPage - center) * 7).toLong())
        val candidate = weekStart.plusDays((selectedDate.dayOfWeek.value % 7).toLong())
        if (candidate != selectedDate) onSelect(candidate)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
    ) { page ->
        val start = todayWeekStart.plusDays(((page - center) * 7).toLong())
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

private fun weekStartOf(date: LocalDate): LocalDate =
    date.minusDays((date.dayOfWeek.value % 7).toLong())

private fun weeksBetween(base: LocalDate, date: LocalDate): Int =
    ((weekStartOf(date).toEpochDay() - base.toEpochDay()) / 7).toInt()



private fun dateToMillis(date: LocalDate): Long =
    date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
