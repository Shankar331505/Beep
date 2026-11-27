package com.shankar.beep.ui.sounds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shankar.beep.data.Settings
import com.shankar.beep.detect.Category
import com.shankar.beep.detect.DetectionEngine
import com.shankar.beep.detect.Fingerprint
import com.shankar.beep.detect.LoudnessWatcher
import com.shankar.beep.detect.Mode
import com.shankar.beep.detect.SoundCatalog
import com.shankar.beep.detect.SoundSpec
import com.shankar.beep.detect.Source
import com.shankar.beep.service.ListenStatus
import com.shankar.beep.ui.MainViewModel
import com.shankar.beep.ui.components.BeepCard
import com.shankar.beep.ui.components.Meter
import com.shankar.beep.ui.components.Pill
import com.shankar.beep.ui.components.SectionLabel
import com.shankar.beep.ui.components.SoundBadge
import com.shankar.beep.ui.components.SoundIcons
import com.shankar.beep.ui.theme.Beep
import com.shankar.beep.ui.theme.categoryColor
import kotlin.math.roundToInt

@Composable
fun SoundsScreen(vm: MainViewModel, onOpenName: () -> Unit, onTeach: () -> Unit) {
    val p = Beep.palette
    val settings by vm.settings.collectAsStateWithLifecycle()
    val custom by vm.customSounds.collectAsStateWithLifecycle()
    val nameSamples by vm.nameSampleCount.collectAsStateWithLifecycle()
    val s = settings ?: return
    var tuning by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<SoundSpec?>(null) }

    val specs = vm.allSpecs(custom)
    val groups = Category.entries.associateWith { cat -> specs.filter { it.category == cat } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        item {
            Text("Sounds", style = MaterialTheme.typography.headlineLarge, color = p.text)
            Text(
                "${s.enabled.size} switched on · tap a sound to tune it",
                style = MaterialTheme.typography.bodyMedium, color = p.muted,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Mode.entries.forEach { mode ->
                    Pill(mode.title, s.mode == mode, onClick = { vm.setMode(mode) }, icon = SoundIcons.of(mode.icon))
                }
            }
            Text(
                s.mode?.tagline ?: "Custom selection",
                style = MaterialTheme.typography.bodySmall, color = p.muted,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
            )
        }
        Category.entries.forEach { cat ->
            val list = groups[cat].orEmpty()
            item(key = "h-$cat") {
                Spacer(Modifier.height(12.dp))
                SectionLabel(cat.title)
            }
            items(list, key = { it.id }) { spec ->
                SoundRow(
                    spec = spec,
                    enabled = spec.id in s.enabled && (spec.id != SoundCatalog.NAME || nameSamples >= 2),
                    subtitle = when {
                        spec.id == SoundCatalog.NAME && nameSamples < 2 -> "Set up needed — record your name"
                        spec.id == SoundCatalog.NAME -> "\"${s.userName.ifBlank { "Your name" }}\" · $nameSamples voice samples"
                        else -> spec.description
                    },
                    onToggle = { on ->
                        if (spec.id == SoundCatalog.NAME && on && nameSamples < 2) onOpenName()
                        else vm.setEnabled(spec.id, on)
                    },
                    onClick = { if (spec.id == SoundCatalog.NAME) onOpenName() else tuning = spec.id },
                )
            }
            if (cat == Category.CUSTOM) {
                item(key = "teach") {
                    BeepCard(Modifier.fillMaxWidth().padding(top = 8.dp), onClick = onTeach) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Add, null, tint = p.signal)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Teach Beep a sound", style = MaterialTheme.typography.titleSmall, color = p.text)
                                Text(
                                    "Your microwave, intercom, a pet — record it 3 times",
                                    style = MaterialTheme.typography.bodySmall, color = p.muted,
                                )
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = p.faint)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }

    tuning?.let { id ->
        val spec = specs.firstOrNull { it.id == id }
        if (spec == null) tuning = null
        else TuneSheet(
            vm = vm, spec = spec, settings = s,
            onDelete = if (spec.source == Source.CUSTOM) ({ deleting = spec; tuning = null }) else null,
            onDismiss = { tuning = null },
        )
    }

    deleting?.let { spec ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Forget “${spec.title}”?") },
            text = { Text("Beep will stop listening for this sound and delete what it learned.") },
            confirmButton = { TextButton(onClick = { vm.deleteCustom(spec.id); deleting = null }) { Text("Forget") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SoundRow(spec: SoundSpec, enabled: Boolean, subtitle: String, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    val p = Beep.palette
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SoundBadge(spec.icon, spec.category, dimmed = !enabled)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(spec.title, style = MaterialTheme.typography.titleSmall, color = p.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 2)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = categoryColor(spec.category),
                checkedThumbColor = p.canvas,
                uncheckedTrackColor = p.cardHigh,
                uncheckedBorderColor = p.line,
                uncheckedThumbColor = p.muted,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TuneSheet(
    vm: MainViewModel,
    spec: SoundSpec,
    settings: Settings,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val p = Beep.palette
    val live by vm.live.collectAsStateWithLifecycle()
    val color = categoryColor(spec.category)
    var value by remember(spec.id) { mutableFloatStateOf(settings.sensitivity[spec.id] ?: spec.defaultSensitivity) }
    val listening = live.status is ListenStatus.Listening

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = p.card,
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SoundBadge(spec.icon, spec.category, 56.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(spec.title, style = MaterialTheme.typography.titleLarge, color = p.text)
                    Text(spec.description, style = MaterialTheme.typography.bodySmall, color = p.muted)
                }
            }
            Spacer(Modifier.height(28.dp))
            Text("SENSITIVITY", style = MaterialTheme.typography.labelSmall, color = p.muted)
            Slider(
                value = value,
                onValueChange = { value = it },
                onValueChangeFinished = { vm.setSensitivity(spec.id, value) },
                colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = p.cardHigh),
            )
            Row {
                Text("Fewer false alarms", style = MaterialTheme.typography.bodySmall, color = p.muted, modifier = Modifier.weight(1f))
                Text("Catch more", style = MaterialTheme.typography.bodySmall, color = p.muted)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when (spec.source) {
                    Source.MODEL -> "Alerts when the AI is at least ${(DetectionEngine.modelThreshold(value) * 100).roundToInt()}% sure" +
                        if (spec.shape == com.shankar.beep.detect.Shape.SUSTAINED) ", for at least a second." else "."
                    Source.LOUDNESS -> "Alerts above ${LoudnessWatcher.absoluteThreshold(value).roundToInt()} dB when that's a sudden jump of ${LoudnessWatcher.MIN_JUMP_DB.roundToInt()}+ dB over the room."
                    Source.CUSTOM -> "Alerts when a sound is ${(Fingerprint.threshold(value) * 100).roundToInt()}% similar to your samples."
                    Source.NAME -> "Higher accepts looser matches of your name."
                },
                style = MaterialTheme.typography.bodySmall, color = p.text,
            )

            Spacer(Modifier.height(28.dp))
            Text("LIVE MATCH", style = MaterialTheme.typography.labelSmall, color = p.muted)
            Spacer(Modifier.height(10.dp))
            val (score, threshold) = when (spec.source) {
                Source.MODEL -> (live.scores[spec.id] ?: 0f) to DetectionEngine.modelThreshold(value)
                Source.CUSTOM -> (live.scores[spec.id] ?: 0f) to Fingerprint.threshold(value)
                Source.LOUDNESS -> ((live.levelDb - 30f) / 70f) to ((LoudnessWatcher.absoluteThreshold(value) - 30f) / 70f)
                Source.NAME -> (live.scores[spec.id] ?: 0f) to 0.5f
            }
            Meter(if (listening) score else 0f, color, threshold = threshold, height = 12.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                if (listening) "Make the sound now — when the bar crosses the line, Beep alerts."
                else "Start listening on the home screen to test this sound live.",
                style = MaterialTheme.typography.bodySmall, color = p.muted,
            )
            if (onDelete != null) {
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Forget this sound")
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { onDismiss(); vm.testAlert(spec) }, modifier = Modifier.fillMaxWidth()) {
                Text("Preview this alert", color = color)
            }
        }
    }
}
