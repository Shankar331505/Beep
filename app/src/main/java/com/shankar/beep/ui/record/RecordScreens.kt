package com.shankar.beep.ui.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shankar.beep.detect.NameProfile
import com.shankar.beep.ui.beepViewModel
import com.shankar.beep.ui.components.BeepCard
import com.shankar.beep.ui.components.SectionLabel
import com.shankar.beep.ui.components.SoundIcons
import com.shankar.beep.ui.theme.Beep
import com.shankar.beep.ui.theme.CustomBlue
import com.shankar.beep.ui.theme.HomeTeal
import com.shankar.beep.ui.theme.PeopleViolet
import com.shankar.beep.ui.theme.SafetyRed

/** Big round record button with a progress ring and live level. */
@Composable
fun RecordButton(state: RecState, idleLabel: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = Beep.palette
    val progress = (state as? RecState.Recording)?.progress ?: 0f
    val level = (state as? RecState.Recording)?.levelDb?.let { ((it - 35f) / 50f).coerceIn(0f, 1f) } ?: 0f
    val animatedLevel by animateFloatAsState(level, label = "level")
    val busy = state != RecState.Idle
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(132.dp)
                .clip(CircleShape)
                .clickable(enabled = !busy, onClick = onClick)
                .semantics { contentDescription = idleLabel },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 6.dp.toPx()
                drawCircle(p.line, radius = size.minDimension / 2 - stroke, style = Stroke(stroke))
                drawArc(
                    p.signal, -90f, 360f * progress, false,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke, stroke),
                    size = androidx.compose.ui.geometry.Size(size.width - 2 * stroke, size.height - 2 * stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                drawCircle(
                    if (busy) SafetyRed.copy(alpha = 0.2f + 0.5f * animatedLevel) else p.signal,
                    radius = size.minDimension / 2 * (0.58f + 0.12f * animatedLevel),
                )
            }
            when (state) {
                is RecState.Countdown -> Text("${state.seconds}", style = MaterialTheme.typography.displayMedium, color = p.text)
                RecState.Processing -> Text("…", style = MaterialTheme.typography.headlineLarge, color = p.text)
                is RecState.Recording -> Icon(Icons.Rounded.Mic, null, tint = p.text, modifier = Modifier.size(40.dp))
                RecState.Idle -> Icon(Icons.Rounded.Mic, null, tint = p.onSignal, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            when (state) {
                is RecState.Countdown -> "Get ready…"
                is RecState.Recording -> "Listening…"
                RecState.Processing -> "Learning…"
                RecState.Idle -> idleLabel
            },
            style = MaterialTheme.typography.titleSmall, color = p.text,
        )
    }
}

@Composable
fun FeedbackLine(feedback: Feedback?) {
    val p = Beep.palette
    AnimatedVisibility(feedback != null) {
        val f = feedback ?: return@AnimatedVisibility
        val color = when (f.tone) { Tone.GOOD -> HomeTeal; Tone.BAD -> SafetyRed; Tone.NEUTRAL -> p.muted }
        Text(
            f.text, style = MaterialTheme.typography.bodyMedium, color = color,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun beepFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Beep.palette.signal,
    unfocusedBorderColor = Beep.palette.line,
    cursorColor = Beep.palette.signal,
    focusedLabelColor = Beep.palette.signal,
)

@Composable
fun ScreenHeader(title: String, subtitle: String, onBack: (() -> Unit)?) {
    val p = Beep.palette
    Column {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = p.text)
            }
        }
        Text(title, style = MaterialTheme.typography.headlineLarge, color = p.text)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = p.muted)
    }
}

