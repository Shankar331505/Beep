package com.shankar.beep.detect

import com.shankar.beep.audio.Dsp
import com.shankar.beep.audio.Dtw
import com.shankar.beep.audio.Mfcc

/** Voice samples of the user's name, recorded during setup. */
data class NameProfile(
    val name: String,
    val templates: List<Array<FloatArray>>,
) {
    /** Typical distance between the user's own samples; the yardstick for "close enough". */
    val calibration: Float = calibrate(templates)

    val isReady: Boolean get() = templates.size >= MIN_SAMPLES

    companion object {
        const val MIN_SAMPLES = 2
        const val MAX_SAMPLES = 6
        private const val FALLBACK = 3.2f

        fun calibrate(templates: List<Array<FloatArray>>): Float {
            if (templates.size < 2) return FALLBACK
            var sum = 0f; var count = 0
            for (i in templates.indices) for (j in templates.indices) {
                if (i == j) continue
                val d = Dtw.subsequence(templates[i], templates[j])
                if (d < Float.MAX_VALUE) { sum += d; count++ }
            }
            return if (count == 0) FALLBACK else (sum / count).coerceIn(1.0f, 8f)
        }
    }
}

data class NameMatch(val cost: Float, val threshold: Float, val confidence: Float) {
    val matched: Boolean get() = cost <= threshold
}

/**
 * Keyword spotting for the user's name, fully on-device and from scratch:
 *
 * 1. A voice-activity detector splits the stream into short utterances
 *    (energy well above the room's noise floor, 0.25–2 s long).
 * 2. Each utterance becomes an MFCC sequence.
 * 3. Subsequence DTW looks for the best alignment of each enrolled sample
 *    anywhere inside the utterance — so "hey Shankar!" still matches "Shankar".
 * 4. The match is accepted when its cost is within a margin of how much the
 *    user's own samples differ from each other (self-calibrating threshold),
 *    and YAMNet agrees that someone was speaking.
 *
 * It never touches audio focus or the speech recogniser, so music keeps playing.
 */
class NameSpotter(private val clock: () -> Long = System::currentTimeMillis) {

    private val mfcc = Mfcc()
    private val frame = 160 // 10 ms
    private var profile: NameProfile? = null
    var sensitivity: Float = 0.5f

    // Voice activity detection state.
    private var noiseFloorDb = -60f
    private val segment = FloatArray(Dsp.SAMPLE_RATE * MAX_SEGMENT_MS / 1000)
    private var segmentLength = 0
    private var voicedMs = 0
    private var silenceMs = 0
    private var inSegment = false
    private val preRoll = FloatArray(frame * PRE_ROLL_FRAMES)
    private var preRollFilled = 0
    private var pendingFrame = FloatArray(frame)
    private var pendingCount = 0

    private var lastSpeechAt = 0L
    private var lastSpeechProbability = 0f

    var lastMatch: NameMatch? = null
        private set

    fun setProfile(profile: NameProfile?) {
        this.profile = profile?.takeIf { it.isReady }
    }

    /** YAMNet's opinion that someone is talking; used as a sanity gate. */
    fun onSpeechProbability(p: Float) {
        if (p >= SPEECH_GATE) lastSpeechAt = clock()
        lastSpeechProbability = p
    }

    /** Feed raw audio; returns a match when the name was just heard. */
    fun process(chunk: FloatArray): NameMatch? {
        if (profile == null) return null
        var result: NameMatch? = null
        var i = 0
        while (i < chunk.size) {
            val take = minOf(frame - pendingCount, chunk.size - i)
            System.arraycopy(chunk, i, pendingFrame, pendingCount, take)
            pendingCount += take
            i += take
            if (pendingCount == frame) {
                pendingCount = 0
                onFrame(pendingFrame)?.let { result = it }
            }
        }
        return result
    }

