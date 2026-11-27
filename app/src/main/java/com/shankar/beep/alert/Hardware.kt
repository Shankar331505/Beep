package com.shankar.beep.alert

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.shankar.beep.detect.Category
import kotlinx.coroutines.delay

/** Distinct vibration "words" per category, so you can feel what happened. */
class Haptics(context: Context) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun pattern(category: Category): Pair<LongArray, IntArray> = when (category) {
        // Long urgent triple — impossible to miss.
        Category.SAFETY -> longArrayOf(0, 400, 120, 400, 120, 700) to intArrayOf(0, 255, 0, 255, 0, 255)
        // A friendly tap-tap, like someone touching your shoulder.
        Category.PEOPLE -> longArrayOf(0, 120, 110, 220) to intArrayOf(0, 180, 0, 230)
        // Knock-knock.
        Category.HOME -> longArrayOf(0, 80, 90, 80, 300, 80, 90, 80) to intArrayOf(0, 200, 0, 200, 0, 200, 0, 200)
        // Quick buzz-buzz, like a horn.
        Category.STREET -> longArrayOf(0, 250, 100, 250) to intArrayOf(0, 255, 0, 255)
        // A rising ripple.
        Category.CUSTOM -> longArrayOf(0, 90, 60, 120, 60, 160) to intArrayOf(0, 120, 0, 180, 0, 240)
    }

    fun play(category: Category) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val (timings, amplitudes) = pattern(category)
        val effect = if (v.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(timings, -1)
        }
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
        @Suppress("DEPRECATION")
        runCatching { v.vibrate(effect, attrs) }
    }
}

/** Strobes the camera flash — visible from across a dark room. Needs no camera permission. */
class Torch(context: Context) {

    private val cameras = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val cameraId: String? = runCatching {
        cameras?.cameraIdList?.firstOrNull { id ->
            val c = cameras.getCameraCharacteristics(id)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }.getOrNull()

    val available: Boolean get() = cameraId != null

    suspend fun strobe(category: Category) {
        val id = cameraId ?: return
        val pulses = if (category == Category.SAFETY) 8 else 3
        try {
            repeat(pulses) {
                cameras?.setTorchMode(id, true)
                delay(90)
                cameras?.setTorchMode(id, false)
                delay(110)
            }
        } catch (_: Exception) {
            // The camera may be in use by another app; the flash is a nice-to-have.
        } finally {
            runCatching { cameras?.setTorchMode(id, false) }
        }
    }
}

/**
 * Pauses whatever is playing by briefly taking transient audio focus.
 * Well-behaved players (Spotify, YouTube, podcasts...) pause on transient
 * focus loss and resume automatically once Beep gives focus back.
 */
class MediaPauser(context: Context) {

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var request: AudioFocusRequest? = null

    val isMediaPlaying: Boolean get() = audio.isMusicActive

    /** Whether headphones or earbuds are connected (wired or Bluetooth). */
    fun headphonesConnected(): Boolean =
        audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
            it.type in setOf(
                android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET,
                android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                android.media.AudioDeviceInfo.TYPE_USB_HEADSET,
            ) || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET)
        }

    @Synchronized
    fun pause(): Boolean {
        if (request != null) return true
        if (!audio.isMusicActive) return false
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener { }
            .build()
        val granted = audio.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (granted) request = req
        return granted
    }

    @Synchronized
    fun resume() {
        request?.let { audio.abandonAudioFocusRequest(it) }
        request = null
    }
}
