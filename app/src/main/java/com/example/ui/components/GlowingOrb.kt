package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.service.VoiceState
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NeonPink
import com.example.ui.theme.VioletPrimary

@Composable
fun GlowingOrb(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    state: VoiceState = VoiceState.IDLE,
    audioRms: Float = 0f
) {
    val transition = rememberInfiniteTransition(label = "orbTransition")

    // Rotation angle
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Reverse Rotation
    val reverseRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverseRotation"
    )

    // Pulse scale
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == VoiceState.LISTENING) 700 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rmsBoost = (audioRms * 0.04f).coerceIn(0f, 0.35f)
    val effectiveScale = pulse + rmsBoost

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = (size.toPx() / 2.6f) * effectiveScale

            // Colors based on state
            val (coreColor, ring1Color, ring2Color) = when (state) {
                VoiceState.LISTENING -> Triple(CyanAccent, VioletPrimary, NeonPink)
                VoiceState.PROCESSING -> Triple(VioletPrimary, NeonPink, CyanAccent)
                VoiceState.SPEAKING -> Triple(NeonPink, CyanAccent, VioletPrimary)
                VoiceState.IDLE -> Triple(VioletPrimary, CyanAccent, NeonPink)
            }

            // Outer diffused ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.35f),
                        ring1Color.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.5f
                ),
                radius = baseRadius * 1.5f,
                center = center
            )

            // Inner glowing core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        coreColor.copy(alpha = 0.85f),
                        ring2Color.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.85f
                ),
                radius = baseRadius * 0.85f,
                center = center
            )

            // Orbiting orbital ring 1
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        ring1Color,
                        Color.Transparent,
                        ring1Color.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    center = center
                ),
                radius = baseRadius * 1.05f,
                center = center,
                style = Stroke(width = 3.5f)
            )

            // Orbiting orbital ring 2
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        ring2Color.copy(alpha = 0.9f),
                        Color.Transparent,
                        coreColor
                    ),
                    center = center
                ),
                radius = baseRadius * 1.25f,
                center = center,
                style = Stroke(width = 2.5f)
            )
        }
    }
}
