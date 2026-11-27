package com.shankar.beep.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shankar.beep.data.ThemeChoice
import com.shankar.beep.ui.MainViewModel
import com.shankar.beep.ui.components.BeepCard
import com.shankar.beep.ui.components.Pill
import com.shankar.beep.ui.components.SectionLabel
import com.shankar.beep.ui.components.SettingRow
import com.shankar.beep.ui.theme.Beep

@Composable
fun SettingsScreen(vm: MainViewModel, onOpenName: () -> Unit) {
    val p = Beep.palette
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val nameSamples by vm.nameSampleCount.collectAsStateWithLifecycle()
    val s = settings ?: return

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = p.text)
        Spacer(Modifier.height(20.dp))

        SectionLabel("How Beep alerts you")
        BeepCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
            SettingRow("Pause my music & videos", "Briefly pauses Spotify, YouTube and other players, then resumes", s.pauseMedia,
                { on -> vm.updateSettings { it.copy(pauseMedia = on) } }, Icons.Rounded.PauseCircle)
            SettingRow("Keep paused until I respond", "Media stays paused until you tap “Got it” (up to 2 minutes)", s.holdUntilDismissed,
                { on -> vm.updateSettings { it.copy(holdUntilDismissed = on) } }, Icons.Rounded.Timer, enabled = s.pauseMedia)
            Divider()
            SettingRow("Alert tone", "A short chime in your headphones — a different melody per kind of sound", s.chime,
                { on -> vm.updateSettings { it.copy(chime = on) } }, Icons.Rounded.MusicNote)
            SettingRow("Vibration", "A distinct pattern per kind of sound, so you can feel what happened", s.vibrate,
                { on -> vm.updateSettings { it.copy(vibrate = on) } }, Icons.Rounded.Vibration)
            SettingRow(
                "Flashlight strobe",
                if (vm.hasFlash) "Flashes the camera light — visible across a dark room" else "This phone has no camera flash",
                s.flash && vm.hasFlash,
                { on -> vm.updateSettings { it.copy(flash = on) } }, Icons.Rounded.FlashlightOn, enabled = vm.hasFlash,
            )
            SettingRow("Full-screen alert", "Wakes the screen with a big, colour-coded alert when your phone is locked", s.fullScreen,
                { on -> vm.updateSettings { it.copy(fullScreen = on) } }, Icons.Rounded.Fullscreen)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { vm.testAlert() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = p.cardHigh, contentColor = p.text),
        ) {
            Icon(Icons.Rounded.NotificationsActive, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Send a test alert")
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Your name")
        BeepCard(Modifier.fillMaxWidth(), onClick = onOpenName) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.userName.ifBlank { "Not set" }, style = MaterialTheme.typography.titleMedium, color = p.text)
                    Text(
                        if (nameSamples >= 2) "$nameSamples voice samples · tap to re-record or test" else "Record your name so Beep can listen for it",
                        style = MaterialTheme.typography.bodySmall, color = p.muted,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = p.faint)
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Appearance")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChoice.entries.forEach { choice ->
                Pill(choice.title, s.theme == choice, onClick = { vm.updateSettings { it.copy(theme = choice) } })
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Reliability")
        BeepCard(Modifier.fillMaxWidth(), onClick = {
            val intent = Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {
                context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)))
            }
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BatteryChargingFull, null, tint = p.text)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Keep Beep running", style = MaterialTheme.typography.titleSmall, color = p.text)
                    Text(
                        "Some phones stop background apps. Set Beep to “Unrestricted” battery use for all-day listening.",
                        style = MaterialTheme.typography.bodySmall, color = p.muted,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = p.faint)
            }
        }
        if (Build.VERSION.SDK_INT >= 34) {
            Spacer(Modifier.height(10.dp))
            BeepCard(Modifier.fillMaxWidth(), onClick = {
                runCatching {
                    context.startActivity(
                        Intent(AndroidSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + context.packageName))
                    )
                }
            }) {
                Text("Allow full-screen alerts", style = MaterialTheme.typography.titleSmall, color = p.text)
                Text(
                    "Android 14+ asks you to allow this so alerts can appear over the lock screen.",
                    style = MaterialTheme.typography.bodySmall, color = p.muted,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Privacy")
        BeepCard(Modifier.fillMaxWidth()) {
            Row {
                Icon(Icons.Rounded.PrivacyTip, null, tint = p.signal)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Everything stays on your phone", style = MaterialTheme.typography.titleSmall, color = p.text)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Beep analyses sound in memory and throws it away within a second. It never records, saves or uploads audio, " +
                            "and has no internet permission at all. Your timeline keeps only the name of what was heard.",
                        style = MaterialTheme.typography.bodySmall, color = p.muted,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("About")
        Text(
            "Beep 2.0 · Sound recognition by YAMNet (521 AudioSet classes) running on-device with TensorFlow Lite. " +
                "Name spotting with MFCC + dynamic time warping. Made as a final-year project.",
            style = MaterialTheme.typography.bodySmall, color = p.faint,
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = Beep.palette.line, modifier = Modifier.padding(vertical = 4.dp))
