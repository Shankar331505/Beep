package com.shankar.beep.detect

import com.shankar.beep.audio.Dsp
import com.shankar.beep.audio.Mfcc
import kotlin.math.sqrt

/** A sound the user taught Beep by recording a few examples. */
data class CustomSound(
    val id: String,
    val name: String,
    val icon: String,
    val prints: List<FloatArray>,
    val createdAt: Long,
)

/**
 * A compact "what does this sound like" vector used for sounds the user
 * teaches Beep (their own microwave, intercom, a pet's whine...).
 *
 * It mixes two views of the same second of audio:
 *  - what YAMNet thinks it is (its 521 class scores — a semantic view), and
 *  - its timbre (mean and spread of MFCCs — an acoustic view),
 * so two different beeps that YAMNet both calls "Beep" can still be told apart.
 */
object Fingerprint {

    private const val SEMANTIC_WEIGHT = 0.8f
    private val acousticWeight = sqrt(1f - SEMANTIC_WEIGHT * SEMANTIC_WEIGHT)

    private val mfcc = Mfcc()

    fun size(classCount: Int) = classCount + 2 * mfcc.featureSize

    @Synchronized
    fun of(scores: FloatArray, window: FloatArray): FloatArray {
        val semantic = FloatArray(scores.size) { sqrt(scores[it].coerceAtLeast(0f)) }
        Dsp.l2Normalize(semantic)

        val frames = mfcc.compute(window, normalize = false)
        val dims = mfcc.featureSize
        val acoustic = FloatArray(dims * 2)
        if (frames.isNotEmpty()) {
            for (row in frames) for (d in 0 until dims) acoustic[d] += row[d]
            for (d in 0 until dims) acoustic[d] /= frames.size
            for (row in frames) for (d in 0 until dims) {
                val diff = row[d] - acoustic[d]
                acoustic[dims + d] += diff * diff
            }
            for (d in 0 until dims) acoustic[dims + d] = sqrt(acoustic[dims + d] / frames.size)
        }
        Dsp.l2Normalize(acoustic)

        val out = FloatArray(semantic.size + acoustic.size)
        for (i in semantic.indices) out[i] = semantic[i] * SEMANTIC_WEIGHT
        for (i in acoustic.indices) out[semantic.size + i] = acoustic[i] * acousticWeight
        return out
    }

    /** Best similarity of [print] against any example of [sound]. */
    fun similarity(print: FloatArray, sound: CustomSound): Float =
        sound.prints.filter { it.size == print.size }.maxOfOrNull { Dsp.cosine(print, it) } ?: 0f

    /** Sensitivity 0..1 → minimum cosine similarity required. */
    fun threshold(sensitivity: Float): Float = 0.93f - 0.16f * sensitivity.coerceIn(0f, 1f)
}
