package com.developer_rahul.meetmind_ai.feature.meetingroom.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.media.audio.AudioDeviceManager
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
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import android.util.Log

class LiveMeetingViewModel(
    private val meetingCallRepository: MeetingCallRepository,
    private val meetingRepository: MeetingRepository,
    private val meetingWebSocketManager: MeetingWebSocketManager,
    private val textToSpeechManager: TextToSpeechManager,
    private val meetingId: Long
) : ViewModel() {

    private var activeMeetingId: Long = meetingId
    private val _uiState = MutableStateFlow(LiveMeetingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeEvents()
        observeAiProxySpeech()
    }

    private var callStarted = false

    fun startCall(targetMeetingId: Long? = null) {
        if (targetMeetingId != null && targetMeetingId > 0L) {
            activeMeetingId = targetMeetingId
        }
        if (callStarted) return
        callStarted = true
        joinMeeting()
    }

    fun updateMeetingId(newMeetingId: Long) {
        if (newMeetingId > 0L && activeMeetingId != newMeetingId) {
            val oldId = activeMeetingId
            activeMeetingId = newMeetingId
            if (callStarted) {
                meetingWebSocketManager.unsubscribeFromAiProxySpeech(oldId)
                meetingWebSocketManager.subscribeToAiProxySpeech(newMeetingId)
            }
        }
    }

    fun initAudioRouting(context: android.content.Context) {
        selectAudioDevice(context, _uiState.value.selectedAudioDevice)
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
                if (dto.meetingId == activeMeetingId) {
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
            val result = meetingRepository.joinMeeting(activeMeetingId)
            if (result is NetworkResult.Error) {
                Log.w("LiveMeetingVM", "REST joinMeeting non-fatal error: ${result.error.message}, proceeding to start media call...")
            }
            meetingWebSocketManager.subscribeToAiProxySpeech(activeMeetingId)
            try {
                meetingCallRepository.joinMeeting(activeMeetingId)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                callStarted = false
                Log.e("LiveMeetingVM", "Error starting WebRTC call: ${e.message}", e)
                _uiState.update {
                    it.copy(isLoading = false, error = "Unable to start camera and microphone: ${e.message}")
                }
            }
        }
    }

    private fun handleEvent(event: MeetingCallEvent) {
        when (event) {
            is MeetingCallEvent.LocalStreamReady -> {
                _uiState.update { it.copy(localVideoTrack = event.videoTrack, localSfuVideoTrack = event.sfuVideoTrack) }
            }
            is MeetingCallEvent.LocalScreenStreamReady -> {
                _uiState.update {
                    it.copy(
                        localScreenTrack = event.videoTrack,
                        localSfuScreenTrack = event.sfuVideoTrack,
                        isScreenSharing = (event.videoTrack != null || event.sfuVideoTrack != null)
                    )
                }
            }
            is MeetingCallEvent.RemoteStreamReady -> {
                updateParticipantState(event.userId) {
                    it.copy(
                        videoTrack = event.videoTrack,
                        sfuVideoTrack = event.sfuVideoTrack,
                        videoEnabled = true
                    )
                }
            }
            is MeetingCallEvent.RemoteStreamRemoved -> {
                removeParticipantFromMap(event.userId)
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
                updateParticipantState(event.userId) {
                    it.copy(
                        screenTrack = event.videoTrack,
                        sfuScreenTrack = event.sfuVideoTrack,
                        isScreenSharing = true
                    )
                }
            }
            is MeetingCallEvent.ScreenShareStopped -> {
                updateParticipantState(event.userId) {
                    it.copy(
                        screenTrack = null,
                        sfuScreenTrack = null,
                        isScreenSharing = false
                    )
                }
            }
            is MeetingCallEvent.Error -> {
                _uiState.update { it.copy(error = event.message) }
            }
            is MeetingCallEvent.HostMuted -> {
                _uiState.update {
                    it.copy(
                        isMutedByHost = true,
                        hostNotification = if (event.isMuteAll) "The host muted everyone's microphone" else "The host muted your microphone"
                    )
                }
            }
            is MeetingCallEvent.HostVideoStopped -> {
                _uiState.update {
                    it.copy(
                        isVideoStoppedByHost = true,
                        hostNotification = "The host turned off your camera"
                    )
                }
            }
            is MeetingCallEvent.HostRemoved -> {
                _uiState.update {
                    it.copy(
                        wasRemovedByHost = true,
                        hostNotification = "You were removed from the meeting by the host"
                    )
                }
                leaveMeeting()
            }
            is MeetingCallEvent.HostLockChanged -> {
                _uiState.update {
                    it.copy(
                        isMeetingLocked = event.isLocked,
                        hostNotification = if (event.isLocked) "Meeting locked by host" else "Meeting unlocked by host"
                    )
                }
            }
            is MeetingCallEvent.HostPermissionsUpdated -> {
                _uiState.update {
                    it.copy(
                        allowScreenShare = event.allowScreenShare,
                        allowChat = event.allowChat,
                        allowUnmute = event.allowUnmute
                    )
                }
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

    private fun removeParticipantFromMap(userId: Long) {
        _uiState.update { state ->
            val updatedParticipants = state.participants.toMutableMap()
            updatedParticipants.remove(userId)
            state.copy(participants = updatedParticipants)
        }
    }

    fun muteParticipant(userId: Long) {
        meetingCallRepository.sendHostMute(userId)
    }

    fun muteAllParticipants() {
        meetingCallRepository.sendHostMuteAll()
    }

    fun stopParticipantVideo(userId: Long) {
        meetingCallRepository.sendHostStopVideo(userId)
    }

    fun removeParticipant(userId: Long) {
        viewModelScope.launch {
            try {
                meetingRepository.removeParticipant(activeMeetingId, userId)
            } catch (e: Exception) {
                Log.e("LiveMeetingVM", "Error executing REST removeParticipant: ${e.message}")
            }
            meetingCallRepository.sendHostRemove(userId)
            removeParticipantFromMap(userId)
        }
    }

    fun setMeetingLocked(locked: Boolean) {
        viewModelScope.launch {
            try {
                meetingRepository.lockMeeting(activeMeetingId, locked)
            } catch (e: Exception) {
                Log.e("LiveMeetingVM", "Error executing REST lockMeeting: ${e.message}")
            }
            meetingCallRepository.sendHostLock(locked)
            _uiState.update { it.copy(isMeetingLocked = locked) }
        }
    }

    fun updateHostPermissions(allowScreenShare: Boolean, allowChat: Boolean, allowUnmute: Boolean) {
        meetingCallRepository.sendHostPermissions(allowScreenShare, allowChat, allowUnmute)
        _uiState.update {
            it.copy(
                allowScreenShare = allowScreenShare,
                allowChat = allowChat,
                allowUnmute = allowUnmute
            )
        }
    }

    fun clearHostNotification() {
        _uiState.update { it.copy(hostNotification = null) }
    }

    fun setAudioEnabled(enabled: Boolean) {
        meetingCallRepository.setAudioEnabled(enabled)
    }

    fun setVideoEnabled(enabled: Boolean) {
        meetingCallRepository.setVideoEnabled(enabled)
    }

    fun selectAudioDevice(context: android.content.Context, deviceName: String) {
        AudioDeviceManager.selectAudioDevice(context, deviceName)
        _uiState.update { it.copy(selectedAudioDevice = deviceName) }
    }

    fun switchCamera() {
        meetingCallRepository.switchCamera()
    }

    fun startScreenSharing(mediaProjectionData: android.content.Intent) {
        _uiState.update { it.copy(isScreenSharing = true, error = null) }
        meetingCallRepository.startScreenSharing(mediaProjectionData)
        val p2pTrack = meetingCallRepository.getLocalScreenTrack()
        val sfuTrack = meetingCallRepository.getLocalSfuScreenTrack()
        _uiState.update {
            it.copy(
                isScreenSharing = true,
                localScreenTrack = p2pTrack,
                localSfuScreenTrack = sfuTrack
            )
        }
    }

    fun stopScreenSharing() {
        meetingCallRepository.stopScreenSharing()
        _uiState.update { it.copy(isScreenSharing = false, localScreenTrack = null, localSfuScreenTrack = null) }
    }

    fun leaveMeeting() {
        if (!callStarted) return
        callStarted = false
        viewModelScope.launch {
            try {
                meetingRepository.leaveMeeting(activeMeetingId)
            } catch (e: Exception) {
                Log.e("LiveMeetingVM", "Error executing REST leaveMeeting: ${e.message}")
            }
            meetingWebSocketManager.unsubscribeFromAiProxySpeech(activeMeetingId)
            textToSpeechManager.stop()
            meetingCallRepository.leaveMeeting()
        }
    }

    override fun onCleared() {
        super.onCleared()
        meetingWebSocketManager.unsubscribeFromAiProxySpeech(activeMeetingId)
        textToSpeechManager.stop()
        meetingCallRepository.leaveMeeting()
    }
}

data class LiveMeetingUiState(
    val isLoading: Boolean = false,
    val localVideoTrack: VideoTrack? = null,
    val localSfuVideoTrack: io.livekit.android.room.track.VideoTrack? = null,
    val localScreenTrack: VideoTrack? = null,
    val localSfuScreenTrack: io.livekit.android.room.track.VideoTrack? = null,
    val participants: Map<Long, ParticipantMediaState> = emptyMap(),
    val isScreenSharing: Boolean = false,
    val error: String? = null,
    val activeAiSpeech: AiProxySpeechDto? = null,
    val selectedAudioDevice: String = "Speaker",
    val isMeetingLocked: Boolean = false,
    val allowScreenShare: Boolean = true,
    val allowChat: Boolean = true,
    val allowUnmute: Boolean = true,
    val isMutedByHost: Boolean = false,
    val isVideoStoppedByHost: Boolean = false,
    val wasRemovedByHost: Boolean = false,
    val hostNotification: String? = null
)
