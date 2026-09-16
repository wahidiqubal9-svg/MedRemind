package com.medremind.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

    LaunchedEffect(selectedDate, medicines) {
        doses = vm.dosesOn(selectedDate)
    }

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedDate == LocalDate.now()) "Today's doses"
                else SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(dateToMillis(selectedDate)),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text("${doses.size} doses", style = MaterialTheme.typography.labelMedium)
        }

        if (doses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No doses scheduled for this day.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(doses, key = { it.timeMillis.toString() + it.medicine.id }) { dose ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val photo = dose.medicine.photoPath
                            if (photo != null) {
                                AsyncImage(
                                    model = File(photo),
                                    contentDescription = dose.medicine.name,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("?")
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dose.medicine.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    timeFormat.format(Date(dose.timeMillis)),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = doseStatusLabel(dose.status),
                                color = doseStatusColor(dose.status),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .pointerInput(expanded) {
                detectVerticalDragGestures { _, dragAmount -> onDrag(dragAmount) }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onToggle() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(headerText, style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (expanded) "Pull up to collapse" else "Pull down for full calendar",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Text(if (expanded) "\u25B2" else "\u25BC")
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevMonth) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(
                        text = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                            .format(dateToMillis(month.atDay(1))),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onNextMonth) {
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                        Text(
                            text = label,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))

                val firstOfMonth = month.atDay(1)
                val leading = firstOfMonth.dayOfWeek.value - 1
                val totalCells = leading + month.lengthOfMonth()
                val rows = (totalCells + 6) / 7
                val today = LocalDate.now()

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
                                        .height(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable { onSelectDay(date) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onPrimaryContainer,
                                            style = MaterialTheme.typography.bodyMedium
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

private fun CalendarHeader(date: LocalDate): String {
    val today = LocalDate.now()
    val prefix = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> SimpleDateFormat("EEEE", Locale.getDefault()).format(dateToMillis(date))
    }
    return prefix + " · " + SimpleDateFormat("d MMM", Locale.getDefault()).format(dateToMillis(date))
}

private fun dateToMillis(date: LocalDate): Long =
    date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun doseStatusLabel(status: String): String = when (status) {
    DoseStatus.TAKEN -> "Taken"
    DoseStatus.SKIPPED -> "Skipped"
    DoseStatus.MISSED -> "Missed"
    else -> "Upcoming"
}

private fun doseStatusColor(status: String): Color = when (status) {
    DoseStatus.TAKEN -> Color(0xFF2E7D32)
    DoseStatus.SKIPPED -> Color(0xFF757575)
    DoseStatus.MISSED -> Color(0xFFC62828)
    else -> Color(0xFF1565C0)
}