    private fun onFrame(f: FloatArray): NameMatch? {
        val db = Dsp.dbfs(Dsp.rms(f))
        // Noise floor: drops quickly, rises slowly — tracks the quiet between words.
        noiseFloorDb = if (db < noiseFloorDb) db else noiseFloorDb + 0.015f
        noiseFloorDb = noiseFloorDb.coerceIn(-90f, -20f)
        val voiced = db > noiseFloorDb + VOICE_MARGIN_DB && db > MIN_VOICE_DBFS

        if (!inSegment) {
            if (!voiced) {
                pushPreRoll(f)
                return null
            } else {
                inSegment = true
                segmentLength = 0
                voicedMs = 0
                silenceMs = 0
                // Include a little audio from just before the voice started.
                append(preRoll, preRollFilled)
                preRollFilled = 0
            }
        }

        append(f, f.size)
        if (voiced) { voicedMs += 10; silenceMs = 0 } else silenceMs += 10

        val full = segmentLength + frame > segment.size
        if (silenceMs >= END_SILENCE_MS || full) {
            inSegment = false
            val enoughVoice = voicedMs in MIN_VOICE_MS..MAX_SEGMENT_MS
            val audio = segment.copyOf(segmentLength)
            segmentLength = 0
            if (!enoughVoice) return null
            return evaluate(audio)
        }
        return null
    }

    private fun evaluate(audio: FloatArray): NameMatch? {
        val p = profile ?: return null
        val speechRecently = clock() - lastSpeechAt < SPEECH_MEMORY_MS
        // Trim exactly like the enrolled samples, so mean normalisation sees the same kind of audio.
        val voiced = trimToVoice(audio) ?: return null
        val match = match(p, mfcc.compute(voiced), sensitivity) ?: return null
        lastMatch = match
        return if (match.matched && speechRecently) match else null
    }

    private fun append(src: FloatArray, count: Int) {
        val n = minOf(count, segment.size - segmentLength)
        System.arraycopy(src, 0, segment, segmentLength, n)
        segmentLength += n
    }

    private fun pushPreRoll(f: FloatArray) {
        if (preRollFilled + frame > preRoll.size) {
            System.arraycopy(preRoll, frame, preRoll, 0, preRoll.size - frame)
            preRollFilled = preRoll.size - frame
        }
        System.arraycopy(f, 0, preRoll, preRollFilled, frame)
        preRollFilled += frame
    }

    fun reset() {
        inSegment = false
        segmentLength = 0
        preRollFilled = 0
        pendingCount = 0
    }

    companion object {
        private const val VOICE_MARGIN_DB = 10f
        private const val MIN_VOICE_DBFS = -58f
        private const val MIN_VOICE_MS = 220
        private const val MAX_SEGMENT_MS = 2200
        private const val END_SILENCE_MS = 280
        private const val PRE_ROLL_FRAMES = 10
        private const val SPEECH_GATE = 0.12f
        private const val SPEECH_MEMORY_MS = 2500L

        /** Sensitivity 0..1 → how far past the user's own variation we still accept. */
        fun margin(sensitivity: Float): Float = 1.05f + 0.45f * sensitivity.coerceIn(0f, 1f)

        fun match(profile: NameProfile, features: Array<FloatArray>, sensitivity: Float): NameMatch? {
            if (features.isEmpty()) return null
            val shortest = profile.templates.minOf { it.size }
            if (features.size < shortest * 0.6f) return null
            val cost = profile.templates.minOf { Dtw.subsequence(it, features) }
            val threshold = profile.calibration * margin(sensitivity)
            val confidence = (1.5f - cost / threshold).coerceIn(0f, 1f) // 1.0 at half the threshold, 0.5 at it
            return NameMatch(cost, threshold, confidence)
        }

        /**
         * Cuts the silence off a recorded sample and returns its MFCC sequence,
         * or null when no clear voice was found.
         */
        fun templateFrom(recording: FloatArray, mfcc: Mfcc = Mfcc()): Array<FloatArray>? {
            val voiced = trimToVoice(recording) ?: return null
            return mfcc.compute(voiced).takeIf { it.size >= 15 }
        }

        fun trimToVoice(recording: FloatArray, frame: Int = 160): FloatArray? {
            val frames = recording.size / frame
            if (frames == 0) return null
            val levels = FloatArray(frames) { Dsp.dbfs(Dsp.rms(recording, it * frame, (it + 1) * frame)) }
            val peak = levels.max()
            if (peak < MIN_VOICE_DBFS + 8f) return null
            val gate = maxOf(peak - 28f, MIN_VOICE_DBFS)
            val first = levels.indexOfFirst { it > gate }
            val last = levels.indexOfLast { it > gate }
            if (first < 0 || (last - first + 1) * 10 < 200) return null
            val start = maxOf(0, first - 5) * frame
            val end = minOf(frames, last + 6) * frame
            return recording.copyOfRange(start, end)
        }
    }
}
