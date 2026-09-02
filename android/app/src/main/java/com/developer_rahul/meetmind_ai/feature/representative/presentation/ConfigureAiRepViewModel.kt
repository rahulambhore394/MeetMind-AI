package com.developer_rahul.meetmind_ai.feature.representative.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.MeetingRepository
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.MeetingStatus
import com.developer_rahul.meetmind_ai.feature.representative.domain.model.AiRepresentative
import com.developer_rahul.meetmind_ai.feature.representative.domain.repository.LiveRepresentativeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ConfigureAiRepViewModel(
    private val meetingRepository: MeetingRepository,
    private val representativeRepository: LiveRepresentativeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigureAiRepUiState())
    val uiState: StateFlow<ConfigureAiRepUiState> = _uiState.asStateFlow()

    init {
        loadUpcomingMeetings()
    }

    private fun loadUpcomingMeetings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMeetings = true) }
            val result = meetingRepository.getAllMeetings()
            when (result) {
                is NetworkResult.Success -> {
                    val meetingsList: List<Meeting> = result.data
                    val upcoming = meetingsList.filter { it.status == MeetingStatus.SCHEDULED }
                    _uiState.update { it.copy(upcomingMeetings = upcoming, isLoadingMeetings = false) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoadingMeetings = false, error = result.error.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoadingMeetings = false) }
                }
            }
        }
    }

    fun selectMeeting(meeting: Meeting) {
        _uiState.update { it.copy(selectedMeeting = meeting) }
    }

    fun updateTopics(topics: String) {
        _uiState.update { it.copy(monitoredTopics = topics) }
    }

    fun updateQuestions(questions: String) {
        _uiState.update { it.copy(monitoredQuestions = questions) }
    }

    fun updateImportantPeople(people: String) {
        _uiState.update { it.copy(importantPeople = people) }
    }

    fun saveConfiguration() {
        val meetingId = _uiState.value.selectedMeeting?.id ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val topics = _uiState.value.monitoredTopics.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val questions = _uiState.value.monitoredQuestions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val people = _uiState.value.importantPeople.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            
            val result = representativeRepository.createRepresentative(
                meetingId = meetingId,
                monitoredTopics = topics,
                monitoredQuestions = questions,
                importantPeople = people
            )
            
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true, representative = result.data) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.error.message) }
                }
                else -> {
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun uploadMedia(mediaPath: String) {
        val meetingId = _uiState.value.selectedMeeting?.id ?: return
        val repId = _uiState.value.representative?.id ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingMedia = true) }
            val result = representativeRepository.uploadMedia(meetingId, repId, mediaPath)
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isUploadingMedia = false, representative = result.data) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isUploadingMedia = false, error = result.error.message) }
                }
                else -> {
                    _uiState.update { it.copy(isUploadingMedia = false) }
                }
            }
        }
    }
}

data class ConfigureAiRepUiState(
    val upcomingMeetings: List<Meeting> = emptyList(),
    val selectedMeeting: Meeting? = null,
    val monitoredTopics: String = "",
    val monitoredQuestions: String = "",
    val importantPeople: String = "",
    val isLoadingMeetings: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isUploadingMedia: Boolean = false,
    val representative: AiRepresentative? = null,
    val error: String? = null
)
