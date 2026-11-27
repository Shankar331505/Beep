package com.shankar.beep.detect

import com.shankar.beep.audio.Dsp
import com.shankar.beep.audio.SpectrumAnalyzer
import com.shankar.beep.ml.SoundClassifier

/** What the user has switched on and how sensitive each sound should be. */
data class EngineConfig(
    val enabled: Set<String> = Mode.EVERYDAY.sounds,
    val sensitivity: Map<String, Float> = emptyMap(),
    val nameProfile: NameProfile? = null,
    val customSounds: List<CustomSound> = emptyList(),
) {
    fun sensitivityOf(spec: SoundSpec): Float = sensitivity[spec.id] ?: spec.defaultSensitivity
}

data class Detection(
    val spec: SoundSpec,
    val confidence: Float,
    val levelDb: Float,
    val timestamp: Long,
)

data class Heard(val label: String, val score: Float)

/** Everything the UI and alerting need to know about the latest 50 ms of audio. */
class Frame(
    val levelDb: Float,
    val bands: FloatArray,
    /** Non-null only on frames where the classifier ran (twice a second). */
    val hearing: List<Heard>?,
    /** Live score (0..1) of every watched sound, for the tuning meter. Null when not classified. */
    val scores: Map<String, Float>?,
    val detections: List<Detection>,
    val classifierSkipped: Boolean,
)

/**
 * The heart of Beep. Pure Kotlin (no Android), so it is fully unit-testable.
 *
 * Audio arrives in 50 ms chunks. Every chunk updates the level meter,
 * spectrogram, loudness watcher and name spotter. Every 0.5 s the last
 * 0.975 s of audio goes through YAMNet, and each watched sound is scored with
 * temporal smoothing (short sounds need one confident frame, long ones need
 * to persist) and a per-sound cooldown so one doorbell = one alert.
 */