/** Name enrolment — used both in onboarding (embedded) and from Sounds. */
@Composable
fun NameEnrollment(vm: NameViewModel, compact: Boolean = false) {
    val p = Beep.palette
    val context = LocalContext.current
    val name by vm.name.collectAsStateWithLifecycle()
    val templates by vm.templates.collectAsStateWithLifecycle()
    val rec by vm.rec.collectAsStateWithLifecycle()
    val feedback by vm.feedback.collectAsStateWithLifecycle()

    DisposableEffect(Unit) { onDispose { vm.restoreListening(context) } }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = name,
            onValueChange = vm::setName,
            label = { Text("What do people call you?") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            colors = beepFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(if (compact) 20.dp else 28.dp))
        RecordButton(
            rec,
            idleLabel = if (templates.isEmpty()) "Tap and say “${name.ifBlank { "your name" }}”" else "Record another sample",
            onClick = { vm.recordSample(context) },
        )
        FeedbackLine(feedback)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(maxOf(3, templates.size)) { i ->
                val done = i < templates.size
                Box(
                    Modifier
                        .size(width = 40.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(if (done) PeopleViolet else p.cardHigh)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${templates.size} of ${maxOf(3, templates.size)} samples",
            style = MaterialTheme.typography.bodySmall, color = p.muted,
        )
        if (templates.size >= NameProfile.MIN_SAMPLES) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { vm.test(context) }, enabled = rec == RecState.Idle) { Text("Test it") }
                TextButton(onClick = vm::clearSamples, enabled = rec == RecState.Idle) { Text("Start over", color = p.muted) }
            }
        }
    }
}

@Composable
fun NameScreen(onBack: () -> Unit) {
    val vm = beepViewModel { NameViewModel(it) }
    val p = Beep.palette
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        ScreenHeader(
            "Your name",
            "Beep learns how your name sounds from a few short samples, then listens for it — even mid-song.",
            onBack,
        )
        Spacer(Modifier.height(24.dp))
        BeepCard(Modifier.fillMaxWidth()) { NameEnrollment(vm) }
        Spacer(Modifier.height(20.dp))
        SectionLabel("Tips")
        listOf(
            "Say it the way people call you — “Shankar!” across a room, not a whisper.",
            "Ask a friend or family member to record a sample too: it helps Beep recognise other voices.",
            "Adjust how strict matching is with the sensitivity slider under Sounds → Your name.",
        ).forEach {
            Row(Modifier.padding(vertical = 6.dp)) {
                Text("•  ", color = p.signal)
                Text(it, style = MaterialTheme.typography.bodyMedium, color = p.muted)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeachScreen(onBack: () -> Unit) {
    val vm = beepViewModel { TeachViewModel(it) }
    val p = Beep.palette
    val context = LocalContext.current
    val name by vm.name.collectAsStateWithLifecycle()
    val icon by vm.icon.collectAsStateWithLifecycle()
    val samples by vm.samples.collectAsStateWithLifecycle()
    val rec by vm.rec.collectAsStateWithLifecycle()
    val feedback by vm.feedback.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) { if (saved) onBack() }
    DisposableEffect(Unit) { onDispose { vm.restoreListening(context) } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        ScreenHeader(
            "Teach a sound",
            "Play the sound near your phone 3 times. Beep learns its fingerprint and alerts you when it hears it again.",
            onBack,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { vm.name.value = it.take(28) },
            label = { Text("Name it (e.g. Microwave, Intercom)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = beepFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SoundIcons.customChoices.forEach { key ->
                val selected = key == icon
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) CustomBlue.copy(alpha = 0.2f) else p.card)
                        .border(1.dp, if (selected) CustomBlue else p.line, RoundedCornerShape(14.dp))
                        .clickable { vm.icon.value = key },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(SoundIcons.of(key), key, tint = if (selected) CustomBlue else p.muted)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        BeepCard(Modifier.fillMaxWidth()) {
            RecordButton(
                rec, idleLabel = "Record sample ${samples.size + 1}",
                onClick = { vm.recordSample(context) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            FeedbackLine(feedback)
            samples.forEachIndexed { i, sample ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(CustomBlue))
                    Spacer(Modifier.width(12.dp))
                    Text("Sample ${i + 1}", style = MaterialTheme.typography.titleSmall, color = p.text)
                    Spacer(Modifier.width(8.dp))
                    Text("· ${sample.guess}", style = MaterialTheme.typography.bodySmall, color = p.muted, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.removeSample(i) }) {
                        Icon(Icons.Rounded.Close, "Remove sample ${i + 1}", tint = p.faint)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = vm::save,
            enabled = name.isNotBlank() && samples.size >= 3 && rec == RecState.Idle,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = p.signal, contentColor = p.onSignal),
        ) { Text("Save sound", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(8.dp))
        Text(
            "Only a numeric fingerprint is stored — never the recording.",
            style = MaterialTheme.typography.bodySmall, color = p.faint,
            modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
    }
}
