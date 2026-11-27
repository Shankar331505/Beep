package com.shankar.beep.alert

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.shankar.beep.detect.Category
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Synthesises Beep's alert tones in code — no audio files. Each category has
 * its own melody, so with practice you can tell what happened by ear alone.
 */
object Chime {

    private const val RATE = 22_050

    /** (frequency Hz, duration ms); frequency 0 = rest. */
    fun melody(category: Category): List<Pair<Int, Int>> = when (category) {
        Category.SAFETY -> listOf(1319 to 150, 988 to 150, 1319 to 150, 988 to 150, 1319 to 150, 988 to 260)
        Category.PEOPLE -> listOf(659 to 170, 988 to 340)
        Category.HOME -> listOf(784 to 280, 622 to 460)
        Category.STREET -> listOf(1047 to 110, 0 to 60, 1047 to 110, 0 to 60, 1397 to 220)
        Category.CUSTOM -> listOf(523 to 130, 659 to 130, 784 to 300)
    }

    fun durationMs(category: Category): Int = melody(category).sumOf { it.second }

    fun render(category: Category): ShortArray {
        val notes = melody(category)
        val total = notes.sumOf { it.second } * RATE / 1000
        val pcm = ShortArray(total)
        var offset = 0
        for ((freq, ms) in notes) {
            val n = ms * RATE / 1000
            if (freq > 0) {
                for (i in 0 until n) {
                    val t = i.toDouble() / RATE
                    val attack = minOf(1.0, i / (0.006 * RATE))
                    val decay = exp(-t * 5.5)
                    // Fundamental plus soft overtones gives a bell-like, non-harsh tone.
                    val wave = sin(2 * PI * freq * t) * 0.72 +
                        sin(2 * PI * freq * 2 * t) * 0.18 +
                        sin(2 * PI * freq * 3 * t) * 0.06
                    val release = if (i > n - RATE / 200) (n - i) / (RATE / 200.0) else 1.0
                    pcm[offset + i] = (wave * attack * decay * release * 0.6 * Short.MAX_VALUE).toInt().toShort()
                }
            }
            offset += n
        }
        return pcm
    }

    /** Plays the melody once. Returns how long it lasts in ms. */
    fun play(category: Category): Int {
        val pcm = render(category)
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
        }.getOrNull() ?: return 0
        track.write(pcm, 0, pcm.size)
        track.setNotificationMarkerPosition(pcm.size)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack) { t.release() }
            override fun onPeriodicNotification(t: AudioTrack) {}
        })
        runCatching { track.play() }.onFailure { track.release() }
        return durationMs(category)
    }
}
