package com.shankar.beep.ui.record

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shankar.beep.AppContainer
import com.shankar.beep.audio.Dsp
import com.shankar.beep.audio.Mfcc
import com.shankar.beep.audio.MicStream
import com.shankar.beep.detect.Fingerprint
import com.shankar.beep.detect.NameProfile
import com.shankar.beep.detect.NameSpotter
import com.shankar.beep.ml.YamNet
import com.shankar.beep.service.ListenStatus
import com.shankar.beep.service.ListeningService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface RecState {
    data object Idle : RecState
    data class Countdown(val seconds: Int) : RecState
    data class Recording(val progress: Float, val levelDb: Float) : RecState
    data object Processing : RecState
}

enum class Tone { NEUTRAL, GOOD, BAD }
data class Feedback(val text: String, val tone: Tone)

/** Records a fixed-length clip from the mic, reporting progress and level. */
private suspend fun recordClip(mic: MicStream, durationMs: Int, onProgress: (Float, Float) -> Unit): FloatArray {
    val total = Dsp.SAMPLE_RATE * durationMs / 1000
    val out = FloatArray(total)
    var filled = 0
    mic.chunks().first { chunk ->
        val n = minOf(chunk.size, total - filled)
        System.arraycopy(chunk, 0, out, filled, n)
        filled += n
        onProgress(filled / total.toFloat(), Dsp.spl(Dsp.rms(chunk)))
        filled >= total
    }
    return out
}

/**
 * Base for screens that record from the mic. The listening service is paused
 * while recording (only one reader should hold the mic) and restarted after.
 */
abstract class RecorderViewModel(protected val c: AppContainer) : ViewModel() {

    protected val _rec = MutableStateFlow<RecState>(RecState.Idle)
    val rec: StateFlow<RecState> = _rec.asStateFlow()

    protected val _feedback = MutableStateFlow<Feedback?>(null)
    val feedback: StateFlow<Feedback?> = _feedback.asStateFlow()

    private var pausedService = false

    protected suspend fun capture(context: Context, durationMs: Int, countdown: Int = 0): FloatArray? {
        if (!c.mic.hasPermission()) {
            _feedback.value = Feedback("Beep needs microphone access to record.", Tone.BAD)
            return null
        }
        val status = c.hub.live.value.status
        if (status is ListenStatus.Listening || status is ListenStatus.Starting) {
            pausedService = true
            ListeningService.stop(context.applicationContext)
            delay(400) // let the service release the mic
        }
        for (s in countdown downTo 1) {
            _rec.value = RecState.Countdown(s)
            delay(700)
        }
        return try {
            recordClip(c.mic, durationMs) { p, db -> _rec.value = RecState.Recording(p, db) }
        } catch (e: Exception) {
            _feedback.value = Feedback("The microphone is busy. Try again in a moment.", Tone.BAD)
            null
        } finally {
            _rec.value = RecState.Processing
        }
    }

    fun restoreListening(context: Context) {
        if (pausedService) {
            pausedService = false
            ListeningService.start(context.applicationContext)
        }
    }
}

/** Enrol and test the user's name. */
class NameViewModel(c: AppContainer) : RecorderViewModel(c) {

    val templates = c.voiceStore.nameTemplates
    val name = MutableStateFlow("")
    private val mfcc = Mfcc()

    init {
        viewModelScope.launch { name.value = c.settingsRepository.current().userName }
    }

    fun setName(value: String) {
        name.value = value.take(30)
        viewModelScope.launch { c.settingsRepository.setUserName(value) }
    }

    fun recordSample(context: Context) = viewModelScope.launch {
        if (templates.value.size >= NameProfile.MAX_SAMPLES) {
            _feedback.value = Feedback("That's plenty — ${NameProfile.MAX_SAMPLES} samples is the maximum.", Tone.NEUTRAL)
            return@launch
        }
        _feedback.value = null
        val clip = capture(context, 2200, countdown = 2) ?: return@launch idle()
        val template = withContext(Dispatchers.Default) { NameSpotter.templateFrom(clip, mfcc) }
        if (template == null) {
            _feedback.value = Feedback("Didn't catch that. Say your name clearly, a little louder.", Tone.BAD)
        } else {
            val existing = templates.value
            val consistency = if (existing.isNotEmpty()) {
                withContext(Dispatchers.Default) {
                    existing.minOf { com.shankar.beep.audio.Dtw.subsequence(it, template) } / NameProfile.calibrate(existing + listOf(template))
                }
            } else 1f
            c.voiceStore.saveNameTemplates(existing + listOf(template))
            val count = existing.size + 1
            _feedback.value = when {
                consistency > 1.8f -> Feedback("Saved — but it sounded quite different from the others. Re-record if that was a mistake.", Tone.NEUTRAL)
                count < 3 -> Feedback("Got it. ${3 - count} more to go.", Tone.GOOD)
                else -> Feedback("Great — Beep knows your name. Extra samples (or a friend's voice) improve accuracy.", Tone.GOOD)
            }
        }
        idle()
    }

