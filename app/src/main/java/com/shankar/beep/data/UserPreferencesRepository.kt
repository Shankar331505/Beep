package com.shankar.beep.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "beep_preferences")

data class UserSettings(
    val userName: String = "",
    val isOnboardingCompleted: Boolean = false,
    val isMonitoringActive: Boolean = false,
    val enabledSoundIds: Set<String> = setOf(
        SoundCatalog.SOUND_ID_NAME,
        SoundCatalog.SOUND_ID_CLAP,
        SoundCatalog.SOUND_ID_GLASS,
        SoundCatalog.SOUND_ID_KNOCK,
        SoundCatalog.SOUND_ID_DOORBELL,
        SoundCatalog.SOUND_ID_FIRE_ALARM,
        SoundCatalog.SOUND_ID_SIREN,
        SoundCatalog.SOUND_ID_SUDDEN_LOUD
    ),
    val soundThresholds: Map<String, Float> = emptyMap(),
    val isHeadphoneAudioAlertEnabled: Boolean = true,
    val isHapticVibrationEnabled: Boolean = true,
    val isFlashlightStrobeEnabled: Boolean = false,
    val activePresetName: String = "Balanced"
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val MONITORING_ACTIVE = booleanPreferencesKey("monitoring_active")
        val ENABLED_SOUNDS = stringSetPreferencesKey("enabled_sounds")
        val HEADPHONE_AUDIO = booleanPreferencesKey("headphone_audio")
        val HAPTIC_VIBRATION = booleanPreferencesKey("haptic_vibration")
        val FLASHLIGHT_STROBE = booleanPreferencesKey("flashlight_strobe")
        val ACTIVE_PRESET = stringPreferencesKey("active_preset")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val userName = preferences[PreferencesKeys.USER_NAME] ?: ""
            val onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
            val monitoringActive = preferences[PreferencesKeys.MONITORING_ACTIVE] ?: false
            val enabledSounds = preferences[PreferencesKeys.ENABLED_SOUNDS] ?: setOf(
                SoundCatalog.SOUND_ID_NAME,
                SoundCatalog.SOUND_ID_CLAP,
                SoundCatalog.SOUND_ID_GLASS,
                SoundCatalog.SOUND_ID_KNOCK,
                SoundCatalog.SOUND_ID_DOORBELL,
                SoundCatalog.SOUND_ID_FIRE_ALARM,
                SoundCatalog.SOUND_ID_SIREN,
                SoundCatalog.SOUND_ID_SUDDEN_LOUD
            )
            val headphoneAudio = preferences[PreferencesKeys.HEADPHONE_AUDIO] ?: true
            val hapticVibration = preferences[PreferencesKeys.HAPTIC_VIBRATION] ?: true
            val flashlightStrobe = preferences[PreferencesKeys.FLASHLIGHT_STROBE] ?: false
            val activePreset = preferences[PreferencesKeys.ACTIVE_PRESET] ?: "Balanced"

            // Extract per-sound thresholds
            val thresholds = mutableMapOf<String, Float>()
            for (sound in SoundCatalog.DEFAULT_CATALOG) {
                val key = floatPreferencesKey("threshold_${sound.id}")
                val value = preferences[key] ?: sound.defaultConfidenceThreshold
                thresholds[sound.id] = value
            }

            UserSettings(
                userName = userName,
                isOnboardingCompleted = onboardingCompleted,
                isMonitoringActive = monitoringActive,
                enabledSoundIds = enabledSounds,
                soundThresholds = thresholds,
                isHeadphoneAudioAlertEnabled = headphoneAudio,
                isHapticVibrationEnabled = hapticVibration,
                isFlashlightStrobeEnabled = flashlightStrobe,
                activePresetName = activePreset
            )
        }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name.trim()
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setMonitoringActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONITORING_ACTIVE] = active
        }
    }

    suspend fun toggleSoundEnabled(soundId: String, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.ENABLED_SOUNDS]?.toMutableSet() ?: mutableSetOf()
            if (enabled) {
                current.add(soundId)
            } else {
                current.remove(soundId)
            }
            preferences[PreferencesKeys.ENABLED_SOUNDS] = current
        }
    }

    suspend fun setEnabledSounds(soundIds: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLED_SOUNDS] = soundIds
        }
    }

    suspend fun setSoundThreshold(soundId: String, threshold: Float) {
        context.dataStore.edit { preferences ->
            val key = floatPreferencesKey("threshold_$soundId")
            preferences[key] = threshold
        }
    }

    suspend fun setAlertPreferences(headphoneAudio: Boolean, haptic: Boolean, flashlight: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HEADPHONE_AUDIO] = headphoneAudio
            preferences[PreferencesKeys.HAPTIC_VIBRATION] = haptic
            preferences[PreferencesKeys.FLASHLIGHT_STROBE] = flashlight
        }
    }

    suspend fun setActivePreset(presetName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_PRESET] = presetName
        }
    }
}
