package com.shankar.beep.audio

import android.content.Context
import android.util.Log
import com.shankar.beep.model.MonitoredSound
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.audio.classifier.Classifications
import java.io.BufferedReader
import java.io.InputStreamReader

data class ClassifiedSoundResult(
    val monitoredSound: MonitoredSound,
    val matchedLabel: String,
    val confidence: Float
)

class YAMNetClassifier(private val context: Context) {

    private val tag = "YAMNetClassifier"
    private var classifier: AudioClassifier? = null
    private var tensorAudio: TensorAudio? = null
    private val labelMap = mutableMapOf<Int, String>()

    init {
        loadLabelMap()
        initializeClassifier()
    }

    private fun loadLabelMap() {
        try {
            context.assets.open("yamnet_class_map.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header
                    reader.readLine()
                    var line: String? = reader.readLine()
                    while (line != null) {
                        val parts = line.split(",")
                        if (parts.size >= 3) {
                            val index = parts[0].trim().toIntOrNull()
                            // Display name might be quoted if it contains comma
                            val displayName = if (parts.size == 3) {
                                parts[2].trim().replace("\"", "")
                            } else {
                                parts.subList(2, parts.size).joinToString(",").trim().replace("\"", "")
                            }
                            if (index != null) {
                                labelMap[index] = displayName
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
            Log.d(tag, "Loaded ${labelMap.size} YAMNet labels from CSV")
        } catch (e: Exception) {
            Log.e(tag, "Could not load yamnet_class_map.csv", e)
        }
    }

    private fun initializeClassifier() {
        try {
            classifier = AudioClassifier.createFromFile(context, "yamnet.tflite")
            tensorAudio = classifier?.createInputTensorAudio()
            Log.d(tag, "AudioClassifier initialized successfully with yamnet.tflite")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize AudioClassifier from yamnet.tflite", e)
        }
    }

    /**
     * Classifies a 16kHz mono PCM buffer and maps detected events to configured MonitoredSounds.
     */
    fun classifyAudioBuffer(
        buffer: ShortArray,
        readSize: Int,
        enabledSounds: List<MonitoredSound>
    ): List<ClassifiedSoundResult> {
        val currentClassifier = classifier ?: return emptyList()
        val currentTensorAudio = tensorAudio ?: return emptyList()

        return try {
            currentTensorAudio.load(buffer, 0, readSize)
            val output: List<Classifications> = currentClassifier.classify(currentTensorAudio)
            val results = mutableListOf<ClassifiedSoundResult>()

            if (output.isNotEmpty()) {
                val categories = output[0].categories
                for (category in categories) {
                    val label = category.label ?: labelMap[category.index] ?: continue
                    val score = category.score

                    // Check which enabled monitored sound matches this detected label
                    for (sound in enabledSounds) {
                        if (sound.yamnetLabels.isEmpty()) continue

                        val isMatch = sound.yamnetLabels.any { targetLabel ->
                            label.equals(targetLabel, ignoreCase = true) ||
                                label.startsWith("$targetLabel,", ignoreCase = true) ||
                                label.contains(targetLabel, ignoreCase = true)
                        }

                        if (isMatch && score >= sound.confidenceThreshold) {
                            results.add(
                                ClassifiedSoundResult(
                                    monitoredSound = sound,
                                    matchedLabel = label,
                                    confidence = score
                                )
                            )
                        }
                    }
                }
            }
            results.distinctBy { it.monitoredSound.id }
        } catch (e: Exception) {
            Log.e(tag, "Error during audio classification", e)
            emptyList()
        }
    }

    fun close() {
        try {
            classifier?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing classifier", e)
        } finally {
            classifier = null
            tensorAudio = null
        }
    }
}
