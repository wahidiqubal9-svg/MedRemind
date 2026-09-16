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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.medremind.app.data.Medicine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    medicines: List<Medicine>,
    vm: MedicineViewModel,
    onAdd: () -> Unit,
    onEdit: (Medicine) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit
) {
    var upcoming by remember { mutableStateOf<UpcomingAlarm?>(null) }

    LaunchedEffect(medicines, medicines.size) {
        upcoming = vm.upcomingAlarms().firstOrNull()
    }

    Scaffold(
        topBar = {
            MedTopAppBar(
                title = "MedRemind",
                actions = {
                    MedTopBarAction("History") { onOpenHistory() }
                    MedTopBarAction("Settings") { onOpenSettings() }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val next = upcoming
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("NEXT DOSE", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    if (next == null) {
                        Text(
                            "No reminders scheduled",
                            style = MaterialTheme.typography.titleLarge
                        )
                    } else {
                        Text(next.medicineName, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault())
                                .format(Date(next.triggerAt)),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            if (medicines.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No medicines yet.\nTap \"+\" to add one.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(medicines, key = { it.id }) { medicine ->
                        Card(
                            onClick = { onEdit(medicine) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val photo = medicine.photoPath
                                if (photo != null) {
                                    AsyncImage(
                                        model = File(photo),
                                        contentDescription = medicine.name,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("?")
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = medicine.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (medicine.strength.isNotBlank()) {
                                        Text(
                                            text = medicine.strength,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
