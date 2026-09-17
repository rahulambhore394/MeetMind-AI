package com.developer_rahul.meetmind_ai.feature.meetingroom.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.media.tts.TextToSpeechManager
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import com.developer_rahul.meetmind_ai.feature.meetingroom.data.repository.MeetingCallRepository
import com.developer_rahul.meetmind_ai.feature.meetingroom.domain.model.MeetingCallEvent
import com.developer_rahul.meetmind_ai.feature.meetingroom.domain.model.ParticipantMediaState
import com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto.AiProxySpeechDto
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.webrtc.VideoTrack

import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.MeetingRepository
import android.util.Log

class LiveMeetingViewModel(
    private val meetingCallRepository: MeetingCallRepository,
    private val meetingRepository: MeetingRepository,
    private val meetingWebSocketManager: MeetingWebSocketManager,
    private val textToSpeechManager: TextToSpeechManager,
    private val meetingId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveMeetingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeEvents()
        observeAiProxySpeech()
        joinMeeting()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            meetingCallRepository.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun observeAiProxySpeech() {
        viewModelScope.launch {
            meetingWebSocketManager.aiProxySpeech.collect { dto ->
                if (dto.meetingId == meetingId) {
                    Log.d("LiveMeetingVM", "AI Proxy Speech received: ${dto.spokenText} for owner ${dto.ownerName}")
                    _uiState.update { it.copy(activeAiSpeech = dto) }

                    // Synthesize spoken voice in real time through in-call audio
                    textToSpeechManager.speak(dto.spokenText, dto.language)

                    // Animate AI Representative speaking state
                    updateParticipantState(dto.ownerId) { it.copy(isSpeaking = true) }

                    // Auto-dismiss speaking indicator after estimated speech duration
                    val words = dto.spokenText.split("\\s+".toRegex()).size
                    val durationMs = ((words / 2.5) * 1000L).toLong().coerceIn(3500L, 20000L)
                    launch {
                        kotlinx.coroutines.delay(durationMs)
                        _uiState.update { current ->
                            if (current.activeAiSpeech == dto) current.copy(activeAiSpeech = null) else current
                        }
                        updateParticipantState(dto.ownerId) { it.copy(isSpeaking = false) }
                    }
                }
            }
        }
    }

    private fun joinMeeting() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                meetingRepository.joinMeeting(meetingId)
            } catch (e: Exception) {
                Log.e("LiveMeetingVM", "Error executing REST joinMeeting: ${e.message}")
            }
            meetingWebSocketManager.subscribeToAiProxySpeech(meetingId)
            meetingCallRepository.joinMeeting(meetingId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun handleEvent(event: MeetingCallEvent) {
        when (event) {
            is MeetingCallEvent.LocalStreamReady -> {
                _uiState.update { it.copy(localVideoTrack = event.videoTrack) }
            }
            is MeetingCallEvent.LocalScreenStreamReady -> {
                _uiState.update { it.copy(localScreenTrack = event.videoTrack, isScreenSharing = (event.videoTrack != null)) }
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

    fun switchCamera() {
        meetingCallRepository.switchCamera()
    }

    fun startScreenSharing(mediaProjectionData: android.content.Intent) {
        meetingCallRepository.startScreenSharing(mediaProjectionData)
        val track = meetingCallRepository.getLocalScreenTrack()
        if (track != null) {
            _uiState.update { it.copy(isScreenSharing = true, localScreenTrack = track) }
        } else {
            _uiState.update { it.copy(isScreenSharing = false, localScreenTrack = null, error = "Failed to start screen capture") }
        }
    }

    fun stopScreenSharing() {
        meetingCallRepository.stopScreenSharing()
        _uiState.update { it.copy(isScreenSharing = false, localScreenTrack = null) }
    }

    fun leaveMeeting() {
        viewModelScope.launch {
            try {
                meetingRepository.leaveMeeting(meetingId)
            } catch (e: Exception) {
                Log.e("LiveMeetingVM", "Error executing REST leaveMeeting: ${e.message}")
            }
            meetingWebSocketManager.unsubscribeFromAiProxySpeech(meetingId)
            textToSpeechManager.stop()
            meetingCallRepository.leaveMeeting()
        }
    }

    override fun onCleared() {
        super.onCleared()
        meetingWebSocketManager.unsubscribeFromAiProxySpeech(meetingId)
        textToSpeechManager.stop()
        meetingCallRepository.leaveMeeting()
    }
}

data class LiveMeetingUiState(
    val isLoading: Boolean = false,
    val localVideoTrack: VideoTrack? = null,
    val localScreenTrack: VideoTrack? = null,
    val participants: Map<Long, ParticipantMediaState> = emptyMap(),
    val isScreenSharing: Boolean = false,
    val error: String? = null,
    val activeAiSpeech: AiProxySpeechDto? = null
)
