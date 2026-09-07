package com.shankar.beep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shankar.beep.model.MonitoredSound
import com.shankar.beep.model.SoundCategory
import com.shankar.beep.ui.theme.*

@Composable
fun SoundItemCard(
    sound: MonitoredSound,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showThreshold: Boolean = true
) {
    val iconBgColor = when (sound.category) {
        SoundCategory.EMERGENCY -> EmergencyRedBg
        SoundCategory.DOMESTIC -> DarkSurface
        SoundCategory.SOCIAL -> AmberBg
    }

    val iconTint = when (sound.category) {
        SoundCategory.EMERGENCY -> EmergencyCoral
        SoundCategory.DOMESTIC -> PrimaryBlue
        SoundCategory.SOCIAL -> AmberWarning
    }

    val cardBorder = if (isEnabled) DarkCardBorder else DarkCardBorder.copy(alpha = 0.4f)
    val cardBg = if (isEnabled) DarkCard else DarkCard.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                SoundIcon(
                    iconName = sound.iconName,
                    contentDescription = sound.displayName,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = sound.displayName,
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isEnabled) TextPrimary else TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = sound.description,
                    style = Typography.bodyMedium,
                    color = if (isEnabled) TextSecondary else TextMuted,
                    maxLines = 1
                )

                if (showThreshold) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryBadge(category = sound.category)
                        MetricChip(
                            label = "Sensitivity",
                            value = "${(sound.confidenceThreshold * 100).toInt()}%"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Toggle Switch
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextPrimary,
                    checkedTrackColor = PrimaryIndigo,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkSurface
                )
            )
        }
    }
}
