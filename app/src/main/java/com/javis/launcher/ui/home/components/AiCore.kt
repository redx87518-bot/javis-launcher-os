package com.javis.launcher.ui.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.javis.launcher.data.model.CoreState
import com.javis.launcher.ui.theme.*
import kotlin.math.*

@Composable
fun AiCore(
    state: CoreState = CoreState.IDLE,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "core")

    val outerRingAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "outer"
    )
    val innerRingAngle by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "inner"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = when (state) {
                CoreState.LISTENING -> 400
                CoreState.THINKING -> 600
                CoreState.SPEAKING -> 300
                else -> 1500
            }, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = when (state) {
            CoreState.IDLE -> 0.7f
            CoreState.LISTENING -> 1.0f
            CoreState.THINKING -> 0.9f
            CoreState.SPEAKING -> 1.0f
            else -> 0.85f
        },
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val particleAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "particles"
    )

    val coreColor = when (state) {
        CoreState.IDLE -> JavisRed
        CoreState.LISTENING -> JavisGreen
        CoreState.THINKING -> JavisBlue
        CoreState.SPEAKING -> JavisWhite
        CoreState.EXECUTING -> JavisGold
        CoreState.COMPLETED -> JavisGreenDim
        CoreState.ERROR -> Color(0xFFFF4444)
    }
    val glowColor = when (state) {
        CoreState.IDLE -> GlowRed
        CoreState.LISTENING -> GlowGreen
        CoreState.THINKING -> GlowBlue
        CoreState.SPEAKING -> GlowWhite
        CoreState.EXECUTING -> Color(0x80FFD700)
        else -> GlowRed
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(220.dp)
            .scale(pulseScale)
            .clickable { onTap() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val maxR = size.minDimension / 2

            // Ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(glowColor.copy(alpha = glowAlpha * 0.5f), Color.Transparent),
                    center = Offset(cx, cy), radius = maxR
                )
            )

            // Outer energy arc
            drawArc(
                color = coreColor.copy(alpha = 0.8f), startAngle = outerRingAngle, sweepAngle = 240f,
                useCenter = false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(cx - maxR * 0.9f, cy - maxR * 0.9f),
                size = androidx.compose.ui.geometry.Size(maxR * 1.8f, maxR * 1.8f)
            )
            drawArc(
                color = coreColor.copy(alpha = 0.3f), startAngle = outerRingAngle + 240f, sweepAngle = 120f,
                useCenter = false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(cx - maxR * 0.9f, cy - maxR * 0.9f),
                size = androidx.compose.ui.geometry.Size(maxR * 1.8f, maxR * 1.8f)
            )

            // Mid ring
            drawArc(
                color = coreColor.copy(alpha = 0.6f), startAngle = innerRingAngle, sweepAngle = 180f,
                useCenter = false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(cx - maxR * 0.7f, cy - maxR * 0.7f),
                size = androidx.compose.ui.geometry.Size(maxR * 1.4f, maxR * 1.4f)
            )

            // Hex nodes
            for (i in 0 until 6) {
                val angle = Math.toRadians((outerRingAngle / 3 + i * 60).toDouble())
                val r = maxR * 0.72f
                drawCircle(
                    color = coreColor.copy(alpha = if (i % 2 == 0) 0.9f else 0.4f),
                    radius = 4.dp.toPx(),
                    center = Offset(cx + r * cos(angle).toFloat(), cy + r * sin(angle).toFloat())
                )
            }

            // Green status dots
            for (i in 0 until 3) {
                val angle = Math.toRadians(120.0 * i + 90)
                val r = maxR * 0.82f
                val pos = Offset(cx + r * cos(angle).toFloat(), cy + r * sin(angle).toFloat())
                drawCircle(color = JavisGreen.copy(alpha = glowAlpha), radius = 5.dp.toPx(), center = pos)
                drawCircle(color = JavisGreen.copy(alpha = 0.3f), radius = 10.dp.toPx(), center = pos)
            }

            // Inner core
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(coreColor.copy(0.9f), coreColor.copy(0.4f), Color.Transparent),
                    center = Offset(cx, cy), radius = maxR * 0.5f
                )
            )

            // White center
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(0.95f), JavisWhite.copy(0.7f), coreColor.copy(0.3f), Color.Transparent),
                    center = Offset(cx, cy), radius = maxR * 0.28f
                )
            )

            // Particles when active
            if (state != CoreState.IDLE) {
                for (i in 0 until 8) {
                    val pAngle = Math.toRadians((particleAngle + i * 45.0))
                    val r = maxR * (0.55f + sin(Math.toRadians(particleAngle.toDouble() * 3 + i * 40)) * 0.15f).toFloat()
                    drawCircle(
                        color = coreColor.copy(0.6f), radius = 2.5.dp.toPx(),
                        center = Offset(cx + r * cos(pAngle).toFloat(), cy + r * sin(pAngle).toFloat())
                    )
                }
            }

            // Listening waves
            if (state == CoreState.LISTENING) {
                for (i in 1..3) {
                    drawCircle(
                        color = JavisGreen.copy(alpha = (0.4f - i * 0.1f) * glowAlpha),
                        radius = maxR * (0.35f + i * 0.18f),
                        center = Offset(cx, cy),
                        style = Stroke(1.5.dp.toPx())
                    )
                }
            }

            // Thinking arcs
            if (state == CoreState.THINKING) {
                for (i in 0 until 4) {
                    drawArc(
                        color = JavisBlue.copy(0.5f),
                        startAngle = innerRingAngle * 2 + i * 90f, sweepAngle = 30f,
                        useCenter = false, style = Stroke(1.5.dp.toPx()),
                        topLeft = Offset(cx - maxR * 0.55f, cy - maxR * 0.55f),
                        size = androidx.compose.ui.geometry.Size(maxR * 1.1f, maxR * 1.1f)
                    )
                }
            }
        }
    }
}

@Composable
fun CoreStateLabel(state: CoreState, modifier: Modifier = Modifier) {
    val text = when (state) {
        CoreState.IDLE -> "TAP TO SPEAK"
        CoreState.LISTENING -> "LISTENING..."
        CoreState.THINKING -> "PROCESSING..."
        CoreState.SPEAKING -> "SPEAKING..."
        CoreState.EXECUTING -> "EXECUTING..."
        CoreState.COMPLETED -> "COMPLETED"
        CoreState.ERROR -> "ERROR"
    }
    val color = when (state) {
        CoreState.IDLE -> JavisTextDim
        CoreState.LISTENING -> JavisGreen
        CoreState.THINKING -> JavisBlue
        CoreState.SPEAKING -> JavisWhite
        CoreState.EXECUTING -> JavisGold
        CoreState.COMPLETED -> JavisGreenDim
        CoreState.ERROR -> Color(0xFFFF4444)
    }
    androidx.compose.material3.Text(
        text = text,
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
            color = color,
            letterSpacing = 2.sp
        ),
        modifier = modifier
    )
}
