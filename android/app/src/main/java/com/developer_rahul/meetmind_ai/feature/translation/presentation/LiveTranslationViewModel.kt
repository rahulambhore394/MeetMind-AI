package com.developer_rahul.meetmind_ai.feature.translation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.media.speech.LiveSpeechProvider
import com.developer_rahul.meetmind_ai.core.media.tts.TextToSpeechManager
import com.developer_rahul.meetmind_ai.feature.translation.domain.model.Subtitle
import com.developer_rahul.meetmind_ai.feature.translation.domain.repository.LiveTranslationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class LiveTranslationViewModel(
    private val translationRepository: LiveTranslationRepository,
    private val speechProvider: LiveSpeechProvider,
    private val textToSpeechManager: TextToSpeechManager,
    private val meetingId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveTranslationUiState())
    val uiState = _uiState.asStateFlow()

    private val _subtitles = MutableStateFlow<List<Subtitle>>(emptyList())
    val subtitles = _subtitles.asStateFlow()

    private var speechJob: Job? = null

    init {
        observeSubtitles()
    }

    private fun observeSubtitles() {
        viewModelScope.launch {
            translationRepository.subtitles.collect { subtitle ->
                if (subtitle.meetingId == meetingId) {
                    _subtitles.update { current ->
                        val newList = current + subtitle
                        if (newList.size > 5) newList.takeLast(5) else newList
                    }
                    if (_uiState.value.audioTranslationEnabled) {
                        textToSpeechManager.speak(subtitle.translatedText, subtitle.targetLanguage)
                    }
                }
            }
        }
    }

    fun toggleSubtitles(enabled: Boolean) {
        _uiState.update { it.copy(subtitlesEnabled = enabled) }
        if (enabled) {
            startLiveTranscription()
            translationRepository.subscribeToSubtitles(meetingId, _uiState.value.selectedTargetLanguage)
            viewModelScope.launch {
                translationRepository.setLanguagePreference(meetingId, _uiState.value.selectedTargetLanguage)
            }
        } else {
            stopLiveTranscription()
            translationRepository.unsubscribeFromSubtitles(meetingId, _uiState.value.selectedTargetLanguage)
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
        _uiState.update { it.copy(sourceLanguage = languageCode) }
        if (_uiState.value.subtitlesEnabled) {
            startLiveTranscription()
        }
    }

    fun setTargetLanguage(languageCode: String) {
        val oldLang = _uiState.value.selectedTargetLanguage
        translationRepository.unsubscribeFromSubtitles(meetingId, oldLang)
        
        _uiState.update { it.copy(selectedTargetLanguage = languageCode) }
        
        if (_uiState.value.subtitlesEnabled) {
            translationRepository.subscribeToSubtitles(meetingId, languageCode)
        }
        
        viewModelScope.launch {
            translationRepository.setLanguagePreference(meetingId, languageCode)
        }
    }

    fun startLiveTranscription() {
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            speechProvider.startListening(_uiState.value.sourceLanguage).collect { result ->
                when (result) {
                    is LiveSpeechProvider.SpeechResult.Final -> {
                        translationRepository.sendLiveSpeech(meetingId, result.text, _uiState.value.sourceLanguage)
                        _uiState.update { it.copy(currentOriginalText = "") }
                        // Automatically restart listening for continuous speech input
                        if (_uiState.value.subtitlesEnabled) {
                            startLiveTranscription()
                        }
                    }
                    is LiveSpeechProvider.SpeechResult.Partial -> {
                        _uiState.update { it.copy(currentOriginalText = result.text) }
                    }
                    is LiveSpeechProvider.SpeechResult.Error -> {
                        _uiState.update { it.copy(error = result.message) }
                    }
                }
            }
        }
    }

    fun stopLiveTranscription() {
        speechJob?.cancel()
        speechJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveTranscription()
        textToSpeechManager.stop()
        translationRepository.unsubscribeFromSubtitles(meetingId, _uiState.value.selectedTargetLanguage)
    }
}

data class LiveTranslationUiState(
    val subtitlesEnabled: Boolean = false,
    val audioTranslationEnabled: Boolean = true,
    val sourceLanguage: String = "en-US",
    val selectedTargetLanguage: String = "en",
    val currentOriginalText: String = "",
    val error: String? = null
)
