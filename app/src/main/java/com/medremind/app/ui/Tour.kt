package com.medremind.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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

/**
 * One stop of the guided tour.
 *
 * [anchor] is the id of a real UI element to spotlight (see [Modifier.tourAnchor]).
 * When [tapToAdvance] is true, the tour expects the user to tap that element: the
 * tap passes through to the real control (so the feature actually opens) and the
 * tour then moves on. Steps with no anchor are read-only and advance with "Next".
 */
data class TourStep(
    val anchor: String?,
    val title: String,
    val body: String,
    val tapToAdvance: Boolean = anchor != null
)

/**
 * An interactive walkthrough: each step highlights a real button and asks the user
 * to tap it. Tapping opens that feature for real, then the tour continues.
 */
val defaultTourSteps = listOf(
    TourStep(
        anchor = null,
        title = "Welcome to MedRemind",
        body = "Let's take a quick look around. At each step, tap the highlighted " +
            "button to open that feature \u2014 it's the real thing.",
        tapToAdvance = false
    ),
    TourStep(
        anchor = "nav_med",
        title = "Medicines",
        body = "Tap the Medicines button below to open your cabinet."
    ),
    TourStep(
        anchor = "fab_add",
        title = "Add a medicine",
        body = "Tap the \u201cAdd medicine\u201d button to add one \u2014 with a photo, " +
            "a schedule and stock details."
    ),
    TourStep(
        anchor = "nav_progress",
        title = "Progress",
        body = "Tap Progress to see your adherence over time and export a report."
    ),
    TourStep(
        anchor = "nav_health",
        title = "Health",
        body = "Tap Health to log blood pressure, glucose and weight."
    ),
    TourStep(
        anchor = "nav_today",
        title = "Today",
        body = "Tap Today for your day's doses with their times."
    ),
    TourStep(
        anchor = "people",
        title = "People you care for",
        body = "Tap the arrow beside your name to add or open the people you help."
    ),
    TourStep(
        anchor = "bell",
        title = "Notifications",
        body = "Tap the bell for reminders, missed doses and caregiver activity."
    ),
    TourStep(
        anchor = null,
        title = "You're all set",
        body = "You can replay this tour anytime from Settings \u2192 Help.",
        tapToAdvance = false
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
    val step = controller.current
    val current = step.anchor == id && step.tapToAdvance
    return this
        .onGloballyPositioned { controller.anchors[id] = it.boundsInRoot() }
        .then(
            if (current) {
                // The user tapped the highlighted control: advance the tour.
                // We never consume, so the control's own click still fires and
                // the feature really opens.
                Modifier.pointerInput(controller.index) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        controller.next()
                    }
                }
            } else {
                Modifier
            }
        )
}

@Composable
fun TourOverlay(
    controller: TourController,
    onFinish: () -> Unit
) {
    val step = controller.current
    val bounds = step.anchor?.let { controller.anchors[it] }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenHeight = with(density) { maxHeight.toPx() }
        val pad = with(density) { 8.dp.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .pointerInput(controller.index, bounds) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val onTarget = step.tapToAdvance &&
                            bounds?.contains(down.position) == true
                        if (!onTarget) {
                            // Swallow taps that miss the highlighted control so the
                            // user interacts only with the spotlighted feature.
                            down.consume()
                            var event = awaitPointerEvent()
                            while (event.changes.any { it.pressed }) {
                                event.changes.forEach { it.consume() }
                                event = awaitPointerEvent()
                            }
                        }
                        // On the target: do nothing and don't consume, so the real
                        // control receives the tap (its tourAnchor hook advances us).
                    }
                }
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
                if (step.tapToAdvance && bounds != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            "\uD83D\uDC46 Tap the highlighted button to continue",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
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
