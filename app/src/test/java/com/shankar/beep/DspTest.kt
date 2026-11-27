package com.shankar.beep

import com.shankar.beep.audio.Dsp
import com.shankar.beep.audio.Dtw
import com.shankar.beep.audio.Mfcc
import com.shankar.beep.audio.SpectrumAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class DspTest {

    private fun tone(hz: Double, seconds: Double, amp: Double = 0.5) =
        FloatArray((Dsp.SAMPLE_RATE * seconds).toInt()) { i -> (amp * sin(2 * PI * hz * i / Dsp.SAMPLE_RATE)).toFloat() }

    @Test
    fun fftFindsTheRightBin() {
        val n = 1024
        val hz = 1000.0
        val re = FloatArray(n) { i -> sin(2 * PI * hz * i / Dsp.SAMPLE_RATE).toFloat() }
        val im = FloatArray(n)
        Dsp.fft(re, im)
        val power = Dsp.powerSpectrum(re, im)
        val peak = power.indices.maxBy { power[it] }
        assertTrue(kotlin.math.abs((hz * n / Dsp.SAMPLE_RATE).toInt() - peak) <= 1)
    }

    @Test
    fun rmsAndDecibels() {
        val sine = tone(440.0, 0.5, amp = 1.0)
        assertEquals(0.707f, Dsp.rms(sine), 0.01f)
        assertEquals(-3f, Dsp.dbfs(Dsp.rms(sine)), 0.2f)
        assertEquals(Dsp.MIN_DBFS, Dsp.dbfs(0f), 0.001f)
        assertTrue(Dsp.spl(0.5f) > Dsp.spl(0.05f))
    }

    @Test
    fun cosineAndMedian() {
        assertEquals(1f, Dsp.cosine(floatArrayOf(1f, 2f, 3f), floatArrayOf(2f, 4f, 6f)), 1e-5f)
        assertEquals(0f, Dsp.cosine(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f)), 1e-5f)
        assertEquals(3f, Dsp.median(floatArrayOf(5f, 1f, 3f, 4f, 2f)), 0f)
    }

    @Test
    fun mfccShapeAndNormalisation() {
        val mfcc = Mfcc()
        val features = mfcc.compute(tone(300.0, 1.0))
        assertEquals(1 + (16_000 - 400) / 160, features.size)
        assertEquals(12, features[0].size)
        // Cepstral mean normalisation → every coefficient averages to ~0.
        for (d in 0 until 12) assertEquals(0f, features.map { it[d] }.average().toFloat(), 1e-3f)
    }

    @Test
    fun dtwPrefersTheSameSound() {
        val mfcc = Mfcc()
        val a = mfcc.compute(tone(300.0, 0.6) + tone(900.0, 0.4))
        val aSlow = mfcc.compute(tone(300.0, 0.8) + tone(900.0, 0.5)) // same "word", spoken slower
        val b = mfcc.compute(tone(1500.0, 0.5) + tone(200.0, 0.5))
        assertTrue(Dtw.distance(a, aSlow) < Dtw.distance(a, b))
        assertTrue(Dtw.subsequence(a, aSlow) < Dtw.subsequence(a, b))
    }

    @Test
    fun subsequenceFindsAWordInsideASentence() {
        val mfcc = Mfcc()
        val word = tone(300.0, 0.4) + tone(900.0, 0.3)
        val sentence = tone(1800.0, 0.5) + word + tone(2400.0, 0.5)
        val unrelated = tone(1800.0, 0.5) + tone(2400.0, 0.7) + tone(1200.0, 0.5)
        val template = mfcc.compute(word)
        assertTrue(Dtw.subsequence(template, mfcc.compute(sentence)) < Dtw.subsequence(template, mfcc.compute(unrelated)))
    }

    @Test
    fun spectrumLightsUpTheToneBand() {
        val analyzer = SpectrumAnalyzer()
        val low = analyzer.analyze(tone(150.0, 0.05, amp = 0.8))
        val high = analyzer.analyze(tone(5000.0, 0.05, amp = 0.8))
        assertTrue(low.indices.maxBy { low[it] } < high.indices.maxBy { high[it] })
        assertTrue(low.all { it in 0f..1f })
    }
}
