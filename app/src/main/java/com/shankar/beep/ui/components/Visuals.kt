package com.shankar.beep.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.shankar.beep.ui.theme.Beep
import com.shankar.beep.ui.theme.PeopleViolet
import kotlinx.coroutines.flow.Flow

private val BeaconDeepDark = Color(0xFF9A3412)

enum class OrbState { OFF, STARTING, LISTENING, RESTING, PAUSED }

/**
 * The big button on the home screen. When Beep is listening, ripples breathe
 * outward and the core swells with the loudness of the room.
 */
@Composable
fun ListeningOrb(
    state: OrbState,
    levelDb: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 236.dp,
) {
    val p = Beep.palette
    val active = state == OrbState.LISTENING || state == OrbState.RESTING || state == OrbState.STARTING
    val level = ((levelDb - 30f) / 60f).coerceIn(0f, 1f)
    val swell by animateFloatAsState(if (active) level else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "swell")
    val glow by animateFloatAsState(if (active) 1f else 0f, tween(600), label = "glow")
    val ripple = rememberInfiniteTransition(label = "ripple")
    val phase by ripple.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (state == OrbState.RESTING) 4200 else 2400, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val signal = p.signal

    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onClick = onClick,
            )
            .semantics {
                contentDescription = "Listening"
                stateDescription = if (active) "On" else "Off"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            val c = center
            // Breathing ripples.
            if (glow > 0f) {
                for (i in 0 until 3) {
                    val t = (phase + i / 3f) % 1f
                    val radius = r * (0.52f + 0.48f * t)
                    drawCircle(
                        color = signal.copy(alpha = (1f - t) * 0.35f * glow),
                        radius = radius,
                        center = c,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
            // Outer track.
            drawCircle(p.line, radius = r * 0.62f, center = c, style = Stroke(1.dp.toPx()))
            // Core, swelling with the room's loudness.
            val coreR = r * (0.46f + 0.12f * swell)
            val coreColor = lerp(p.cardHigh, signal, glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(lerp(coreColor, Color.White, 0.18f * glow), coreColor, lerp(coreColor, PeopleViolet, 0.25f * glow)),
                    center = c + Offset(-coreR * 0.3f, -coreR * 0.35f),
                    radius = coreR * 1.6f,
                ),
                radius = coreR,
                center = c,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val (icon, label) = when (state) {
                OrbState.OFF -> Icons.Rounded.PowerSettingsNew to "Tap to listen"
                OrbState.STARTING -> Icons.Rounded.Hearing to "Starting…"
                OrbState.LISTENING -> Icons.Rounded.Hearing to "Listening"
                OrbState.RESTING -> Icons.Rounded.Hearing to "Quiet · resting"
                OrbState.PAUSED -> Icons.Rounded.Pause to "Paused"
            }
            val fg = if (active) p.onSignal else p.text
            Icon(icon, null, tint = fg, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
        }
    }
}

/**
 * A scrolling spectrogram ("waterfall"): time flows right-to-left, low
 * frequencies at the bottom, brightness = energy. Drawn into a tiny bitmap
 * and scaled up, so it costs almost nothing to render 20 times a second.
 */
@Composable
fun Spectrogram(bands: Flow<FloatArray>, modifier: Modifier = Modifier, columns: Int = 150, rows: Int = 48) {
    val p = Beep.palette
    val bitmap = remember(columns, rows) { Bitmap.createBitmap(columns, rows, Bitmap.Config.ARGB_8888) }
    val pixels = remember(columns, rows) { IntArray(columns * rows) }
    val ramp = remember(p) {
        val stops = if (p.isDark) {
            listOf(p.card, Color(0xFF2B1F4A), PeopleViolet, p.signal, Color(0xFFFFE3A3))
        } else {
            listOf(p.card, Color(0xFFE6DEFA), Color(0xFFB9A4F5), p.signal, BeaconDeepDark)
        }
        IntArray(256) { i ->
            val x = i / 255f * (stops.size - 1)
            val idx = x.toInt().coerceAtMost(stops.size - 2)
            lerp(stops[idx], stops[idx + 1], x - idx).toArgb()
        }
    }
    var frame by remember { mutableIntStateOf(0) }

    LaunchedEffect(bands, ramp) {
        pixels.fill(ramp[0])
        bands.collect { column ->
            for (r in 0 until rows) {
                val rowStart = r * columns
                System.arraycopy(pixels, rowStart + 1, pixels, rowStart, columns - 1)
                val band = column.getOrElse(rows - 1 - r) { 0f } // high frequencies on top
                pixels[rowStart + columns - 1] = ramp[(band * 255).toInt().coerceIn(0, 255)]
            }
            bitmap.setPixels(pixels, 0, columns, 0, 0, columns, rows)
            frame++
        }
    }

    Canvas(modifier) {
        if (frame < 0) return@Canvas // reading the state makes each new column redraw
        drawImage(
            bitmap.asImageBitmap(),
            dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()),
            filterQuality = FilterQuality.Low,
        )
    }
}
