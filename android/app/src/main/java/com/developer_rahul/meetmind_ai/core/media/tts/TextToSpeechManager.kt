package com.developer_rahul.meetmind_ai.core.media.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "TextToSpeechManager"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.ENGLISH
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS Utterance started: $utteranceId")
                }
                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS Utterance done: $utteranceId")
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS Utterance error: $utteranceId")
                }
            })
            Log.d(TAG, "TextToSpeech engine successfully initialized")
        } else {
            isInitialized = false
            Log.e(TAG, "TextToSpeech initialization failed with status $status")
        }
    }

    fun speak(text: String, languageCode: String = "en") {
        if (text.isBlank()) return
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not initialized yet. Skipping speech output.")
            return
        }

        val locale = getLocaleForLanguage(languageCode)
        try {
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Language $languageCode ($locale) missing data/not supported. Fallback to English.")
                tts?.language = Locale.ENGLISH
            }
            val utteranceId = "tts_" + System.currentTimeMillis()
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
            Log.d(TAG, "Speaking [$languageCode]: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Error during TTS speak: ${e.message}")
        }
    }

    fun stop() {
        try {
            if (isInitialized && tts != null) {
                tts?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS: ${e.message}")
        }
    }

    fun shutdown() {
        try {
            if (tts != null) {
                tts?.stop()
                tts?.shutdown()
                tts = null
                isInitialized = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS: ${e.message}")
        }
    }

    private fun getLocaleForLanguage(languageCode: String): Locale {
        val cleanCode = languageCode.lowercase().trim()
        return when {
            cleanCode.startsWith("hi") -> Locale("hi", "IN")
            cleanCode.startsWith("mr") -> Locale("mr", "IN")
            cleanCode.startsWith("gu") -> Locale("gu", "IN")
            cleanCode.startsWith("ta") -> Locale("ta", "IN")
            cleanCode.startsWith("te") -> Locale("te", "IN")
            cleanCode.startsWith("kn") -> Locale("kn", "IN")
            cleanCode.startsWith("ml") -> Locale("ml", "IN")
            cleanCode.startsWith("pa") -> Locale("pa", "IN")
            cleanCode.startsWith("bn") -> Locale("bn", "IN")
            cleanCode.startsWith("es") -> Locale("es", "ES")
            cleanCode.startsWith("fr") -> Locale.FRENCH
            cleanCode.startsWith("de") -> Locale.GERMAN
            cleanCode.startsWith("ja") -> Locale.JAPANESE
            cleanCode.startsWith("zh") -> Locale.CHINESE
            cleanCode.startsWith("ru") -> Locale("ru", "RU")
            else -> Locale.ENGLISH
        }
    }
}