    fun removeSample(index: Int) = viewModelScope.launch {
        val list = templates.value.toMutableList()
        if (index in list.indices) list.removeAt(index)
        c.voiceStore.saveNameTemplates(list)
        _feedback.value = null
    }

    fun clearSamples() = viewModelScope.launch {
        c.voiceStore.saveNameTemplates(emptyList())
        _feedback.value = null
    }

    /** Records once and reports whether Beep would have alerted. */
    fun test(context: Context) = viewModelScope.launch {
        val profile = NameProfile(name.value, templates.value)
        if (!profile.isReady) {
            _feedback.value = Feedback("Record at least ${NameProfile.MIN_SAMPLES} samples first.", Tone.NEUTRAL)
            return@launch
        }
        _feedback.value = null
        val clip = capture(context, 2500, countdown = 2) ?: return@launch idle()
        val sensitivity = c.settings.value?.sensitivity?.get("name") ?: 0.5f
        val result = withContext(Dispatchers.Default) {
            val voiced = NameSpotter.trimToVoice(clip) ?: return@withContext null
            NameSpotter.match(profile, mfcc.compute(voiced), sensitivity)
        }
        _feedback.value = when {
            result == null -> Feedback("Didn't hear a voice. Try again a little louder.", Tone.BAD)
            result.matched -> Feedback("Match! Beep would have alerted you (${(result.confidence * 100).toInt()}% confident).", Tone.GOOD)
            else -> Feedback("No match. Try saying it the way people usually call you, or add another sample.", Tone.BAD)
        }
        idle()
    }

    private fun idle() { _rec.value = RecState.Idle }
}

/** Teach Beep a new sound from a few examples. */
class TeachViewModel(c: AppContainer) : RecorderViewModel(c) {

    data class Sample(val print: FloatArray, val guess: String)

    val name = MutableStateFlow("")
    val icon = MutableStateFlow("custom")
    private val _samples = MutableStateFlow<List<Sample>>(emptyList())
    val samples: StateFlow<List<Sample>> = _samples.asStateFlow()
    val saved = MutableStateFlow(false)

    fun recordSample(context: Context) = viewModelScope.launch {
        val classifier = c.classifierOrNull()
        if (classifier == null) {
            _feedback.value = Feedback("The sound model could not be loaded on this device.", Tone.BAD)
            return@launch
        }
        _feedback.value = null
        val clip = capture(context, 3000, countdown = 3) ?: return@launch idle()
        val result = withContext(Dispatchers.Default) {
            val window = loudestWindow(clip, YamNet.WINDOW)
            val level = Dsp.spl(Dsp.rms(window))
            if (level < 45f) return@withContext null
            val scores = classifier.classify(window)
            val best = scores.indices.maxBy { scores[it] }
            Sample(Fingerprint.of(scores, window), classifier.labels.getOrElse(best) { "Unknown" })
        }
        if (result == null) {
            _feedback.value = Feedback("Too quiet. Hold the phone closer to the sound and try again.", Tone.BAD)
        } else {
            _samples.update { it + result }
            val n = _samples.value.size
            _feedback.value = Feedback(
                if (n < 3) "Sample $n saved · sounds like \"${result.guess}\". ${3 - n} more to go."
                else "Sample $n saved · sounds like \"${result.guess}\". Ready to save.",
                Tone.GOOD,
            )
        }
        idle()
    }

    fun removeSample(index: Int) = _samples.update { list -> list.filterIndexed { i, _ -> i != index } }

    fun save() = viewModelScope.launch {
        val n = name.value.trim()
        if (n.isEmpty() || _samples.value.size < 3) return@launch
        val sound = c.voiceStore.addCustomSound(n, icon.value, _samples.value.map { it.print })
        c.settingsRepository.setEnabled(sound.id, true)
        saved.value = true
    }

    private fun idle() { _rec.value = RecState.Idle }

    companion object {
        /** Picks the loudest ~1 s of the clip, where the sound most likely is. */
        fun loudestWindow(clip: FloatArray, size: Int): FloatArray {
            if (clip.size <= size) return clip.copyOf(size)
            var bestStart = 0
            var bestEnergy = -1.0
            var start = 0
            while (start + size <= clip.size) {
                var e = 0.0
                var i = start
                while (i < start + size) { e += clip[i] * clip[i]; i += 4 }
                if (e > bestEnergy) { bestEnergy = e; bestStart = start }
                start += 800
            }
            return clip.copyOfRange(bestStart, bestStart + size)
        }
    }
}
