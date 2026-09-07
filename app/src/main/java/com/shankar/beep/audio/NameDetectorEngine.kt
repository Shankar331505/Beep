package com.shankar.beep.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class NameDetectorEngine(
    private val context: Context,
    private val onNameDetected: (confidence: Float) -> Unit
) {

    private val tag = "NameDetectorEngine"
    private var speechRecognizer: SpeechRecognizer? = null
    private var targetName: String = ""
    private var aliases: List<String> = emptyList()
    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun updateTargetName(name: String) {
        val trimmed = name.trim().lowercase()
        targetName = trimmed
        aliases = if (trimmed.isNotEmpty()) {
            listOf(
                trimmed,
                "hey $trimmed",
                "hello $trimmed",
                "hi $trimmed",
                "ok $trimmed"
            )
        } else {
            emptyList()
        }
        Log.d(tag, "Target name updated: $targetName, aliases: $aliases")
    }

    fun startListening() {
        if (targetName.isEmpty()) return

        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    initializeRecognizer()
                    startRecognizerIntent()
                    isListening = true
                } else {
                    Log.w(tag, "Speech recognition not available on this device")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error starting speech recognition", e)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            isListening = false
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e(tag, "Error stopping speech recognition", e)
            }
        }
    }

    private fun initializeRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    // Automatically restart listening if still enabled
                    if (isListening) {
                        mainHandler.postDelayed({
                            if (isListening) {
                                startRecognizerIntent()
                            }
                        }, 500)
                    }
                }

                override fun onResults(results: Bundle?) {
                    handleSpeechResults(results)
                    if (isListening) {
                        mainHandler.postDelayed({
                            if (isListening) {
                                startRecognizerIntent()
                            }
                        }, 300)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    handleSpeechResults(partialResults)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startRecognizerIntent() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Prefer offline recognition where available
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start listening intent", e)
        }
    }

    private fun handleSpeechResults(bundle: Bundle?) {
        val matches = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        for (match in matches) {
            val lower = match.lowercase()
            for (alias in aliases) {
                if (lower.contains(alias) || fuzzyMatch(lower, alias)) {
                    Log.i(tag, "Name detected in speech: '$lower' matches target '$alias'")
                    onNameDetected(0.92f)
                    return
                }
            }
        }
    }

    /**
     * Fallback lightweight string distance match for phonetic tolerances.
     */
    private fun fuzzyMatch(text: String, target: String): Boolean {
        if (target.length < 3) return false
        val words = text.split(" ")
        for (word in words) {
            if (levenshteinDistance(word, target) <= 1) {
                return true
            }
        }
        return false
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
