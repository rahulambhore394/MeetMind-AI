package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import com.developer_rahul.meetmind_ai.core.media.recording.RecordingManager
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import com.developer_rahul.meetmind_ai.feature.intelligence.data.repository.IntelligenceRepository
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.MeetingRepository
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.PresenceRepository
import com.developer_rahul.meetmind_ai.feature.recording.data.remote.dto.RecordingResponseDto
import com.developer_rahul.meetmind_ai.feature.recording.data.repository.RecordingRepository
import com.developer_rahul.meetmind_ai.feature.recording.domain.model.RecordingState
import com.developer_rahul.meetmind_ai.feature.recording.domain.model.RecordingStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeetingViewModelRecordingTest {

    private val meetingRepository: MeetingRepository = mockk(relaxed = true)
    private val webSocketManager: MeetingWebSocketManager = mockk(relaxed = true)
    private val presenceRepository: PresenceRepository = mockk(relaxed = true)
    private val recordingRepository: RecordingRepository = mockk(relaxed = true)
    private val intelligenceRepository: IntelligenceRepository = mockk(relaxed = true)
    private val recordingManager: RecordingManager = mockk(relaxed = true)
    private val recordingUploadManager: com.developer_rahul.meetmind_ai.core.media.recording.RecordingUploadManager = mockk(relaxed = true)
    private val tokenProvider: com.developer_rahul.meetmind_ai.core.network.token.TokenProvider = mockk(relaxed = true)

    private lateinit var viewModel: MeetingViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { recordingManager.state } returns MutableStateFlow(RecordingState())
        every { webSocketManager.events } returns kotlinx.coroutines.flow.MutableSharedFlow<com.developer_rahul.meetmind_ai.feature.meetings.domain.model.MeetingRealtimeEvent>()
        every { presenceRepository.onlineParticipantIds } returns MutableStateFlow<Set<Long>>(emptySet())
        every { recordingRepository.recordingEvents } returns kotlinx.coroutines.flow.MutableSharedFlow<Map<String, String>>()
        viewModel = MeetingViewModel(
            meetingRepository,
            webSocketManager,
            presenceRepository,
            recordingRepository,
            intelligenceRepository,
            recordingManager,
            recordingUploadManager,
            tokenProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startRecording calls repository and manager`() = runTest {
        val meetingId = 1L
        val projectionData: android.content.Intent = mockk()
        val recordingResponse = RecordingResponseDto(
            id = 100, meetingId = meetingId, ownerId = 1, 
            startedAt = "", status = "STARTED", createdAt = ""
        )
        
        coEvery { recordingRepository.startRecording(meetingId) } returns NetworkResult.Success(recordingResponse)
        
        viewModel.startRecording(meetingId, projectionData)
        advanceUntilIdle()
        
        io.mockk.verify { recordingManager.startRecording(meetingId, projectionData) }
    }
}
