package com.shankar.beep.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shankar.beep.detect.Mode
import com.shankar.beep.ui.MainViewModel
import com.shankar.beep.ui.beepViewModel
import com.shankar.beep.ui.components.BeepCard
import com.shankar.beep.ui.components.ListeningOrb
import com.shankar.beep.ui.components.OrbState
import com.shankar.beep.ui.components.SoundIcons
import com.shankar.beep.ui.record.NameEnrollment
import com.shankar.beep.ui.record.NameViewModel
import com.shankar.beep.ui.theme.Beep
import com.shankar.beep.ui.theme.HomeTeal
import com.shankar.beep.ui.theme.PeopleViolet
import com.shankar.beep.ui.theme.SafetyRed

@Composable
fun OnboardingScreen(vm: MainViewModel, onFinished: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val p = Beep.palette

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        // Progress dots
        Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.weight(1f).height(4.dp).clip(CircleShape)
                        .background(if (i <= step) p.signal else p.cardHigh)
                )
            }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "onboarding",
        ) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                when (s) {
                    0 -> Welcome()
                    1 -> Permissions()
                    2 -> NameStep()
                    else -> ModeStep(vm)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text("Back", color = p.muted) }
            Spacer(Modifier.weight(1f))
            if (step == 2) TextButton(onClick = { step++ }) { Text("Skip for now", color = p.muted) }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { if (step < 3) step++ else onFinished() },
                colors = ButtonDefaults.buttonColors(containerColor = p.signal, contentColor = p.onSignal),
                modifier = Modifier.height(52.dp),
            ) {
                Text(
                    when (step) { 0 -> "Get started"; 3 -> "Start listening"; else -> "Continue" },
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun Welcome() {
    val p = Beep.palette
    Spacer(Modifier.height(8.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ListeningOrb(OrbState.LISTENING, 60f, onClick = {}, size = 200.dp)
    }
    Spacer(Modifier.height(24.dp))
    Text("Never miss the\nsounds that matter.", style = MaterialTheme.typography.headlineLarge, color = p.text)
    Spacer(Modifier.height(12.dp))
    Text(
        "Headphones on, deep in focus, or hard of hearing — Beep listens to the world around you and taps you on the shoulder when something important happens.",
        style = MaterialTheme.typography.bodyLarge, color = p.muted,
    )
    Spacer(Modifier.height(24.dp))
    Feature(Icons.Rounded.GraphicEq, SafetyRed, "Recognises 20+ sounds", "Alarms, sirens, doorbells, knocks, babies, horns and more")
    Feature(Icons.Rounded.Badge, PeopleViolet, "Knows your name", "Pauses your music when someone calls you")
    Feature(Icons.Rounded.Lock, HomeTeal, "Private by design", "Runs entirely on your phone. No recording, no internet")
}

@Composable
private fun Feature(icon: ImageVector, color: androidx.compose.ui.graphics.Color, title: String, body: String) {
    val p = Beep.palette
    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = p.text)
            Text(body, style = MaterialTheme.typography.bodySmall, color = p.muted)
        }
    }
}

@Composable
private fun Permissions() {
    val p = Beep.palette
    val context = LocalContext.current
    fun granted(perm: String) = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    var mic by remember { mutableStateOf(granted(Manifest.permission.RECORD_AUDIO)) }
    var notif by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS))
    }
    LifecycleResumeEffect(Unit) {
        mic = granted(Manifest.permission.RECORD_AUDIO)
        notif = Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS)
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        result[Manifest.permission.RECORD_AUDIO]?.let { mic = it }
        result["android.permission.POST_NOTIFICATIONS"]?.let { notif = it }
    }

    Spacer(Modifier.height(8.dp))
    Text("Two quick permissions", style = MaterialTheme.typography.headlineLarge, color = p.text)
    Spacer(Modifier.height(12.dp))
    Text(
        "Beep needs to hear the room and to tell you what it heard. That's all it asks for.",
        style = MaterialTheme.typography.bodyLarge, color = p.muted,
    )
    Spacer(Modifier.height(28.dp))
    PermissionCard(
        Icons.Rounded.Mic, "Microphone", "To recognise sounds around you. Audio is analysed in memory and immediately discarded.",
        mic,
    ) { launcher.launch(arrayOf(Manifest.permission.RECORD_AUDIO)) }
    Spacer(Modifier.height(12.dp))
    PermissionCard(
        Icons.Rounded.Notifications, "Notifications", "To alert you — even when your phone is locked or in your pocket.",
        notif,
    ) { if (Build.VERSION.SDK_INT >= 33) launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) }
    if (!mic) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Without the microphone Beep can't listen. You can still look around and grant it later.",
            style = MaterialTheme.typography.bodySmall, color = p.faint,
        )
    }
}

@Composable
private fun PermissionCard(icon: ImageVector, title: String, body: String, granted: Boolean, onRequest: () -> Unit) {
    val p = Beep.palette
    BeepCard(Modifier.fillMaxWidth(), onClick = if (granted) null else onRequest) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = p.text)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = p.text)
                Text(body, style = MaterialTheme.typography.bodySmall, color = p.muted)
            }
            Spacer(Modifier.width(12.dp))
            if (granted) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(HomeTeal), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, "Granted", tint = p.canvas, modifier = Modifier.size(18.dp))
                }
            } else {
                Text("Allow", style = MaterialTheme.typography.labelLarge, color = p.signal)
            }
        }
    }
}

@Composable
private fun NameStep() {
    val p = Beep.palette
    val nameVm = beepViewModel { NameViewModel(it) }
    Spacer(Modifier.height(8.dp))
    Text("Teach Beep your name", style = MaterialTheme.typography.headlineLarge, color = p.text)
    Spacer(Modifier.height(12.dp))
    Text(
        "Say your name 3 times, the way people call you. Beep will pause your music when it hears it.",
        style = MaterialTheme.typography.bodyLarge, color = p.muted,
    )
    Spacer(Modifier.height(24.dp))
    NameEnrollment(nameVm, compact = true)
}

@Composable
private fun ModeStep(vm: MainViewModel) {
    val p = Beep.palette
    val settings by vm.settings.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(settings?.mode ?: Mode.EVERYDAY) }
    Spacer(Modifier.height(8.dp))
    Text("Where are you now?", style = MaterialTheme.typography.headlineLarge, color = p.text)
    Spacer(Modifier.height(12.dp))
    Text(
        "Pick a starting point. You can switch modes anytime or fine-tune every sound.",
        style = MaterialTheme.typography.bodyLarge, color = p.muted,
    )
    Spacer(Modifier.height(24.dp))
    Mode.entries.forEach { mode ->
        val isSel = mode == selected
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (isSel) p.signal.copy(alpha = 0.12f) else p.card)
                .border(1.dp, if (isSel) p.signal else p.line, RoundedCornerShape(20.dp))
                .clickable {
                    selected = mode
                    vm.setMode(mode)
                }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(SoundIcons.of(mode.icon), null, tint = if (isSel) p.signal else p.muted)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(mode.title, style = MaterialTheme.typography.titleSmall, color = p.text)
                Text("${mode.tagline} · ${mode.sounds.size} sounds", style = MaterialTheme.typography.bodySmall, color = p.muted)
            }
        }
    }
}
