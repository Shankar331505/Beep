package com.shankar.beep.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shankar.beep.data.LoggedEvent
import com.shankar.beep.detect.Mode
import com.shankar.beep.service.ListenStatus
import com.shankar.beep.ui.MainViewModel
import com.shankar.beep.ui.components.BeepCard
import com.shankar.beep.ui.components.ListeningOrb
import com.shankar.beep.ui.components.Meter
import com.shankar.beep.ui.components.OrbState
import com.shankar.beep.ui.components.Pill
import com.shankar.beep.ui.components.SectionLabel
import com.shankar.beep.ui.components.SoundBadge
import com.shankar.beep.ui.components.SoundIcons
import com.shankar.beep.ui.components.Spectrogram
import com.shankar.beep.ui.components.describeLevel
import com.shankar.beep.ui.components.timeAgo
import com.shankar.beep.ui.theme.Beep
import com.shankar.beep.ui.theme.categoryColor
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onToggleListening: () -> Unit,
    onOpenSounds: () -> Unit,
    onOpenTimeline: () -> Unit,
) {
    val p = Beep.palette
    val settings by vm.settings.collectAsStateWithLifecycle()
    val live by vm.live.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val custom by vm.customSounds.collectAsStateWithLifecycle()
    val s = settings ?: return

    val status = live.status
    val orbState = when (status) {
        ListenStatus.Idle, is ListenStatus.Error -> OrbState.OFF
        ListenStatus.Starting -> OrbState.STARTING
        is ListenStatus.Paused -> OrbState.PAUSED
        ListenStatus.Listening -> if (live.resting) OrbState.RESTING else OrbState.LISTENING
    }
    val active = orbState != OrbState.OFF && orbState != OrbState.PAUSED

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Beep", style = MaterialTheme.typography.headlineLarge, color = p.text)
                Text(
                    greeting(s.userName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.muted,
                )
            }
            Uptime(live.startedAt, active)
        }

        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ListeningOrb(orbState, live.levelDb, onClick = onToggleListening)
        }

        // Level readout
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                if (active) "${live.levelDb.roundToInt()}" else "—",
                style = MaterialTheme.typography.displayMedium,
                color = p.text,
            )
            Text(" dB", style = MaterialTheme.typography.titleMedium, color = p.muted, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(
            when (status) {
                is ListenStatus.Error -> status.message
                is ListenStatus.Paused -> "Paused · resumes ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(status.until))}"
                ListenStatus.Idle -> "Not listening"
                else -> if (live.resting) "Silent room — the AI is resting to save battery" else describeLevel(live.levelDb)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (status is ListenStatus.Error) MaterialTheme.colorScheme.error else p.muted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        // Mode chips
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Mode.entries.forEach { mode ->
                Pill(mode.title, s.mode == mode, onClick = { vm.setMode(mode) }, icon = SoundIcons.of(mode.icon))
            }
            if (s.mode == null) Pill("Custom", true, onClick = onOpenSounds)
        }

        Spacer(Modifier.height(20.dp))

        // Live spectrogram + what the AI hears
        BeepCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Box(Modifier.fillMaxWidth().height(120.dp)) {
                Spectrogram(vm.bands, Modifier.fillMaxSize())
                if (!active) {
                    Box(Modifier.fillMaxSize().background(p.card.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                        Text("The live sound map appears while Beep listens", style = MaterialTheme.typography.bodySmall, color = p.muted)
                    }
                }
            }
            Column(Modifier.padding(20.dp).animateContentSize()) {
                Text("WHAT BEEP HEARS", style = MaterialTheme.typography.labelSmall, color = p.muted)
                Spacer(Modifier.height(12.dp))
                val hearing = if (active) live.hearing else emptyList()
                if (hearing.isEmpty()) {
                    Text(
                        when {
                            !active -> "Start listening to see the AI at work."
                            live.resting -> "Silence."
                            else -> "Listening for something recognisable…"
                        },
                        style = MaterialTheme.typography.bodyMedium, color = p.faint,
                    )
                } else {
                    hearing.forEachIndexed { i, h ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                h.label, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (i == 0) p.text else p.muted,
                                modifier = Modifier.width(150.dp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Meter(h.score, if (i == 0) p.signal else p.faint, Modifier.weight(1f), height = 6.dp)
                            Text(
                                "${(h.score * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium, color = p.muted,
                                modifier = Modifier.width(44.dp).padding(start = 8.dp),
                            )
                        }
                        if (i < hearing.lastIndex) Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Watching for
        val specs = vm.allSpecs(custom).filter { it.id in s.enabled }
        SectionLabel("Watching for ${specs.size} sounds") {
            TextButton(onClick = onOpenSounds) { Text("Edit") }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            specs.forEach { spec ->
                Column(
                    Modifier.width(72.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onOpenSounds).padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SoundBadge(spec.icon, spec.category, 48.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        spec.title, style = MaterialTheme.typography.labelMedium, color = p.muted,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        LatestCard(events.firstOrNull(), onOpenTimeline)

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = p.faint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Audio is analysed on this phone and never recorded or uploaded.",
                style = MaterialTheme.typography.bodySmall, color = p.faint,
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun LatestCard(event: LoggedEvent?, onOpenTimeline: () -> Unit) {
    val p = Beep.palette
    BeepCard(Modifier.fillMaxWidth(), onClick = onOpenTimeline) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (event == null) {
                Column(Modifier.weight(1f)) {
                    Text("Nothing yet", style = MaterialTheme.typography.titleMedium, color = p.text)
                    Text("Alerts will appear here and in your timeline.", style = MaterialTheme.typography.bodySmall, color = p.muted)
                }
            } else {
                SoundBadge(event.icon, event.category)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("LATEST ALERT", style = MaterialTheme.typography.labelSmall, color = categoryColor(event.category))
                    Text(event.title, style = MaterialTheme.typography.titleMedium, color = p.text)
                    Text(
                        "${timeAgo(event.timestamp)} · ${(event.confidence * 100).roundToInt()}% · ${event.levelDb.roundToInt()} dB",
                        style = MaterialTheme.typography.bodySmall, color = p.muted,
                    )
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = p.faint)
        }
    }
}

@Composable
private fun Uptime(startedAt: Long, active: Boolean) {
    val p = Beep.palette
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active) {
        while (active) { now = System.currentTimeMillis(); delay(1000) }
    }
    AnimatedVisibility(active && startedAt > 0) {
        val mins = ((now - startedAt) / 60_000).coerceAtLeast(0)
        Row(
            Modifier.clip(CircleShape).background(p.card).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(p.signal))
            Spacer(Modifier.width(8.dp))
            Text(
                if (mins < 60) "${mins}m" else "${mins / 60}h ${mins % 60}m",
                style = MaterialTheme.typography.labelMedium, color = p.text,
            )
        }
    }
}

private fun greeting(name: String): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val part = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
    return if (name.isBlank()) part else "$part, ${name.trim().split(" ").first()}"
}
