package com.shankar.beep.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shankar.beep.data.SoundCatalog
import com.shankar.beep.model.MonitoredSound
import com.shankar.beep.model.SoundCategory
import com.shankar.beep.ui.components.*
import com.shankar.beep.ui.dashboard.DashboardViewModel
import com.shankar.beep.ui.theme.*

@Composable
fun SoundDetailScreen(
    soundId: String,
    onBack: () -> Unit,
    viewModel: DashboardViewModel
) {
    val sound = remember(soundId) { SoundCatalog.getSoundById(soundId) }
    val userSettings by viewModel.userSettings.collectAsState()

    if (sound == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(BgDark),
            contentAlignment = Alignment.Center
        ) {
            Text("Sound not found", color = TextWhite)
        }
        return
    }

    val currentThreshold = userSettings.soundThresholds[sound.id] ?: sound.defaultConfidenceThreshold
    var sliderValue by remember(currentThreshold) { mutableFloatStateOf(currentThreshold) }
    val isEnabled = userSettings.enabledSoundIds.contains(sound.id)

    Scaffold(
        containerColor = BgDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack
                )

                Text(
                    text = sound.displayName,
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(44.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Sound Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(CardDark)
                    .border(
                        1.5.dp,
                        if (isEnabled) MintBorder else CardBorderDark,
                        RoundedCornerShape(26.dp)
                    )
                    .padding(22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEnabled) MintSoftBg else SurfaceDark
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        SoundIcon(
                            iconName = sound.iconName,
                            contentDescription = sound.displayName,
                            tint = if (isEnabled) MintPrimary else TextMutedGrey,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sound.displayName,
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = sound.category.displayName,
                            color = MintPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    SleekPowerToggle(
                        checked = isEnabled,
                        onCheckedChange = { viewModel.toggleSoundEnabled(sound.id, it) }
                    )
                }
            }

            // Description Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorderDark, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = "Description",
                        color = TextMutedGrey,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sound.description,
                        color = TextWhite,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            // Confidence Sensitivity Slider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorderDark, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detection Sensitivity",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(sliderValue * 100).toInt()}%",
                            color = MintPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Fine-tune the confidence threshold required before triggering alerts into your headphones.",
                        color = TextMutedGrey,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            viewModel.setSoundThreshold(sound.id, sliderValue)
                        },
                        valueRange = 0.45f..0.95f,
                        colors = SliderDefaults.colors(
                            thumbColor = MintPrimary,
                            activeTrackColor = MintPrimary,
                            inactiveTrackColor = SurfaceDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Simulate Test Alert (Mint Pill Button)
            MintPillButton(
                text = "Simulate & Test Alert",
                leadingIcon = Icons.Default.PlayArrow,
                onClick = { viewModel.testTriggerAlert(sound) },
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
