package com.shankar.beep.detect

import com.shankar.beep.audio.Dsp

/**
 * Catches sudden loud noises without any AI. It keeps a 10-second picture of
 * how loud the room normally is (the median) and fires when a short burst is
 * both loud in absolute terms and far above that baseline. Adapting to the
 * room means a noisy street does not trigger constantly.
 */
class LoudnessWatcher(historySize: Int = 200) {

    private val history = FloatArray(historySize)
    private var filled = 0
    private var cursor = 0
    private var cachedBaseline = 40f
    private var sinceBaseline = 0

    val baselineDb: Float get() = cachedBaseline

    data class Spike(val levelDb: Float, val aboveBaseline: Float, val confidence: Float)

    fun onLevel(levelDb: Float, sensitivity: Float): Spike? {
        val baseline = cachedBaseline
        history[cursor] = levelDb
        cursor = (cursor + 1) % history.size
        if (filled < history.size) filled++
        if (++sinceBaseline >= 5 || filled < 20) {
            cachedBaseline = Dsp.median(history, filled)
            sinceBaseline = 0
        }
        if (filled < 20) return null // still learning the room
        val above = levelDb - baseline
        val absolute = absoluteThreshold(sensitivity)
        if (levelDb >= absolute && above >= MIN_JUMP_DB) {
            val confidence = (0.6f + (above - MIN_JUMP_DB) / 50f).coerceIn(0.6f, 1f)
            return Spike(levelDb, above, confidence)
        }
        return null
    }

    companion object {
        const val MIN_JUMP_DB = 18f

        /** Sensitivity 0..1 → 95 dB (least sensitive) down to 75 dB (most sensitive). */
        fun absoluteThreshold(sensitivity: Float): Float = 95f - 20f * sensitivity.coerceIn(0f, 1f)
    }
}
