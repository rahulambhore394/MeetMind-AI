package com.developer_rahul.meetmind_ai.feature.translation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.media.speech.LiveSpeechProvider
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
                }
            }
        }
    }

    fun toggleSubtitles(enabled: Boolean) {
        _uiState.update { it.copy(subtitlesEnabled = enabled) }
        if (enabled) {
            startLiveTranscription()
            translationRepository.subscribeToSubtitles(meetingId, _uiState.value.selectedTargetLanguage)
        } else {
            stopLiveTranscription()
            translationRepository.unsubscribeFromSubtitles(meetingId, _uiState.value.selectedTargetLanguage)
            _subtitles.value = emptyList()
        }
    }

    fun setTargetLanguage(languageCode: String) {
        val oldLang = _uiState.value.selectedTargetLanguage
        translationRepository.unsubscribeFromSubtitles(meetingId, oldLang)
        
        _uiState.update { it.copy(selectedTargetLanguage = languageCode) }
        translationRepository.subscribeToSubtitles(meetingId, languageCode)
        
        viewModelScope.launch {
            translationRepository.setLanguagePreference(meetingId, languageCode)
        }
    }

    private fun startLiveTranscription() {
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            speechProvider.startListening(_uiState.value.sourceLanguage).collect { result ->
                when (result) {
                    is LiveSpeechProvider.SpeechResult.Final -> {
                        translationRepository.sendLiveSpeech(meetingId, result.text, _uiState.value.sourceLanguage)
                        _uiState.update { it.copy(currentOriginalText = "") }
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

    private fun stopLiveTranscription() {
        speechJob?.cancel()
        speechJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveTranscription()
        translationRepository.unsubscribeFromSubtitles(meetingId, _uiState.value.selectedTargetLanguage)
    }
}

data class LiveTranslationUiState(
    val subtitlesEnabled: Boolean = false,
    val sourceLanguage: String = "en-US",
    val selectedTargetLanguage: String = "en",
    val currentOriginalText: String = "",
    val error: String? = null
)
