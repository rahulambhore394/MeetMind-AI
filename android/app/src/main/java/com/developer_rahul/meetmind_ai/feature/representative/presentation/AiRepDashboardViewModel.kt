package com.developer_rahul.meetmind_ai.feature.representative.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.MeetingRepository
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting
import com.developer_rahul.meetmind_ai.feature.representative.domain.model.AiRepresentative
import com.developer_rahul.meetmind_ai.feature.representative.domain.repository.LiveRepresentativeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.ArrayList
import java.util.HashMap

class AiRepDashboardViewModel(
    private val meetingRepository: MeetingRepository,
    private val representativeRepository: LiveRepresentativeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiRepDashboardUiState())
    val uiState: StateFlow<AiRepDashboardUiState> = _uiState.asStateFlow()

    init {
        loadRepresentatives()
    }

    private fun loadRepresentatives() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val meetingsResult = meetingRepository.getAllMeetings()
            when (meetingsResult) {
                is NetworkResult.Success -> {
                    val meetings = meetingsResult.data
                    val representatives = ArrayList<AiRepresentative>()
                    
                    for (meeting in meetings) {
                        val repResult = representativeRepository.getRepresentative(meeting.id)
                        if (repResult is NetworkResult.Success) {
                            representatives.add(repResult.data)
                        }
                    }
                    
                    val meetingsMap = HashMap<Long, Meeting>()
                    for (m in meetings) {
                        meetingsMap.put(m.id, m)
                    }
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            representatives = representatives,
                            meetings = meetingsMap
                        ) 
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = meetingsResult.error.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}

data class AiRepDashboardUiState(
    val representatives: List<AiRepresentative> = emptyList(),
    val meetings: Map<Long, Meeting> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)
