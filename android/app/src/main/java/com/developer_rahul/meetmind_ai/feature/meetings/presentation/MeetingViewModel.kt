package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import com.developer_rahul.meetmind_ai.core.media.recording.RecordingManager
import com.developer_rahul.meetmind_ai.core.media.recording.RecordingUploadManager
import com.developer_rahul.meetmind_ai.feature.intelligence.data.repository.IntelligenceRepository
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.MeetingRepository
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.PresenceRepository
import com.developer_rahul.meetmind_ai.feature.recording.data.repository.RecordingRepository
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.*
import com.developer_rahul.meetmind_ai.core.network.token.TokenProvider
import com.developer_rahul.meetmind_ai.feature.recording.domain.model.RecordingStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MeetingViewModel(
    private val meetingRepository: MeetingRepository,
    private val webSocketManager: MeetingWebSocketManager,
    private val presenceRepository: PresenceRepository,
    private val recordingRepository: RecordingRepository,
    private val intelligenceRepository: IntelligenceRepository,
    private val recordingManager: RecordingManager,
    private val recordingUploadManager: RecordingUploadManager,
    private val tokenProvider: TokenProvider
) : ViewModel() {

    val currentUserId: Long
        get() = tokenProvider.getUserId()

    private val _uiState = MutableStateFlow(MeetingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadMeetings()
        observeRealtimeEvents()
        observePresence()
        observeRecordingEvents()
        observeLocalRecordingState()
        webSocketManager.connect()
    }

    private fun observeLocalRecordingState() {
        viewModelScope.launch {
            recordingManager.state.collect { state ->
                _uiState.update { it.copy(localRecordingState = state) }
            }
        }
    }

    private fun observeRecordingEvents() {
        viewModelScope.launch {
            recordingRepository.recordingEvents.collect { event ->
                val type = event["type"]
                val meetingId = event["meetingId"]?.toLongOrNull()
                val recordingId = event["recordingId"]?.toLongOrNull()
                
                if (meetingId == _uiState.value.selectedMeeting?.id) {
                    when (type) {
                        "RECORDING_STARTED" -> {
                            _uiState.update { it.copy(isRecording = true) }
                        }
                        "RECORDING_STOPPED" -> {
                            _uiState.update { it.copy(isRecording = false) }
                            loadMeetingDetails(meetingId.toString())
                        }
                        "RECORDING_STATUS_UPDATE" -> {
                            val newStatus = event["status"]
                            _uiState.update { state ->
                                state.copy(
                                    recordings = state.recordings.map { 
                                        if (it.id == recordingId) it.copy(status = newStatus ?: it.status) else it 
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observePresence() {
        viewModelScope.launch {
            presenceRepository.onlineParticipantIds.collect { onlineIds ->
                _uiState.update { state ->
                    state.copy(
                        onlineParticipantIds = onlineIds,
                        participants = state.participants.map { participant ->
                            participant.copy(isOnline = onlineIds.contains(participant.userId))
                        }
                    )
                }
            }
        }
    }

    private fun observeRealtimeEvents() {
        viewModelScope.launch {
            webSocketManager.events.collect { event ->
                handleRealtimeEvent(event)
            }
        }
    }

    private fun handleRealtimeEvent(event: MeetingRealtimeEvent) {
        _uiState.update { state ->
            if (state.selectedMeeting?.id != event.meetingId) return@update state

            when (event) {
                is MeetingRealtimeEvent.MeetingStarted -> {
                    state.copy(selectedMeeting = state.selectedMeeting.copy(status = MeetingStatus.LIVE))
                }
                is MeetingRealtimeEvent.MeetingEnded -> {
                    if (state.localRecordingState.status == RecordingStatus.RECORDING) {
                        recordingManager.stopRecording()
                    }
                    state.copy(selectedMeeting = state.selectedMeeting.copy(status = MeetingStatus.ENDED))
                }
                is MeetingRealtimeEvent.ParticipantJoined -> {
                    val alreadyInList = state.participants.any { it.userId == event.userId }
                    if (!alreadyInList) {
                        val newParticipant = Participant(
                            id = -1,
                            userId = event.userId,
                            name = event.userName,
                            email = "",
                            role = ParticipantRole.PARTICIPANT,
                            status = ParticipantStatus.JOINED
                        )
                        state.copy(participants = state.participants + newParticipant)
                    } else {
                        state.copy(participants = state.participants.map { 
                            if (it.userId == event.userId) it.copy(status = ParticipantStatus.JOINED) else it 
                        })
                    }
                }
                is MeetingRealtimeEvent.ParticipantLeft -> {
                    state.copy(participants = state.participants.map { 
                        if (it.userId == event.userId) it.copy(status = ParticipantStatus.LEFT) else it 
                    })
                }
                is MeetingRealtimeEvent.ParticipantAccepted -> {
                    state.copy(participants = state.participants.map { 
                        if (it.userId == event.userId) it.copy(status = ParticipantStatus.ACCEPTED) else it 
                    })
                }
                is MeetingRealtimeEvent.ParticipantDeclined -> {
                    state.copy(participants = state.participants.map { 
                        if (it.userId == event.userId) it.copy(status = ParticipantStatus.DECLINED) else it
                    })
                }
            }
        }

        if (event is MeetingRealtimeEvent.ParticipantJoined || event is MeetingRealtimeEvent.ParticipantLeft) {
            loadMeetingDetails(event.meetingId.toString())
        }
    }

    fun loadMeetings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = meetingRepository.getAllMeetings()) {
                is NetworkResult.Success -> {
                    val sorted = result.data.sortedByDescending { it.id }
                    _uiState.update { it.copy(
                        isLoading = false,
                        meetings = sorted,
                        upcomingMeetings = sorted.filter { m -> m.status == MeetingStatus.SCHEDULED },
                        liveMeetings = sorted.filter { m -> m.status == MeetingStatus.LIVE },
                        pastMeetings = sorted.filter { m -> m.status == MeetingStatus.ENDED }
                    ) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message ?: "Failed to load meetings") }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun loadMeetingDetails(meetingIdOrCode: String) {
        val numericId = meetingIdOrCode.toLongOrNull()
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetails = true, detailsError = null) }
            
            val meetingResult = if (numericId != null) {
                webSocketManager.subscribeToMeeting(numericId)
                presenceRepository.subscribeToPresence(numericId)
                recordingRepository.subscribeToRecordingEvents(numericId)
                meetingRepository.getMeeting(numericId)
            } else {
                meetingRepository.getMeetingByCode(meetingIdOrCode)
            }

            val fetchedId = when (meetingResult) {
                is NetworkResult.Success -> {
                    val m = meetingResult.data
                    if (numericId == null) {
                        webSocketManager.subscribeToMeeting(m.id)
                        presenceRepository.subscribeToPresence(m.id)
                        recordingRepository.subscribeToRecordingEvents(m.id)
                    }
                    m.id
                }
                else -> numericId
            }

            val participantsResult = if (fetchedId != null) meetingRepository.getParticipants(fetchedId) else NetworkResult.Error(com.developer_rahul.meetmind_ai.core.network.model.MeetMindError(type = com.developer_rahul.meetmind_ai.core.network.model.ErrorType.NOT_FOUND, message = "Invalid ID"))
            val recordingsResult = if (fetchedId != null) recordingRepository.listRecordings(fetchedId) else NetworkResult.Error(com.developer_rahul.meetmind_ai.core.network.model.MeetMindError(type = com.developer_rahul.meetmind_ai.core.network.model.ErrorType.NOT_FOUND, message = "Invalid ID"))

            _uiState.update { state ->
                var newState = state.copy(isLoadingDetails = false)
                
                when (meetingResult) {
                    is NetworkResult.Success -> {
                        newState = newState.copy(selectedMeeting = meetingResult.data)
                    }
                    is NetworkResult.Error -> {
                        newState = newState.copy(detailsError = meetingResult.error.message)
                    }
                    else -> {}
                }

                when (participantsResult) {
                    is NetworkResult.Success -> {
                        newState = newState.copy(participants = participantsResult.data)
                    }
                    else -> {}
                }

                when (recordingsResult) {
                    is NetworkResult.Success -> {
                        // isRecording is true only when backend says STARTED *and* local recording is active.
                        // This prevents ghost REC 00:00 badge appearing on re-join from a stale backend recording.
                        val backendRecording = recordingsResult.data.any { it.status == "STARTED" }
                        val locallyRecording = recordingManager.state.value.status == RecordingStatus.RECORDING
                        newState = newState.copy(
                            recordings = recordingsResult.data,
                            isRecording = backendRecording && locallyRecording
                        )
                    }
                    else -> {}
                }
                
                newState
            }
            
            if (fetchedId != null) {
                loadIntelligenceSummary(fetchedId)
            }
        }
    }

    private fun loadIntelligenceSummary(meetingId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingIntelligence = true) }
            when (val result = intelligenceRepository.getSummary(meetingId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoadingIntelligence = false, intelligenceSummary = result.data) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoadingIntelligence = false) }
                }
                else -> {}
            }
        }
    }

    fun startRecording(meetingId: Long, projectionData: android.content.Intent) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            when (val result = recordingRepository.startRecording(meetingId)) {
                is NetworkResult.Success -> {
                    val recording = result.data
                    _uiState.update { state ->
                        state.copy(
                            actionInProgress = false,
                            isRecording = true,
                            recordings = state.recordings.filter { it.id != recording.id } + recording
                        )
                    }
                    recordingManager.startRecording(meetingId, projectionData)
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(actionInProgress = false, error = result.error.message) }
                }
                else -> {
                    _uiState.update { it.copy(actionInProgress = false) }
                }
            }
        }
    }

    fun startAudioOnlyRecording(meetingId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            when (val result = recordingRepository.startRecording(meetingId)) {
                is NetworkResult.Success -> {
                    val recording = result.data
                    _uiState.update { state ->
                        state.copy(
                            actionInProgress = false,
                            isRecording = true,
                            recordings = state.recordings.filter { it.id != recording.id } + recording
                        )
                    }
                    recordingManager.startAudioOnlyRecording(meetingId)
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(actionInProgress = false, error = result.error.message) }
                }
                else -> {
                    _uiState.update { it.copy(actionInProgress = false) }
                }
            }
        }
    }

    fun stopRecording(meetingId: Long, recordingId: Long? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, isRecording = false) }

            // Snapshot localUri BEFORE stopping — the manager preserves it in COMPLETED state
            val localUriBeforeStop = recordingManager.state.value.localUri
            recordingManager.stopRecording()
            // After stop, localUri is also kept in COMPLETED state as a safety net
            val localUri = localUriBeforeStop ?: recordingManager.state.value.localUri

            android.util.Log.d("MeetingViewModel", "stopRecording: localUri=$localUri")

            val targetRecordingId = recordingId
                ?: _uiState.value.recordings.find { it.status == "STARTED" }?.id
                ?: _uiState.value.recordings.lastOrNull()?.id

            if (targetRecordingId == null || targetRecordingId <= 0L) {
                _uiState.update { it.copy(actionInProgress = false) }
                android.util.Log.w("MeetingViewModel", "No target recording ID found; skipping backend stop")
                return@launch
            }

            when (val result = recordingRepository.stopRecording(meetingId, targetRecordingId)) {
                is NetworkResult.Success -> {
                    val updatedRecording = result.data
                    _uiState.update { state ->
                        state.copy(
                            actionInProgress = false,
                            recordings = state.recordings.map {
                                if (it.id == targetRecordingId) updatedRecording else it
                            }
                        )
                    }
                    if (localUri != null) {
                        val file = java.io.File(localUri)
                        if (file.exists() && file.length() > 0) {
                            android.util.Log.d("MeetingViewModel", "Enqueuing upload: $localUri (${file.length()} bytes)")
                            recordingUploadManager.startUpload(meetingId, targetRecordingId, localUri)
                            observeUploadProgress(targetRecordingId)
                        } else {
                            android.util.Log.e("MeetingViewModel", "Recording file missing or empty: $localUri")
                        }
                    } else {
                        android.util.Log.e("MeetingViewModel", "localUri is null — recording was not captured locally")
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(actionInProgress = false, error = result.error.message) }
                }
                else -> {
                    _uiState.update { it.copy(actionInProgress = false) }
                }
            }
        }
    }

    private fun observeUploadProgress(recordingId: Long) {
        viewModelScope.launch {
            recordingUploadManager.getUploadProgress(recordingId).collect { progress ->
                _uiState.update { it.copy(uploadProgress = progress) }
            }
        }
        viewModelScope.launch {
            recordingUploadManager.getUploadStatus(recordingId).collect { status ->
                _uiState.update { it.copy(uploadStatus = status?.name) }
                if (status == androidx.work.WorkInfo.State.SUCCEEDED) {
                    recordingManager.reset()
                    _uiState.value.selectedMeeting?.id?.let { mId ->
                        loadMeetingDetails(mId.toString())
                    }
                }
            }
        }
    }

    fun resetRecordingState() {
        recordingManager.reset()
        _uiState.update { it.copy(isRecording = false, localRecordingState = com.developer_rahul.meetmind_ai.feature.recording.domain.model.RecordingState()) }
    }

    fun createMeeting(title: String, description: String, scheduledAt: String, invitedEmails: List<String>? = null) {
        if (_uiState.value.isCreating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, creationError = null, creationSuccess = false) }
            when (val result = meetingRepository.createMeeting(title, description, scheduledAt, invitedEmails)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isCreating = false, creationSuccess = true, selectedMeeting = result.data) }
                    loadMeetings()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isCreating = false, creationError = result.error.message ?: "Failed to create meeting") }
                }
                else -> {
                    _uiState.update { it.copy(isCreating = false) }
                }
            }
        }
    }

    fun joinByCode(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            when (val result = meetingRepository.getMeetingByCode(code)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(actionInProgress = false, joinSuccess = true, selectedMeeting = result.data) }
                    loadMeetings()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(actionInProgress = false, error = result.error.message ?: "Invalid meeting code") }
                }
                else -> _uiState.update { it.copy(actionInProgress = false) }
            }
        }
    }

    fun startMeeting(meetingId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            when (val result = meetingRepository.startMeeting(meetingId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(actionInProgress = false, selectedMeeting = result.data) }
                    loadMeetings()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(actionInProgress = false, error = result.error.message) }
                }
                else -> _uiState.update { it.copy(actionInProgress = false) }
            }
        }
    }

    fun endMeeting(meetingId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            when (val result = meetingRepository.endMeeting(meetingId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(actionInProgress = false, selectedMeeting = result.data) }
                    loadMeetings()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(actionInProgress = false, error = result.error.message) }
                }
                else -> _uiState.update { it.copy(actionInProgress = false) }
            }
        }
    }

    fun deleteMeeting(meetingId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            when (val result = meetingRepository.deleteMeeting(meetingId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(actionInProgress = false) }
                    loadMeetings()
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(actionInProgress = false, error = result.error.message ?: "Failed to delete meeting") }
                }
                else -> _uiState.update { it.copy(actionInProgress = false) }
            }
        }
    }

    fun inviteParticipant(meetingId: Long, email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val result = meetingRepository.inviteParticipant(meetingId, email)) {
                is NetworkResult.Success -> {
                    loadMeetingDetails(meetingId.toString())
                    onResult(true, "Invitation sent to $email")
                }
                is NetworkResult.Error -> {
                    onResult(false, result.error.message ?: "Failed to send invitation")
                }
                else -> onResult(false, "Unknown error")
            }
        }
    }

    fun batchInviteParticipants(meetingId: Long, rawEmailInput: String, onResult: ((Boolean, String?) -> Unit)? = null) {
        val emailList = rawEmailInput.split(",", "\n", " ", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (emailList.isEmpty()) {
            onResult?.invoke(false, "Please enter at least one valid email address.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, error = null) }
            val result = meetingRepository.batchInviteParticipants(meetingId, emailList)

            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(actionInProgress = false) }
                    loadMeetingDetails(meetingId.toString())
                    val msg = if (emailList.size == 1) "Invitation sent to ${emailList.first()}"
                    else "Broadcast invitations sent to ${emailList.size} recipients!"
                    onResult?.invoke(true, msg)
                }
                is NetworkResult.Error -> {
                    val errMsg = result.error.message ?: "Failed to send invitations"
                    _uiState.update { it.copy(actionInProgress = false, error = errMsg) }
                    onResult?.invoke(false, errMsg)
                }
                else -> {
                    _uiState.update { it.copy(actionInProgress = false) }
                    onResult?.invoke(false, "Unknown error")
                }
            }
        }
    }

    fun joinMeeting(meetingId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            when (val result = meetingRepository.joinMeeting(meetingId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(actionInProgress = false, joinSuccess = true) }
                    loadMeetingDetails(meetingId.toString())
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(actionInProgress = false, error = result.error.message) }
                }
                else -> _uiState.update { it.copy(actionInProgress = false) }
            }
        }
    }

    fun inviteParticipant(meetingId: Long, email: String) {
        batchInviteParticipants(meetingId, email)
    }
    
    fun leaveMeeting(meetingId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            
            // Stop recording if active
            if (_uiState.value.localRecordingState.status == RecordingStatus.RECORDING) {
                recordingManager.stopRecording()
            }

            when (val result = meetingRepository.leaveMeeting(meetingId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(actionInProgress = false) }
                    webSocketManager.unsubscribeFromMeeting(meetingId)
                    presenceRepository.unsubscribeFromPresence(meetingId)
                    recordingRepository.unsubscribeFromRecordingEvents(meetingId)
                    loadMeetings()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(actionInProgress = false, error = result.error.message) }
                }
                else -> _uiState.update { it.copy(actionInProgress = false) }
            }
        }
    }
    
    fun resetCreationState() {
        _uiState.update { it.copy(creationSuccess = false, creationError = null) }
    }

    fun resetJoinState() {
        _uiState.update { it.copy(joinSuccess = false) }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}

