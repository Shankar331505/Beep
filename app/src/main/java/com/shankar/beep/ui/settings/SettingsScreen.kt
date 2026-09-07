package com.shankar.beep.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shankar.beep.data.SoundCatalog
import com.shankar.beep.data.UserPreferencesRepository
import com.shankar.beep.ui.components.BeepTopBar
import com.shankar.beep.ui.components.SectionLabel
import com.shankar.beep.ui.components.SurfaceCard
import com.shankar.beep.ui.dashboard.DashboardViewModel
import com.shankar.beep.ui.onboarding.AlertChannelCard
import com.shankar.beep.ui.theme.Brass
import com.shankar.beep.ui.theme.Hairline
import com.shankar.beep.ui.theme.Ink
import com.shankar.beep.ui.theme.Ivory
import com.shankar.beep.ui.theme.IvoryMuted
import com.shankar.beep.ui.theme.Panel
import com.shankar.beep.ui.theme.Stone
import com.shankar.beep.ui.theme.UiSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel,
    userPreferencesRepository: UserPreferencesRepository
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val scope = rememberCoroutineScope()

    var nameInput by remember(userSettings.userName) { mutableStateOf(userSettings.userName) }

    LaunchedEffect(nameInput) {
        delay(400)
        if (nameInput.trim() != userSettings.userName) {
            userPreferencesRepository.setUserName(nameInput)
        }
    }

    Scaffold(
        containerColor = Ink,
        topBar = {
            BeepTopBar(
                title = "Preferences",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                Column {
                    SectionLabel(text = "Identity")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        placeholder = { Text("Your name", color = Stone, fontFamily = UiSans) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Brass,
                            unfocusedBorderColor = Hairline,
                            focusedTextColor = Ivory,
                            unfocusedTextColor = Ivory,
                            cursorColor = Brass,
                            focusedContainerColor = Panel,
                            unfocusedContainerColor = Panel
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Used to detect when someone calls you.",
                        color = Stone,
                        fontFamily = UiSans,
                        fontSize = 12.sp
                    )
                }
            }

            item {
                Column {
                    SectionLabel(text = "Listening mode")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "One-tap bundles for common rooms.",
                        color = IvoryMuted,
                        fontFamily = UiSans,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SoundCatalog.LISTENING_PRESETS.forEach { preset ->
                            val isSelected = userSettings.activePresetName == preset.key
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Panel)
                                    .border(
                                        1.dp,
                                        if (isSelected) Brass.copy(alpha = 0.45f) else Hairline,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(color = Brass.copy(alpha = 0.2f))
                                    ) { viewModel.applyPreset(preset.key) }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.label,
                                            color = if (isSelected) Brass else Ivory,
                                            fontFamily = UiSans,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = preset.description,
                                            color = IvoryMuted,
                                            fontFamily = UiSans,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Brass)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    SectionLabel(text = "Alert channels")
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AlertChannelCard(
                            title = "Headphone chime",
                            subtitle = "Ducks music and plays a tone in-ear",
                            checked = userSettings.isHeadphoneAudioAlertEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    userPreferencesRepository.setAlertPreferences(
                                        headphoneAudio = checked,
                                        haptic = userSettings.isHapticVibrationEnabled,
                                        flashlight = userSettings.isFlashlightStrobeEnabled
                                    )
                                }
                            }
                        )

                        AlertChannelCard(
                            title = "Haptics",
                            subtitle = "Distinct patterns for emergency, knock, and name",
                            checked = userSettings.isHapticVibrationEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    userPreferencesRepository.setAlertPreferences(
                                        headphoneAudio = userSettings.isHeadphoneAudioAlertEnabled,
                                        haptic = checked,
                                        flashlight = userSettings.isFlashlightStrobeEnabled
                                    )
                                }
                            }
                        )

                        AlertChannelCard(
                            title = "Torch strobe",
                            subtitle = "Brief flashes when the phone is face down",
                            checked = userSettings.isFlashlightStrobeEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    userPreferencesRepository.setAlertPreferences(
                                        headphoneAudio = userSettings.isHeadphoneAudioAlertEnabled,
                                        haptic = userSettings.isHapticVibrationEnabled,
                                        flashlight = checked
                                    )
                                }
                            }
                        )
                    }
                }
            }

            item {
                Column {
                    SectionLabel(text = "Engine")
                    Spacer(modifier = Modifier.height(10.dp))
                    SurfaceCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "YAMNet, on device",
                                color = Ivory,
                                fontFamily = UiSans,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "AudioSet classifier · 521 classes\n16 kHz mono · 0.975s rolling window\nNo network. Nothing is uploaded.",
                                color = IvoryMuted,
                                fontFamily = UiSans,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
