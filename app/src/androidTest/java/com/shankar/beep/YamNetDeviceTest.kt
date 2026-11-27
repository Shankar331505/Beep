package com.shankar.beep

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shankar.beep.detect.DetectionEngine
import com.shankar.beep.detect.EngineConfig
import com.shankar.beep.detect.Fingerprint
import com.shankar.beep.ml.YamNet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Runs the real YAMNet model on the device to prove the on-device pipeline works end to end. */
@RunWith(AndroidJUnit4::class)
class YamNetDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val yamnet = YamNet(context)

    private fun top(scores: FloatArray) = yamnet.labels[scores.indices.maxBy { scores[it] }]

    @Test
    fun modelLoadsWithAllLabels() {
        assertEquals(521, yamnet.labels.size)
        assertEquals("Speech", yamnet.labels[0])
        assertEquals(15_600, yamnet.windowSize)
    }

    @Test
    fun silenceIsSilence() {
        val scores = yamnet.classify(FloatArray(YamNet.WINDOW))
        assertEquals(521, scores.size)
        assertTrue(scores.all { it in 0f..1f })
        assertEquals("Silence", top(scores))
    }

    @Test
    fun soundIsNotMistakenForSilence() {
        // A 2 kHz tone switched on and off every 120 ms.
        val window = FloatArray(YamNet.WINDOW) { i ->
            if ((i / 1920) % 2 == 0) (0.5 * sin(2 * PI * 2000 * i / 16_000)).toFloat() else 0f
        }
        val scores = yamnet.classify(window)
        val best = scores.indices.maxBy { scores[it] }
        android.util.Log.i("YamNetDeviceTest", "Top for beep tone: ${yamnet.labels[best]} (${scores[best]})")
        assertTrue(yamnet.labels[best] != "Silence")
        assertTrue(scores[best] > 0.1f)
    }

    @Test
    fun fingerprintsDistinguishSounds() {
        val rnd = Random(3)
        val tone = FloatArray(YamNet.WINDOW) { i -> (0.4 * sin(2 * PI * 1500 * i / 16_000)).toFloat() }
        val noise = FloatArray(YamNet.WINDOW) { (rnd.nextFloat() - 0.5f) * 0.6f }
        val a = Fingerprint.of(yamnet.classify(tone), tone)
        val b = Fingerprint.of(yamnet.classify(noise), noise)
        val sound = com.shankar.beep.detect.CustomSound("custom:t", "Tone", "bell", listOf(a), 0)
        assertTrue(Fingerprint.similarity(a, sound) > 0.99f)
        assertTrue(Fingerprint.similarity(b, sound) < Fingerprint.threshold(0.5f))
    }

    @Test
    fun engineRunsTheRealModelInTime() {
        val engine = DetectionEngine(yamnet)
        engine.config = EngineConfig(enabled = emptySet())
        val chunk = FloatArray(800) { i -> (0.2 * sin(2 * PI * 600 * i / 16_000)).toFloat() }
        val start = System.nanoTime()
        var classified = 0
        repeat(100) { if (engine.process(chunk).hearing != null) classified++ } // 5 s of audio
        val ms = (System.nanoTime() - start) / 1_000_000
        android.util.Log.i("YamNetDeviceTest", "5 s of audio processed in $ms ms, $classified classifications")
        assertTrue(classified >= 8)
        assertTrue("too slow: $ms ms for 5 s of audio", ms < 5_000)
    }
}