data class MeetingUiState(
    val meetings: List<Meeting> = emptyList(),
    val upcomingMeetings: List<Meeting> = emptyList(),
    val liveMeetings: List<Meeting> = emptyList(),
    val pastMeetings: List<Meeting> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    
    val selectedMeeting: Meeting? = null,
    val participants: List<Participant> = emptyList(),
    val onlineParticipantIds: Set<Long> = emptySet(),
    val isLoadingDetails: Boolean = false,
    val detailsError: String? = null,

    val isRecording: Boolean = false,
    val recordings: List<com.developer_rahul.meetmind_ai.feature.recording.data.remote.dto.RecordingResponseDto> = emptyList(),
    val localRecordingState: com.developer_rahul.meetmind_ai.feature.recording.domain.model.RecordingState = com.developer_rahul.meetmind_ai.feature.recording.domain.model.RecordingState(),
    val intelligenceSummary: com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.SummaryDetailResponseDto? = null,
    val isLoadingIntelligence: Boolean = false,

    val isCreating: Boolean = false,
    val creationSuccess: Boolean = false,
    val creationError: String? = null,
    
    val actionInProgress: Boolean = false,
    val joinSuccess: Boolean = false,
    val uploadProgress: Int? = null,
    val uploadStatus: String? = null
)
