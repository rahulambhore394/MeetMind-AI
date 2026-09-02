package com.developer_rahul.meetmind_ai.feature.representative.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.representative.domain.model.AiRepresentative
import com.developer_rahul.meetmind_ai.feature.representative.domain.repository.LiveRepresentativeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AiRepStatusViewModel(
    private val meetingId: Long,
    private val representativeRepository: LiveRepresentativeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiRepStatusUiState())
    val uiState: StateFlow<AiRepStatusUiState> = _uiState.asStateFlow()

    init {
        loadRepresentativeStatus()
        observeStatusUpdates()
    }

    private fun loadRepresentativeStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = representativeRepository.getRepresentative(meetingId)
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, representative = result.data) }
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

    private fun observeStatusUpdates() {
        representativeRepository.representativeEvents
            .onEach { event ->
                val eventMeetingId = event["meetingId"]?.toLongOrNull()
                if (eventMeetingId == meetingId) {
                    val status = event["status"]
                    if (status != null) {
                        _uiState.update { state ->
                            val currentRep = state.representative
                            if (currentRep != null) {
                                state.copy(representative = currentRep.copy(status = status))
                            } else {
                                state
                            }
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun cancelRepresentative() {
        val repId = _uiState.value.representative?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true) }
            val result = representativeRepository.cancelRepresentative(meetingId, repId)
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isCancelling = false, representative = result.data) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isCancelling = false, error = result.error.message) }
                }
                else -> {
                    _uiState.update { it.copy(isCancelling = false) }
                }
            }
        }
    }
}

data class AiRepStatusUiState(
    val representative: AiRepresentative? = null,
    val isLoading: Boolean = false,
    val isCancelling: Boolean = false,
    val error: String? = null
)
