package com.medremind.app.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Single source of truth for motion. Screens and components should pull durations
 * and easing from here instead of hard-coding tweens, so the whole app feels like
 * one product.
 */
object MedMotion {
    const val Instant = 90
    const val Fast = 160
    const val Medium = 260
    const val Slow = 420

    /** Default easing for entering content. */
    val Standard: Easing = FastOutSlowInEasing

    /** Emphasized easing for hero / expressive transitions. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Decelerate: things coming to rest. */
    val Decelerate: Easing = LinearOutSlowInEasing

    /** Accelerate: things leaving the screen. */
    val Accelerate: Easing = FastOutLinearInEasing
}

fun <T> motionTween(
    durationMillis: Int = MedMotion.Medium,
    delayMillis: Int = 0,
    easing: Easing = MedMotion.Standard
): TweenSpec<T> = tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = easing)

/** True when the user (or the system) has asked for reduced motion. */
val LocalReduceMotion = compositionLocalOf { false }

private fun systemAnimationsDisabled(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    ) == 0f
}.getOrDefault(false)

fun resolveReduceMotion(context: Context, userPref: Boolean): Boolean =
    userPref || systemAnimationsDisabled(context)
