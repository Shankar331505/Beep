package com.shankar.beep.ui.dashboard

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shankar.beep.alerts.AlertManager
import com.shankar.beep.data.DetectionHistoryRepository
import com.shankar.beep.data.SoundCatalog
import com.shankar.beep.data.UserPreferencesRepository
import com.shankar.beep.data.UserSettings
import com.shankar.beep.model.MonitoredSound
import com.shankar.beep.model.SoundEvent
import com.shankar.beep.service.AudioMonitoringService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val alertManager = AlertManager(application)
    private val historyRepository = DetectionHistoryRepository.instance

    val userSettings: StateFlow<UserSettings> = userPreferencesRepository.userSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    val currentDecibel: StateFlow<Float> = AudioMonitoringService.currentDecibel
    val isServiceRunning: StateFlow<Boolean> = AudioMonitoringService.isServiceRunning
    val recentEvents: StateFlow<List<SoundEvent>> = historyRepository.events
    val lastAlert: StateFlow<SoundEvent?> = historyRepository.lastAlert

    private val _permissionMessage = MutableStateFlow<String?>(null)
    val permissionMessage: StateFlow<String?> = _permissionMessage.asStateFlow()

    fun toggleListening() {
        val context = getApplication<Application>()
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val currentlyRunning = isServiceRunning.value
        if (currentlyRunning) {
            AudioMonitoringService.stop(context)
            viewModelScope.launch {
                userPreferencesRepository.setMonitoringActive(false)
            }
        } else {
            if (!hasMicPermission) {
                _permissionMessage.value = "Microphone permission is required to listen for sounds. Grant it in system settings."
                return
            }
            AudioMonitoringService.start(context)
            viewModelScope.launch {
                userPreferencesRepository.setMonitoringActive(true)
            }
        }
    }

    fun consumePermissionMessage() {
        _permissionMessage.value = null
    }

    fun toggleSoundEnabled(soundId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.toggleSoundEnabled(soundId, isEnabled)
            userPreferencesRepository.setActivePreset("")
        }
    }

    fun setSoundsEnabled(soundIds: Collection<String>, enabled: Boolean) {
        viewModelScope.launch {
            val next = userSettings.value.enabledSoundIds.toMutableSet()
            if (enabled) next.addAll(soundIds) else next.removeAll(soundIds.toSet())
            userPreferencesRepository.setEnabledSounds(next)
            userPreferencesRepository.setActivePreset("")
        }
    }

    fun setSoundThreshold(soundId: String, threshold: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setSoundThreshold(soundId, threshold)
        }
    }

    fun applyPreset(presetName: String) {
        viewModelScope.launch {
            userPreferencesRepository.setActivePreset(presetName)
            userPreferencesRepository.setEnabledSounds(SoundCatalog.soundsForPreset(presetName))
        }
    }

    fun testTriggerAlert(sound: MonitoredSound) {
        val settings = userSettings.value
        alertManager.triggerAlert(
            sound = sound,
            confidence = 0.96f,
            isHeadphoneAudioEnabled = settings.isHeadphoneAudioAlertEnabled,
            isHapticEnabled = settings.isHapticVibrationEnabled,
            isFlashlightEnabled = settings.isFlashlightStrobeEnabled
        )
        historyRepository.addEvent(
            SoundEvent(
                soundId = sound.id,
                soundName = sound.displayName,
                category = sound.category,
                confidence = 0.96f,
                decibel = currentDecibel.value
            )
        )
    }

    fun dismissLastAlert() {
        historyRepository.clearAlert()
    }

    fun clearHistory() {
        historyRepository.clearHistory()
    }
}
