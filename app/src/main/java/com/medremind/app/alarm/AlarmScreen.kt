package com.medremind.app.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.Medicine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmScreen(doseEventId: Long, onAction: (String) -> Unit) {
    val context = LocalContext.current
    var medicine by remember { mutableStateOf<Medicine?>(null) }
    var scheduledText by remember { mutableStateOf("") }

    LaunchedEffect(doseEventId) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.get(context)
            val event = db.doseEventDao().byId(doseEventId)
            if (event != null) {
                val med = db.medicineDao().byId(event.medicineId)
                val text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(event.scheduledAt))
                withContext(Dispatchers.Main) {
                    medicine = med
                    scheduledText = text
                }
            }
        }
    }

    val med = medicine
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Time to take your medicine",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            val photo = med?.photoPath
            if (photo != null) {
                AsyncImage(
                    model = File(photo),
                    contentDescription = med.name,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No photo", style = MaterialTheme.typography.headlineMedium)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(text = med?.name ?: "Medicine", style = MaterialTheme.typography.headlineMedium)
            if (!med?.strength.isNullOrBlank()) {
                Text(text = med?.strength ?: "", style = MaterialTheme.typography.titleMedium)
            }
            if (scheduledText.isNotBlank()) {
                Text(text = scheduledText, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = { onAction("TAKEN") }, modifier = Modifier.weight(1f)) {
                    Text("Taken")
                }
                OutlinedButton(onClick = { onAction("SNOOZE") }, modifier = Modifier.weight(1f)) {
                    Text("Snooze 5m")
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { onAction("SKIPPED") }, modifier = Modifier.fillMaxWidth()) {
                Text("Skip")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
