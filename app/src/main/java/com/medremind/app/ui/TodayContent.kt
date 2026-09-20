package com.medremind.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
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
import androidx.compose.ui.unit.sp
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
    greetingName: String? = null,
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
            profilePhoto = profilePhoto,
            greetingName = greetingName
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
                    targetValue = if (expanded) 0.dp else 100.dp,
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                    label = "stripHeight"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(stripHeight)
                        .clipToBounds()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        ),
                        shadowElevation = MedElevation.card
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 200.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!dosesLoaded) {
                item(key = "loading") {
                    TodaySkeleton()
                }
            } else {
            val nextDose = doses
                .filter { it.status == DoseStatus.PENDING }
                .minByOrNull { it.timeMillis }
            if (doses.isNotEmpty()) {
                item(key = "summary") {
                    SummaryCard(
                        taken = doses.count { it.status == DoseStatus.TAKEN },
                        total = doses.size,
                        missed = doses.count { it.status == DoseStatus.MISSED }
                    )
                }
            }
            if (nextDose != null) {
                item(key = "next_dose") {
                    NextDoseBanner(
                        dose = nextDose,
                        onTake = { record(nextDose, DoseStatus.TAKEN) }
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
                                .padding(vertical = 28.dp)
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
private fun SummaryCard(
    taken: Int,
    total: Int,
    missed: Int
) {
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
    val encouragement = when {
        targetPct >= 100 -> "All done \u2014 great job!"
        targetPct >= 50 -> "You're doing great today!"
        else -> "Let's get back on track."
    }

    MedCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "DAILY ADHERENCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "$takenAnim of $total doses",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "taken today \u00b7 ${pct}% complete",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.ThumbUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        encouragement,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            ProgressRing(
                percent = pct,
                modifier = Modifier.size(96.dp),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                progressColor = MaterialTheme.colorScheme.primary
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$pct%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "TAKEN",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NextDoseBanner(dose: TodayDose, onTake: () -> Unit) {
    val haptics = rememberMedHaptics()
    val minutes = ((dose.timeMillis - System.currentTimeMillis()) / 60_000L).toInt()
    val label = when {
        minutes > 1 -> "UP NEXT IN $minutes MINS"
        minutes == 1 -> "UP NEXT IN 1 MIN"
        minutes == 0 -> "DUE NOW"
        else -> "OVERDUE"
    }
    val timeText = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dose.timeMillis))
    val intake = com.medremind.app.data.IntakeInstruction.label(dose.medicine.intakeInstruction)
    val subtitle = listOf(timeText, intake).filter { it.isNotBlank() }.joinToString(" \u00b7 ")

    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "nextPulse")
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = motionTween(MedMotion.Slow, easing = MedMotion.Decelerate),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "nextPulseAlpha"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        shadowElevation = MedElevation.raised
    ) {
        Box(modifier = Modifier.background(MedGradients.heroHorizontal())) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7EF0B2).copy(alpha = dotAlpha))
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = listOf(dose.medicine.name, dose.medicine.strength)
                            .filter { it.isNotBlank() }.joinToString(" "),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Surface(
                    onClick = {
                        haptics.confirm()
                        onTake()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Take",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun timeOfDayColor(hour: Int): Color = when {
    hour in 5..11 -> Color(0xFFD97706)
    hour in 12..16 -> Color(0xFFF59E0B)
    hour in 17..20 -> Color(0xFF9333EA)
    else -> Color(0xFF6D5BD0)
}

private fun timeOfDayIcon(hour: Int): ImageVector = when {
    hour in 5..11 -> Icons.Rounded.WbSunny
    hour in 12..16 -> Icons.Rounded.WbTwilight
    hour in 17..20 -> Icons.Rounded.NightsStay
    else -> Icons.Rounded.Bedtime
}

private fun segmentLabel(hour: Int): String = when {
    hour in 5..11 -> "Morning"
    hour in 12..16 -> "Afternoon"
    hour in 17..20 -> "Evening"
    else -> "Night"
}

@Composable
private fun TodayHeader(
    selectedDate: LocalDate,
    expanded: Boolean,
    onToggleCalendar: () -> Unit,
    onOpenMe: () -> Unit,
    profilePhoto: String? = null,
    greetingName: String? = null
) {
    val isToday = selectedDate == LocalDate.now()
    val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    val title = if (greetingName.isNullOrBlank()) greeting else "$greeting, $greetingName"
    val dateLabel = SimpleDateFormat(
        if (selectedDate.year == LocalDate.now().year) "EEEE, MMM d" else "EEEE, MMM d, yyyy",
        Locale.getDefault()
    ).format(dateToMillis(selectedDate)) + if (isToday) " \u00b7 Today" else ""
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
            .height(100.dp)
    ) { page ->
        val start = todayWeekStart.plusDays(((page - center) * 7).toLong())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (i in 0 until 7) {
                val date = start.plusDays(i.toLong())
                val isSelected = date == selectedDate
                val isToday = date == today
                val active = isSelected || isToday
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
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isToday -> MaterialTheme.colorScheme.primary
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                            color = when {
                                isToday -> MaterialTheme.colorScheme.onPrimary
                                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    date in markedDates && isToday ->
                                        MaterialTheme.colorScheme.onPrimary
                                    date in markedDates -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                }
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
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = MedElevation.card
    ) {
        Column {
            val hour = remember(doses) {
                java.util.Calendar.getInstance().apply { timeInMillis = doses.first().timeMillis }
                    .get(java.util.Calendar.HOUR_OF_DAY)
            }
            val allDone = doses.all { it.status != DoseStatus.PENDING }
            val anyDone = doses.any { it.status != DoseStatus.PENDING }
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
                        imageVector = timeOfDayIcon(hour),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${segmentLabel(hour)} \u00b7 $time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${doses.size} medicine" + if (doses.size == 1) "" else "s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SegmentStatusChip(allDone = allDone, anyDone = anyDone)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                doses.forEachIndexed { index, dose ->
                    DoseRow(dose = dose, onTake = onTake, onSkip = onSkip)
                    if (index != doses.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 74.dp),
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
private fun SegmentStatusChip(allDone: Boolean, anyDone: Boolean) {
    val label: String
    val container: Color
    val content: Color
    when {
        allDone -> {
            label = "All completed"
            container = MaterialTheme.colorScheme.tertiaryContainer
            content = MaterialTheme.colorScheme.onTertiaryContainer
        }
        anyDone -> {
            label = "In progress"
            container = MaterialTheme.colorScheme.primaryContainer
            content = MaterialTheme.colorScheme.onPrimaryContainer
        }
        else -> {
            label = "Upcoming"
            container = MaterialTheme.colorScheme.surfaceContainerHigh
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(content)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StrengthPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun DoseRow(
    dose: TodayDose,
    onTake: (TodayDose) -> Unit,
    onSkip: (TodayDose) -> Unit
) {
    val completed = dose.status != DoseStatus.PENDING
    val pill = listOf(dose.medicine.strength, dose.schedule.doseLabel)
        .filter { it.isNotBlank() }
        .joinToString(" \u00b7 ")
    val instruction = com.medremind.app.data.IntakeInstruction
        .label(dose.medicine.intakeInstruction)
        .ifBlank { schedulePatternLabel(dose.schedule) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MedAvatar(
            name = dose.medicine.name,
            photoPath = dose.medicine.photoPath,
            size = 44.dp,
            accent = medicineAccent(dose.medicine.id)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dose.medicine.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                    color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (pill.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    StrengthPill(pill)
                }
            }
            if (instruction.isNotBlank()) {
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        RoundStatusIcon(status = dose.status)
    }
}

@Composable
private fun RoundStatusIcon(status: String) {
    val tint = statusTint(status)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f))
            .border(1.dp, tint.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = statusIcon(status),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
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
    val strength = dose.medicine.strength
    val perTime = dose.schedule.doseLabel
    return listOf(strength, perTime)
        .filter { it.isNotBlank() }
        .joinToString(" \u00b7 ")
        .ifBlank { "1 dose" }
}

private fun weekdayLetter(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault())

private fun weekStartOf(date: LocalDate): LocalDate =
    date.minusDays((date.dayOfWeek.value % 7).toLong())

private fun weeksBetween(base: LocalDate, date: LocalDate): Int =
    ((weekStartOf(date).toEpochDay() - base.toEpochDay()) / 7).toInt()



private fun dateToMillis(date: LocalDate): Long =
    date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
