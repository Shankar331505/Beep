package com.shankar.beep.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shankar.beep.data.SoundCatalog
import com.shankar.beep.model.SoundCategory
import com.shankar.beep.ui.components.BeepTopBar
import com.shankar.beep.ui.components.CircularIconButton
import com.shankar.beep.ui.components.FilterChip
import com.shankar.beep.ui.components.HeroAmbientCard
import com.shankar.beep.ui.components.ListeningStatusPill
import com.shankar.beep.ui.components.SectionLabel
import com.shankar.beep.ui.components.SoundRowCard
import com.shankar.beep.ui.components.ActivityItemCard
import com.shankar.beep.ui.theme.Ink
import com.shankar.beep.ui.theme.IvoryMuted
import com.shankar.beep.ui.theme.UiSans
import java.util.concurrent.TimeUnit

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
    val categoryFilters = listOf("All", "Social", "Emergency", "Home")

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(permissionMessage) {
        permissionMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumePermissionMessage()
        }
    }

    val filteredSounds = remember(selectedCategoryFilter, userSettings.enabledSoundIds) {
        SoundCatalog.DEFAULT_CATALOG.filter { sound ->
            when (selectedCategoryFilter) {
                "Emergency" -> sound.category == SoundCategory.EMERGENCY
                "Home" -> sound.category == SoundCategory.DOMESTIC
                "Social" -> sound.category == SoundCategory.SOCIAL
                else -> true
            }
        }
    }

    val allFilteredEnabled = filteredSounds.isNotEmpty() &&
        filteredSounds.all { userSettings.enabledSoundIds.contains(it.id) }

    Scaffold(
        containerColor = Ink,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BeepTopBar(
                title = "Beep",
                leading = {
                    ListeningStatusPill(isListening = isListening)
                },
                trailing = {
                    CircularIconButton(
                        icon = Icons.Outlined.Tune,
                        contentDescription = "Preferences",
                        onClick = onNavigateToSettings
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            item {
                Column {
                    SectionLabel(text = "Listening mode")
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SoundCatalog.LISTENING_PRESETS) { preset ->
                            FilterChip(
                                text = preset.label,
                                isSelected = userSettings.activePresetName == preset.key,
                                onClick = { viewModel.applyPreset(preset.key) }
                            )
                        }
                    }
                }
            }

            item {
                Column {
                    SectionLabel(
                        text = "Sounds",
                        action = if (allFilteredEnabled) "Disable all" else "Enable all",
                        onAction = {
                            viewModel.setSoundsEnabled(
                                filteredSounds.map { it.id },
                                !allFilteredEnabled
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categoryFilters) { filter ->
                            FilterChip(
                                text = filter,
                                isSelected = selectedCategoryFilter == filter,
                                onClick = { selectedCategoryFilter = filter }
                            )
                        }
                    }
                }
            }

            items(filteredSounds, key = { it.id }) { sound ->
                val isEnabled = userSettings.enabledSoundIds.contains(sound.id)
                SoundRowCard(
                    sound = sound,
                    isEnabled = isEnabled,
                    onToggle = { viewModel.toggleSoundEnabled(sound.id, it) },
                    onClick = { onNavigateToSoundDetail(sound.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionLabel(
                    text = "Activity",
                    action = if (recentEvents.isNotEmpty()) "Clear" else null,
                    onAction = if (recentEvents.isNotEmpty()) {
                        { viewModel.clearHistory() }
                    } else {
                        null
                    }
                )
            }

            if (recentEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isListening) {
                                "Nothing heard yet. Beep is listening."
                            } else {
                                "History appears here once a sound is detected."
                            },
                            color = IvoryMuted,
                            fontFamily = UiSans,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(recentEvents, key = { it.id }) { event ->
                    ActivityItemCard(
                        event = event,
                        formattedTime = relativeTime(event.timestamp)
                    )
                }
            }
        }
    }
}

private fun relativeTime(timestamp: Long): String {
    val delta = System.currentTimeMillis() - timestamp
    val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
    val hours = TimeUnit.MILLISECONDS.toHours(delta)
    return when {
        minutes < 1 -> "Now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        else -> "${TimeUnit.MILLISECONDS.toDays(delta)}d"
    }
}
