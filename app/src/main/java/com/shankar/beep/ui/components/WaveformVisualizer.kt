package com.shankar.beep.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shankar.beep.ui.theme.Brass
import com.shankar.beep.ui.theme.Hairline
import com.shankar.beep.ui.theme.Oxblood
import com.shankar.beep.ui.theme.Stone
import kotlin.math.abs
import kotlin.math.sin

/**
 * Thin instrument bars. Energy follows ambient dB while listening.
 */
@Composable
fun WaveformVisualizer(
    decibel: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 36,
    maxHeight: Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_pulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_anim"
    )

    val normalizedEnergy = if (isListening) {
        ((decibel - 25f) / 65f).coerceIn(0.12f, 1.0f)
    } else {
        0.10f
    }

    val activeColor: Color = when {
        !isListening -> Hairline
        decibel > 75f -> Oxblood
        else -> Brass
    }
    val idleColor = Stone.copy(alpha = 0.45f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val barFactor = (sin(i * 0.38 + phase) * 0.35 + 0.65).toFloat()
            val centerWeight = 1.0f - (abs(i - barCount / 2f) / (barCount / 2f)) * 0.45f
            val barHeightFraction = (normalizedEnergy * barFactor * centerWeight).coerceIn(0.08f, 1.0f)
            val barHeight = maxHeight * barHeightFraction

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isListening) activeColor else idleColor)
            )
        }
    }
}
