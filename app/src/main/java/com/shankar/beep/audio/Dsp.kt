package com.shankar.beep.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Small, dependency-free signal-processing toolkit used by every part of the
 * listening pipeline. Everything here is pure Kotlin so it can be unit tested
 * on the JVM without an Android device.
 */
object Dsp {

    const val SAMPLE_RATE = 16_000

    /** Approximate offset between digital full-scale and acoustic SPL on a phone mic. */
    const val SPL_OFFSET_DB = 90f

    /** Floor used for silent buffers so that log10 never sees zero. */
    const val MIN_DBFS = -96f

    fun rms(samples: FloatArray, from: Int = 0, to: Int = samples.size): Float {
        if (to <= from) return 0f
        var sum = 0.0
        for (i in from until to) {
            val s = samples[i]
            sum += s * s
        }
        return sqrt(sum / (to - from)).toFloat()
    }

    /** RMS level in dB relative to full scale (0 dBFS = loudest possible signal). */
    fun dbfs(rms: Float): Float =
        if (rms <= 1e-7f) MIN_DBFS else max(MIN_DBFS, 20f * log10(rms))

    /** Estimated sound pressure level. Phone mics are not calibrated, so this is approximate. */
    fun spl(rms: Float): Float = (dbfs(rms) + SPL_OFFSET_DB).coerceIn(0f, 130f)

    /** Converts 16-bit PCM to floats in -1..1 as YAMNet expects. */
    fun pcmToFloat(pcm: ShortArray, count: Int = pcm.size, out: FloatArray = FloatArray(count)): FloatArray {
        for (i in 0 until count) out[i] = pcm[i] / 32768f
        return out
    }

    fun hann(size: Int): FloatArray =
        FloatArray(size) { i -> (0.5 - 0.5 * cos(2.0 * PI * i / (size - 1))).toFloat() }

    /**
     * In-place iterative radix-2 Cooley–Tukey FFT.
     * [re] and [im] must have the same power-of-two length.
     */
    fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        require(n == im.size && n > 0 && (n and (n - 1)) == 0) { "FFT size must be a power of two" }
        // Bit-reversal permutation.
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wRe = cos(ang).toFloat()
            val wIm = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var curRe = 1f
                var curIm = 0f
                for (k in 0 until len / 2) {
                    val a = i + k
                    val b = a + len / 2
                    val tRe = re[b] * curRe - im[b] * curIm
                    val tIm = re[b] * curIm + im[b] * curRe
                    re[b] = re[a] - tRe
                    im[b] = im[a] - tIm
                    re[a] += tRe
                    im[a] += tIm
                    val nextRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = nextRe
                }
                i += len
            }
            len = len shl 1
        }
    }

    /** Power spectrum (|X|²) of the first n/2+1 bins. */
    fun powerSpectrum(re: FloatArray, im: FloatArray, out: FloatArray = FloatArray(re.size / 2 + 1)): FloatArray {
        for (k in out.indices) out[k] = re[k] * re[k] + im[k] * im[k]
        return out
    }

    fun hzToMel(hz: Double): Double = 2595.0 * log10(1.0 + hz / 700.0)
    fun melToHz(mel: Double): Double = 700.0 * (Math.pow(10.0, mel / 2595.0) - 1.0)

    /** Cosine similarity of two equal-length vectors (0 if either is all zeros). */
    fun cosine(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size)
        var dot = 0.0; var na = 0.0; var nb = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        if (na == 0.0 || nb == 0.0) return 0f
        return (dot / (sqrt(na) * sqrt(nb))).toFloat()
    }

    fun l2Normalize(v: FloatArray): FloatArray {
        var n = 0.0
        for (x in v) n += x * x
        val norm = sqrt(n).toFloat()
        if (norm > 0f) for (i in v.indices) v[i] /= norm
        return v
    }

    fun median(values: FloatArray, count: Int = values.size): Float {
        if (count == 0) return 0f
        val copy = values.copyOf(count)
        copy.sort()
        return if (count % 2 == 1) copy[count / 2] else (copy[count / 2 - 1] + copy[count / 2]) / 2f
    }
}
