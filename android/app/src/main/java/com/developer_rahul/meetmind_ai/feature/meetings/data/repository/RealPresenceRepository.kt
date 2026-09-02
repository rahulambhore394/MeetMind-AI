package com.developer_rahul.meetmind_ai.feature.meetings.data.repository

import android.util.Log
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RealPresenceRepository(
    private val webSocketManager: MeetingWebSocketManager
) : PresenceRepository {

    private val _onlineParticipantIds = MutableStateFlow<Set<Long>>(emptySet())
    override val onlineParticipantIds: StateFlow<Set<Long>> = _onlineParticipantIds.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        repositoryScope.launch {
            webSocketManager.signalingMessages.collect { message ->
                handleSignalingMessage(message)
            }
        }
    }

    private fun handleSignalingMessage(message: com.developer_rahul.meetmind_ai.core.network.websocket.model.SignalingMessageDto) {
        when (message.type) {
            "JOIN" -> {
                message.senderId?.let { id ->
                    _onlineParticipantIds.update { it + id }
                }
            }
            "LEAVE" -> {
                message.senderId?.let { id ->
                    _onlineParticipantIds.update { it - id }
                }
            }
            "PEER_LIST" -> {
                // Payload format: id1:name1,id2:name2
                val peerIds = message.payload?.split(",")?.mapNotNull { 
                    it.split(":").firstOrNull()?.toLongOrNull() 
                }?.toSet() ?: emptySet()
                
                _onlineParticipantIds.update { it + peerIds }
            }
        }
    }

    override fun subscribeToPresence(meetingId: Long) {
        _onlineParticipantIds.value = emptySet()
        webSocketManager.subscribeToSignaling(meetingId)
    }

    override fun unsubscribeFromPresence(meetingId: Long) {
        webSocketManager.unsubscribeFromSignaling(meetingId)
        _onlineParticipantIds.value = emptySet()
    }
}
