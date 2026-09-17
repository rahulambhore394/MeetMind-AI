package com.developer_rahul.meetmind_ai.feature.meetingroom.presentation

import com.developer_rahul.meetmind_ai.feature.meetingroom.data.repository.MeetingCallRepository
import com.developer_rahul.meetmind_ai.feature.meetingroom.domain.model.MeetingCallEvent
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.MeetingRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.webrtc.VideoTrack

@OptIn(ExperimentalCoroutinesApi::class)
class LiveMeetingParticipantVisibilityTest {

    private val meetingCallRepository = mockk<MeetingCallRepository>(relaxed = true)
    private val meetingRepository = mockk<MeetingRepository>(relaxed = true)
    private val meetingWebSocketManager = mockk<MeetingWebSocketManager>(relaxed = true)
    private val textToSpeechManager = mockk<TextToSpeechManager>(relaxed = true)
    private val eventsFlow = MutableSharedFlow<MeetingCallEvent>()
    private lateinit var viewModel: LiveMeetingViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { meetingCallRepository.events } returns eventsFlow
        viewModel = LiveMeetingViewModel(
            meetingCallRepository = meetingCallRepository,
            meetingRepository = meetingRepository,
            meetingWebSocketManager = meetingWebSocketManager,
            textToSpeechManager = textToSpeechManager,
            meetingId = 42L
        )
    }

    @Test
    fun `all participants joining live meeting are concurrently visible with their media tracks`() = runTest {
        val trackUser101 = mockk<VideoTrack>()
        val trackUser102 = mockk<VideoTrack>()
        val trackUser103 = mockk<VideoTrack>()

        // Simulate 3 participants joining the live meeting
        eventsFlow.emit(MeetingCallEvent.RemoteStreamReady(101L, trackUser101))
        eventsFlow.emit(MeetingCallEvent.RemoteStreamReady(102L, trackUser102))
        eventsFlow.emit(MeetingCallEvent.RemoteStreamReady(103L, trackUser103))

        val participants = viewModel.uiState.value.participants
        
        // Verify all 3 participants are visible concurrently
        assertEquals(3, participants.size)
        assertTrue(participants.containsKey(101L))
        assertTrue(participants.containsKey(102L))
        assertTrue(participants.containsKey(103L))

        // Verify each participant has their own distinct video track
        assertEquals(trackUser101, participants[101L]?.videoTrack)
        assertEquals(trackUser102, participants[102L]?.videoTrack)
        assertEquals(trackUser103, participants[103L]?.videoTrack)

        // Verify video is enabled by default for all visible peers
        assertTrue(participants[101L]?.videoEnabled == true)
        assertTrue(participants[102L]?.videoEnabled == true)
        assertTrue(participants[103L]?.videoEnabled == true)
    }

    @Test
    fun `media state changes update specific participant without affecting visibility of other peers`() = runTest {
        val trackUser101 = mockk<VideoTrack>()
        val trackUser102 = mockk<VideoTrack>()

        eventsFlow.emit(MeetingCallEvent.RemoteStreamReady(101L, trackUser101))
        eventsFlow.emit(MeetingCallEvent.RemoteStreamReady(102L, trackUser102))

        // Participant 101 mutes audio
        eventsFlow.emit(MeetingCallEvent.MediaStateChanged(101L, audioEnabled = false, videoEnabled = true, isSpeaking = false))
        
        // Participant 102 starts speaking
        eventsFlow.emit(MeetingCallEvent.MediaStateChanged(102L, audioEnabled = true, videoEnabled = true, isSpeaking = true))

        val participants = viewModel.uiState.value.participants

        // Both participants remain visible
        assertEquals(2, participants.size)

        // Participant 101 is muted and not speaking
        assertFalse(participants[101L]?.audioEnabled ?: true)
        assertFalse(participants[101L]?.isSpeaking ?: true)

        // Participant 102 is active speaker
        assertTrue(participants[102L]?.audioEnabled ?: false)
        assertTrue(participants[102L]?.isSpeaking ?: false)
    }

    @Test
    fun `screen share by a participant updates their tile while keeping all other participants visible`() = runTest {
        val videoTrack101 = mockk<VideoTrack>()
        val videoTrack102 = mockk<VideoTrack>()
        val screenTrack102 = mockk<VideoTrack>()

        eventsFlow.emit(MeetingCallEvent.RemoteStreamReady(101L, videoTrack101))
        eventsFlow.emit(MeetingCallEvent.RemoteStreamReady(102L, videoTrack102))

        // Participant 102 starts sharing their screen
        eventsFlow.emit(MeetingCallEvent.ScreenShareStarted(102L, screenTrack102))

        val participantsWithScreenShare = viewModel.uiState.value.participants

        // Both peers remain visible in the grid
        assertEquals(2, participantsWithScreenShare.size)
        assertTrue(participantsWithScreenShare[102L]?.isScreenSharing == true)
        assertEquals(screenTrack102, participantsWithScreenShare[102L]?.screenTrack)
        assertFalse(participantsWithScreenShare[101L]?.isScreenSharing == true)

        // Participant 102 stops sharing screen
        eventsFlow.emit(MeetingCallEvent.ScreenShareStopped(102L))

        val participantsAfterStop = viewModel.uiState.value.participants
        assertFalse(participantsAfterStop[102L]?.isScreenSharing == true)
        assertNull(participantsAfterStop[102L]?.screenTrack)
        // Video tracks remain intact
        assertEquals(videoTrack102, participantsAfterStop[102L]?.videoTrack)
    }

    @Test
    fun `participant disconnection removes departed participant while remaining participants stay visible`() = runTest {
        val trackUser101 = mockk<VideoTrack>()
        val trackUser102 = mockk<VideoTrack>()
        val trackUser103 = mockk<VideoTrack>()

        eventsFlow.emit(MeetingCallEvent.RemoteStreamReady(101L, trackUser101))
        eventsFlow.emit(MeetingCallEvent.RemoteStreamReady(102L, trackUser102))
        eventsFlow.emit(MeetingCallEvent.RemoteStreamReady(103L, trackUser103))

        // User 102 leaves the live meeting
        eventsFlow.emit(MeetingCallEvent.RemoteStreamRemoved(102L))

        val participants = viewModel.uiState.value.participants

        // Exactly 2 participants remaining
        assertEquals(2, participants.size)
        assertFalse(participants.containsKey(102L))
        assertTrue(participants.containsKey(101L))
        assertTrue(participants.containsKey(103L))
    }

    @Test
    fun `connection state is tracked per participant independently`() = runTest {
        eventsFlow.emit(MeetingCallEvent.RemoteStreamReady(101L, mockk()))
        eventsFlow.emit(MeetingCallEvent.RemoteStreamReady(102L, mockk()))

        eventsFlow.emit(MeetingCallEvent.ConnectionStateChanged(101L, "CONNECTED"))
        eventsFlow.emit(MeetingCallEvent.ConnectionStateChanged(102L, "RECONNECTING"))

        val participants = viewModel.uiState.value.participants
        assertEquals("CONNECTED", participants[101L]?.connectionState)
        assertEquals("RECONNECTING", participants[102L]?.connectionState)
    }
}
