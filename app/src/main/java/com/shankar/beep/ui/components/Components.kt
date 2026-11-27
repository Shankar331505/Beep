package com.shankar.beep.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.shankar.beep.detect.Category
import com.shankar.beep.ui.theme.Beep
import com.shankar.beep.ui.theme.categoryColor
import java.util.concurrent.TimeUnit

@Composable
fun BeepCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = Beep.palette
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(p.card)
            .border(BorderStroke(1.dp, p.line), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Beep.palette.muted,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** A sound's icon on a softly tinted disc in its category colour. */
@Composable
fun SoundBadge(icon: String, category: Category, size: Dp = 44.dp, dimmed: Boolean = false) {
    val color = categoryColor(category)
    val tint by animateColorAsState(if (dimmed) Beep.palette.faint else color, label = "badgeTint")
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = if (Beep.palette.isDark) 0.16f else 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(SoundIcons.of(icon), contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun Pill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    color: Color = Beep.palette.signal,
) {
    val p = Beep.palette
    val bg by animateColorAsState(if (selected) color else p.card, label = "pillBg")
    val fg by animateColorAsState(if (selected) p.onSignal else p.text, label = "pillFg")
    Row(
        Modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) color else p.line, CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/** A horizontal meter with an optional threshold tick — used for live scores. */
@Composable
fun Meter(value: Float, color: Color, modifier: Modifier = Modifier, threshold: Float? = null, height: Dp = 8.dp) {
    val p = Beep.palette
    val animated by animateFloatAsState(value.coerceIn(0f, 1f), tween(220), label = "meter")
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(p.cardHigh)) {
        Box(Modifier.fillMaxWidth(animated).height(height).clip(CircleShape).background(color))
        if (threshold != null) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.fillMaxWidth(threshold.coerceIn(0.01f, 0.99f)))
                Box(Modifier.width(2.dp).height(height).background(p.text.copy(alpha = 0.7f)))
            }
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val p = Beep.palette
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (enabled) p.text else p.faint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (enabled) p.text else p.faint)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = p.signal,
                checkedThumbColor = p.onSignal,
                uncheckedTrackColor = p.cardHigh,
                uncheckedBorderColor = p.line,
                uncheckedThumbColor = p.muted,
            ),
        )
    }
}

@Composable
fun StatBlock(value: String, label: String, modifier: Modifier = Modifier, accent: Color = Beep.palette.text) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Beep.palette.muted)
    }
}

/** Plain-language description of a sound level. */
fun describeLevel(db: Float): String = when {
    db < 30 -> "Silent"
    db < 42 -> "Very quiet"
    db < 55 -> "Quiet room"
    db < 68 -> "Conversation"
    db < 80 -> "Busy"
    db < 92 -> "Loud"
    else -> "Very loud"
}

fun timeAgo(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val diff = (now - timestamp).coerceAtLeast(0)
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} min ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} h ago"
        else -> "${TimeUnit.MILLISECONDS.toDays(diff)} d ago"
    }
}
