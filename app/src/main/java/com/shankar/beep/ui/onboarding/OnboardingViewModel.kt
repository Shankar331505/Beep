package com.shankar.beep.ui.onboarding

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shankar.beep.data.SoundCatalog
import com.shankar.beep.data.UserPreferencesRepository
import com.shankar.beep.service.AudioMonitoringService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _selectedSoundIds = MutableStateFlow<Set<String>>(
        setOf(
            SoundCatalog.SOUND_ID_NAME,
            SoundCatalog.SOUND_ID_CLAP,
            SoundCatalog.SOUND_ID_GLASS,
            SoundCatalog.SOUND_ID_KNOCK,
            SoundCatalog.SOUND_ID_FIRE_ALARM,
            SoundCatalog.SOUND_ID_SIREN,
            SoundCatalog.SOUND_ID_SUDDEN_LOUD
        )
    )
    val selectedSoundIds: StateFlow<Set<String>> = _selectedSoundIds.asStateFlow()

    private val _headphoneAlertEnabled = MutableStateFlow(true)
    val headphoneAlertEnabled: StateFlow<Boolean> = _headphoneAlertEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val _flashlightEnabled = MutableStateFlow(false)
    val flashlightEnabled: StateFlow<Boolean> = _flashlightEnabled.asStateFlow()

    fun setStep(step: Int) {
        _currentStep.value = step
    }

    fun nextStep() {
        _currentStep.value = (_currentStep.value + 1).coerceAtMost(3)
    }

    fun prevStep() {
        _currentStep.value = (_currentStep.value - 1).coerceAtLeast(0)
    }

    fun updateUserName(name: String) {
        _userName.value = name
    }

    fun toggleSound(soundId: String) {
        val current = _selectedSoundIds.value.toMutableSet()
        if (current.contains(soundId)) {
            current.remove(soundId)
        } else {
            current.add(soundId)
        }
        _selectedSoundIds.value = current
    }

    fun selectAll() {
        _selectedSoundIds.value = SoundCatalog.DEFAULT_CATALOG.map { it.id }.toSet()
    }

    fun setHeadphoneAlert(enabled: Boolean) {
        _headphoneAlertEnabled.value = enabled
    }

    fun setHaptic(enabled: Boolean) {
        _hapticEnabled.value = enabled
    }

    fun setFlashlight(enabled: Boolean) {
        _flashlightEnabled.value = enabled
    }

    fun completeOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.setUserName(_userName.value.ifBlank { "User" })
            userPreferencesRepository.setEnabledSounds(_selectedSoundIds.value)
            userPreferencesRepository.setAlertPreferences(
                headphoneAudio = _headphoneAlertEnabled.value,
                haptic = _hapticEnabled.value,
                flashlight = _flashlightEnabled.value
            )
            userPreferencesRepository.setOnboardingCompleted(true)

            // "Activate Beep" really activates: start listening right away if we can
            val application = getApplication<Application>()
            val hasMicPermission = ContextCompat.checkSelfPermission(
                application,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasMicPermission) {
                AudioMonitoringService.start(application)
                userPreferencesRepository.setMonitoringActive(true)
            }

            onFinished()
        }
    }
}
