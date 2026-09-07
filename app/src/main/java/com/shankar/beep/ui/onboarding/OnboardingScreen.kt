package com.shankar.beep.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shankar.beep.data.SoundCatalog
import com.shankar.beep.ui.components.BeepToggle
import com.shankar.beep.ui.components.BeepTopBar
import com.shankar.beep.ui.components.PrimaryButton
import com.shankar.beep.ui.components.SoundRowCard
import com.shankar.beep.ui.components.SurfaceCard
import com.shankar.beep.ui.theme.Ash
import com.shankar.beep.ui.theme.Brass
import com.shankar.beep.ui.theme.BrassWash
import com.shankar.beep.ui.theme.DisplaySerif
import com.shankar.beep.ui.theme.Hairline
import com.shankar.beep.ui.theme.Ink
import com.shankar.beep.ui.theme.Ivory
import com.shankar.beep.ui.theme.IvoryMuted
import com.shankar.beep.ui.theme.Ochre
import com.shankar.beep.ui.theme.Panel
import com.shankar.beep.ui.theme.Stone
import com.shankar.beep.ui.theme.UiSans

@Composable
fun OnboardingScreen(
    onOnboardingFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val selectedSoundIds by viewModel.selectedSoundIds.collectAsState()
    val headphoneAlertEnabled by viewModel.headphoneAlertEnabled.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val flashlightEnabled by viewModel.flashlightEnabled.collectAsState()

    var permissionsCheckKey by remember { mutableIntStateOf(0) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionsCheckKey++
    }

    val context = LocalContext.current
    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    val micGranted = remember(permissionsCheckKey) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }
    val notificationGranted = remember(permissionsCheckKey) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val cameraGranted = remember(permissionsCheckKey) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    Scaffold(
        containerColor = Ink,
        topBar = {
            BeepTopBar(
                title = "Beep",
                onBack = if (currentStep > 0) ({ viewModel.prevStep() }) else null
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (step in 0..3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (step <= currentStep) Brass else Hairline)
                    )
                }
            }

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it / 4 } + fadeIn() togetherWith
                            slideOutHorizontally { -it / 4 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 4 } + fadeIn() togetherWith
                            slideOutHorizontally { it / 4 } + fadeOut()
                    }
                },
                label = "onboarding_step",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    0 -> OnboardingWelcomeStep(
                        onNext = { viewModel.nextStep() },
                        onRequestPermissions = { permissionsLauncher.launch(requiredPermissions) },
                        micGranted = micGranted,
                        notificationGranted = notificationGranted,
                        cameraGranted = cameraGranted
                    )
                    1 -> OnboardingNameStep(
                        userName = userName,
                        onNameChange = { viewModel.updateUserName(it) },
                        onNext = { viewModel.nextStep() }
                    )
                    2 -> OnboardingSoundsStep(
                        selectedIds = selectedSoundIds,
                        onToggleSound = { viewModel.toggleSound(it) },
                        onNext = { viewModel.nextStep() }
                    )
                    3 -> OnboardingAlertsStep(
                        headphone = headphoneAlertEnabled,
                        onHeadphoneChange = { viewModel.setHeadphoneAlert(it) },
                        haptic = hapticEnabled,
                        onHapticChange = { viewModel.setHaptic(it) },
                        flashlight = flashlightEnabled,
                        onFlashlightChange = { viewModel.setFlashlight(it) },
                        onFinish = { viewModel.completeOnboarding(onOnboardingFinished) }
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingWelcomeStep(
    onNext: () -> Unit,
    onRequestPermissions: () -> Unit,
    micGranted: Boolean,
    notificationGranted: Boolean,
    cameraGranted: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Hear what your headphones hide.",
            color = Ivory,
            fontFamily = DisplaySerif,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Beep watches the room while you listen. Names, knocks, alarms — delivered into your ears. Audio never leaves the device.",
            color = IvoryMuted,
            fontFamily = UiSans,
            fontSize = 15.sp,
            lineHeight = 23.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(BrassWash),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Brass,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "On-device only",
                        color = Ivory,
                        fontFamily = UiSans,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Classification runs in memory. No cloud, no recordings stored.",
                        color = IvoryMuted,
                        fontFamily = UiSans,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SurfaceCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Access",
                        color = Ivory,
                        fontFamily = UiSans,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (micGranted && notificationGranted) "Ready" else "Required",
                        color = if (micGranted && notificationGranted) Brass else Ochre,
                        fontFamily = UiSans,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                PermissionStatusRow(
                    label = "Microphone",
                    detail = "Hears the room around you",
                    granted = micGranted
                )
                Spacer(modifier = Modifier.height(12.dp))
                PermissionStatusRow(
                    label = "Notifications",
                    detail = "Heads-up alerts when a sound is found",
                    granted = notificationGranted
                )
                Spacer(modifier = Modifier.height(12.dp))
                PermissionStatusRow(
                    label = "Camera light",
                    detail = "Optional. Used only for the torch strobe.",
                    granted = cameraGranted
                )

                if (!micGranted || !notificationGranted) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BrassWash)
                            .border(1.dp, Brass.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Brass.copy(alpha = 0.25f))
                            ) { onRequestPermissions() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Grant access",
                            color = Brass,
                            fontFamily = UiSans,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        PrimaryButton(
            text = "Continue",
            onClick = onNext
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PermissionStatusRow(
    label: String,
    detail: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (granted) Brass else Ash)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Ivory,
                fontFamily = UiSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = detail,
                color = IvoryMuted,
                fontFamily = UiSans,
                fontSize = 12.sp
            )
        }
        Text(
            text = if (granted) "Granted" else "Off",
            color = if (granted) Brass else Stone,
            fontFamily = UiSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun OnboardingNameStep(
    userName: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = "What should we listen for?",
                color = Ivory,
                fontFamily = DisplaySerif,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your name, or a nickname people actually use. Beep will catch it through music.",
                color = IvoryMuted,
                fontFamily = UiSans,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = userName,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                placeholder = {
                    Text("Your name", color = Stone, fontFamily = UiSans)
                },
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

            Spacer(modifier = Modifier.height(16.dp))

            SurfaceCard {
                val displayName = userName.trim().ifBlank { "your name" }
                Column {
                    Text(
                        text = "PHRASES",
                        color = Stone,
                        fontFamily = UiSans,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$displayName\nHey $displayName\nHello $displayName",
                        color = IvoryMuted,
                        fontFamily = UiSans,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)) {
            PrimaryButton(
                text = "Continue",
                onClick = onNext
            )
        }
    }
}

@Composable
fun OnboardingSoundsStep(
    selectedIds: Set<String>,
    onToggleSound: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Choose what matters.",
            color = Ivory,
            fontFamily = DisplaySerif,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Only these sounds will interrupt you.",
            color = IvoryMuted,
            fontFamily = UiSans,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(SoundCatalog.DEFAULT_CATALOG, key = { it.id }) { sound ->
                val isEnabled = selectedIds.contains(sound.id)
                SoundRowCard(
                    sound = sound,
                    isEnabled = isEnabled,
                    onToggle = { onToggleSound(sound.id) },
                    onClick = { onToggleSound(sound.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        PrimaryButton(
            text = "Continue · ${selectedIds.size} selected",
            onClick = onNext,
            modifier = Modifier.padding(bottom = 24.dp, top = 12.dp)
        )
    }
}

@Composable
fun OnboardingAlertsStep(
    headphone: Boolean,
    onHeadphoneChange: (Boolean) -> Unit,
    haptic: Boolean,
    onHapticChange: (Boolean) -> Unit,
    flashlight: Boolean,
    onFlashlightChange: (Boolean) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = "How should we reach you?",
                color = Ivory,
                fontFamily = DisplaySerif,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Each channel can work alone or together.",
                color = IvoryMuted,
                fontFamily = UiSans,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            AlertChannelCard(
                title = "Headphone chime",
                subtitle = "Ducks music and plays a short tone in-ear",
                checked = headphone,
                onCheckedChange = onHeadphoneChange
            )

            Spacer(modifier = Modifier.height(10.dp))

            AlertChannelCard(
                title = "Haptics",
                subtitle = "A distinct pattern for emergency, knock, and name",
                checked = haptic,
                onCheckedChange = onHapticChange
            )

            Spacer(modifier = Modifier.height(10.dp))

            AlertChannelCard(
                title = "Torch strobe",
                subtitle = "Brief flashes when the phone is face down",
                checked = flashlight,
                onCheckedChange = onFlashlightChange
            )
        }

        Column(modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)) {
            PrimaryButton(
                text = "Start listening",
                onClick = onFinish
            )
        }
    }
}

@Composable
fun AlertChannelCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(
                1.dp,
                if (checked) Brass.copy(alpha = 0.4f) else Hairline,
                RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Ivory,
                    fontFamily = UiSans,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = IvoryMuted,
                    fontFamily = UiSans,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            BeepToggle(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SleekAlertChannelCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    AlertChannelCard(
        title = title,
        subtitle = subtitle,
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}
