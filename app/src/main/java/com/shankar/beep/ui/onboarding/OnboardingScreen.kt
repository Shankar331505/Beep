package com.shankar.beep.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.shankar.beep.model.MonitoredSound
import com.shankar.beep.ui.components.*
import com.shankar.beep.ui.theme.*

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

    // Bump this key whenever the permission dialog returns so statuses recompute
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

    LaunchedEffect(Unit) {
        permissionsLauncher.launch(requiredPermissions)
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            // Reference Style Top Bar with Circular Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    CircularIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = { viewModel.prevStep() }
                    )
                } else {
                    Spacer(modifier = Modifier.size(44.dp))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            // Step indicator dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (step in 0..3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (step == currentStep) MintPrimary else CardBorderDark
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "onboarding_step"
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

/**
 * Step 0: Welcome with privacy card + permission status.
 */
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
        Column {
            Text(
                text = "Welcome to Beep",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Never miss what matters while wearing headphones. Get started on your ambient sound shield in a few simple steps.",
                color = TextMutedGrey,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy Assurance Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorderDark, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MintSoftBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MintPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "100% On-Device Privacy",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Audio is analyzed directly in phone memory. Zero cloud transmission.",
                            color = TextMutedGrey,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Permission status card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorderDark, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Permissions",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (micGranted && notificationGranted) "Ready" else "Action needed",
                            color = if (micGranted && notificationGranted) MintPrimary else AccentAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionStatusRow(
                        label = "Microphone",
                        detail = "Required — hears the sounds around you",
                        granted = micGranted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PermissionStatusRow(
                        label = "Notifications",
                        detail = "Required — heads-up sound alerts",
                        granted = notificationGranted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PermissionStatusRow(
                        label = "Camera (flashlight)",
                        detail = "Optional — only for the LED strobe alert",
                        granted = cameraGranted
                    )

                    if (!micGranted || !notificationGranted) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MintSoftBg)
                                .border(1.dp, MintPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .clickable { onRequestPermissions() }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Grant permissions",
                                color = MintPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        MintPillButton(
            text = "Get Started",
            onClick = onNext
        )
        Spacer(modifier = Modifier.height(30.dp))
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
                .size(10.dp)
                .clip(CircleShape)
                .background(if (granted) MintPrimary else AccentAmber)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                color = TextMutedGrey,
                fontSize = 12.sp
            )
        }
        Text(
            text = if (granted) "Granted" else "Denied",
            color = if (granted) MintPrimary else TextMutedGrey,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Step 1: Name Input (Styled like Screen 1 text inputs).
 */
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
        Column {
            Text(
                text = "Your Identity",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Enter your name or nickname. Beep will listen for people calling you even through loud music.",
                color = TextMutedGrey,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Sleek Text Field matching reference
            OutlinedTextField(
                value = userName,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                placeholder = { Text("Enter your name (e.g. Shankar)", color = TextSubtle) },
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

            Spacer(modifier = Modifier.height(20.dp))

            // Variations Info Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorderDark, RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column {
                    val displayName = if (userName.isBlank()) "Shankar" else userName.trim()
                    Text(
                        text = "Phrases detected automatically:",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• \"$displayName\"\n• \"Hey $displayName\"\n• \"Hello $displayName\"",
                        color = TextMutedGrey,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(bottom = 30.dp)) {
            MintPillButton(
                text = "Continue",
                onClick = onNext
            )
        }
    }
}

/**
 * Step 2: Choose Sounds (Sleek grid of cards matching reference).
 */
@Composable
fun OnboardingSoundsStep(
    selectedIds: Set<String>,
    onToggleSound: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Select Sounds",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Choose which environmental sounds should alert your headphones.",
                color = TextMutedGrey,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val chunked = SoundCatalog.DEFAULT_CATALOG.chunked(2)
                items(chunked) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (sound in pair) {
                            val isEnabled = selectedIds.contains(sound.id)
                            SoundGridCard(
                                sound = sound,
                                isEnabled = isEnabled,
                                onToggle = { onToggleSound(sound.id) },
                                onClick = { onToggleSound(sound.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        Column(modifier = Modifier.padding(bottom = 30.dp)) {
            MintPillButton(
                text = "Continue (${selectedIds.size} sounds)",
                onClick = onNext
            )
        }
    }
}

/**
 * Step 3: Alert Channels Setup.
 */
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
        Column {
            Text(
                text = "Alert Channels",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Configure how Beep notifies you when an event occurs.",
                color = TextMutedGrey,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Channel Rows
            SleekAlertChannelCard(
                title = "Headphone Audio Injection",
                subtitle = "Ducks active music and plays alert chime in headphones",
                checked = headphone,
                onCheckedChange = onHeadphoneChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            SleekAlertChannelCard(
                title = "Custom Haptic Patterns",
                subtitle = "Distinct vibration patterns for emergency, knocks, and name",
                checked = haptic,
                onCheckedChange = onHapticChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            SleekAlertChannelCard(
                title = "Camera Flashlight Strobe",
                subtitle = "Blinks LED torch when phone is face down on a desk",
                checked = flashlight,
                onCheckedChange = onFlashlightChange
            )
        }

        Column(modifier = Modifier.padding(bottom = 30.dp)) {
            MintPillButton(
                text = "Activate Beep",
                onClick = onFinish
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardDark)
            .border(
                1.dp,
                if (checked) MintPrimary.copy(alpha = 0.5f) else CardBorderDark,
                RoundedCornerShape(20.dp)
            )
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextMutedGrey,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            SleekPowerToggle(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
