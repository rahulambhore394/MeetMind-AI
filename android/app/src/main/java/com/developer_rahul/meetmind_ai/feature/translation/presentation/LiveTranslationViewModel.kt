package com.developer_rahul.meetmind_ai.feature.translation.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.media.speech.LiveSpeechProvider
import com.developer_rahul.meetmind_ai.core.media.tts.TextToSpeechManager
import com.developer_rahul.meetmind_ai.feature.translation.domain.model.Subtitle
import com.developer_rahul.meetmind_ai.feature.translation.domain.repository.LiveTranslationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LiveTranslationViewModel(
    private val translationRepository: LiveTranslationRepository,
    private val speechProvider: LiveSpeechProvider,
    private val textToSpeechManager: TextToSpeechManager,
    private val meetingId: Long
) : ViewModel() {

    private var activeMeetingId: Long = meetingId
    private val TAG = "LiveTranslationVM"
    private var currentUserName: String = ""
    private var isAudioMuted: Boolean = false

    private val _uiState = MutableStateFlow(LiveTranslationUiState())
    val uiState = _uiState.asStateFlow()

    private val _subtitles = MutableStateFlow<List<Subtitle>>(emptyList())
    val subtitles = _subtitles.asStateFlow()

    private var speechJob: Job? = null

    init {
        observeSubtitles()
    }

    fun setCurrentUserName(name: String) {
        currentUserName = name
    }

    fun onAudioMuteChanged(isMuted: Boolean) {
        if (isAudioMuted == isMuted) return
        isAudioMuted = isMuted
        if (isMuted) {
            stopLiveTranscription()
        } else if (_uiState.value.subtitlesEnabled) {
            startLiveTranscription()
        }
    }

    fun updateMeetingId(newMeetingId: Long) {
        if (newMeetingId > 0L && activeMeetingId != newMeetingId) {
            val oldId = activeMeetingId
            val currentLang = _uiState.value.selectedTargetLanguage
            if (_uiState.value.subtitlesEnabled) {
                translationRepository.unsubscribeFromSubtitles(oldId, currentLang)
            }
            activeMeetingId = newMeetingId
            if (_uiState.value.subtitlesEnabled) {
                translationRepository.subscribeToSubtitles(activeMeetingId, currentLang)
                viewModelScope.launch {
                    translationRepository.setLanguagePreference(activeMeetingId, currentLang)
                }
            }
        }
    }

    private fun observeSubtitles() {
        viewModelScope.launch {
            translationRepository.subtitles.collect { subtitle ->
                if (subtitle.meetingId == activeMeetingId) {
                    _subtitles.update { current ->
                        // Deduplicate if we already displayed an optimistic local "You" entry for this original text
                        val filtered = current.filterNot { 
                            it.speaker == "You" && it.originalText.equals(subtitle.originalText, ignoreCase = true) 
                        }
                        val newList = filtered + subtitle
                        if (newList.size > 4) newList.takeLast(4) else newList
                    }
                    if (_uiState.value.audioTranslationEnabled) {
                        val isSelf = subtitle.speaker.equals("You", ignoreCase = true) ||
                                (currentUserName.isNotBlank() && subtitle.speaker.equals(currentUserName, ignoreCase = true))
                        if (!isSelf) {
                            textToSpeechManager.speak(subtitle.translatedText, subtitle.targetLanguage)
                        }
                    }

                    // Auto-fade older subtitles after 7 seconds of inactivity
                    launch {
                        delay(7000)
                        _subtitles.update { current ->
                            val cutoff = System.currentTimeMillis() - 7000
                            current.filter { it.timestamp > cutoff }
                        }
                    }
                }
            }
        }
    }

    fun toggleSubtitles(enabled: Boolean) {
        _uiState.update { it.copy(subtitlesEnabled = enabled, error = null) }
        if (enabled) {
            startLiveTranscription()
            translationRepository.subscribeToSubtitles(activeMeetingId, _uiState.value.selectedTargetLanguage)
            viewModelScope.launch {
                translationRepository.setLanguagePreference(activeMeetingId, _uiState.value.selectedTargetLanguage)
            }
        } else {
            stopLiveTranscription()
            translationRepository.unsubscribeFromSubtitles(activeMeetingId, _uiState.value.selectedTargetLanguage)
            _subtitles.value = emptyList()
            textToSpeechManager.stop()
        }
    }

    fun toggleAudioTranslation(enabled: Boolean) {
        _uiState.update { it.copy(audioTranslationEnabled = enabled) }
        if (!enabled) {
            textToSpeechManager.stop()
        }
    }

    fun setSourceLanguage(languageCode: String) {
        if (_uiState.value.sourceLanguage == languageCode) return
        _uiState.update { it.copy(sourceLanguage = languageCode, error = null) }
        if (_uiState.value.subtitlesEnabled) {
            startLiveTranscription()
        }
    }

    fun setTargetLanguage(languageCode: String) {
        val oldLang = _uiState.value.selectedTargetLanguage
        if (oldLang == languageCode) return
        translationRepository.unsubscribeFromSubtitles(activeMeetingId, oldLang)

        _uiState.update { it.copy(selectedTargetLanguage = languageCode) }

        if (_uiState.value.subtitlesEnabled) {
            translationRepository.subscribeToSubtitles(activeMeetingId, languageCode)
        }

        viewModelScope.launch {
            translationRepository.setLanguagePreference(activeMeetingId, languageCode)
        }
    }

    fun startLiveTranscription() {
        if (isAudioMuted) {
            Log.d(TAG, "Mic is muted; not starting speech recognition")
            return
        }
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            Log.d(TAG, "Starting continuous live speech recognition in ${_uiState.value.sourceLanguage}")
            var consecutiveErrors = 0
            while (isActive && _uiState.value.subtitlesEnabled && !isAudioMuted) {
                try {
                    speechProvider.startListening(_uiState.value.sourceLanguage).collect { result ->
                        when (result) {
                            is LiveSpeechProvider.SpeechResult.Final -> {
                                consecutiveErrors = 0
                                val spoken = result.text.trim()
                                if (spoken.isNotEmpty()) {
                                    Log.d(TAG, "Spoken speech final: '$spoken'")
                                    // Instantly show local speaking subtitle in the overlay
                                    val optimisticSubtitle = Subtitle(
                                        id = System.currentTimeMillis(),
                                        meetingId = activeMeetingId,
                                        speaker = "You",
                                        originalText = spoken,
                                        translatedText = spoken,
                                        sourceLanguage = _uiState.value.sourceLanguage,
                                        targetLanguage = _uiState.value.selectedTargetLanguage,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    _subtitles.update { current ->
                                        val newList = current + optimisticSubtitle
                                        if (newList.size > 4) newList.takeLast(4) else newList
                                    }

                                    translationRepository.sendLiveSpeech(
                                        meetingId = activeMeetingId,
                                        text = spoken,
                                        sourceLanguage = _uiState.value.sourceLanguage
                                    )
                                }
                                _uiState.update { it.copy(currentOriginalText = "") }
                            }
                            is LiveSpeechProvider.SpeechResult.Partial -> {
                                consecutiveErrors = 0
                                _uiState.update { it.copy(currentOriginalText = result.text) }
                            }
                            is LiveSpeechProvider.SpeechResult.Error -> {
                                if (!result.isTransient) {
                                    Log.e(TAG, "Fatal speech recognition error: ${result.message}")
                                    _uiState.update { it.copy(error = result.message) }
                                    return@collect
                                } else {
                                    consecutiveErrors++
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Exception during speech collection: ${e.message}")
                    consecutiveErrors++
                }

                // Stop loop if a non-transient error occurred
                if (_uiState.value.error != null) {
                    break
                }

                // Debounce between speech sessions, backing off if consecutive errors occur
                if (_uiState.value.subtitlesEnabled && isActive) {
                    val backoff = if (consecutiveErrors > 3) 2000L else 400L
                    delay(backoff)
                }
            }
        }
    }

    fun stopLiveTranscription() {
        speechJob?.cancel()
        speechJob = null
        _uiState.update { it.copy(currentOriginalText = "") }
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveTranscription()
        textToSpeechManager.stop()
        translationRepository.unsubscribeFromSubtitles(activeMeetingId, _uiState.value.selectedTargetLanguage)
    }
}

data class LiveTranslationUiState(
    val subtitlesEnabled: Boolean = false,
    val audioTranslationEnabled: Boolean = false,
    val sourceLanguage: String = "en-US",
    val selectedTargetLanguage: String = "hi",
    val currentOriginalText: String = "",
    val error: String? = null
) {
    val activeLanguageBadge: String
        get() {
            val src = sourceLanguage.take(2).uppercase()
            val tgt = selectedTargetLanguage.take(2).uppercase()
            return "$src ➔ $tgt"
        }
}
