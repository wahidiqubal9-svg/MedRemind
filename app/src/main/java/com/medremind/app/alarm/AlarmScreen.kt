package com.medremind.app.alarm

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
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
import com.medremind.app.ui.medicineAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Fraction of the whole screen covered by the medicine photo on the alarm.
// 100% = the picture takes over the entire screen; smaller values shrink it.
fun alarmImageFraction(sizePercent: Int) = sizePercent.coerceIn(40, 100) / 100f

@Composable
fun AlarmScreen(doseEventId: Long, snoozeMinutes: Int = 5, onAction: (String) -> Unit) {
    // Block the system back gesture/button: the alarm can only be dismissed by
    // Taken, Skipped or Snooze.
    androidx.activity.compose.BackHandler(enabled = true) { }
    val context = LocalContext.current
    var medicine by remember { mutableStateOf<Medicine?>(null) }
    var scheduledText by remember { mutableStateOf("") }
    var doseLine by remember { mutableStateOf("") }
    var instructionLine by remember { mutableStateOf("") }
    val imageSize = remember {
        context.getSharedPreferences("medremind_settings", android.content.Context.MODE_PRIVATE)
            .getInt("alarm_image_size", 100)
    }
    val imageFraction = alarmImageFraction(imageSize)

    LaunchedEffect(doseEventId) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.get(context)
            val event = db.doseEventDao().byId(doseEventId)
            if (event != null) {
                val med = db.medicineDao().byId(event.medicineId)
                val schedule = if (event.scheduleId > 0L) {
                    db.scheduleDao().byId(event.scheduleId)
                } else null
                val text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(event.scheduledAt))
                withContext(Dispatchers.Main) {
                    medicine = med
                    scheduledText = text
                    doseLine = schedule?.doseLabel.orEmpty()
                    instructionLine = com.medremind.app.data.IntakeInstruction
                        .label(med?.intakeInstruction.orEmpty())
                }
            }
        }
    }

    val med = medicine
    val glowColor = med?.let { medicineAccent(it.id) } ?: MaterialTheme.colorScheme.primary
    val glow = rememberInfiniteTransition(label = "alarmGlow")
    val glowAlpha by glow.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B10))
        )

        val photo = med?.photoPath
        if (photo != null) {
            AsyncImage(
                model = File(photo),
                contentDescription = med?.name,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(imageFraction)
                    .clip(RoundedCornerShape(if (imageFraction >= 0.99f) 0.dp else 28.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Medication,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size((260f * imageFraction).dp)
            )
        }

        // Keeps the text readable when the picture fills the screen.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.80f)
                        )
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.44f)
            val radius = size.minDimension * 1.05f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = glowAlpha),
                        glowColor.copy(alpha = glowAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
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
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = med?.name ?: "Medicine",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (!med?.strength.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = med?.strength ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
            if (doseLine.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = doseLine,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
            if (instructionLine.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = instructionLine,
                    style = MaterialTheme.typography.titleMedium,
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
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Text(
                        "Snooze ${snoozeMinutes}m",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedButton(
                    onClick = { onAction("SKIPPED") },
                    shape = RoundedCornerShape(50),
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
        }
    }
}
