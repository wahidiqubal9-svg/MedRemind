package com.medremind.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** Global switch driven by the user's "Haptic feedback" setting. */
object HapticPrefs {
    @Volatile
    var enabled: Boolean = true
}

/**
 * Thin wrapper over the platform haptics so the app can use consistent,
 * meaningful feedback instead of raw vibration.
 */
class MedHaptics(private val view: View) {
    private fun perform(constant: Int) {
        if (!HapticPrefs.enabled) return
        runCatching { view.performHapticFeedback(constant) }
    }

    /** Light tick for scrolling / small state changes. */
    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)

    /** Standard context tap for nav / chip selection. */
    fun tap() = perform(HapticFeedbackConstants.CONTEXT_CLICK)

    /** Confirmation for a completed positive action. */
    fun confirm() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.LONG_PRESS
    )

    /** Negative feedback, e.g. a rejected input. */
    fun reject() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
        else HapticFeedbackConstants.LONG_PRESS
    )

    /** Long press / destructive warning. */
    fun longPress() = perform(HapticFeedbackConstants.LONG_PRESS)
}

@Composable
fun rememberMedHaptics(): MedHaptics {
    val view = LocalView.current
    return remember(view) { MedHaptics(view) }
}
