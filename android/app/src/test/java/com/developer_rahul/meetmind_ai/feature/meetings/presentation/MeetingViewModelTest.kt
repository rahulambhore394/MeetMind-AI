package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.MeetingRepository
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.MeetingStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeetingViewModelTest {

    private val meetingRepository = mockk<MeetingRepository>(relaxed = true)
    private val webSocketManager = mockk<com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager>(relaxed = true)
    private val presenceRepository = mockk<com.developer_rahul.meetmind_ai.feature.meetings.data.repository.PresenceRepository>(relaxed = true)
    private val recordingRepository = mockk<com.developer_rahul.meetmind_ai.feature.recording.data.repository.RecordingRepository>(relaxed = true)
    private val intelligenceRepository = mockk<com.developer_rahul.meetmind_ai.feature.intelligence.data.repository.IntelligenceRepository>(relaxed = true)
    private lateinit var viewModel: MeetingViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Initial load happens in init, so mock it before creating viewModel
        coEvery { meetingRepository.getAllMeetings() } returns NetworkResult.Success(emptyList())
        viewModel = MeetingViewModel(
            meetingRepository, 
            webSocketManager, 
            presenceRepository, 
            recordingRepository, 
            intelligenceRepository
        )
    }

    @Test
    fun `loadMeetings success updates uiState with meetings`() {
        val meetings = listOf(
            Meeting(1L, "Title", "Desc", 1L, "Host", "host@test.com", "2023-10-15T10:00:00", null, null, "2023-10-15T09:00:00", MeetingStatus.SCHEDULED)
        )
        coEvery { meetingRepository.getAllMeetings() } returns NetworkResult.Success(meetings)

        viewModel.loadMeetings()

        val state = viewModel.uiState.value
        assertEquals(meetings, state.meetings)
        assertEquals(1, state.upcomingMeetings.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `createMeeting success updates creationSuccess state`() {
        val meeting = Meeting(1L, "Title", "Desc", 1L, "Host", "host@test.com", "2023-10-15T10:00:00", null, null, "2023-10-15T09:00:00", MeetingStatus.SCHEDULED)
        coEvery { meetingRepository.createMeeting(any(), any(), any()) } returns NetworkResult.Success(meeting)

        viewModel.createMeeting("Title", "Desc", "2023-10-15T10:00:00")

        val state = viewModel.uiState.value
        assertTrue(state.creationSuccess)
        assertFalse(state.isCreating)
    }

    @Test
    fun `loadMeetingDetails success updates uiState`() {
        val meeting = Meeting(1L, "Title", "Desc", 1L, "Host", "host@test.com", "2023-10-15T10:00:00", null, null, "2023-10-15T09:00:00", MeetingStatus.SCHEDULED)
        coEvery { meetingRepository.getMeeting(1L) } returns NetworkResult.Success(meeting)
        coEvery { meetingRepository.getParticipants(1L) } returns NetworkResult.Success(emptyList())

        viewModel.loadMeetingDetails("1")

        val state = viewModel.uiState.value
        assertEquals(meeting, state.selectedMeeting)
        assertFalse(state.isLoadingDetails)
    }

    @Test
    fun `loadMeetingDetails should fetch recordings and intelligence`() {
        val meetingId = 1L
        val recording = com.developer_rahul.meetmind_ai.feature.recording.data.remote.dto.RecordingResponseDto(
            id = 1, meetingId = meetingId, ownerId = 1, startedAt = "", status = "STARTED", createdAt = ""
        )
        val meeting = Meeting(meetingId, "Title", "Desc", 1L, "Host", "host@test.com", "2023-10-15T10:00:00", null, null, "2023-10-15T09:00:00", MeetingStatus.LIVE)
        
        coEvery { meetingRepository.getMeeting(meetingId) } returns NetworkResult.Success(meeting)
        coEvery { meetingRepository.getParticipants(meetingId) } returns NetworkResult.Success(emptyList())
        coEvery { recordingRepository.listRecordings(meetingId) } returns NetworkResult.Success(listOf(recording))
        coEvery { intelligenceRepository.getSummary(meetingId) } returns NetworkResult.Error(com.developer_rahul.meetmind_ai.core.network.model.MeetMindError(com.developer_rahul.meetmind_ai.core.network.model.ErrorType.UNKNOWN_ERROR, "Not found"))

        viewModel.loadMeetingDetails(meetingId.toString())
        
        val state = viewModel.uiState.value
        assertTrue(state.isRecording)
        assertEquals(1, state.recordings.size)
        assertEquals(null, state.intelligenceSummary)
    }
}
