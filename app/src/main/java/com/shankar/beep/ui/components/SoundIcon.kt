package com.shankar.beep.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun SoundIcon(
    iconName: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val imageVector: ImageVector = when (iconName) {
        "person" -> Icons.Default.Person
        "pan_tool" -> Icons.Default.FrontHand
        "music_note" -> Icons.Default.MusicNote
        "warning" -> Icons.Default.Warning
        "local_police" -> Icons.Default.LocalPolice
        "record_voice_over" -> Icons.Default.RecordVoiceOver
        "bolt" -> Icons.Default.Bolt
        "sensor_door" -> Icons.Default.MeetingRoom
        "notifications_active" -> Icons.Default.NotificationsActive
        "broken_image" -> Icons.Default.WarningAmber
        "child_care" -> Icons.Default.ChildCare
        "pets" -> Icons.Default.Pets
        "headset" -> Icons.Default.Headphones
        "vibration" -> Icons.Default.Vibration
        "flash_on" -> Icons.Default.FlashOn
        "mic" -> Icons.Default.Mic
        "graphic_eq" -> Icons.Default.GraphicEq
        "tune" -> Icons.Default.Tune
        "settings" -> Icons.Default.Settings
        "history" -> Icons.Default.History
        "security" -> Icons.Default.Security
        "volume_up" -> Icons.AutoMirrored.Filled.VolumeUp
        else -> Icons.Default.Notifications
    }

    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
