package com.javis.launcher.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// JAVIS Red Theme (Default)
val JavisRed = Color(0xFFCF1020)
val JavisRedDark = Color(0xFF8B0000)
val JavisRedLight = Color(0xFFFF4444)
val JavisWhite = Color(0xFFF0F4FF)
val JavisGreen = Color(0xFF00FF88)
val JavisGreenDim = Color(0xFF00CC6A)
val JavisBlue = Color(0xFF0066FF)
val JavisGold = Color(0xFFFFD700)

val JavisBg = Color(0xFF050810)
val JavisBgCard = Color(0xFF0D1020)
val JavisBgElevated = Color(0xFF141828)
val JavisGlass = Color(0x1A4080FF)
val JavisGlassBorder = Color(0x33FFFFFF)
val JavisTextPrimary = Color(0xFFF0F4FF)
val JavisTextSecondary = Color(0xFFAABBCC)
val JavisTextDim = Color(0xFF556677)

// Energy glow colors
val GlowRed = Color(0x80CF1020)
val GlowGreen = Color(0x8000FF88)
val GlowBlue = Color(0x800066FF)
val GlowWhite = Color(0x40FFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = JavisRed,
    onPrimary = JavisWhite,
    primaryContainer = JavisRedDark,
    onPrimaryContainer = JavisWhite,
    secondary = JavisGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003322),
    onSecondaryContainer = JavisGreen,
    tertiary = JavisBlue,
    background = JavisBg,
    onBackground = JavisTextPrimary,
    surface = JavisBgCard,
    onSurface = JavisTextPrimary,
    surfaceVariant = JavisBgElevated,
    onSurfaceVariant = JavisTextSecondary,
    outline = JavisGlassBorder,
    error = Color(0xFFFF4444),
    onError = Color.White
)

@Composable
fun JavisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = JavisTypography,
        content = content
    )
}
