package com.shankar.beep

import com.shankar.beep.audio.DecibelMeter
import org.junit.Assert.*
import org.junit.Test

class DecibelMeterTest {

    @Test
    fun testSilenceProducesZeroOrLowDecibel() {
        val silentBuffer = ShortArray(1600) { 0 }
        val db = DecibelMeter.calculateDecibel(silentBuffer, silentBuffer.size)
        assertEquals(0f, db, 0.01f)
    }

    @Test
    fun testLoudAudioProducesHigherDecibel() {
        // Full-scale sine wave
        val loudBuffer = ShortArray(1600) { (32000 * kotlin.math.sin(it * 0.1)).toInt().toShort() }
        val db = DecibelMeter.calculateDecibel(loudBuffer, loudBuffer.size)
        assertTrue("Loud audio should be > 75 dB, got $db", db > 75f)
    }

    @Test
    fun testSuddenLoudSpikeDetection() {
        assertFalse(DecibelMeter.isSuddenLoudSpike(60f, 85f))
        assertTrue(DecibelMeter.isSuddenLoudSpike(88f, 85f))
    }
}
