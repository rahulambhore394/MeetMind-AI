package com.developer_rahul.meetmind_ai.feature.meetingroom.data.repository

import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import com.developer_rahul.meetmind_ai.core.webrtc.WebRtcManager
import com.developer_rahul.meetmind_ai.core.webrtc.data.remote.WebRtcApiService
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import io.mockk.every
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
class MeetingCallRepositoryTest {

    private val apiService = mockk<WebRtcApiService>(relaxed = true)
    private val webSocketManager = mockk<MeetingWebSocketManager>(relaxed = true)
    private val webRtcManager = mockk<WebRtcManager>(relaxed = true)
    private lateinit var repository: MeetingCallRepository

    @Before
    fun setup() {
        every { webSocketManager.signalingMessages } returns MutableSharedFlow()
        repository = MeetingCallRepository(apiService, webSocketManager, webRtcManager)
    }

    @org.junit.Test
    fun `joinMeeting should subscribe to signaling and create local stream`() = kotlinx.coroutines.test.runTest {
        repository.joinMeeting(1L)
        
        io.mockk.verify { 
            webRtcManager.createLocalStream()
            webSocketManager.subscribeToSignaling(1L)
        }
    }
}
