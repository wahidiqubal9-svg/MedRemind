package com.medremind.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medremind.app.R

val MedFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold)
)

val LightColors = lightColorScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF3730A3),
    inversePrimary = Color(0xFFA5B4FC),
    secondary = Color(0xFF667085),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2F4F7),
    onSecondaryContainer = Color(0xFF344054),
    tertiary = Color(0xFFA855F7),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E8FF),
    onTertiaryContainer = Color(0xFF6B21A8),
    error = Color(0xFFF43F5E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDECEF),
    onErrorContainer = Color(0xFFD53862),
    background = Color(0xFFF4F5FB),
    onBackground = Color(0xFF101828),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFEDEFF4),
    onSurfaceVariant = Color(0xFF667085),
    surfaceTint = Color(0xFF6366F1),
    inverseSurface = Color(0xFF101828),
    inverseOnSurface = Color(0xFFF4F5FB),
    outline = Color(0xFFD7DBE6),
    outlineVariant = Color(0xFFEDEFF4),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE8EAF0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF2F4F7),
    surfaceContainerHighest = Color(0xFFE8EAF0)
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF4338CA),
    onPrimaryContainer = Color(0xFFE0E7FF),
    inversePrimary = Color(0xFF6366F1),
    secondary = Color(0xFFC2C6D0),
    onSecondary = Color(0xFF2B2F36),
    secondaryContainer = Color(0xFF41454D),
    onSecondaryContainer = Color(0xFFE2E5EC),
    tertiary = Color(0xFFD8B4FE),
    onTertiary = Color(0xFF3B0764),
    tertiaryContainer = Color(0xFF7E22CE),
    onTertiaryContainer = Color(0xFFF3E8FF),
    error = Color(0xFFFDA4AF),
    onError = Color(0xFF4C0519),
    errorContainer = Color(0xFF9F1239),
    onErrorContainer = Color(0xFFFFE4E6),
    background = Color(0xFF0B0C10),
    onBackground = Color(0xFFE4E7EC),
    surface = Color(0xFF14161C),
    onSurface = Color(0xFFE4E7EC),
    surfaceVariant = Color(0xFF2A2F3A),
    onSurfaceVariant = Color(0xFFB9C0CC),
    surfaceTint = Color(0xFFA5B4FC),
    inverseSurface = Color(0xFFE4E7EC),
    inverseOnSurface = Color(0xFF2B2F36),
    outline = Color(0xFF4A5160),
    outlineVariant = Color(0xFF2A2F3A),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2A2F3A),
    surfaceDim = Color(0xFF0B0C10),
    surfaceContainerLowest = Color(0xFF0E1015),
    surfaceContainerLow = Color(0xFF14161C),
    surfaceContainer = Color(0xFF181B22),
    surfaceContainerHigh = Color(0xFF22262F),
    surfaceContainerHighest = Color(0xFF2C313B)
)

val HighContrastLight = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF000000),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF000000),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF000000),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFF000000),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF000000),
    onTertiaryContainer = Color(0xFFFFFFFF),
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFB00020),
    onErrorContainer = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF000000),
    surfaceTint = Color(0xFF000000),
    inverseSurface = Color(0xFF000000),
    inverseOnSurface = Color(0xFFFFFFFF),
    outline = Color(0xFF000000),
    outlineVariant = Color(0xFF000000),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE0E0E0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F5F5),
    surfaceContainer = Color(0xFFEDEDED),
    surfaceContainerHigh = Color(0xFFE0E0E0),
    surfaceContainerHighest = Color(0xFFD6D6D6)
)

private val MedShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val MedTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = MedFontFamily,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

object MedGradients {
    @Composable
    fun hero(): Brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

    @Composable
    fun heroHorizontal(): Brush = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

    @Composable
    fun scrim(): Brush = Brush.verticalGradient(
        colors = listOf(
            Color.Black.copy(alpha = 0.85f),
            Color.Black.copy(alpha = 0.35f),
            Color.Black.copy(alpha = 0.85f)
        )
    )
}

@Composable
fun MedRemindTheme(
    highContrast: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors = when {
        highContrast -> HighContrastLight
        dynamic && darkTheme -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = MedShapes,
        typography = MedTypography,
        content = content
    )
}
