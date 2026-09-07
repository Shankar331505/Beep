package com.shankar.beep.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioCaptureEngine(
    private val onAudioChunkReady: (buffer: ShortArray, readSize: Int) -> Unit
) {

    private val tag = "AudioCaptureEngine"
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    // YAMNet standard window is 15600 samples (~0.975 seconds)
    // We sample in sub-chunks of 8000 samples (0.5s) with overlapping rolling buffer
    private val frameSize = 8000
    private val rollingBufferSize = 15600
    private val rollingBuffer = ShortArray(rollingBufferSize)

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    fun startCapture() {
        if (recordingJob?.isActive == true) return

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, frameSize * 2)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(tag, "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            Log.d(tag, "AudioRecord started successfully at $sampleRate Hz")

            recordingJob = scope.launch {
                val readBuffer = ShortArray(frameSize)

                while (isActive) {
                    val read = audioRecord?.read(readBuffer, 0, frameSize) ?: 0
                    if (read > 0 && read <= rollingBufferSize) {
                        System.arraycopy(
                            rollingBuffer,
                            read,
                            rollingBuffer,
                            0,
                            rollingBufferSize - read
                        )
                        System.arraycopy(
                            readBuffer,
                            0,
                            rollingBuffer,
                            rollingBufferSize - read,
                            read
                        )
                        onAudioChunkReady(rollingBuffer, rollingBufferSize)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error starting AudioCaptureEngine", e)
        }
    }

    fun stopCapture() {
        try {
            recordingJob?.cancel()
            recordingJob = null
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d(tag, "AudioCaptureEngine stopped")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping AudioCaptureEngine", e)
        }
    }
}
