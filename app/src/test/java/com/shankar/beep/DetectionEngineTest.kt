package com.shankar.beep

import com.shankar.beep.audio.Dsp
import com.shankar.beep.detect.DetectionEngine
import com.shankar.beep.detect.EngineConfig
import com.shankar.beep.detect.LoudnessWatcher
import com.shankar.beep.detect.SoundCatalog
import com.shankar.beep.ml.SoundClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/** A stand-in for YAMNet that returns whatever scores the test sets. */
private class FakeClassifier : SoundClassifier {
    override val windowSize = 15_600
    override val labels = List(521) { "class $it" }
    var scores = FloatArray(521)
    var calls = 0
    override fun classify(window: FloatArray): FloatArray { calls++; return scores.copyOf() }
}

class DetectionEngineTest {

    private var now = 1_000_000L
    private val fake = FakeClassifier()
    private val engine = DetectionEngine(fake, clock = { now })

    private fun noise(amp: Float) = FloatArray(800) { i -> (amp * sin(2 * PI * 440 * i / Dsp.SAMPLE_RATE)).toFloat() }

    /** Feeds [seconds] of audio and returns all detections. */
    private fun run(seconds: Double, amp: Float = 0.05f) = buildList {
        repeat((seconds * 20).toInt()) {
            now += 50
            addAll(engine.process(noise(amp)).detections)
        }
    }

    private fun score(classIndex: Int, value: Float) {
        fake.scores = FloatArray(521).also { it[classIndex] = value }
    }

    @Test
    fun impulseSoundFiresOnceThenCoolsDown() {
        engine.config = EngineConfig(enabled = setOf("knock"))
        run(1.0) // fill the 1 s window
        score(353, 0.9f) // Knock
        val first = run(1.0)
        assertEquals(1, first.size)
        assertEquals("knock", first[0].spec.id)
        // Still knocking: cooldown prevents a flood of alerts.
        assertTrue(run(2.0).isEmpty())
        // After the cooldown it can alert again.
        now += 20_000
        assertEquals(1, run(1.0).size)
    }

    @Test
    fun sustainedSoundNeedsToPersist() {
        engine.config = EngineConfig(enabled = setOf("smoke"))
        run(1.0)
        score(393, 0.9f) // Smoke detector
        // One classification (0.5 s) is not enough for a sustained sound...
        val oneFrame = buildList { repeat(10) { now += 50; addAll(engine.process(noise(0.05f)).detections) } }
        assertTrue(oneFrame.isEmpty())
        // ...but it fires once it lasts.
        assertEquals(1, run(1.0).size)
    }

    @Test
    fun disabledSoundsNeverFire() {
        engine.config = EngineConfig(enabled = setOf("doorbell"))
        run(1.0)
        score(353, 0.95f) // Knock — not enabled
        assertTrue(run(2.0).isEmpty())
    }

    @Test
    fun sensitivityMovesTheThreshold() {
        engine.config = EngineConfig(enabled = setOf("knock"), sensitivity = mapOf("knock" to 0f)) // threshold 0.75
        run(1.0)
        score(353, 0.6f)
        assertTrue(run(1.0).isEmpty())
        engine.config = EngineConfig(enabled = setOf("knock"), sensitivity = mapOf("knock" to 1f)) // threshold 0.15
        assertEquals(1, run(1.0).size)
    }

    @Test
    fun snoozeSilencesASound() {
        engine.config = EngineConfig(enabled = setOf("knock"))
        engine.snooze("knock", 10)
        run(1.0)
        score(353, 0.9f)
        assertTrue(run(2.0).isEmpty())
        now += 11 * 60_000L
        assertEquals(1, run(1.0).size)
    }

    @Test
    fun silentRoomSkipsTheModel() {
        engine.config = EngineConfig(enabled = setOf("knock"))
        val frames = buildList { repeat(60) { now += 50; add(engine.process(FloatArray(800))) } }
        assertEquals(0, fake.calls)
        assertTrue(frames.any { it.classifierSkipped })
    }

    @Test
    fun suddenLoudNoiseFiresWithoutTheModel() {
        val noModel = DetectionEngine(null, clock = { now })
        noModel.config = EngineConfig(enabled = setOf(SoundCatalog.LOUD))
        repeat(60) { now += 50; noModel.process(noise(0.003f)) } // quiet room ≈ 47 dB
        now += 50
        val bang = noModel.process(noise(0.9f)) // ≈ 96 dB
        assertNotNull(bang.detections.firstOrNull { it.spec.id == SoundCatalog.LOUD })
    }

    @Test
    fun loudnessAdaptsToANoisyRoom() {
        val watcher = LoudnessWatcher()
        repeat(200) { watcher.onLevel(82f, 0.5f) } // a loud street is the new normal
        assertNull(watcher.onLevel(88f, 0.5f)) // not a big jump over the baseline
        assertNotNull(watcher.onLevel(104f, 0.5f))
    }

    @Test
    fun catalogIndicesAreValid() {
        SoundCatalog.all.forEach { spec -> spec.classes.forEach { assertTrue("${spec.id}: $it", it in 0 until 521) } }
        assertEquals(SoundCatalog.all.size, SoundCatalog.all.map { it.id }.toSet().size)
        com.shankar.beep.detect.Mode.entries.forEach { mode ->
            mode.sounds.forEach { assertNotNull("${mode.name} → $it", SoundCatalog.find(it)) }
        }
    }

    @Test
    fun liveScoresAreReported() {
        engine.config = EngineConfig(enabled = emptySet())
        run(1.0)
        score(349, 0.7f) // Doorbell
        val frame = buildList { repeat(10) { now += 50; add(engine.process(noise(0.05f))) } }.last { it.scores != null }
        assertEquals(0.7f, frame.scores!!["doorbell"]!!, 1e-4f)
    }
}
