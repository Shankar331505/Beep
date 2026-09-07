package com.shankar.beep.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.shankar.beep.MainActivity
import com.shankar.beep.R
import com.shankar.beep.alerts.AlertManager
import com.shankar.beep.audio.AudioCaptureEngine
import com.shankar.beep.audio.DecibelMeter
import com.shankar.beep.audio.NameDetectorEngine
import com.shankar.beep.audio.YAMNetClassifier
import com.shankar.beep.data.DetectionHistoryRepository
import com.shankar.beep.data.SoundCatalog
import com.shankar.beep.data.UserPreferencesRepository
import com.shankar.beep.data.UserSettings
import com.shankar.beep.model.MonitoredSound
import com.shankar.beep.model.SoundCategory
import com.shankar.beep.model.SoundEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioMonitoringService : Service() {

    private val tag = "AudioMonitoringService"
    private val scope = CoroutineScope(Dispatchers.Default)

    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var alertManager: AlertManager
    private lateinit var yamnetClassifier: YAMNetClassifier
    private lateinit var nameDetectorEngine: NameDetectorEngine
    private lateinit var audioCaptureEngine: AudioCaptureEngine

    private var wakeLock: PowerManager.WakeLock? = null
    private var preferencesJob: Job? = null
    private var currentSettings = UserSettings()

    companion object {
        const val ACTION_START = "com.shankar.beep.action.START_MONITORING"
        const val ACTION_STOP = "com.shankar.beep.action.STOP_MONITORING"
        const val NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 1002

        private val _currentDecibel = MutableStateFlow(32f)
        val currentDecibel: StateFlow<Float> = _currentDecibel.asStateFlow()

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, AudioMonitoringService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AudioMonitoringService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        userPreferencesRepository = UserPreferencesRepository(this)
        alertManager = AlertManager(this)
        yamnetClassifier = YAMNetClassifier(this)

        nameDetectorEngine = NameDetectorEngine(this) { confidence ->
            handleSoundDetected(
                soundId = SoundCatalog.SOUND_ID_NAME,
                detectedName = "Your Name Called",
                category = SoundCategory.SOCIAL,
                confidence = confidence
            )
        }

        audioCaptureEngine = AudioCaptureEngine { buffer, readSize ->
            handleAudioBuffer(buffer, readSize)
        }

        acquireWakeLock()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                startMonitoring()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        _isServiceRunning.value = true

        preferencesJob?.cancel()
        preferencesJob = scope.launch {
            userPreferencesRepository.userSettingsFlow.collect { settings ->
                currentSettings = settings
                nameDetectorEngine.updateTargetName(settings.userName)
            }
        }

        audioCaptureEngine.startCapture()
        nameDetectorEngine.startListening()
        Log.i(tag, "Audio monitoring service started")
    }

    private fun stopMonitoring() {
        _isServiceRunning.value = false
        preferencesJob?.cancel()
        audioCaptureEngine.stopCapture()
        nameDetectorEngine.stopListening()
        releaseWakeLock()
        Log.i(tag, "Audio monitoring service stopped")
    }

    private fun handleAudioBuffer(buffer: ShortArray, readSize: Int) {
        val decibel = DecibelMeter.calculateDecibel(buffer, readSize)
        _currentDecibel.value = decibel

        // 1. Sudden loud impact detection
        if (currentSettings.enabledSoundIds.contains(SoundCatalog.SOUND_ID_SUDDEN_LOUD)) {
            val threshold = currentSettings.soundThresholds[SoundCatalog.SOUND_ID_SUDDEN_LOUD] ?: 0.65f
            val triggerDb = 75f + (threshold * 20f) // maps 0.5-0.9 to 85-93 dB
            if (DecibelMeter.isSuddenLoudSpike(decibel, triggerDb)) {
                handleSoundDetected(
                    soundId = SoundCatalog.SOUND_ID_SUDDEN_LOUD,
                    detectedName = "Sudden Loud Impact",
                    category = SoundCategory.EMERGENCY,
                    confidence = 0.95f
                )
            }
        }

        // 2. YAMNet Environmental Sound Classification
        val enabledSounds = SoundCatalog.DEFAULT_CATALOG.filter { sound ->
            currentSettings.enabledSoundIds.contains(sound.id) && sound.yamnetLabels.isNotEmpty()
        }.map { sound ->
            val userThreshold = currentSettings.soundThresholds[sound.id] ?: sound.defaultConfidenceThreshold
            sound.copy(confidenceThreshold = userThreshold)
        }

        if (enabledSounds.isNotEmpty()) {
            val results = yamnetClassifier.classifyAudioBuffer(buffer, readSize, enabledSounds)
            for (res in results) {
                handleSoundDetected(
                    soundId = res.monitoredSound.id,
                    detectedName = res.monitoredSound.displayName,
                    category = res.monitoredSound.category,
                    confidence = res.confidence
                )
            }
        }
    }

    private fun handleSoundDetected(
        soundId: String,
        detectedName: String,
        category: SoundCategory,
        confidence: Float
    ) {
        val sound = SoundCatalog.getSoundById(soundId) ?: return
        val currentDb = _currentDecibel.value

        val triggered = alertManager.triggerAlert(
            sound = sound,
            confidence = confidence,
            isHeadphoneAudioEnabled = currentSettings.isHeadphoneAudioAlertEnabled,
            isHapticEnabled = currentSettings.isHapticVibrationEnabled,
            isFlashlightEnabled = currentSettings.isFlashlightStrobeEnabled
        )

        if (triggered) {
            val event = SoundEvent(
                soundId = soundId,
                soundName = detectedName,
                category = category,
                confidence = confidence,
                decibel = currentDb
            )
            DetectionHistoryRepository.instance.addEvent(event)
            showHeadsUpAlertNotification(event)
        }
    }

    private fun showHeadsUpAlertNotification(event: SoundEvent) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alertNotification = NotificationCompat.Builder(this, getString(R.string.alert_notification_channel_id))
            .setSmallIcon(R.drawable.ic_stat_beep)
            .setContentTitle("Beep Alert: ${event.soundName}")
            .setContentText("Detected with ${(event.confidence * 100).toInt()}% confidence at ${event.decibel.toInt()} dB")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID, alertNotification)
    }

    private fun buildForegroundNotification(): Notification {
        val stopIntent = Intent(this, AudioMonitoringService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingMain = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, getString(R.string.service_notification_channel_id))
            .setSmallIcon(R.drawable.ic_stat_beep)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_desc))
            .setContentIntent(pendingMain)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(0, "Pause Listening", pendingStop)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Background Monitoring Channel (Silent ongoing)
            val serviceChannel = NotificationChannel(
                getString(R.string.service_notification_channel_id),
                getString(R.string.service_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active background sound listening state"
                setShowBadge(false)
            }

            // Urgent Sound Alert Channel (High priority heads-up)
            val alertChannel = NotificationChannel(
                getString(R.string.alert_notification_channel_id),
                getString(R.string.alert_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical heads-up sound alerts"
                enableVibration(true)
                setShowBadge(true)
            }

            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Beep::AudioMonitoringWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12 hours max safety limit
            }
        } catch (e: Exception) {
            Log.e(tag, "Error acquiring WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(tag, "Error releasing WakeLock", e)
        }
    }

    override fun onDestroy() {
        stopMonitoring()
        yamnetClassifier.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
