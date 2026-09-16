package com.medremind.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.medremind.app.data.DoseStatus
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, vm: MedicineViewModel) {
    val history by vm.history.collectAsState()

    LaunchedEffect(Unit) { vm.markOverdueAsMissed() }

    val todayStart = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val todayItems = history.filter { it.event.scheduledAt >= todayStart }
    val takenToday = todayItems.count { it.event.status == DoseStatus.TAKEN }
    val skippedToday = todayItems.count { it.event.status == DoseStatus.SKIPPED }
    val missedToday = todayItems.count { it.event.status == DoseStatus.MISSED }
    val pendingToday = todayItems.count { it.event.status == DoseStatus.PENDING }

    val grouped = remember(history) {
        history.groupBy { dateKey(it.event.scheduledAt) }
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Today", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.size(4.dp))
                        Text("Taken: $takenToday    Missed: $missedToday")
                        Text("Skipped: $skippedToday    Pending: $pendingToday")
                    }
                }
            }

            if (history.isEmpty()) {
                item {
                    Text(
                        "No doses recorded yet. When a reminder fires and you mark it, it will appear here.",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            grouped.forEach { (day, dayItems) ->
                item {
                    Text(day, style = MaterialTheme.typography.titleMedium)
                }
                items(dayItems, key = { it.event.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val photo = item.photoPath
                            if (photo != null) {
                                AsyncImage(
                                    model = File(photo),
                                    contentDescription = item.medicineName,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("?")
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.medicineName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    timeFormat.format(Date(item.event.scheduledAt)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = statusLabel(item.event.status),
                                color = statusColor(item.event.status),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun dateKey(millis: Long): String =
    SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(Date(millis))

private fun statusLabel(status: String): String = when (status) {
    DoseStatus.TAKEN -> "Taken"
    DoseStatus.SKIPPED -> "Skipped"
    DoseStatus.MISSED -> "Missed"
    else -> "Pending"
}

private fun statusColor(status: String): Color = when (status) {
    DoseStatus.TAKEN -> Color(0xFF2E7D32)
    DoseStatus.SKIPPED -> Color(0xFF757575)
    DoseStatus.MISSED -> Color(0xFFC62828)
    else -> Color(0xFF1565C0)
}
