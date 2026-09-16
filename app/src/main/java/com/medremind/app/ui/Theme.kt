package com.medremind.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LightColors = lightColorScheme(
    primary = Color(0xFF10695E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB6E4DA),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF4A6360),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E3),
    onSecondaryContainer = Color(0xFF051F1C),
    tertiary = Color(0xFF8C6A2F),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6FBF9),
    onBackground = Color(0xFF161D1C),
    surface = Color(0xFFF6FBF9),
    onSurface = Color(0xFF161D1C),
    surfaceVariant = Color(0xFFDAE5E1),
    onSurfaceVariant = Color(0xFF3F4946),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF6F7976)
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD1C6),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFFB6E4DA),
    secondary = Color(0xFFB1CCC7),
    onSecondary = Color(0xFF1C3532),
    secondaryContainer = Color(0xFF324B48),
    onSecondaryContainer = Color(0xFFCCE8E3),
    tertiary = Color(0xFFE6C486),
    onTertiary = Color(0xFF402D02),
    background = Color(0xFF0E1514),
    onBackground = Color(0xFFDDE4E2),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFDDE4E2),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBEC9C5),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF899390)
)

val HighContrastLight = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF000000),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF000000),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF000000),
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF000000)
)

private val MedShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MedRemindTheme(
    highContrast: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = when {
        highContrast -> HighContrastLight
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = MedShapes,
        typography = Typography(),
        content = content
    )
}
