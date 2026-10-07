package com.medremind.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** One stop of the guided tour: which element to spotlight and what to say. */
data class TourStep(
    val anchor: String?,
    val title: String,
    val body: String,
    val tab: Int
)

val defaultTourSteps = listOf(
    TourStep(
        anchor = null,
        title = "Welcome to MedRemind",
        body = "Let's take a quick look around the important parts. It only takes a few seconds.",
        tab = 0
    ),
    TourStep(
        anchor = "nav_today",
        title = "Today",
        body = "Every dose for the day shows here with its time. Slide to take, snooze or skip.",
        tab = 0
    ),
    TourStep(
        anchor = "fab_add",
        title = "Add a medicine",
        body = "Tap here to add a medicine \u2014 with a photo, a schedule and stock details.",
        tab = 1
    ),
    TourStep(
        anchor = "nav_progress",
        title = "Progress",
        body = "See your adherence over time and export a report for your doctor.",
        tab = 2
    ),
    TourStep(
        anchor = "nav_health",
        title = "Health",
        body = "Log blood pressure, glucose and weight, and watch the trends.",
        tab = 3
    ),
    TourStep(
        anchor = "people",
        title = "People you care for",
        body = "Tap the arrow beside your name to add or open the people you help.",
        tab = 0
    ),
    TourStep(
        anchor = "bell",
        title = "Notifications",
        body = "Reminders, missed doses and caregiver activity show up here.",
        tab = 0
    )
)

class TourController(val steps: List<TourStep> = defaultTourSteps) {
    var index by mutableIntStateOf(0)
        private set

    /** Element bounds (in root coordinates), filled in by [tourAnchor]. */
    val anchors = mutableStateMapOf<String, Rect>()

    val current: TourStep get() = steps[index.coerceIn(0, steps.lastIndex)]

    fun next() {
        if (index < steps.lastIndex) index++
    }
}

val LocalTour = compositionLocalOf<TourController?> { null }

/** Marks a UI element so the tour can spotlight it. No-op when no tour is active. */
@Composable
fun Modifier.tourAnchor(id: String): Modifier {
    val controller = LocalTour.current ?: return this
    return this.onGloballyPositioned { controller.anchors[id] = it.boundsInRoot() }
}

@Composable
fun TourOverlay(
    controller: TourController,
    onTabChange: (Int) -> Unit,
    onFinish: () -> Unit
) {
    val step = controller.current
    LaunchedEffect(controller.index) { onTabChange(step.tab) }
    val bounds = step.anchor?.let { controller.anchors[it] }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { /* block interaction */ } }
    ) {
        val density = LocalDensity.current
        val screenHeight = with(density) { maxHeight.toPx() }
        val pad = with(density) { 8.dp.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color.Black.copy(alpha = 0.75f))
            if (bounds != null) {
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(bounds.left - pad, bounds.top - pad),
                    size = Size(bounds.width + pad * 2, bounds.height + pad * 2),
                    cornerRadius = CornerRadius(24f, 24f),
                    blendMode = BlendMode.Clear
                )
            }
        }

        if (bounds != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(bounds.left - pad, bounds.top - pad),
                    size = Size(bounds.width + pad * 2, bounds.height + pad * 2),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 3f)
                )
            }
        }

        val targetLow = bounds != null && bounds.center.y > screenHeight * 0.5f
        val alignment = when {
            bounds == null -> Alignment.Center
            targetLow -> Alignment.TopCenter
            else -> Alignment.BottomCenter
        }

        Surface(
            modifier = Modifier
                .align(alignment)
                .padding(horizontal = 20.dp)
                .padding(top = 92.dp, bottom = 116.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 14.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    step.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${controller.index + 1} of ${controller.steps.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onFinish) { Text("Skip") }
                    GradientPillButton(
                        text = if (controller.index == controller.steps.lastIndex) "Finish" else "Next",
                        onClick = {
                            if (controller.index == controller.steps.lastIndex) onFinish()
                            else controller.next()
                        }
                    )
                }
            }
        }
    }
}
