package com.shankar.beep.ui.alert

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shankar.beep.detect.Category
import com.shankar.beep.ui.components.SoundIcons
import com.shankar.beep.ui.components.timeAgo
import com.shankar.beep.ui.theme.categoryColor
import kotlin.math.roundToInt

/** What an alert page needs to show; built from a Detection or from intent extras. */
data class AlertInfo(
    val soundId: String,
    val title: String,
    val description: String,
    val category: Category,
    val icon: String,
    val confidence: Float,
    val levelDb: Float,
    val timestamp: Long,
)

/**
 * The full-screen alert: the whole screen pulses in the sound's colour so it
 * can be noticed from the corner of your eye — designed with Deaf and
 * hard-of-hearing users in mind.
 */
@Composable
fun AlertContent(info: AlertInfo, onDismiss: () -> Unit, onSnooze: () -> Unit) {
    val color = categoryColor(info.category)
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(if (info.category == Category.SAFETY) 450 else 900), RepeatMode.Reverse), label = "glow",
    )
    val bg = Color(0xFF0B0C0F)
    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            .background(Brush.radialGradient(listOf(color.copy(alpha = 0.55f * glow), bg), radius = 1400f)),
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                if (info.category == Category.SAFETY) "LOOK AROUND NOW" else "LOOK AROUND",
                style = MaterialTheme.typography.labelSmall, color = color,
            )
            Spacer(Modifier.height(28.dp))
            Box(
                Modifier.size(168.dp).scale(0.94f + 0.06f * glow).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(SoundIcons.of(info.icon), null, tint = bg, modifier = Modifier.size(84.dp))
            }
            Spacer(Modifier.height(32.dp))
            Text(
                info.title,
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Heard ${timeAgo(info.timestamp)} · ${(info.confidence * 100).roundToInt()}% sure · ${info.levelDb.roundToInt()} dB",
                style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(56.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = bg),
            ) { Text("Got it", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Mute ${info.title.lowercase()} for 10 min", color = Color.White.copy(alpha = 0.85f)) }
        }
    }
}
