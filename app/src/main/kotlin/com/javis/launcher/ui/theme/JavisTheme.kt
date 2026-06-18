package com.javis.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// JAVIS Color Palette — deep space dark with electric cyan accents
@Immutable
data class JavisColors(
    val background: Color = Color(0xFF050A14),
    val surface: Color = Color(0xFF0A1628),
    val surfaceVariant: Color = Color(0xFF0D1F3C),
    val primary: Color = Color(0xFF00B4FF),
    val primaryDim: Color = Color(0xFF0077AA),
    val secondary: Color = Color(0xFF7B61FF),
    val accent: Color = Color(0xFF00FFD1),
    val orbCore: Color = Color(0xFF00B4FF),
    val orbGlow: Color = Color(0x4400B4FF),
    val orbRing: Color = Color(0x8800B4FF),
    val onBackground: Color = Color(0xFFE8F4FD),
    val onSurface: Color = Color(0xFFB0C8DC),
    val onSurfaceDim: Color = Color(0xFF607080),
    val divider: Color = Color(0xFF1A2F4A),
    val error: Color = Color(0xFFFF4C6A),
    val success: Color = Color(0xFF00E676),
    val warning: Color = Color(0xFFFFC107),
    val glass: Color = Color(0x1A00B4FF),
    val glassBorder: Color = Color(0x3300B4FF),
    val cardBackground: Color = Color(0x0DFFFFFF),
)

@Immutable
data class JavisTypography(
    val displayLarge: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    val headlineLarge: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 32.sp,
        letterSpacing = 0.sp
    ),
    val headlineMedium: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    val titleLarge: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp
    ),
    val titleMedium: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp
    ),
    val labelLarge: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 1.25.sp
    ),
    val labelSmall: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp
    ),
    val mono: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.sp
    )
)

val LocalJavisColors = staticCompositionLocalOf { JavisColors() }
val LocalJavisTypography = staticCompositionLocalOf { JavisTypography() }

object JavisTheme {
    val colors: JavisColors
        @Composable get() = LocalJavisColors.current
    val typography: JavisTypography
        @Composable get() = LocalJavisTypography.current
}

@Composable
fun JavisTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalJavisColors provides JavisColors(),
        LocalJavisTypography provides JavisTypography()
    ) {
        content()
    }
}
