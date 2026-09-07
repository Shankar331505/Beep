package com.shankar.beep.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shankar.beep.ui.theme.*
import kotlin.math.sin

/**
 * Live audio waveform. Bars pulse with the ambient decibel level while listening;
 * a flat baseline is shown when paused.
 */
@Composable
fun WaveformVisualizer(
    decibel: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    maxHeight: Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_pulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_anim"
    )

    // Normalize decibel from ~25..90 to 0.12..1.0
    val normalizedEnergy = if (isListening) {
        ((decibel - 25f) / 65f).coerceIn(0.12f, 1.0f)
    } else {
        0.12f
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val barFactor = (sin(i * 0.45 + phase) * 0.4 + 0.6).toFloat()
            val centerWeight = (1.0f - (kotlin.math.abs(i - barCount / 2f) / (barCount / 2f)) * 0.5f)
            val barHeightFraction = (normalizedEnergy * barFactor * centerWeight).coerceIn(0.08f, 1.0f)

            val barHeight = maxHeight * barHeightFraction

            val barBrush = if (isListening) {
                if (decibel > 75f) {
                    Brush.verticalGradient(listOf(AccentEmergencyRed, AccentAmber))
                } else {
                    Brush.verticalGradient(listOf(MintPrimary, MintPressed))
                }
            } else {
                Brush.verticalGradient(listOf(CardBorderDark, CardDarkElevated))
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barBrush)
            )
        }
    }
}