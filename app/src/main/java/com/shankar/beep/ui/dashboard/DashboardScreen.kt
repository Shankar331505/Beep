package com.shankar.beep.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shankar.beep.data.SoundCatalog
import com.shankar.beep.model.SoundCategory
import com.shankar.beep.ui.components.*
import com.shankar.beep.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToSoundDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val isListening by viewModel.isServiceRunning.collectAsState()
    val currentDecibel by viewModel.currentDecibel.collectAsState()
    val lastAlert by viewModel.lastAlert.collectAsState()
    val recentEvents by viewModel.recentEvents.collectAsState()
    val permissionMessage by viewModel.permissionMessage.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf("All") }
    val categoryFilters = listOf("All", "Social", "Emergency", "Domestic")

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(permissionMessage) {
        permissionMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumePermissionMessage()
        }
    }

    // Filter sounds
    val filteredSounds = remember(selectedCategoryFilter, userSettings.enabledSoundIds) {
        SoundCatalog.DEFAULT_CATALOG.filter { sound ->
            when (selectedCategoryFilter) {
                "Emergency" -> sound.category == SoundCategory.EMERGENCY
                "Domestic" -> sound.category == SoundCategory.DOMESTIC
                "Social" -> sound.category == SoundCategory.SOCIAL
                else -> true
            }
        }
    }

    Scaffold(
        containerColor = BgDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Centered title with live status on the left and settings on the right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                ListeningStatusPill(
                    isListening = isListening,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                Text(
                    text = "Beep",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )

                CircularIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    onClick = onNavigateToSettings,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Hero card: decibel + waveform + power control
            item {
                HeroAmbientCard(
                    decibel = currentDecibel,
                    isListening = isListening,
                    activeSoundsCount = userSettings.enabledSoundIds.size,
                    totalDetections = recentEvents.size,
                    lastAlertName = lastAlert?.soundName,
                    onToggleListening = { viewModel.toggleListening() }
                )
            }

            // 2. Listening presets (one-tap sound bundles)
            item {
                Column {
                    Text(
                        text = "Listening Mode",
                        color = TextMutedGrey,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SoundCatalog.LISTENING_PRESETS) { preset ->
                            SleekCapsuleChip(
                                text = preset.label,
                                isSelected = userSettings.activePresetName == preset.key,
                                onClick = { viewModel.applyPreset(preset.key) }
                            )
                        }
                    }
                }
            }

            // 3. Horizontal category chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoryFilters) { filter ->
                        SleekCapsuleChip(
                            text = filter,
                            isSelected = selectedCategoryFilter == filter,
                            onClick = { selectedCategoryFilter = filter }
                        )
                    }
                }
            }

            // 4. Section Title: "Sounds" + "Enable all"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sounds",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Enable all",
                        color = MintPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            for (sound in SoundCatalog.DEFAULT_CATALOG) {
                                viewModel.toggleSoundEnabled(sound.id, true)
                            }
                        }
                    )
                }
            }

            // 5. 2x2 Grid of Sound Cards
            val chunkedSounds = filteredSounds.chunked(2)
            items(chunkedSounds) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    for (sound in pair) {
                        val isEnabled = userSettings.enabledSoundIds.contains(sound.id)
                        SoundGridCard(
                            sound = sound,
                            isEnabled = isEnabled,
                            onToggle = { viewModel.toggleSoundEnabled(sound.id, it) },
                            onClick = { onNavigateToSoundDetail(sound.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 6. Activity Timeline
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (recentEvents.isNotEmpty()) {
                        Text(
                            text = "Clear history",
                            color = TextMutedGrey,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { viewModel.clearHistory() }
                        )
                    }
                }
            }

            if (recentEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No sound detections yet. Listening in background...",
                            color = TextMutedGrey,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                items(recentEvents) { event ->
                    val formatted = timeFormat.format(Date(event.timestamp))
                    ActivityItemCard(
                        event = event,
                        formattedTime = formatted
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}