package com.shankar.beep.audio

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

object DecibelMeter {

    /**
     * Calculates the decibel level from a 16-bit PCM audio buffer.
     * Normalized roughly between 20 dB (silence/quiet room) to 100+ dB (loud crash/yell).
     */
    fun calculateDecibel(buffer: ShortArray, readSize: Int): Float {
        if (readSize <= 0) return 0f

        var sumSquares = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble()
            sumSquares += sample * sample
        }

        val rms = sqrt(sumSquares / readSize)
        if (rms <= 0.0) return 0f

        // Reference amplitude for 16-bit PCM is 32767.0
        // Standard formula: 20 * log10(rms / reference) gives negative dBFS (-90 to 0)
        // Convert to human-readable positive dB SPL approximation (30 dB to 100 dB)
        val dbFs = 20.0 * log10(rms / 32767.0)
        val approxDbSpl = max(20.0, dbFs + 90.0).toFloat()

        return approxDbSpl
    }

    /**
     * Checks whether the current decibel level constitutes a sudden loud anomaly.
     */
    fun isSuddenLoudSpike(decibel: Float, thresholdDb: Float = 84f): Boolean {
        return decibel >= thresholdDb
    }
}
