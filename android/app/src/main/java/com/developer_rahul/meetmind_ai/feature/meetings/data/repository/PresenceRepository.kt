package com.developer_rahul.meetmind_ai.feature.meetings.data.repository

import com.developer_rahul.meetmind_ai.core.network.websocket.model.SignalingMessageDto
import kotlinx.coroutines.flow.StateFlow

interface PresenceRepository {
    val onlineParticipantIds: StateFlow<Set<Long>>
    fun subscribeToPresence(meetingId: Long)
    fun unsubscribeFromPresence(meetingId: Long)
}
