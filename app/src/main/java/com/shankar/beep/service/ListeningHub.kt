package com.shankar.beep.service

import com.shankar.beep.detect.Detection
import com.shankar.beep.detect.Heard
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

sealed interface ListenStatus {
    data object Idle : ListenStatus
    data object Starting : ListenStatus
    data object Listening : ListenStatus
    data class Paused(val until: Long) : ListenStatus
    data class Error(val message: String) : ListenStatus
}

data class LiveState(
    val status: ListenStatus = ListenStatus.Idle,
    val levelDb: Float = 0f,
    val baselineDb: Float = 0f,
    val hearing: List<Heard> = emptyList(),
    val scores: Map<String, Float> = emptyMap(),
    /** True when the room is so quiet the AI is resting to save battery. */
    val resting: Boolean = false,
    val startedAt: Long = 0L,
)

/**
 * The single place where the listening service publishes what is going on,
 * and where the UI reads it. Lives as long as the app process.
 */
class ListeningHub {
    val live = MutableStateFlow(LiveState())

    /** Spectrogram columns at 20 per second, kept separate so the UI can draw them cheaply. */
    private val _bands = MutableSharedFlow<FloatArray>(
        replay = 1, extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val bands: SharedFlow<FloatArray> = _bands
    fun publishBands(bands: FloatArray) { _bands.tryEmit(bands) }

    /** The alert currently on screen (in-app banner or full-screen page), if any. */
    val activeAlert = MutableStateFlow<Detection?>(null)

    /** Whether a Beep screen is visible; if so we show the alert in-app instead of full screen. */
    val appVisible = MutableStateFlow(false)
}
