package com.shankar.beep

import com.shankar.beep.audio.Dsp
import com.shankar.beep.audio.Mfcc
import com.shankar.beep.data.SettingsRepository
import com.shankar.beep.data.VoiceStore
import com.shankar.beep.detect.CustomSound
import com.shankar.beep.detect.Fingerprint
import com.shankar.beep.detect.NameProfile
import com.shankar.beep.detect.NameSpotter
import com.shankar.beep.ml.YamNet
import com.shankar.beep.ui.record.TeachViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class NameAndStorageTest {

    private fun tone(hz: Double, seconds: Double, amp: Double = 0.3) =
        FloatArray((Dsp.SAMPLE_RATE * seconds).toInt()) { i -> (amp * sin(2 * PI * hz * i / Dsp.SAMPLE_RATE)).toFloat() }

    private fun silence(seconds: Double) = FloatArray((Dsp.SAMPLE_RATE * seconds).toInt())

    /** A synthetic "name": two vowel-like tones, slightly varied per take. */
    private fun sayName(variation: Double) =
        silence(0.3) + tone(320.0 * variation, 0.25) + tone(780.0 * variation, 0.3) + silence(0.4)

    @Test
    fun trimFindsTheVoice() {
        val trimmed = NameSpotter.trimToVoice(sayName(1.0))
        assertNotNull(trimmed)
        assertTrue(trimmed!!.size < sayName(1.0).size)
        assertNull(NameSpotter.trimToVoice(silence(1.0)))
    }

    @Test
    fun nameMatchesItselfButNotAnotherWord() {
        val mfcc = Mfcc()
        val templates = listOf(1.0, 1.03, 0.97).map { NameSpotter.templateFrom(sayName(it), mfcc)!! }
        val profile = NameProfile("Test", templates)
        assertTrue(profile.isReady)

        val same = NameSpotter.match(profile, mfcc.compute(NameSpotter.trimToVoice(sayName(1.01))!!), 0.5f)!!
        val otherWord = silence(0.3) + tone(1500.0, 0.3) + tone(2200.0, 0.3) + silence(0.3)
        val other = NameSpotter.match(profile, mfcc.compute(NameSpotter.trimToVoice(otherWord)!!), 0.5f)!!
        assertTrue("same=${same.cost} other=${other.cost}", same.cost < other.cost)
        assertTrue(same.matched)
        assertFalse(other.matched)
    }

    @Test
    fun streamingSpotterHearsTheName() {
        val mfcc = Mfcc()
        val templates = listOf(1.0, 1.03, 0.97).map { NameSpotter.templateFrom(sayName(it), mfcc)!! }
        var now = 0L
        val spotter = NameSpotter { now }
        spotter.setProfile(NameProfile("Test", templates))
        val rnd = Random(1)
        val stream = FloatArray(Dsp.SAMPLE_RATE) { (rnd.nextFloat() - 0.5f) * 0.0005f } + sayName(1.0) + silence(0.6)
        var heard = false
        var i = 0
        while (i + 800 <= stream.size) {
            now += 50
            spotter.onSpeechProbability(0.8f)
            if (spotter.process(stream.copyOfRange(i, i + 800)) != null) heard = true
            i += 800
        }
        assertTrue(heard)
    }

    @Test
    fun fingerprintSimilarity() {
        val scores = FloatArray(521).also { it[475] = 0.8f; it[362] = 0.3f }
        val window = tone(2000.0, 0.975)
        val a = Fingerprint.of(scores, window)
        val sound = CustomSound("custom:x", "Microwave", "microwave", listOf(a), 0)
        assertEquals(1f, Fingerprint.similarity(a, sound), 1e-4f)
        val other = Fingerprint.of(FloatArray(521).also { it[69] = 0.9f }, tone(300.0, 0.975))
        assertTrue(Fingerprint.similarity(other, sound) < Fingerprint.threshold(1f))
    }

    @Test
    fun loudestWindowPicksTheSound() {
        val clip = silence(1.0) + tone(1000.0, 1.0, amp = 0.8) + silence(1.0)
        val window = TeachViewModel.loudestWindow(clip, 15_600)
        assertTrue(Dsp.rms(window) > 0.5f)
    }

    @Test
    fun matricesRoundTrip() {
        val file = File.createTempFile("beep", ".bin")
        val data = listOf(Array(3) { r -> FloatArray(4) { c -> r * 10f + c } }, Array(2) { FloatArray(4) { 7f } })
        VoiceStore.writeMatrices(file, data)
        val back = VoiceStore.readMatrices(file)
        assertEquals(2, back.size)
        assertEquals(12f, back[0][1][2], 0f)
        assertEquals(7f, back[1][1][3], 0f)
        file.delete()
    }

    @Test
    fun sensitivityEncoding() {
        val map = mapOf("knock" to 0.25f, "custom:ab12" to 0.9f)
        assertEquals(map, SettingsRepository.decodeSensitivity(SettingsRepository.encodeSensitivity(map)))
    }

    @Test
    fun csvLabelParsing() {
        assertEquals("Speech", YamNet.parseCsvName("0,/m/09x0r,Speech"))
        assertEquals("Child speech, kid speaking", YamNet.parseCsvName("1,/m/0ytgt,\"Child speech, kid speaking\""))
    }
}
