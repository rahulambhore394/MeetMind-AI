package com.developer_rahul.meetmind_ai.feature.transcript.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.transcript.domain.model.Transcript
import com.developer_rahul.meetmind_ai.feature.transcript.domain.repository.TranscriptRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TranscriptViewModel(
    private val transcriptRepository: TranscriptRepository,
    private val meetingId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranscriptUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadTranscript()
    }

    fun loadTranscript() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // First list transcripts to find the latest one
            when (val listResult = transcriptRepository.getTranscripts(meetingId)) {
                is NetworkResult.Success -> {
                    val latestTranscript = listResult.data.maxByOrNull { it.id }
                    if (latestTranscript != null) {
                        fetchDetails(latestTranscript.id)
                    } else {
                        _uiState.update { it.copy(isLoading = false, isEmpty = true) }
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = listResult.error.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private suspend fun fetchDetails(transcriptId: Long) {
        when (val result = transcriptRepository.getTranscriptDetails(meetingId, transcriptId)) {
            is NetworkResult.Success -> {
                _uiState.update { it.copy(
                    isLoading = false,
                    transcript = result.data,
                    filteredSegments = result.data.segments,
                    isEmpty = result.data.segments.isEmpty()
                ) }
            }
            is NetworkResult.Error -> {
                _uiState.update { it.copy(isLoading = false, error = result.error.message) }
            }
            else -> {}
        }
    }

    fun onSearch(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                state.transcript?.segments ?: emptyList()
            } else {
                state.transcript?.segments?.filter { 
                    it.text.contains(query, ignoreCase = true) || 
                    it.speaker.contains(query, ignoreCase = true)
                } ?: emptyList()
            }
            state.copy(searchQuery = query, filteredSegments = filtered)
        }
    }
}

data class TranscriptUiState(
    val isLoading: Boolean = false,
    val transcript: Transcript? = null,
    val filteredSegments: List<com.developer_rahul.meetmind_ai.feature.transcript.domain.model.TranscriptSegment> = emptyList(),
    val searchQuery: String = "",
    val isEmpty: Boolean = false,
    val error: String? = null
)
