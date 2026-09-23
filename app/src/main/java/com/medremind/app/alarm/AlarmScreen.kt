package com.medremind.app.alarm

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NoMeals
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.IntakeInstruction
import com.medremind.app.data.Medicine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Fraction of the available photo area used by the medicine picture.
fun alarmImageFraction(sizePercent: Int) = sizePercent.coerceIn(40, 100) / 100f

private val BgDeep = Color(0xFF06122D)
private val BgMid = Color(0xFF0B1D43)
private val BgBase = Color(0xFF071633)
private val GreetingBg = Color(0xFFDCEEFF)
private val GreetingFg = Color(0xFF0A2450)
private val CardCream = Color(0xFFFFFDF3)
private val CardFg = Color(0xFF102044)
private val ScheduleFg = Color(0xFF53627B)
private val InstructionBg = Color(0xFFD9F4E0)
private val InstructionFg = Color(0xFF145C28)
private val LaterBg = Color(0xFFDBE8FF)
private val LaterFg = Color(0xFF164A9E)
private val SkipBg = Color(0xFFFFD9D9)
private val SkipFg = Color(0xFFA51E1E)
private val TakeGreen = Color(0xFF2FB65A)
private val TakeGreenDark = Color(0xFF159447)

@Composable
fun AlarmScreen(doseEventId: Long, snoozeMinutes: Int = 5, onAction: (String) -> Unit) {
    // Block the system back gesture/button: the alarm can only be dismissed by
    // Taken, Later or Skip.
    androidx.activity.compose.BackHandler(enabled = true) { }
    val context = LocalContext.current
    var medicine by remember { mutableStateOf<Medicine?>(null) }
    var scheduledText by remember { mutableStateOf("") }
    var doseLine by remember { mutableStateOf("") }
    var instructionLine by remember { mutableStateOf("") }

    val prefs = remember {
        context.getSharedPreferences("medremind_settings", android.content.Context.MODE_PRIVATE)
    }
    val imageFraction = alarmImageFraction(prefs.getInt("alarm_image_size", 100))
    val greetingName = remember {
        prefs.getString("profile_name", "")?.trim()
            ?.split(Regex("\\s+"))
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
    }

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
                    instructionLine = IntakeInstruction.label(med?.intakeInstruction.orEmpty())
                }
            }
        }
    }

    val med = medicine
    val medicineTitle = listOfNotNull(med?.name, med?.strength?.takeIf { it.isNotBlank() })
        .joinToString(" ")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(BgDeep, BgMid, BgBase)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(Color(0x613F8BFF), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = size.maxDimension * 0.55f
                )
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(Color(0x57255BB4), Color.Transparent),
                    center = Offset(size.width, size.height * 0.7f),
                    radius = size.maxDimension * 0.55f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 18.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (greetingName != null) {
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = GreetingBg,
                    contentColor = GreetingFg
                ) {
                    Text(
                        "Hi, $greetingName",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 27.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Text(
                text = "TIME FOR\nYOUR MEDICINE",
                fontSize = 26.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.8).sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = CardCream,
                contentColor = CardFg,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Take ")
                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                append(medicineTitle.ifBlank { "your medicine" })
                            }
                            if (doseLine.isNotBlank()) {
                                append("  \u2022  ")
                                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                    append(doseLine)
                                }
                            }
                        },
                        fontSize = 21.sp,
                        lineHeight = 26.sp,
                        textAlign = TextAlign.Center
                    )
                    if (scheduledText.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = buildAnnotatedString {
                                append("Scheduled at ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(scheduledText)
                                }
                            },
                            fontSize = 17.sp,
                            color = ScheduleFg
                        )
                    }
                }
            }

            if (instructionLine.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = InstructionBg,
                    contentColor = InstructionFg,
                    shadowElevation = 5.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 25.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (med?.intakeInstruction == IntakeInstruction.EMPTY_STOMACH) {
                                Icons.Rounded.NoMeals
                            } else {
                                Icons.Rounded.Restaurant
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            instructionLine,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Medicine photo — the patient's main way to recognise the medicine.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                val photo = med?.photoPath
                val shape = RoundedCornerShape(25.dp)
                if (photo != null) {
                    AsyncImage(
                        model = File(photo),
                        contentDescription = med?.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(imageFraction)
                            .clip(shape)
                            .border(2.dp, Color.White.copy(alpha = 0.85f), shape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(imageFraction)
                            .clip(shape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(2.dp, Color.White.copy(alpha = 0.6f), shape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Medication,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }

            // Breathing room so the medicine photo and the action cards don't look
            // cramped together.
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                ActionButton(
                    label = "Take after\n$snoozeMinutes minutes",
                    container = LaterBg,
                    content = LaterFg,
                    icon = Icons.Rounded.Schedule,
                    modifier = Modifier.weight(1f)
                ) { onAction("SNOOZE") }

                SwipeUpToTake(
                    text = "Take now",
                    modifier = Modifier.weight(1.08f)
                ) { onAction("TAKEN") }

                ActionButton(
                    label = "Skip now",
                    container = SkipBg,
                    content = SkipFg,
                    icon = Icons.Rounded.Close,
                    modifier = Modifier.weight(1f)
                ) { onAction("SKIPPED") }
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    container: Color,
    content: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(23.dp),
        modifier = modifier.height(92.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(7.dp))
            Text(
                label,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SwipeUpToTake(
    text: String,
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit
) {
    val density = LocalDensity.current
    val threshold = with(density) { 85.dp.toPx() }
    val minOffset = with(density) { (-115).dp.toPx() }
    val bobAmplitude = with(density) { (-18).dp.toPx() }
    val dragOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var confirmed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }

    // The button gently rises and falls to hint that it should be swiped up —
    // no text needed.
    val transition = rememberInfiniteTransition(label = "takeHint")
    val bob = transition.animateFloat(
        initialValue = 0f,
        targetValue = bobAmplitude,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "takeBob"
    )

    val resting = !dragging && !confirmed

    Surface(
        color = if (confirmed) TakeGreenDark else TakeGreen,
        contentColor = Color.White,
        shape = RoundedCornerShape(23.dp),
        shadowElevation = 10.dp,
        modifier = modifier
            .height(92.dp)
            .offset {
                IntOffset(0, (if (resting) bob.value else dragOffset.value).roundToInt())
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        dragging = true
                        scope.launch { dragOffset.snapTo(bob.value) }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            dragOffset.snapTo(
                                (dragOffset.value + dragAmount).coerceIn(minOffset, 0f)
                            )
                        }
                    },
                    onDragEnd = {
                        dragging = false
                        if (-dragOffset.value >= threshold) {
                            confirmed = true
                            onConfirm()
                        } else {
                            scope.launch {
                                dragOffset.animateTo(
                                    0f,
                                    spring(
                                        dampingRatio = 0.5f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        dragging = false
                        scope.launch { dragOffset.animateTo(0f, spring()) }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color(0xFF299E4E),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                if (confirmed) "Taken" else text,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
