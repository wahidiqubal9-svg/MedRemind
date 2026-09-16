package com.medremind.app.alarm

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.Medicine
import com.medremind.app.ui.SlideToAction
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
    val fallbackBrush = Brush.verticalGradient(
        listOf(Color(0xFF063B36), Color(0xFF04211E))
    )
    val scrimBrush = Brush.verticalGradient(
        listOf(
            Color.Black.copy(alpha = 0.55f),
            Color.Black.copy(alpha = 0.20f),
            Color.Black.copy(alpha = 0.55f)
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        val photo = med?.photoPath
        if (photo != null) {
            AsyncImage(
                model = File(photo),
                contentDescription = med.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fallbackBrush)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimBrush)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Text(
                text = "TIME TO TAKE YOUR MEDICINE",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = scheduledText.ifBlank { "Now" },
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = med?.name ?: "Medicine",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (!med?.strength.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = med?.strength ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))

            SlideToAction(
                text = "Slide to take",
                icon = Icons.Rounded.CheckCircle,
                onConfirm = { onAction("TAKEN") },
                containerColor = Color.White,
                contentColor = Color(0xFF04352F)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onAction("SNOOZE") },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.5.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Text(
                        "Snooze 5m",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedButton(
                    onClick = { onAction("SKIPPED") },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Text(
                        "Skip",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}