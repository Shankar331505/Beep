package com.shankar.beep.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shankar.beep.AppContainer
import com.shankar.beep.container
import com.shankar.beep.data.LoggedEvent
import com.shankar.beep.data.Settings
import com.shankar.beep.detect.CustomSound
import com.shankar.beep.detect.Detection
import com.shankar.beep.detect.Mode
import com.shankar.beep.detect.SoundCatalog
import com.shankar.beep.detect.SoundSpec
import com.shankar.beep.service.LiveState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Builds a ViewModel with access to the app container. */
@Composable
inline fun <reified VM : ViewModel> beepViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = LocalContext.current.container
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}

class MainViewModel(private val c: AppContainer) : ViewModel() {

    val settings: StateFlow<Settings?> = c.settings
    val live: StateFlow<LiveState> = c.hub.live
    val bands: SharedFlow<FloatArray> = c.hub.bands
    val events: StateFlow<List<LoggedEvent>> = c.eventLog.events
    val customSounds: StateFlow<List<CustomSound>> = c.voiceStore.customSounds
    val activeAlert: StateFlow<Detection?> = c.hub.activeAlert
    val nameSampleCount: StateFlow<Int> = c.voiceStore.nameTemplates
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val hasFlash: Boolean get() = c.alerter.hasFlash

    fun allSpecs(custom: List<CustomSound>): List<SoundSpec> = SoundCatalog.all + custom.map(SoundCatalog::customSpec)

    fun setMode(mode: Mode) = viewModelScope.launch {
        c.settingsRepository.applyMode(mode, customSounds.value.map { it.id })
    }

    fun setEnabled(id: String, on: Boolean) = viewModelScope.launch { c.settingsRepository.setEnabled(id, on) }

    fun setSensitivity(id: String, value: Float) = viewModelScope.launch { c.settingsRepository.setSensitivity(id, value) }

    fun updateSettings(transform: (Settings) -> Settings) = viewModelScope.launch { c.settingsRepository.update(transform) }

    fun clearHistory() = c.eventLog.clear()

    fun deleteCustom(id: String) = viewModelScope.launch {
        c.voiceStore.deleteCustomSound(id)
        c.settingsRepository.removeSound(id)
    }

    fun dismissAlert() = c.alerter.dismiss()

    fun snooze(id: String) {
        c.engine.snooze(id, 10)
        c.alerter.dismiss()
    }

    fun completeOnboarding(mode: Mode) = viewModelScope.launch {
        c.settingsRepository.applyMode(mode, customSounds.value.map { it.id })
        c.settingsRepository.completeOnboarding()
    }

    /** Fires a realistic sample alert so the user can feel what each channel does. */
    fun testAlert(spec: SoundSpec = SoundCatalog.find("doorbell")!!) {
        val s = settings.value ?: return
        c.alerter.fire(Detection(spec, 0.92f, 64f, System.currentTimeMillis()), s)
    }
}
