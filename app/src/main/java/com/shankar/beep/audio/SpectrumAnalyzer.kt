package com.shankar.beep.audio

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/**
 * Turns short chunks of audio into a handful of log-spaced frequency bands
 * (0..1) for the live spectrogram on the home screen.
 */
class SpectrumAnalyzer(
    val bands: Int = 48,
    private val fftSize: Int = 1024,
    sampleRate: Int = Dsp.SAMPLE_RATE,
    minHz: Float = 60f,
    maxHz: Float = 8000f,
    private val floorDb: Float = -100f,
    private val ceilDb: Float = -25f,
) {
    private val re = FloatArray(fftSize)
    private val im = FloatArray(fftSize)
    private val power = FloatArray(fftSize / 2 + 1)
    private var window = Dsp.hann(fftSize)
    private val edges: IntArray = IntArray(bands + 1) { i ->
        val hz = minHz * (maxHz / minHz).pow(i / bands.toFloat())
        (hz * fftSize / sampleRate).toInt().coerceIn(1, fftSize / 2)
    }

    fun analyze(samples: FloatArray, count: Int = samples.size): FloatArray {
        val n = minOf(count, fftSize)
        if (window.size != n) window = Dsp.hann(n)
        re.fill(0f); im.fill(0f)
        for (i in 0 until n) re[i] = samples[i] * window[i]
        Dsp.fft(re, im)
        Dsp.powerSpectrum(re, im, power)
        val norm = (n / 2f) * (n / 2f)
        return FloatArray(bands) { b ->
            val lo = edges[b]
            val hi = max(edges[b + 1], lo + 1)
            var peak = 0f
            for (k in lo until hi) if (power[k] > peak) peak = power[k]
            val db = 10f * log10(max(peak / norm, 1e-12f))
            ((db - floorDb) / (ceilDb - floorDb)).coerceIn(0f, 1f)
        }
    }
}
