package com.shankar.beep.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class MicUnavailableException(message: String) : Exception(message)

/**
 * Streams the microphone as 16 kHz mono float chunks.
 *
 * Beep only ever reads audio into memory: nothing is written to disk and
 * nothing is sent anywhere. The stream never requests audio focus, so music
 * and videos keep playing while Beep listens.
 */
class MicStream(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun chunks(chunkSize: Int = CHUNK_SIZE): Flow<FloatArray> = flow {
        if (!hasPermission()) throw MicUnavailableException("Microphone permission is not granted")
        val minBuffer = AudioRecord.getMinBufferSize(
            Dsp.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) throw MicUnavailableException("This device cannot record at 16 kHz")
        val bufferBytes = maxOf(minBuffer, chunkSize * 2 * 8)

        val record = openRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, bufferBytes)
            ?: openRecord(MediaRecorder.AudioSource.MIC, bufferBytes)
            ?: throw MicUnavailableException("The microphone is busy or unavailable")

        val pcm = ShortArray(chunkSize)
        try {
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw MicUnavailableException("The microphone could not be started")
            }
            while (currentCoroutineContext().isActive) {
                var filled = 0
                while (filled < chunkSize) {
                    val read = record.read(pcm, filled, chunkSize - filled)
                    if (read < 0) throw MicUnavailableException("Microphone read failed ($read)")
                    filled += read
                }
                emit(Dsp.pcmToFloat(pcm, chunkSize))
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }.flowOn(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    private fun openRecord(source: Int, bufferBytes: Int): AudioRecord? {
        val record = runCatching {
            AudioRecord(source, Dsp.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferBytes)
        }.getOrNull() ?: return null
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return null
        }
        return record
    }

    companion object {
        /** 50 ms of audio: small enough for a smooth visualiser. */
        const val CHUNK_SIZE = 800
    }
}
