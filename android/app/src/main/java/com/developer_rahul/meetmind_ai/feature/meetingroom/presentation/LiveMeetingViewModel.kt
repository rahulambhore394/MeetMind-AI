package com.developer_rahul.meetmind_ai.feature.meetingroom.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.feature.meetingroom.data.repository.MeetingCallRepository
import com.developer_rahul.meetmind_ai.feature.meetingroom.domain.model.MeetingCallEvent
import com.developer_rahul.meetmind_ai.feature.meetingroom.domain.model.ParticipantMediaState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.webrtc.VideoTrack

class LiveMeetingViewModel(
    private val meetingCallRepository: MeetingCallRepository,
    private val meetingId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveMeetingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeEvents()
        joinMeeting()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            meetingCallRepository.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun joinMeeting() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            meetingCallRepository.joinMeeting(meetingId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun handleEvent(event: MeetingCallEvent) {
        when (event) {
            is MeetingCallEvent.LocalStreamReady -> {
                _uiState.update { it.copy(localVideoTrack = event.videoTrack) }
            }
            is MeetingCallEvent.RemoteStreamReady -> {
                updateParticipantState(event.userId) { it.copy(videoTrack = event.videoTrack, videoEnabled = true) }
            }
            is MeetingCallEvent.RemoteStreamRemoved -> {
                removeParticipant(event.userId)
            }
            is MeetingCallEvent.MediaStateChanged -> {
                updateParticipantState(event.userId) {
                    it.copy(
                        audioEnabled = event.audioEnabled ?: it.audioEnabled,
                        videoEnabled = event.videoEnabled ?: it.videoEnabled,
                        isSpeaking = event.isSpeaking ?: it.isSpeaking
                    )
                }
            }
            is MeetingCallEvent.ConnectionStateChanged -> {
                updateParticipantState(event.userId) { it.copy(connectionState = event.state) }
            }
            is MeetingCallEvent.ScreenShareStarted -> {
                updateParticipantState(event.userId) { it.copy(screenTrack = event.videoTrack, isScreenSharing = true) }
            }
            is MeetingCallEvent.ScreenShareStopped -> {
                updateParticipantState(event.userId) { it.copy(screenTrack = null, isScreenSharing = false) }
            }
            is MeetingCallEvent.Error -> {
                _uiState.update { it.copy(error = event.message) }
            }
        }
    }

    private fun updateParticipantState(userId: Long, update: (ParticipantMediaState) -> ParticipantMediaState) {
        _uiState.update { state ->
            val updatedParticipants = state.participants.toMutableMap()
            val current = updatedParticipants[userId] ?: ParticipantMediaState(userId = userId)
            updatedParticipants[userId] = update(current)
            state.copy(participants = updatedParticipants)
        }
    }

    private fun removeParticipant(userId: Long) {
        _uiState.update { state ->
            val updatedParticipants = state.participants.toMutableMap()
            updatedParticipants.remove(userId)
            state.copy(participants = updatedParticipants)
        }
    }

    fun setAudioEnabled(enabled: Boolean) {
        meetingCallRepository.setAudioEnabled(enabled)
    }

    fun setVideoEnabled(enabled: Boolean) {
        meetingCallRepository.setVideoEnabled(enabled)
    }

    fun startScreenSharing(mediaProjectionData: android.content.Intent) {
        meetingCallRepository.startScreenSharing(mediaProjectionData)
        _uiState.update { it.copy(isScreenSharing = true) }
    }

    fun stopScreenSharing() {
        meetingCallRepository.stopScreenSharing()
        _uiState.update { it.copy(isScreenSharing = false) }
    }

    fun leaveMeeting() {
        meetingCallRepository.leaveMeeting()
    }

    override fun onCleared() {
        super.onCleared()
        meetingCallRepository.leaveMeeting()
    }
}

data class LiveMeetingUiState(
    val isLoading: Boolean = false,
    val localVideoTrack: VideoTrack? = null,
    val participants: Map<Long, ParticipantMediaState> = emptyMap(),
    val isScreenSharing: Boolean = false,
    val error: String? = null
)
