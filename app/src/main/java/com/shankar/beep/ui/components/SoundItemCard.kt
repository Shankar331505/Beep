package com.shankar.beep.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shankar.beep.model.MonitoredSound

@Composable
fun SoundItemCard(
    sound: MonitoredSound,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showThreshold: Boolean = true
) {
    SoundRowCard(
        sound = sound,
        isEnabled = isEnabled,
        onToggle = onToggle,
        onClick = onClick,
        modifier = modifier
    )
}
