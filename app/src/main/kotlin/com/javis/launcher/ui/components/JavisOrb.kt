package com.javis.launcher.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.javis.launcher.ui.theme.JavisTheme
import com.javis.launcher.voice.VoiceState
import kotlin.math.*

@Composable
fun JavisOrb(
    voiceState: VoiceState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    // Idle breathing pulse
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Ring rotation
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (voiceState) {
                    VoiceState.THINKING -> 800
                    VoiceState.LISTENING -> 1200
                    else -> 4000
                },
                easing = LinearEasing
            )
        ),
        label = "rotation"
    )

    // Ring counter-rotation
    val ringRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "rotation2"
    )

    // Glow intensity
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = when (voiceState) {
            VoiceState.IDLE -> 0.6f
            VoiceState.LISTENING -> 1.0f
            VoiceState.THINKING -> 0.9f
            VoiceState.SPEAKING -> 0.85f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Audio wave animation for listening/speaking
    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing)
        ),
        label = "wave"
    )

    val coreBlue = Color(0xFF00B4FF)
    val glowBlue = Color(0x8800B4FF)
    val accentPurple = Color(0xFF7B61FF)
    val white20 = Color(0x33FFFFFF)

    val scale = when (voiceState) {
        VoiceState.IDLE -> breathScale
        VoiceState.LISTENING -> 1.1f
        VoiceState.THINKING -> 1.05f
        VoiceState.SPEAKING -> breathScale * 1.05f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension / 2) * scale

            // Outer ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowBlue.copy(alpha = glowAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.8f
                ),
                radius = radius * 1.8f,
                center = center
            )

            // Outer ring 1 (rotating)
            rotate(ringRotation, center) {
                drawOrbRing(center, radius * 1.3f, coreBlue.copy(alpha = 0.3f), 1.5f, 5, 0.7f)
            }

            // Outer ring 2 (counter-rotating)
            rotate(ringRotation2, center) {
                drawOrbRing(center, radius * 1.15f, accentPurple.copy(alpha = 0.2f), 1f, 3, 0.5f)
            }

            // Mid glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreBlue.copy(alpha = glowAlpha * 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.1f
                ),
                radius = radius * 1.1f,
                center = center
            )

            // Glass shell
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        white20,
                        Color.Transparent
                    ),
                    center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Border ring
            drawCircle(
                color = coreBlue.copy(alpha = 0.6f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        coreBlue.copy(alpha = 0.8f),
                        coreBlue.copy(alpha = 0.4f)
                    ),
                    center = center,
                    radius = radius * 0.5f
                ),
                radius = radius * 0.5f,
                center = center
            )

            // Voice state: audio waveform
            if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
                drawAudioWave(center, radius, waveAnim, coreBlue.copy(alpha = 0.7f))
            }
        }
    }
}

private fun DrawScope.drawOrbRing(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float,
    dashCount: Int,
    dashFraction: Float
) {
    val circumference = 2 * PI.toFloat() * radius
    val dashLength = circumference / dashCount * dashFraction
    val gapLength = circumference / dashCount * (1 - dashFraction)
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, gapLength), 0f)
        )
    )
}

private fun DrawScope.drawAudioWave(
    center: Offset,
    radius: Float,
    wavePhase: Float,
    color: Color
) {
    val bars = 12
    val barRadius = radius * 1.05f
    for (i in 0 until bars) {
        val angle = (i * 2 * PI / bars).toFloat()
        val wave = sin(wavePhase + angle * 2).absoluteValue * 0.15f + 0.05f
        val innerR = barRadius
        val outerR = barRadius + radius * wave
        val start = Offset(
            center.x + innerR * cos(angle),
            center.y + innerR * sin(angle)
        )
        val end = Offset(
            center.x + outerR * cos(angle),
            center.y + outerR * sin(angle)
        )
        drawLine(color, start, end, strokeWidth = 3f, cap = StrokeCap.Round)
    }
}
