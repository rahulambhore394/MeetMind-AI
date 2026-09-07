package com.developer_rahul.meetmind_ai.feature.transcript.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.transcript.domain.model.Transcript
import com.developer_rahul.meetmind_ai.feature.transcript.domain.model.TranscriptSegment
import com.developer_rahul.meetmind_ai.feature.transcript.domain.repository.TranscriptRepository
import com.developer_rahul.meetmind_ai.feature.translation.domain.model.Subtitle
import com.developer_rahul.meetmind_ai.feature.translation.domain.repository.LiveTranslationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TranscriptViewModel(
    private val transcriptRepository: TranscriptRepository,
    private val liveTranslationRepository: LiveTranslationRepository,
    private val meetingId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranscriptUiState())
    val uiState = _uiState.asStateFlow()

    val availableLanguages = listOf(
        LanguageOption("en", "English 🇬🇧"),
        LanguageOption("hi", "Hindi 🇮🇳 (हिन्दी)"),
        LanguageOption("mr", "Marathi 🇮🇳 (मराठी)"),
        LanguageOption("gu", "Gujarati 🇮🇳 (ગુજરાતી)"),
        LanguageOption("es", "Spanish 🇪🇸 (Español)"),
        LanguageOption("fr", "French 🇫🇷 (Français)"),
        LanguageOption("de", "German 🇩🇪 (Deutsch)"),
        LanguageOption("zh", "Chinese 🇨🇳 (中文)"),
        LanguageOption("ja", "Japanese 🇯🇵 (日本語)")
    )

    init {
        loadTranscript()
        observeLiveSubtitles()
    }

    private fun observeLiveSubtitles() {
        viewModelScope.launch {
            liveTranslationRepository.subtitles.collect { subtitle ->
                if (subtitle.meetingId == meetingId) {
                    val newSegment = TranscriptSegment(
                        id = subtitle.id,
                        speaker = subtitle.speaker,
                        time = formatTimestamp(subtitle.timestamp),
                        text = if (_uiState.value.selectedLanguage != "en" && subtitle.translatedText.isNotBlank()) {
                            subtitle.translatedText
                        } else {
                            subtitle.originalText
                        },
                        startMs = subtitle.timestamp
                    )
                    _uiState.update { state ->
                        val updated = state.liveSegments + newSegment
                        val all = mergeSegments(state.rawSegments, updated)
                        val filtered = filterSegments(all, state.searchQuery)
                        state.copy(
                            liveSegments = updated,
                            filteredSegments = filtered,
                            isEmpty = filtered.isEmpty()
                        )
                    }
                }
            }
        }
    }

    fun loadTranscript() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Subscribe to live translations for target language
            liveTranslationRepository.subscribeToSubtitles(meetingId, _uiState.value.selectedLanguage)
            liveTranslationRepository.setLanguagePreference(meetingId, _uiState.value.selectedLanguage)

            // Fetch stored translations
            val translations = liveTranslationRepository.getTranslations(meetingId, _uiState.value.selectedLanguage)
            val translatedSegments = translations.map { sub ->
                TranscriptSegment(
                    id = sub.id,
                    speaker = sub.speaker,
                    time = formatTimestamp(sub.timestamp),
                    text = if (sub.translatedText.isNotBlank()) sub.translatedText else sub.originalText,
                    startMs = sub.timestamp
                )
            }

            // Also check standard transcripts
            when (val listResult = transcriptRepository.getTranscripts(meetingId)) {
                is NetworkResult.Success -> {
                    val latestTranscript = listResult.data.maxByOrNull { it.id }
                    if (latestTranscript != null) {
                        fetchDetails(latestTranscript.id, translatedSegments)
                    } else {
                        val all = mergeSegments(emptyList(), translatedSegments)
                        _uiState.update { it.copy(
                            isLoading = false,
                            rawSegments = emptyList(),
                            liveSegments = translatedSegments,
                            filteredSegments = all,
                            isEmpty = all.isEmpty()
                        ) }
                    }
                }
                is NetworkResult.Error -> {
                    val all = mergeSegments(emptyList(), translatedSegments)
                    _uiState.update { it.copy(
                        isLoading = false,
                        liveSegments = translatedSegments,
                        filteredSegments = all,
                        isEmpty = all.isEmpty()
                    ) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private suspend fun fetchDetails(transcriptId: Long, liveList: List<TranscriptSegment>) {
        when (val result = transcriptRepository.getTranscriptDetails(meetingId, transcriptId)) {
            is NetworkResult.Success -> {
                val merged = mergeSegments(result.data.segments, liveList)
                val filtered = filterSegments(merged, _uiState.value.searchQuery)
                _uiState.update { it.copy(
                    isLoading = false,
                    transcript = result.data,
                    rawSegments = result.data.segments,
                    filteredSegments = filtered,
                    isEmpty = filtered.isEmpty()
                ) }
            }
            is NetworkResult.Error -> {
                val filtered = filterSegments(liveList, _uiState.value.searchQuery)
                _uiState.update { it.copy(
                    isLoading = false,
                    filteredSegments = filtered,
                    isEmpty = filtered.isEmpty()
                ) }
            }
            else -> {}
        }
    }

    fun selectLanguage(languageCode: String) {
        if (_uiState.value.selectedLanguage == languageCode) return
        val oldLang = _uiState.value.selectedLanguage
        liveTranslationRepository.unsubscribeFromSubtitles(meetingId, oldLang)
        
        _uiState.update { it.copy(selectedLanguage = languageCode) }
        loadTranscript()
    }

    fun onSearch(query: String) {
        _uiState.update { state ->
            val all = mergeSegments(state.rawSegments, state.liveSegments)
            val filtered = filterSegments(all, query)
            state.copy(searchQuery = query, filteredSegments = filtered)
        }
    }

    private fun filterSegments(list: List<TranscriptSegment>, query: String): List<TranscriptSegment> {
        if (query.isBlank()) return list
        return list.filter {
            it.text.contains(query, ignoreCase = true) ||
            it.speaker.contains(query, ignoreCase = true)
        }
    }

    private fun mergeSegments(raw: List<TranscriptSegment>, live: List<TranscriptSegment>): List<TranscriptSegment> {
        val set = mutableMapOf<Long, TranscriptSegment>()
        raw.forEach { set[it.id] = it }
        live.forEach { set[it.id] = it }
        return set.values.sortedBy { it.startMs }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

data class LanguageOption(
    val code: String,
    val displayName: String
)

data class TranscriptUiState(
    val isLoading: Boolean = false,
    val selectedLanguage: String = "en",
    val transcript: Transcript? = null,
    val rawSegments: List<TranscriptSegment> = emptyList(),
    val liveSegments: List<TranscriptSegment> = emptyList(),
    val filteredSegments: List<TranscriptSegment> = emptyList(),
    val searchQuery: String = "",
    val isEmpty: Boolean = false,
    val error: String? = null
)