class DetectionEngine(
    private val classifier: SoundClassifier?,
    private val clock: () -> Long = System::currentTimeMillis,
    private val chunkSize: Int = 800,
) {
    private val windowSize = classifier?.windowSize ?: 15_600
    private val ring = FloatArray(windowSize)
    private var ringPos = 0
    private var ringFilled = 0
    private var chunksSinceClassify = 0
    private val hopChunks = (Dsp.SAMPLE_RATE / 2) / chunkSize // 0.5 s

    private val spectrum = SpectrumAnalyzer()
    private val loudness = LoudnessWatcher()
    private val nameSpotter = NameSpotter(clock)
    private val recentLevels = FloatArray(20)
    private var levelCursor = 0

    private val history = HashMap<String, ArrayDeque<Float>>()
    private val lastFired = HashMap<String, Long>()
    private val snoozedUntil = java.util.concurrent.ConcurrentHashMap<String, Long>()

    @Volatile
    var config: EngineConfig = EngineConfig()
        set(value) {
            field = value
            nameSpotter.setProfile(value.nameProfile)
            customSpecs = value.customSounds.associate { it.id to (SoundCatalog.customSpec(it) to it) }
        }

    private var customSpecs: Map<String, Pair<SoundSpec, CustomSound>> = emptyMap()

    val roomBaselineDb: Float get() = loudness.baselineDb

    fun snooze(soundId: String, minutes: Int) {
        snoozedUntil[soundId] = clock() + minutes * 60_000L
    }

    fun process(chunk: FloatArray): Frame {
        val now = clock()
        val cfg = config
        val levelDb = Dsp.spl(Dsp.rms(chunk))
        val bands = spectrum.analyze(chunk)
        recentLevels[levelCursor] = levelDb
        levelCursor = (levelCursor + 1) % recentLevels.size
        pushToRing(chunk)

        val detections = ArrayList<Detection>(2)

        // 1. Sudden loud noise — instant, no model needed.
        SoundCatalog.find(SoundCatalog.LOUD)?.let { spec ->
            val spike = loudness.onLevel(levelDb, cfg.sensitivityOf(spec))
            if (spike != null && spec.id in cfg.enabled) {
                fire(spec, spike.confidence, levelDb, now)?.let(detections::add)
            }
        }

        // 2. The user's name.
        SoundCatalog.find(SoundCatalog.NAME)?.let { spec ->
            if (spec.id in cfg.enabled) {
                nameSpotter.sensitivity = cfg.sensitivityOf(spec)
                nameSpotter.process(chunk)?.let { match ->
                    fire(spec, match.confidence.coerceAtLeast(0.5f), levelDb, now)?.let(detections::add)
                }
            }
        }

        // 3. YAMNet, twice a second.
        var hearing: List<Heard>? = null
        var scores: Map<String, Float>? = null
        var skipped = false
        chunksSinceClassify++
        if (classifier != null && ringFilled >= windowSize && chunksSinceClassify >= hopChunks) {
            chunksSinceClassify = 0
            val loudest = recentLevels.max()
            if (loudest < QUIET_ROOM_DB) {
                // Smart idle: in a silent room there is nothing to classify — save the battery.
                skipped = true
                hearing = emptyList()
                history.values.forEach { it.addLast(0f); if (it.size > 3) it.removeFirst() }
            } else {
                val window = orderedWindow()
                val raw = classifier.classify(window)
                nameSpotter.onSpeechProbability(maxOf(raw[0], raw.getOrElse(1) { 0f }, raw.getOrElse(2) { 0f }))
                hearing = topLabels(raw)
                val live = HashMap<String, Float>()
                for (spec in SoundCatalog.all) {
                    if (spec.source != Source.MODEL) continue
                    val p = spec.classes.maxOf { raw.getOrElse(it) { 0f } }
                    live[spec.id] = p
                    val recent = history.getOrPut(spec.id) { ArrayDeque() }
                    recent.addLast(p)
                    if (recent.size > 3) recent.removeFirst()
                    if (spec.id !in cfg.enabled) continue
                    val threshold = modelThreshold(cfg.sensitivityOf(spec))
                    val hits = recent.count { it >= threshold }
                    val needed = if (spec.shape == Shape.SUSTAINED) 2 else 1
                    if (p >= threshold && hits >= needed) {
                        fire(spec, recent.max(), levelDb, now)?.let(detections::add)
                    }
                }
                // Sounds the user taught Beep.
                if (customSpecs.isNotEmpty() && loudest > loudness.baselineDb + 4f) {
                    val print = Fingerprint.of(raw, window)
                    for ((id, pair) in customSpecs) {
                        val (spec, sound) = pair
                        val sim = Fingerprint.similarity(print, sound)
                        live[id] = sim
                        if (id in cfg.enabled && sim >= Fingerprint.threshold(cfg.sensitivityOf(spec))) {
                            fire(spec, sim, levelDb, now)?.let(detections::add)
                        }
                    }
                }
                nameSpotter.lastMatch?.let { live[SoundCatalog.NAME] = it.confidence }
                scores = live
            }
        }

        detections.sortWith(compareByDescending<Detection> { it.spec.category.priority }.thenByDescending { it.confidence })
        return Frame(levelDb, bands, hearing, scores, detections, skipped)
    }

    /** Applies snooze and cooldown; returns a detection only if it should alert now. */
    private fun fire(spec: SoundSpec, confidence: Float, levelDb: Float, now: Long): Detection? {
        snoozedUntil[spec.id]?.let { if (now < it) return null else snoozedUntil.remove(spec.id) }
        val last = lastFired[spec.id]
        if (last != null && now - last < spec.cooldownSeconds * 1000L) return null
        lastFired[spec.id] = now
        return Detection(spec, confidence.coerceIn(0f, 1f), levelDb, now)
    }

    private fun pushToRing(chunk: FloatArray) {
        var i = 0
        while (i < chunk.size) {
            val n = minOf(chunk.size - i, windowSize - ringPos)
            System.arraycopy(chunk, i, ring, ringPos, n)
            ringPos = (ringPos + n) % windowSize
            i += n
        }
        ringFilled = minOf(windowSize, ringFilled + chunk.size)
    }

    private fun orderedWindow(): FloatArray {
        val out = FloatArray(windowSize)
        val tail = windowSize - ringPos
        System.arraycopy(ring, ringPos, out, 0, tail)
        System.arraycopy(ring, 0, out, tail, ringPos)
        return out
    }

    private fun topLabels(raw: FloatArray): List<Heard> {
        val labels = classifier?.labels ?: return emptyList()
        return raw.indices
            .sortedByDescending { raw[it] }
            .take(3)
            .filter { raw[it] >= 0.08f }
            .map { Heard(labels.getOrElse(it) { "Class $it" }, raw[it]) }
    }

    fun reset() {
        ringFilled = 0; ringPos = 0; chunksSinceClassify = 0
        history.clear()
        nameSpotter.reset()
    }

    companion object {
        /** Below this (≈ a silent bedroom) YAMNet is skipped to save battery. */
        const val QUIET_ROOM_DB = 32f

        /** Sensitivity 0..1 → minimum YAMNet score: 0.75 (strict) … 0.15 (very sensitive). */
        fun modelThreshold(sensitivity: Float): Float = 0.75f - 0.6f * sensitivity.coerceIn(0f, 1f)
    }
}
