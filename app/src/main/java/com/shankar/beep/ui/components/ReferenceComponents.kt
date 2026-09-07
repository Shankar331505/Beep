package com.shankar.beep.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shankar.beep.model.MonitoredSound
import com.shankar.beep.model.SoundCategory
import com.shankar.beep.model.SoundEvent
import com.shankar.beep.ui.theme.Ash
import com.shankar.beep.ui.theme.Brass
import com.shankar.beep.ui.theme.BrassWash
import com.shankar.beep.ui.theme.DisplaySerif
import com.shankar.beep.ui.theme.Hairline
import com.shankar.beep.ui.theme.Ink
import com.shankar.beep.ui.theme.InkRaised
import com.shankar.beep.ui.theme.Ivory
import com.shankar.beep.ui.theme.IvoryMuted
import com.shankar.beep.ui.theme.OnBrass
import com.shankar.beep.ui.theme.Oxblood
import com.shankar.beep.ui.theme.OxbloodWash
import com.shankar.beep.ui.theme.Panel
import com.shankar.beep.ui.theme.Stone
import com.shankar.beep.ui.theme.UiSans

private val PanelShape = RoundedCornerShape(20.dp)
private val QuietRipple = Color(0x33F3EEE4)

@Composable
fun CircularIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ivory
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Panel)
            .border(1.dp, Hairline, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = QuietRipple),
                role = Role.Button,
                onClick = onClick
            ),
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

@Composable
fun BeepTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            when {
                onBack != null -> CircularIconButton(
                    icon = ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack
                )
                leading != null -> leading()
                else -> Spacer(Modifier.size(44.dp))
            }
        }

        Text(
            text = title,
            color = Ivory,
            fontFamily = if (title == "Beep") DisplaySerif else UiSans,
            fontSize = if (title == "Beep") 26.sp else 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = if (title == "Beep") 1.6.sp else 0.2.sp
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            trailing?.invoke() ?: Spacer(Modifier.size(44.dp))
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val bg by animateColorAsState(
        targetValue = if (enabled) Brass else Panel,
        label = "primary_bg"
    )
    val fg by animateColorAsState(
        targetValue = if (enabled) OnBrass else Stone,
        label = "primary_fg"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = QuietRipple),
                role = Role.Button,
                onClick = onClick
            ),
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
                    tint = fg,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = fg,
                fontFamily = UiSans,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

