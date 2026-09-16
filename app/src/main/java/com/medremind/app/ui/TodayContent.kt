package com.medremind.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medremind.app.data.Medicine
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
    onEdit: (Medicine) -> Unit,
    onOpenSettings: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var doses by remember { mutableStateOf<List<TodayDose>>(emptyList()) }
    var mutedTimes by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(selectedDate, medicines) {
        doses = vm.dosesOn(selectedDate)
    }

    val groups = doses
        .groupBy { formatDoseTime(it.timeMillis) }
        .toList()
        .sortedBy { (_, list) -> list.minOf { it.timeMillis } }

    Column(modifier = modifier.fillMaxSize()) {
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
                WeekStrip(
                    selectedDate = selectedDate,
                    onSelect = {
                        selectedDate = it
                        month = YearMonth.from(it)
                    }
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            MonthGrid(
                month = month,
                selectedDate = selectedDate,
                onPrevMonth = { month = month.minusMonths(1) },
                onNextMonth = { month = month.plusMonths(1) },
                onSelectDay = { date ->
                    selectedDate = date
                    month = YearMonth.from(date)
                }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (doses.isEmpty()) {
                item(key = "empty") {
                    MedEmptyState(
                        icon = Icons.Rounded.CalendarMonth,
                        title = "No doses scheduled",
                        message = "Nothing is scheduled for this day. Add a medicine to get started.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        action = {
                            GradientPillButton(
                                text = "Add medicine",
                                icon = Icons.Rounded.Add,
                                onClick = onAdd
                            )
                        }
                    )
                }
            } else {
                items(groups, key = { it.first }) { (time, list) ->
                    TimeGroupCard(
                        time = time,
                        doses = list,
                        muted = time in mutedTimes,
                        onToggleMute = {
                            mutedTimes = if (time in mutedTimes) mutedTimes - time else mutedTimes + time
                        },
                        onEdit = onEdit,
                        onAdd = onAdd,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
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
                    }
                }
            }
        }
    }


@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
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
    muted: Boolean,
    onToggleMute: () -> Unit,
    onEdit: (Medicine) -> Unit,
    onAdd: () -> Unit,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                SpeakerToggle(muted = muted, onToggle = onToggleMute)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                doses.forEachIndexed { index, dose ->
                    DoseRow(dose = dose, onEdit = onEdit)
                    if (index != doses.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 66.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                DashedAddRow(
                    text = "Med / Tracker",
                    onClick = onAdd,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun DoseRow(
    dose: TodayDose,
    onEdit: (Medicine) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onEdit(dose.medicine) }
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
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = doseSubtitle(dose),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        IconButton(onClick = { onEdit(dose.medicine) }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options for ${dose.medicine.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
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

private fun formatDoseTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis)).lowercase(Locale.getDefault())

private fun weekdayLetter(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault())



private fun dateToMillis(date: LocalDate): Long =
    date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
