package com.developer_rahul.meetmind_ai.feature.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.SummaryDetailResponseDto
import com.developer_rahul.meetmind_ai.feature.intelligence.data.repository.IntelligenceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MeetingIntelligenceViewModel(
    private val intelligenceRepository: IntelligenceRepository,
    private val meetingId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeetingIntelligenceUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadIntelligence()
        observeIntelligenceEvents()
    }

    private fun observeIntelligenceEvents() {
        intelligenceRepository.subscribeToIntelligence(meetingId)
        viewModelScope.launch {
            intelligenceRepository.intelligenceEvents.collect { event ->
                val type = event["type"]
                if ("AI_SUMMARY_READY" == type) {
                    loadIntelligence()
                }
            }
        }
    }

    fun loadIntelligence() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = intelligenceRepository.getSummary(meetingId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        summary = result.data,
                        status = result.data.status
                    ) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun updateActionItemStatus(actionItemId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            val status = if (isCompleted) "COMPLETED" else "OPEN"
            val result = intelligenceRepository.updateActionItemStatus(meetingId, actionItemId, status)
            if (result is NetworkResult.Success) {
                // Refresh to get updated list
                loadIntelligence()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        intelligenceRepository.unsubscribeFromIntelligence(meetingId)
    }
}

data class MeetingIntelligenceUiState(
    val isLoading: Boolean = false,
    val summary: SummaryDetailResponseDto? = null,
    val status: String? = null,
    val error: String? = null
)
