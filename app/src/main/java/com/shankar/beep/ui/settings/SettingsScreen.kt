package com.shankar.beep.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shankar.beep.data.SoundCatalog
import com.shankar.beep.data.UserPreferencesRepository
import com.shankar.beep.ui.components.CircularIconButton
import com.shankar.beep.ui.components.SleekCapsuleChip
import com.shankar.beep.ui.components.SleekPowerToggle
import com.shankar.beep.ui.dashboard.DashboardViewModel
import com.shankar.beep.ui.onboarding.SleekAlertChannelCard
import com.shankar.beep.ui.theme.*
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
                    text = "Preferences",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(44.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "Personal Identity",
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        scope.launch { userPreferencesRepository.setUserName(it) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    placeholder = { Text("Enter your name", color = TextSubtle) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MintPrimary,
                        unfocusedBorderColor = CardBorderDark,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = CardDark,
                        unfocusedContainerColor = CardDark
                    )
                )
            }

            item {
                Text(
                    text = "Listening Mode",
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "One-tap sound bundles for common situations",
                    color = TextMutedGrey,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoundCatalog.LISTENING_PRESETS.forEach { preset ->
                        val isSelected = userSettings.activePresetName == preset.key
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) MintSoftBg else CardDark)
                                .border(
                                    1.dp,
                                    if (isSelected) MintPrimary.copy(alpha = 0.6f) else CardBorderDark,
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable { viewModel.applyPreset(preset.key) }
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.label,
                                        color = if (isSelected) MintPrimary else TextWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = preset.description,
                                        color = TextMutedGrey,
                                        fontSize = 12.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MintPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Alert Delivery Channels",
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SleekAlertChannelCard(
                        title = "Headphone Audio Injection",
                        subtitle = "Ducks active music and plays chime directly in headphones",
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

                    SleekAlertChannelCard(
                        title = "Custom Haptic Patterns",
                        subtitle = "Distinct vibration patterns for emergency, knocks, and name",
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

                    SleekAlertChannelCard(
                        title = "Camera Flashlight Strobe",
                        subtitle = "Blinks LED torch when phone is face down on a desk",
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

            item {
                Text(
                    text = "Engine Architecture",
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardDark)
                        .border(1.dp, CardBorderDark, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "TensorFlow Lite YAMNet Audio Classifier",
                            color = MintPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AudioSet Neural Network • 521 Sound Classes\nSampling: 16 kHz Mono PCM, 0.975s Rolling Window\nZero Cloud Dependencies • 100% On-Device",
                            color = TextMutedGrey,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
