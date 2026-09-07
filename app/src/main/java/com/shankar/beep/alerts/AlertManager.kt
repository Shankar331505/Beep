package com.shankar.beep.alerts

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.shankar.beep.R
import com.shankar.beep.model.MonitoredSound
import com.shankar.beep.model.SoundCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AlertManager(private val context: Context) {

    private val tag = "AlertManager"
    private val scope = CoroutineScope(Dispatchers.Default)

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    // Cooldown tracking per sound ID
    private val lastTriggeredMap = ConcurrentHashMap<String, Long>()

    /**
     * Dispatches multi-channel notification based on user configuration.
     * Returns true if alert was triggered, false if suppressed by cooldown.
     */
    fun triggerAlert(
        sound: MonitoredSound,
        confidence: Float,
        isHeadphoneAudioEnabled: Boolean,
        isHapticEnabled: Boolean,
        isFlashlightEnabled: Boolean
    ): Boolean {
        val now = System.currentTimeMillis()
        val cooldownMillis = sound.cooldownSeconds * 1000L
        val lastTriggered = lastTriggeredMap[sound.id] ?: 0L

        if (now - lastTriggered < cooldownMillis) {
            Log.d(tag, "Suppressed alert for '${sound.displayName}' due to cooldown")
            return false
        }

        lastTriggeredMap[sound.id] = now
        Log.i(tag, "Triggering Alert for: ${sound.displayName} (${(confidence * 100).toInt()}%)")

        // 1. Headphone Audio Injection with Audio Ducking
        if (isHeadphoneAudioEnabled) {
            playHeadphoneAudioAlert(sound)
        }

        // 2. Custom Vibration Haptic Pattern
        if (isHapticEnabled) {
            triggerHapticPattern(sound.category)
        }

        // 3. Camera Flashlight Torch Strobe
        if (isFlashlightEnabled) {
            triggerFlashlightStrobe(sound.category)
        }

        return true
    }

    private fun playHeadphoneAudioAlert(sound: MonitoredSound) {
        val toneRes = sound.alertToneRes ?: when (sound.category) {
            SoundCategory.EMERGENCY -> R.raw.alert_emergency
            SoundCategory.SOCIAL -> R.raw.alert_name
            SoundCategory.DOMESTIC -> R.raw.alert_chime
        }

        scope.launch(Dispatchers.IO) {
            var focusRequest: AudioFocusRequest? = null
            var mediaPlayer: MediaPlayer? = null

            try {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                // Request transient audio focus with ducking so current music volume dips
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(playbackAttributes)
                        .setAcceptsDelayedFocusGain(false)
                        .setWillPauseWhenDucked(false)
                        .build()

                    audioManager?.requestAudioFocus(focusRequest)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager?.requestAudioFocus(
                        null,
                        AudioManager.STREAM_NOTIFICATION,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                    )
                }

                mediaPlayer = MediaPlayer.create(context, toneRes)
                mediaPlayer?.setAudioAttributes(playbackAttributes)
                mediaPlayer?.setOnCompletionListener { mp ->
                    mp.release()
                    // Restore music volume
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                        audioManager?.abandonAudioFocusRequest(focusRequest)
                    } else {
                        @Suppress("DEPRECATION")
                        audioManager?.abandonAudioFocus(null)
                    }
                }
                mediaPlayer?.start()
            } catch (e: Exception) {
                Log.e(tag, "Error playing alert tone", e)
                mediaPlayer?.release()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                    audioManager?.abandonAudioFocusRequest(focusRequest)
                }
            }
        }
    }

    private fun triggerHapticPattern(category: SoundCategory) {
        try {
            val timings: LongArray
            val amplitudes: IntArray

            when (category) {
                SoundCategory.EMERGENCY -> {
                    // Urgent fast triplet pulse
                    timings = longArrayOf(0, 180, 80, 180, 80, 300)
                    amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                }
                SoundCategory.SOCIAL -> {
                    // Friendly double tap
                    timings = longArrayOf(0, 140, 100, 200)
                    amplitudes = intArrayOf(0, 200, 0, 220)
                }
                SoundCategory.DOMESTIC -> {
                    // Rhythmic double knock
                    timings = longArrayOf(0, 90, 80, 90)
                    amplitudes = intArrayOf(0, 180, 0, 180)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(timings, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error triggering haptic pattern", e)
        }
    }

    private fun triggerFlashlightStrobe(category: SoundCategory) {
        val manager = cameraManager ?: return

        scope.launch(Dispatchers.IO) {
            try {
                val cameraIds = manager.cameraIdList
                var rearCameraId: String? = null
                for (id in cameraIds) {
                    val chars = manager.getCameraCharacteristics(id)
                    val hasFlash = chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    if (hasFlash) {
                        rearCameraId = id
                        break
                    }
                }

                val camId = rearCameraId ?: return@launch
                val flashes = if (category == SoundCategory.EMERGENCY) 4 else 2

                for (i in 0 until flashes) {
                    manager.setTorchMode(camId, true)
                    delay(70)
                    manager.setTorchMode(camId, false)
                    delay(70)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error toggling flashlight strobe", e)
            }
        }
    }
}
