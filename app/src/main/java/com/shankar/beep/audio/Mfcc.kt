package com.shankar.beep.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Mel-frequency cepstral coefficients — the classic compact description of
 * how a voice "sounds" over time. Used to recognise the user's name.
 *
 * 25 ms frames, 10 ms hop, 26 mel filters between 80 Hz and 7.6 kHz,
 * 13 coefficients. The 0th coefficient (overall loudness) is dropped and the
 * result is mean-normalised per utterance (CMN) so the match does not depend
 * on how loud or how close the speaker is.
 */
class Mfcc(
    private val sampleRate: Int = Dsp.SAMPLE_RATE,
    val frameSize: Int = 400,
    val hopSize: Int = 160,
    private val fftSize: Int = 512,
    private val melBands: Int = 26,
    val coefficients: Int = 13,
    private val minHz: Double = 80.0,
    private val maxHz: Double = 7600.0,
) {
    private val window = Dsp.hann(frameSize)
    private val filters: Array<FloatArray> = buildFilterBank()
    private val dct: Array<FloatArray> = Array(coefficients) { k ->
        FloatArray(melBands) { n -> cos(PI * k * (n + 0.5) / melBands).toFloat() }
    }
    private val re = FloatArray(fftSize)
    private val im = FloatArray(fftSize)
    private val power = FloatArray(fftSize / 2 + 1)
    private val melEnergies = FloatArray(melBands)

    /** Number of features per frame (coefficients minus c0). */
    val featureSize: Int get() = coefficients - 1

    /**
     * Computes the feature matrix [frames][featureSize] for [samples] in -1..1,
     * with pre-emphasis and cepstral mean normalisation.
     */
    fun compute(samples: FloatArray, normalize: Boolean = true): Array<FloatArray> {
        if (samples.size < frameSize) return emptyArray()
        val frameCount = 1 + (samples.size - frameSize) / hopSize
        val out = Array(frameCount) { FloatArray(featureSize) }
        for (f in 0 until frameCount) {
            val start = f * hopSize
            re.fill(0f); im.fill(0f)
            var prev = if (start > 0) samples[start - 1] else 0f
            for (i in 0 until frameSize) {
                val s = samples[start + i]
                re[i] = (s - 0.97f * prev) * window[i] // pre-emphasis + window
                prev = s
            }
            Dsp.fft(re, im)
            Dsp.powerSpectrum(re, im, power)
            for (m in 0 until melBands) {
                val filter = filters[m]
                var e = 0f
                for (k in power.indices) e += filter[k] * power[k]
                melEnergies[m] = ln(max(e, 1e-10f))
            }
            val row = out[f]
            for (k in 1 until coefficients) {
                var c = 0f
                val basis = dct[k]
                for (n in 0 until melBands) c += basis[n] * melEnergies[n]
                row[k - 1] = c * sqrt(2f / melBands)
            }
        }
        if (normalize) meanNormalize(out)
        return out
    }

    private fun buildFilterBank(): Array<FloatArray> {
        val bins = fftSize / 2 + 1
        val melMin = Dsp.hzToMel(minHz)
        val melMax = Dsp.hzToMel(maxHz)
        val points = DoubleArray(melBands + 2) { i -> Dsp.melToHz(melMin + (melMax - melMin) * i / (melBands + 1)) }
        val binOf = points.map { hz -> hz * fftSize / sampleRate }
        return Array(melBands) { m ->
            val left = binOf[m]; val center = binOf[m + 1]; val right = binOf[m + 2]
            FloatArray(bins) { k ->
                val x = k.toDouble()
                when {
                    x <= left || x >= right -> 0f
                    x <= center -> ((x - left) / (center - left)).toFloat()
                    else -> ((right - x) / (right - center)).toFloat()
                }
            }
        }
    }

    companion object {
        fun meanNormalize(features: Array<FloatArray>) {
            if (features.isEmpty()) return
            val dims = features[0].size
            val mean = FloatArray(dims)
            for (row in features) for (d in 0 until dims) mean[d] += row[d]
            for (d in 0 until dims) mean[d] /= features.size
            for (row in features) for (d in 0 until dims) row[d] -= mean[d]
        }
    }
}
