package com.developer_rahul.meetmind_ai.core.media.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.ArrayList

class LiveSpeechProvider(private val context: Context) {

    private val TAG = "LiveSpeechProvider"
    private val mainHandler = Handler(Looper.getMainLooper())

    fun startListening(language: String = "en-US"): Flow<SpeechResult> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(SpeechResult.Error("Speech recognition not available on this device", isTransient = false))
            close()
            return@callbackFlow
        }

        var speechRecognizer: SpeechRecognizer? = null

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech ($language)")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
            }

            override fun onError(error: Int) {
                val (message, isTransient) = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error" to true
                    SpeechRecognizer.ERROR_CLIENT -> "Client error" to true
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required" to false
                    SpeechRecognizer.ERROR_NETWORK -> "Network issue" to true
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout" to true
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized" to true
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy" to true
                    SpeechRecognizer.ERROR_SERVER -> "Server error" to true
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Silence timeout" to true
                    13 -> "Language not supported on-device" to false
                    14 -> "Speech recognition language unavailable" to false
                    else -> "Speech recognition error ($error)" to false
                }
                Log.d(TAG, "onError: $message (code=$error, transient=$isTransient)")
                trySend(SpeechResult.Error(message, isTransient))
                close()
            }

            override fun onResults(results: Bundle?) {
                val matches: ArrayList<String>? = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty() && matches[0].isNotBlank()) {
                    Log.d(TAG, "onResults: ${matches[0]}")
                    trySend(SpeechResult.Final(matches[0]))
                }
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches: ArrayList<String>? = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty() && matches[0].isNotBlank()) {
                    trySend(SpeechResult.Partial(matches[0]))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        mainHandler.post {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(listener)
                    startListening(recognizerIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognizer: ${e.message}")
                trySend(SpeechResult.Error("Failed to initialize speech: ${e.message}", isTransient = false))
                close()
            }
        }

        awaitClose {
            mainHandler.post {
                try {
                    speechRecognizer?.stopListening()
                    speechRecognizer?.destroy()
                } catch (e: Exception) {
                    Log.w(TAG, "Error destroying speech recognizer: ${e.message}")
                }
                speechRecognizer = null
            }
        }
    }

    sealed class SpeechResult {
        data class Partial(val text: String) : SpeechResult()
        data class Final(val text: String) : SpeechResult()
        data class Error(val message: String, val isTransient: Boolean = true) : SpeechResult()
    }
}
