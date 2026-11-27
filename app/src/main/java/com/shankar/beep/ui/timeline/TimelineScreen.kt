package com.shankar.beep.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shankar.beep.data.LoggedEvent
import com.shankar.beep.ui.MainViewModel
import com.shankar.beep.ui.components.BeepCard
import com.shankar.beep.ui.components.SectionLabel
import com.shankar.beep.ui.components.SoundBadge
import com.shankar.beep.ui.components.StatBlock
import com.shankar.beep.ui.theme.Beep
import com.shankar.beep.ui.theme.categoryColor
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TimelineScreen(vm: MainViewModel) {
    val p = Beep.palette
    val events by vm.events.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L
    val today = events.count { sameDay(it.timestamp, now) }
    val week = events.filter { now - it.timestamp < 7 * dayMs }
    val top = week.groupingBy { it.title }.eachCount().maxByOrNull { it.value }
    val grouped = events.groupBy { dayLabel(it.timestamp, now) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Timeline", style = MaterialTheme.typography.headlineLarge, color = p.text, modifier = Modifier.weight(1f))
                if (events.isNotEmpty()) TextButton(onClick = { confirmClear = true }) { Text("Clear", color = p.muted) }
            }
            Text("Everything Beep alerted you about", style = MaterialTheme.typography.bodyMedium, color = p.muted)
            Spacer(Modifier.height(20.dp))
            BeepCard(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatBlock("$today", "today", Modifier.weight(1f))
                    StatBlock("${week.size}", "this week", Modifier.weight(1f))
                    StatBlock(top?.key ?: "—", "most heard", Modifier.weight(1.6f), accent = p.signal)
                }
                if (week.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    WeekBars(week, now)
                }
            }
        }
        if (events.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.History, null, tint = p.faint, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No alerts yet", style = MaterialTheme.typography.titleMedium, color = p.text)
                    Text("When Beep hears something, it shows up here.", style = MaterialTheme.typography.bodySmall, color = p.muted)
                }
            }
        }
        grouped.forEach { (label, list) ->
            item(key = "d-$label") {
                Spacer(Modifier.height(20.dp))
                SectionLabel(label)
            }
            items(list, key = { "${it.timestamp}-${it.soundId}" }) { e -> EventRow(e) }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear timeline?") },
            text = { Text("This removes all ${events.size} alerts from this phone. It can't be undone.") },
            confirmButton = { TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EventRow(e: LoggedEvent) {
    val p = Beep.palette
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(e.timestamp)),
            style = MaterialTheme.typography.labelMedium, color = p.muted,
            modifier = Modifier.width(64.dp),
        )
        SoundBadge(e.icon, e.category, 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(e.title, style = MaterialTheme.typography.titleSmall, color = p.text)
            Text(
                "${e.category.title} · ${(e.confidence * 100).roundToInt()}% sure · ${e.levelDb.roundToInt()} dB",
                style = MaterialTheme.typography.bodySmall, color = p.muted,
            )
        }
    }
}

/** Seven little bars — alerts per day this week, coloured by the day's most urgent category. */
@Composable
private fun WeekBars(week: List<LoggedEvent>, now: Long) {
    val p = Beep.palette
    val cal = Calendar.getInstance()
    val days = (6 downTo 0).map { back ->
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, -back)
        val dayStart = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val label = SimpleDateFormat("EEEEE", Locale.getDefault()).format(Date(dayStart))
        label to week.filter { it.timestamp >= dayStart && it.timestamp < dayStart + 24 * 3600_000L }
    }
    val max = days.maxOf { it.second.size }.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        days.forEach { (label, list) ->
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                val color = list.maxByOrNull { it.category.priority }?.let { categoryColor(it.category) } ?: p.cardHigh
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((56f * list.size / max).coerceAtLeast(4f).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = p.muted)
            }
        }
    }
}

private fun sameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

private fun dayLabel(ts: Long, now: Long): String = when {
    sameDay(ts, now) -> "Today"
    sameDay(ts, now - 24 * 3600_000L) -> "Yesterday"
    else -> SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(ts))
}
