package com.medremind.app.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.medremind.app.data.DoseStatus
import com.medremind.app.data.Medicine
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Date
import java.util.Locale

@Composable
fun TodayContent(
    modifier: Modifier = Modifier,
    medicines: List<Medicine>,
    vm: MedicineViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var doses by remember { mutableStateOf<List<TodayDose>>(emptyList()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    LaunchedEffect(selectedDate, medicines) {
        doses = vm.dosesOn(selectedDate)
    }

    val now = System.currentTimeMillis()
    val isToday = selectedDate == LocalDate.now()
    val nextDose = doses.firstOrNull {
        it.status == DoseStatus.PENDING && it.timeMillis >= now
    } ?: doses.firstOrNull { it.status == DoseStatus.PENDING }

    val taken = doses.count { it.status == DoseStatus.TAKEN }
    val acted = doses.count { it.status != DoseStatus.PENDING }
    val adherencePct = if (acted == 0) 0 else taken * 100 / acted

    Column(modifier = modifier.fillMaxSize()) {
        CalendarCard(
            headerText = CalendarHeader(selectedDate),
            expanded = expanded,
            month = month,
            selectedDate = selectedDate,
            onToggle = { expanded = !expanded },
            onDrag = { delta ->
                if (!expanded && delta > 12f) expanded = true
                if (expanded && delta < -12f) expanded = false
            },
            onPrevMonth = { month = month.minusMonths(1) },
            onNextMonth = { month = month.plusMonths(1) },
            onSelectDay = { date ->
                selectedDate = date
                month = YearMonth.from(date)
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "hero") {
                NextDoseHero(
                    nextDose = if (isToday) nextDose else null,
                    adherencePct = adherencePct,
                    isToday = isToday,
                    dateLabel = CalendarHeader(selectedDate)
                )
            }

            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isToday) "Today's doses"
                        else SimpleDateFormat("EEE, d MMM", Locale.getDefault())
                            .format(dateToMillis(selectedDate)),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "${doses.size} doses",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (doses.isEmpty()) {
                item(key = "empty") {
                    MedEmptyState(
                        icon = Icons.Rounded.CalendarMonth,
                        title = "No doses scheduled",
                        message = "Nothing is scheduled for this day.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp)
                    )
                }
            } else {
                items(doses, key = { it.timeMillis.toString() + it.medicine.id }) { dose ->
                    DoseCard(dose = dose, timeFormat = timeFormat, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun NextDoseHero(
    nextDose: TodayDose?,
    adherencePct: Int,
    isToday: Boolean,
    dateLabel: String
) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    MedHeroCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isToday) "Next dose" else dateLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(2.dp))
                if (nextDose != null) {
                    Text(
                        text = timeFormat.format(Date(nextDose.timeMillis)),
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = nextDose.medicine.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (nextDose.medicine.strength.isNotBlank()) {
                        Text(
                            text = nextDose.medicine.strength,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                } else {
                    Text(
                        text = if (isToday) "All set" else "No doses",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = if (isToday) "Nothing left to take today."
                        else "Nothing scheduled for this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            ProgressRing(
                percent = adherencePct,
                modifier = Modifier.size(84.dp)
            ) {
                val photo = nextDose?.medicine?.photoPath
                if (photo != null) {
                    AsyncImage(
                        model = File(photo),
                        contentDescription = nextDose.medicine.name,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (adherencePct > 0) "$adherencePct%" else "\u2014",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoseCard(
    dose: TodayDose,
    timeFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    MedClickableCard(onClick = {}, modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val photo = dose.medicine.photoPath
            if (photo != null) {
                AsyncImage(
                    model = File(photo),
                    contentDescription = dose.medicine.name,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dose.medicine.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dose.medicine.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = timeFormat.format(Date(dose.timeMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(
                status = dose.status,
                label = if (dose.status == DoseStatus.PENDING) "Upcoming"
                else doseStatusLabel(dose.status)
            )
        }
    }
}

@Composable
private fun CalendarCard(
    headerText: String,
    expanded: Boolean,
    month: YearMonth,
    selectedDate: LocalDate,
    onToggle: () -> Unit,
    onDrag: (Float) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    MedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .pointerInput(expanded) {
                detectVerticalDragGestures { _, dragAmount -> onDrag(dragAmount) }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = headerText,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            Surface(
                onClick = onToggle,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (expanded) "Collapse" else "Full calendar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (!expanded) {
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                val stripStart = selectedDate.minusDays(2)
                for (i in 0 until 6) {
                    val date = stripStart.plusDays(i.toLong())
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val circleColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else -> Color.Transparent
                    }
                    val numberColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectDay(date) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = weekdayLetter(date),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected || isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(circleColor)
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
                                color = numberColor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected || isToday)
                                    FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevMonth) {
                        Icon(
                            Icons.Rounded.ChevronLeft,
                            contentDescription = "Previous month"
                        )
                    }
                    Text(
                        text = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                            .format(dateToMillis(month.atDay(1))),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onNextMonth) {
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = "Next month"
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
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

                val firstOfMonth = month.atDay(1)
                val leading = firstOfMonth.dayOfWeek.value - 1
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
                                val background = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    else -> Color.Transparent
                                }
                                val textColor = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(background)
                                            .then(
                                                if (isToday && !isSelected) {
                                                    Modifier.border(
                                                        1.5.dp,
                                                        MaterialTheme.colorScheme.primary,
                                                        CircleShape
                                                    )
                                                } else Modifier
                                            )
                                            .clickable { onSelectDay(date) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            color = textColor,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected || isToday)
                                                FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun weekdayLetter(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault())

private fun CalendarHeader(date: LocalDate): String {
    val today = LocalDate.now()
    val prefix = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> SimpleDateFormat("EEEE", Locale.getDefault()).format(dateToMillis(date))
    }
    return prefix + " \u00b7 " + SimpleDateFormat("d MMM", Locale.getDefault()).format(dateToMillis(date))
}

private fun dateToMillis(date: LocalDate): Long =
    date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()