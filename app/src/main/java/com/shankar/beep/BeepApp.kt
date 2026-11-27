package com.shankar.beep

import android.app.Application
import android.content.Context
import android.util.Log
import com.shankar.beep.alert.Alerter
import com.shankar.beep.alert.Notifier
import com.shankar.beep.audio.MicStream
import com.shankar.beep.data.EventLog
import com.shankar.beep.data.Settings
import com.shankar.beep.data.SettingsRepository
import com.shankar.beep.data.VoiceStore
import com.shankar.beep.detect.DetectionEngine
import com.shankar.beep.detect.EngineConfig
import com.shankar.beep.detect.NameProfile
import com.shankar.beep.ml.SoundClassifier
import com.shankar.beep.ml.YamNet
import com.shankar.beep.service.ListeningHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class BeepApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier(this).createChannels()
    }
}

/** Hand-rolled dependency container: one instance of each long-lived object. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository = SettingsRepository(context)
    val eventLog = EventLog(context)
    val voiceStore = VoiceStore(context)
    val hub = ListeningHub()
    val alerter = Alerter(context, hub)
    val mic = MicStream(context)

    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(appScope, SharingStarted.Eagerly, null)

    /** Live engine configuration derived from settings + stored voice data. */
    val engineConfig: StateFlow<EngineConfig> = combine(
        settingsRepository.settings, voiceStore.nameTemplates, voiceStore.customSounds,
    ) { s, templates, custom ->
        EngineConfig(
            enabled = s.enabled,
            sensitivity = s.sensitivity,
            nameProfile = NameProfile(s.userName, templates).takeIf { it.isReady },
            customSounds = custom,
        )
    }.stateIn(appScope, SharingStarted.Eagerly, EngineConfig())

    private val classifier: SoundClassifier? by lazy {
        runCatching { YamNet(context) }
            .onFailure { Log.e("Beep", "Could not load YAMNet", it) }
            .getOrNull()
    }

    val engine: DetectionEngine by lazy { DetectionEngine(classifier) }

    /** For the "teach a sound" flow, which needs raw YAMNet scores. */
    fun classifierOrNull(): SoundClassifier? = classifier
}

val Context.container: AppContainer get() = (applicationContext as BeepApp).container
