package com.developer_rahul.meetmind_ai.feature.meetingroom.presentation

import com.developer_rahul.meetmind_ai.feature.meetingroom.data.repository.MeetingCallRepository
import com.developer_rahul.meetmind_ai.feature.meetingroom.domain.model.MeetingCallEvent
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.MeetingRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.developer_rahul.meetmind_ai.core.media.tts.TextToSpeechManager
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.webrtc.VideoTrack

@OptIn(ExperimentalCoroutinesApi::class)
class LiveMeetingViewModelTest {

    private val repository = mockk<MeetingCallRepository>(relaxed = true)
    private val meetingRepository = mockk<MeetingRepository>(relaxed = true)
    private val meetingWebSocketManager = mockk<MeetingWebSocketManager>(relaxed = true)
    private val textToSpeechManager = mockk<TextToSpeechManager>(relaxed = true)
    private val eventsFlow = MutableSharedFlow<MeetingCallEvent>()
    private lateinit var viewModel: LiveMeetingViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.events } returns eventsFlow
        viewModel = LiveMeetingViewModel(
            meetingCallRepository = repository,
            meetingRepository = meetingRepository,
            meetingWebSocketManager = meetingWebSocketManager,
            textToSpeechManager = textToSpeechManager,
            meetingId = 1L
        )
    }

    @Test
    fun `local stream ready should update uiState`() = runTest {
        val mockTrack = mockk<VideoTrack>()
        eventsFlow.emit(MeetingCallEvent.LocalStreamReady(mockTrack))
        
        assertEquals(mockTrack, viewModel.uiState.value.localVideoTrack)
    }

    @Test
    fun `remote stream ready should add to uiState`() = runTest {
        val mockTrack = mockk<VideoTrack>()
        eventsFlow.emit(MeetingCallEvent.RemoteStreamReady(101L, mockTrack))
        
        assertEquals(mockTrack, viewModel.uiState.value.participants[101L]?.videoTrack)
    }

    @Test
    fun `remote stream removed should update uiState`() = runTest {
        val mockTrack = mockk<VideoTrack>()
        eventsFlow.emit(MeetingCallEvent.RemoteStreamReady(101L, mockTrack))
        eventsFlow.emit(MeetingCallEvent.RemoteStreamRemoved(101L))
        
        assertEquals(null, viewModel.uiState.value.participants[101L])
    }
}
