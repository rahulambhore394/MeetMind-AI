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
                when (type) {
                    "AI_SUMMARY_READY", "ACTION_ITEMS_EXTRACTED", "DECISIONS_UPDATED", "PARKING_LOT_UPDATED", "CHAPTERS_UPDATED" -> {
                        loadIntelligence()
                    }
                    "ACTION_ITEM_UPDATED" -> {
                        val actionItemId = event["actionItemId"]?.toLongOrNull()
                        val newStatus = event["status"]
                        if (actionItemId != null && newStatus != null) {
                            _uiState.update { current ->
                                val updatedSummary = current.summary?.let { s ->
                                    val updatedItems = s.actionItems.map { item ->
                                        if (item.id == actionItemId) item.copy(status = newStatus) else item
                                    }
                                    s.copy(actionItems = updatedItems)
                                }
                                current.copy(summary = updatedSummary)
                            }
                        } else {
                            loadIntelligence()
                        }
                    }
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

    fun updateActionItemStatus(actionItemId: Long, newStatus: String) {
        // Optimistic UI update for instant feedback
        _uiState.update { current ->
            val updatedSummary = current.summary?.let { s ->
                val updatedItems = s.actionItems.map { item ->
                    if (item.id == actionItemId) item.copy(status = newStatus) else item
                }
                s.copy(actionItems = updatedItems)
            }
            current.copy(summary = updatedSummary)
        }

        viewModelScope.launch {
            val result = intelligenceRepository.updateActionItemStatus(meetingId, actionItemId, newStatus)
            if (result is NetworkResult.Error) {
                // If API failed, reload truth from server
                loadIntelligence()
            }
        }
    }

    fun updateActionItemStatus(actionItemId: Long, isCompleted: Boolean) {
        val newStatus = if (isCompleted) "COMPLETED" else "PENDING"
        updateActionItemStatus(actionItemId, newStatus)
    }

    fun addDecision(decisionText: String) {
        val clean = decisionText.trim()
        if (clean.isBlank()) return

        _uiState.update { current ->
            val updatedSummary = current.summary?.let { s ->
                val newDecisions = s.decisions + clean
                s.copy(decisions = newDecisions)
            }
            current.copy(summary = updatedSummary)
        }

        viewModelScope.launch {
            val result = intelligenceRepository.addDecision(meetingId, clean)
            if (result is NetworkResult.Success) {
                _uiState.update { current ->
                    val updatedSummary = current.summary?.copy(decisions = result.data)
                    current.copy(summary = updatedSummary)
                }
            } else if (result is NetworkResult.Error) {
                loadIntelligence()
            }
        }
    }

    fun deleteDecision(index: Int) {
        _uiState.update { current ->
            val updatedSummary = current.summary?.let { s ->
                val newDecisions = s.decisions.toMutableList().apply {
                    if (index in indices) removeAt(index)
                }
                s.copy(decisions = newDecisions)
            }
            current.copy(summary = updatedSummary)
        }

        viewModelScope.launch {
            val result = intelligenceRepository.deleteDecision(meetingId, index)
            if (result is NetworkResult.Success) {
                _uiState.update { current ->
                    val updatedSummary = current.summary?.copy(decisions = result.data)
                    current.copy(summary = updatedSummary)
                }
            } else if (result is NetworkResult.Error) {
                loadIntelligence()
            }
        }
    }

    fun addParkingQuestion(question: String, askedBy: String = "Team", status: String = "UNANSWERED") {
        val clean = question.trim()
        if (clean.isBlank()) return

        val optimistic = com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ParkingQuestionDto(
            id = System.currentTimeMillis(),
            question = clean,
            askedBy = askedBy,
            status = status,
            timeFormatted = "00:00"
        )

        _uiState.update { current ->
            val updatedSummary = current.summary?.let { s ->
                s.copy(parkingLotItems = s.parkingLotItems + optimistic)
            }
            current.copy(summary = updatedSummary)
        }

        viewModelScope.launch {
            val result = intelligenceRepository.addParkingQuestion(meetingId, clean, askedBy, status)
            if (result is NetworkResult.Success) {
                _uiState.update { current ->
                    val updatedSummary = current.summary?.copy(parkingLotItems = result.data)
                    current.copy(summary = updatedSummary)
                }
            } else if (result is NetworkResult.Error) {
                loadIntelligence()
            }
        }
    }

    fun updateParkingQuestionStatus(index: Int, newStatus: String) {
        _uiState.update { current ->
            val updatedSummary = current.summary?.let { s ->
                val updatedItems = s.parkingLotItems.mapIndexed { i, item ->
                    if (i == index) item.copy(status = newStatus) else item
                }
                s.copy(parkingLotItems = updatedItems)
            }
            current.copy(summary = updatedSummary)
        }

        viewModelScope.launch {
            val result = intelligenceRepository.updateParkingQuestionStatus(meetingId, index, newStatus)
            if (result is NetworkResult.Success) {
                _uiState.update { current ->
                    val updatedSummary = current.summary?.copy(parkingLotItems = result.data)
                    current.copy(summary = updatedSummary)
                }
            } else if (result is NetworkResult.Error) {
                loadIntelligence()
            }
        }
    }

    fun deleteParkingQuestion(index: Int) {
        _uiState.update { current ->
            val updatedSummary = current.summary?.let { s ->
                val newItems = s.parkingLotItems.toMutableList().apply {
                    if (index in indices) removeAt(index)
                }
                s.copy(parkingLotItems = newItems)
            }
            current.copy(summary = updatedSummary)
        }

        viewModelScope.launch {
            val result = intelligenceRepository.deleteParkingQuestion(meetingId, index)
            if (result is NetworkResult.Success) {
                _uiState.update { current ->
                    val updatedSummary = current.summary?.copy(parkingLotItems = result.data)
                    current.copy(summary = updatedSummary)
                }
            } else if (result is NetworkResult.Error) {
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
