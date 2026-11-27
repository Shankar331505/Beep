package com.shankar.beep.audio

import kotlin.math.min
import kotlin.math.sqrt

/**
 * Dynamic Time Warping: measures how similar two feature sequences are while
 * allowing one of them to be spoken faster or slower than the other.
 */
object Dtw {

    private fun distance(a: FloatArray, b: FloatArray): Float {
        var s = 0f
        for (i in a.indices) {
            val d = a[i] - b[i]
            s += d * d
        }
        return sqrt(s)
    }

    /**
     * Classic DTW between two whole sequences, normalised by (n + m) so that
     * long and short words are comparable. Lower means more similar.
     */
    fun distance(a: Array<FloatArray>, b: Array<FloatArray>, band: Float = 0.4f): Float {
        if (a.isEmpty() || b.isEmpty()) return Float.MAX_VALUE
        val n = a.size; val m = b.size
        val w = maxOf((maxOf(n, m) * band).toInt(), kotlin.math.abs(n - m) + 1)
        val inf = Float.MAX_VALUE / 4
        var prev = FloatArray(m + 1) { inf }
        var cur = FloatArray(m + 1) { inf }
        prev[0] = 0f
        for (i in 1..n) {
            cur.fill(inf)
            val center = i * m / n
            val lo = maxOf(1, center - w)
            val hi = minOf(m, center + w)
            for (j in lo..hi) {
                val cost = distance(a[i - 1], b[j - 1])
                cur[j] = cost + min(prev[j - 1], min(prev[j], cur[j - 1]))
            }
            val t = prev; prev = cur; cur = t
        }
        val total = prev[m]
        return if (total >= inf) Float.MAX_VALUE else total / (n + m)
    }

    /**
     * Subsequence DTW: finds the best place where [template] occurs anywhere
     * inside the longer [query] (e.g. the name inside "hey Shankar, come here").
     * Returns the best cost normalised by the template length.
     */
    fun subsequence(template: Array<FloatArray>, query: Array<FloatArray>): Float {
        if (template.isEmpty() || query.isEmpty()) return Float.MAX_VALUE
        val n = template.size; val m = query.size
        val inf = Float.MAX_VALUE / 4
        // D[i][j]: best cost aligning template[0..i] ending at query[j]; free start in query.
        var prev = FloatArray(m) { j -> distance(template[0], query[j]) }
        var prevLen = IntArray(m) { 1 }
        var cur = FloatArray(m)
        var curLen = IntArray(m)
        for (i in 1 until n) {
            for (j in 0 until m) {
                val cost = distance(template[i], query[j])
                var best = prev[j]; var len = prevLen[j]
                if (j > 0) {
                    if (prev[j - 1] < best) { best = prev[j - 1]; len = prevLen[j - 1] }
                    if (cur[j - 1] < best) { best = cur[j - 1]; len = curLen[j - 1] }
                }
                cur[j] = if (best >= inf) inf else best + cost
                curLen[j] = len + 1
            }
            val t = prev; prev = cur; cur = t
            val tl = prevLen; prevLen = curLen; curLen = tl
        }
        var best = Float.MAX_VALUE
        for (j in 0 until m) {
            val normalized = prev[j] / prevLen[j]
            if (normalized < best) best = normalized
        }
        return best
    }
}