/** Kept for existing call sites. */
@Composable
fun MintPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    PrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon
    )
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) Ivory else Color.Transparent,
        label = "chip_bg"
    )
    val fg by animateColorAsState(
        targetValue = if (isSelected) Ink else IvoryMuted,
        label = "chip_fg"
    )
    val border by animateColorAsState(
        targetValue = if (isSelected) Ivory else Hairline,
        label = "chip_border"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = QuietRipple),
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontFamily = UiSans,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
fun SleekCapsuleChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(text = text, isSelected = isSelected, onClick = onClick, modifier = modifier)
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            color = Stone,
            fontFamily = UiSans,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = Brass,
                fontFamily = UiSans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAction
                )
            )
        }
    }
}

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
            .clip(RoundedCornerShape(24.dp))
            .background(Panel)
            .border(1.dp, if (isListening) Brass.copy(alpha = 0.35f) else Hairline, RoundedCornerShape(24.dp))
            .padding(22.dp)
    ) {
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

        Spacer(modifier = Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (isListening) "${decibel.toInt()}" else "—",
                color = if (isListening) Ivory else Stone,
                fontFamily = DisplaySerif,
                fontSize = 64.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 64.sp,
                letterSpacing = (-1.5).sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "dB",
                color = if (isListening) Brass else Stone,
                fontFamily = UiSans,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        val statusText = when {
            !isListening -> "Standby. Press power to begin."
            decibel < 45 -> "Quiet room"
            decibel < 70 -> "Moderate room"
            else -> "Loud room"
        }
        Text(
            text = statusText,
            color = IvoryMuted,
            fontFamily = UiSans,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        WaveformVisualizer(
            decibel = decibel,
            isListening = isListening,
            maxHeight = 42.dp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniMetricTile(
                label = "Active",
                value = "$activeSoundsCount",
                modifier = Modifier.weight(1f)
            )
            MiniMetricTile(
                label = "Heard",
                value = "$totalDetections",
                modifier = Modifier.weight(1f)
            )
            MiniMetricTile(
                label = "Last",
                value = lastAlertName ?: "—",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ListeningStatusPill(
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val pulse by rememberInfiniteTransition(label = "status_pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_dot_alpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(InkRaised)
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        (if (isListening) Brass else Stone)
                            .copy(alpha = if (isListening) pulse else 0.55f)
                    )
            )
            Text(
                text = if (isListening) "Listening" else "Paused",
                color = if (isListening) Ivory else Stone,
                fontFamily = UiSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun PowerControlButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isListening) Brass else InkRaised,
        label = "power_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isListening) Brass else Hairline,
        label = "power_border"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isListening) OnBrass else Brass,
        label = "power_icon"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = QuietRipple),
                role = Role.Switch,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = if (isListening) "Pause listening" else "Start listening",
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun MiniMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(InkRaised)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = Stone,
            fontFamily = UiSans,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Ivory,
            fontFamily = UiSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SoundRowCard(
    sound: MonitoredSound,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = categoryAccent(sound.category)
    val wash = categoryWash(sound.category)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Panel)
            .border(1.dp, if (isEnabled) accent.copy(alpha = 0.28f) else Hairline, PanelShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = QuietRipple),
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isEnabled) wash else InkRaised),
                contentAlignment = Alignment.Center
            ) {
                SoundIcon(
                    iconName = sound.iconName,
                    contentDescription = sound.displayName,
                    tint = if (isEnabled) accent else Stone,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sound.displayName,
                    color = if (isEnabled) Ivory else Stone,
                    fontFamily = UiSans,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${sound.category.displayName}  ·  ${(sound.defaultConfidenceThreshold * 100).toInt()}%",
                    color = IvoryMuted,
                    fontFamily = UiSans,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            BeepToggle(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun SoundGridCard(
    sound: MonitoredSound,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SoundRowCard(
        sound = sound,
        isEnabled = isEnabled,
        onToggle = onToggle,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun BeepToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackBg by animateColorAsState(
        targetValue = if (checked) Brass else InkRaised,
        label = "switch_track"
    )
    val thumbBg by animateColorAsState(
        targetValue = if (checked) OnBrass else Ash,
        label = "switch_thumb"
    )
    val alignmentBias by animateFloatAsState(
        targetValue = if (checked) 1f else -1f,
        animationSpec = spring(),
        label = "switch_bias"
    )

    Box(
        modifier = modifier
            .width(44.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(trackBg)
            .border(
                1.dp,
                if (checked) Brass else Hairline,
                RoundedCornerShape(13.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch
            ) { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = androidx.compose.ui.BiasAlignment(alignmentBias, 0f)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(thumbBg)
        )
    }
}

@Composable
fun SleekPowerToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BeepToggle(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
}

@Composable
fun ActivityItemCard(
    event: SoundEvent,
    formattedTime: String,
    modifier: Modifier = Modifier
) {
    val isEmergency = event.category == SoundCategory.EMERGENCY
    val accent = if (isEmergency) Oxblood else Brass
    val wash = if (isEmergency) OxbloodWash else BrassWash

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Panel)
            .border(1.dp, Hairline, PanelShape)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(wash),
                contentAlignment = Alignment.Center
            ) {
                SoundIcon(
                    iconName = iconForSoundId(event.soundId),
                    contentDescription = event.soundName,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.soundName,
                    color = Ivory,
                    fontFamily = UiSans,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${(event.confidence * 100).toInt()}%  ·  ${event.decibel.toInt()} dB",
                    color = IvoryMuted,
                    fontFamily = UiSans,
                    fontSize = 12.sp
                )
            }

            Text(
                text = formattedTime,
                color = Stone,
                fontFamily = UiSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Panel)
            .border(
                1.dp,
                if (highlighted) Brass.copy(alpha = 0.4f) else Hairline,
                PanelShape
            )
            .padding(18.dp)
    ) {
        content()
    }
}

fun categoryAccent(category: SoundCategory): Color = when (category) {
    SoundCategory.EMERGENCY -> Oxblood
    SoundCategory.SOCIAL -> Brass
    SoundCategory.DOMESTIC -> IvoryMuted
}

fun categoryWash(category: SoundCategory): Color = when (category) {
    SoundCategory.EMERGENCY -> OxbloodWash
    SoundCategory.SOCIAL -> BrassWash
    SoundCategory.DOMESTIC -> InkRaised
}

fun iconForSoundId(soundId: String): String = when (soundId) {
    "user_name" -> "person"
    "door_knock" -> "sensor_door"
    "glass_break" -> "broken_image"
    "fire_smoke_alarm" -> "warning"
    "emergency_siren" -> "local_police"
    "clapping" -> "pan_tool"
    "doorbell" -> "notifications_active"
    "baby_cry" -> "child_care"
    "dog_bark" -> "pets"
    "scream" -> "record_voice_over"
    "whistle" -> "music_note"
    "sudden_loud_impact" -> "bolt"
    else -> "volume_up"
}
