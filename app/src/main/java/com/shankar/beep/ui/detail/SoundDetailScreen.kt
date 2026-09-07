package com.shankar.beep.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shankar.beep.data.SoundCatalog
import com.shankar.beep.ui.components.BeepToggle
import com.shankar.beep.ui.components.BeepTopBar
import com.shankar.beep.ui.components.PrimaryButton
import com.shankar.beep.ui.components.SoundIcon
import com.shankar.beep.ui.components.SurfaceCard
import com.shankar.beep.ui.components.categoryAccent
import com.shankar.beep.ui.components.categoryWash
import com.shankar.beep.ui.dashboard.DashboardViewModel
import com.shankar.beep.ui.theme.Brass
import com.shankar.beep.ui.theme.DisplaySerif
import com.shankar.beep.ui.theme.Ink
import com.shankar.beep.ui.theme.InkRaised
import com.shankar.beep.ui.theme.Ivory
import com.shankar.beep.ui.theme.IvoryMuted
import com.shankar.beep.ui.theme.Stone
import com.shankar.beep.ui.theme.UiSans

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
            modifier = Modifier
                .fillMaxSize()
                .background(Ink),
            contentAlignment = Alignment.Center
        ) {
            Text("Sound not found", color = Ivory, fontFamily = UiSans)
        }
        return
    }

    val currentThreshold = userSettings.soundThresholds[sound.id] ?: sound.defaultConfidenceThreshold
    var sliderValue by remember(currentThreshold) { mutableFloatStateOf(currentThreshold) }
    val isEnabled = userSettings.enabledSoundIds.contains(sound.id)
    val accent = categoryAccent(sound.category)
    val wash = categoryWash(sound.category)

    Scaffold(
        containerColor = Ink,
        topBar = {
            BeepTopBar(
                title = sound.displayName,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SurfaceCard(highlighted = isEnabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isEnabled) wash else InkRaised),
                        contentAlignment = Alignment.Center
                    ) {
                        SoundIcon(
                            iconName = sound.iconName,
                            contentDescription = sound.displayName,
                            tint = if (isEnabled) accent else Stone,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sound.displayName,
                            color = Ivory,
                            fontFamily = DisplaySerif,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = sound.category.displayName.uppercase(),
                            color = accent,
                            fontFamily = UiSans,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.2.sp
                        )
                    }

                    BeepToggle(
                        checked = isEnabled,
                        onCheckedChange = { viewModel.toggleSoundEnabled(sound.id, it) }
                    )
                }
            }

            SurfaceCard {
                Column {
                    Text(
                        text = "ABOUT",
                        color = Stone,
                        fontFamily = UiSans,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = sound.description,
                        color = Ivory,
                        fontFamily = UiSans,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            SurfaceCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sensitivity",
                            color = Ivory,
                            fontFamily = UiSans,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(sliderValue * 100).toInt()}%",
                            color = Brass,
                            fontFamily = DisplaySerif,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Higher values wait for a stronger match before interrupting you.",
                        color = IvoryMuted,
                        fontFamily = UiSans,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            viewModel.setSoundThreshold(sound.id, sliderValue)
                        },
                        valueRange = 0.45f..0.95f,
                        colors = SliderDefaults.colors(
                            thumbColor = Brass,
                            activeTrackColor = Brass,
                            inactiveTrackColor = InkRaised
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("More alerts", color = Stone, fontFamily = UiSans, fontSize = 11.sp)
                        Text("Fewer alerts", color = Stone, fontFamily = UiSans, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            PrimaryButton(
                text = "Test this alert",
                leadingIcon = Icons.Outlined.PlayArrow,
                onClick = { viewModel.testTriggerAlert(sound) },
                modifier = Modifier.padding(bottom = 28.dp)
            )
        }
    }
}
