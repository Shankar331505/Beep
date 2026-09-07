package com.shankar.beep.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shankar.beep.model.MonitoredSound
import com.shankar.beep.model.SoundCategory
import com.shankar.beep.model.SoundEvent
import com.shankar.beep.ui.theme.*

/**
 * Circular icon button matching the reference top bar back/options button.
 */
@Composable
fun CircularIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextWhite
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(CardDark)
            .border(1.dp, CardBorderDark, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Primary Mint Pill Button (matching "Create Account" in Screen 1).
 */
@Composable
fun MintPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) MintPrimary else CardDark)
            .border(
                1.dp,
                if (enabled) MintBorder else CardBorderDark,
                RoundedCornerShape(28.dp)
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (enabled) TextOnMint else TextMutedGrey,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (enabled) TextOnMint else TextMutedGrey,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Capsule Chip Filter (matching "Favourite", "Kitchen", "Room 01" in Screen 2).
 */
@Composable
fun SleekCapsuleChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MintPrimary else CardDark,
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) TextOnMint else TextMutedGrey,
        label = "chip_text"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MintBorder else CardBorderDark,
        label = "chip_border"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Hero card: live decibel readout, animated waveform, listening status and
 * a prominent power control. Below it sit quick stats.
 */
@Composable
fun HeroAmbientCard(
    decibel: Float,
    isListening: Boolean,
    activeSoundsCount: Int,
    totalDetections: Int,
    lastAlertName: String?,
    onToggleListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(CardDarkElevated, CardDark)
                )
            )
            .border(
                1.dp,
                if (isListening) MintPrimary.copy(alpha = 0.55f) else CardBorderDark,
                RoundedCornerShape(28.dp)
            )
            .padding(20.dp)
    ) {
        // Status pill + power button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ListeningStatusPill(isListening = isListening)
            PowerControlButton(
                isListening = isListening,
                onClick = onToggleListening
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Big decibel readout
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (isListening) "${decibel.toInt()}" else "--",
                color = if (isListening) TextWhite else TextMutedGrey,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 48.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "dB",
                color = if (isListening) MintPrimary else TextMutedGrey,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        val statusText = when {
            !isListening -> "Standby — tap power to start listening"
            decibel < 45 -> "Calm environment"
            decibel < 70 -> "Moderate noise"
            else -> "Loud environment"
        }
        Text(
            text = statusText,
            color = TextMutedGrey,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        WaveformVisualizer(
            decibel = decibel,
            isListening = isListening,
            maxHeight = 44.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniMetricTile(
                label = "Active Sounds",
                value = "$activeSoundsCount",
                modifier = Modifier.weight(1f)
            )
            MiniMetricTile(
                label = "Detections",
                value = "$totalDetections",
                modifier = Modifier.weight(1f)
            )
            MiniMetricTile(
                label = "Last Alert",
                value = lastAlertName ?: "—",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Small live status capsule: pulsing dot + state text.
 */
@Composable
fun ListeningStatusPill(
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val pulse by rememberInfiniteTransition(label = "status_pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_dot_alpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        (if (isListening) MintPrimary else TextMutedGrey)
                            .copy(alpha = if (isListening) pulse else 0.6f)
                    )
            )
            Text(
                text = if (isListening) "Listening" else "Paused",
                color = if (isListening) TextWhite else TextMutedGrey,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Circular power control for starting / pausing the listening engine.
 */
@Composable
fun PowerControlButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isListening) MintPrimary else CardDarkElevated,
        label = "power_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isListening) MintBorder else MintPrimary.copy(alpha = 0.45f),
        label = "power_border"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isListening) TextOnMint else MintPrimary,
        label = "power_icon"
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = if (isListening) "Pause listening" else "Start listening",
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Compact stat tile used in the hero card.
 */
@Composable
fun MiniMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            color = TextMutedGrey,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * 2x2 Sound Grid Card matching the exact "Air conditioner On/Off" card in Screen 2.
 */
@Composable
fun SoundGridCard(
    sound: MonitoredSound,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = CardDark
    val borderStroke by animateDpAsState(
        targetValue = if (isEnabled) 1.5.dp else 1.dp,
        label = "border_stroke"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isEnabled) MintBorder else CardBorderDark,
        label = "border_color"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(borderStroke, borderColor, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: "On" / "Off" + Switch pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnabled) "On" else "Off",
                    color = if (isEnabled) TextWhite else TextMutedGrey,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Sleek Power Pill Toggle
                SleekPowerToggle(
                    checked = isEnabled,
                    onCheckedChange = onToggle
                )
            }

            // Center: Vector Icon
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isEnabled) {
                                when (sound.category) {
                                    SoundCategory.EMERGENCY -> EmergencySoftBg
                                    SoundCategory.SOCIAL -> MintSoftBg
                                    SoundCategory.DOMESTIC -> SurfaceDark
                                }
                            } else {
                                SurfaceDark
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    SoundIcon(
                        iconName = sound.iconName,
                        contentDescription = sound.displayName,
                        tint = if (isEnabled) {
                            when (sound.category) {
                                SoundCategory.EMERGENCY -> AccentEmergencyRed
                                SoundCategory.SOCIAL -> MintPrimary
                                SoundCategory.DOMESTIC -> MintPrimary
                            }
                        } else {
                            TextMutedGrey
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Bottom: Sound Label
            Text(
                text = sound.displayName,
                color = if (isEnabled) TextWhite else TextMutedGrey,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/**
 * Custom modern pill switch with power icon (matching reference switch in Screen 2).
 */
@Composable
fun SleekPowerToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackBg by animateColorAsState(
        targetValue = if (checked) MintSoftBg else SurfaceDark,
        label = "switch_track"
    )
    val thumbBg by animateColorAsState(
        targetValue = if (checked) MintPrimary else CardDarkElevated,
        label = "switch_thumb"
    )
    val iconColor by animateColorAsState(
        targetValue = if (checked) TextOnMint else TextMutedGrey,
        label = "switch_icon"
    )
    val alignmentBias by animateFloatAsState(
        targetValue = if (checked) 1f else -1f,
        animationSpec = spring(),
        label = "switch_bias"
    )

    Box(
        modifier = modifier
            .width(46.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(trackBg)
            .border(
                1.dp,
                if (checked) MintPrimary.copy(alpha = 0.6f) else CardBorderDark,
                RoundedCornerShape(13.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = androidx.compose.ui.BiasAlignment(alignmentBias, 0f)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(thumbBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

/**
 * Activity Item Card matching Screen 3 of the reference.
 */
@Composable
fun ActivityItemCard(
    event: SoundEvent,
    formattedTime: String,
    modifier: Modifier = Modifier
) {
    val isEmergency = event.category == SoundCategory.EMERGENCY

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail / Icon box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isEmergency) EmergencySoftBg else MintSoftBg
                    ),
                contentAlignment = Alignment.Center
            ) {
                SoundIcon(
                    iconName = when (event.soundId) {
                        "user_name" -> "person"
                        "door_knock" -> "sensor_door"
                        "glass_break" -> "broken_image"
                        "fire_smoke_alarm" -> "warning"
                        "emergency_siren" -> "local_police"
                        "clapping" -> "pan_tool"
                        else -> "volume_up"
                    },
                    contentDescription = event.soundName,
                    tint = if (isEmergency) AccentEmergencyRed else MintPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.soundName,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${(event.confidence * 100).toInt()}% confidence • ${event.decibel.toInt()} dB",
                    color = TextMutedGrey,
                    fontSize = 12.sp
                )
            }

            // Timestamp
            Text(
                text = formattedTime,
                color = TextMutedGrey,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
