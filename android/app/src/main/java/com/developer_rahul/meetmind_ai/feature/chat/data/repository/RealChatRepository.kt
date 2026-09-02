package com.developer_rahul.meetmind_ai.feature.chat.data.repository

import com.developer_rahul.meetmind_ai.core.network.error.ErrorMapper
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.core.network.token.TokenProvider
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import com.developer_rahul.meetmind_ai.feature.chat.data.remote.ChatApiService
import com.developer_rahul.meetmind_ai.feature.chat.domain.mapper.toDomain
import com.developer_rahul.meetmind_ai.feature.chat.domain.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

class RealChatRepository(
    private val chatApiService: ChatApiService,
    private val webSocketManager: MeetingWebSocketManager,
    private val tokenProvider: TokenProvider
) : ChatRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override val messages: SharedFlow<ChatMessage> = webSocketManager.chatMessages
        .map { it.toDomain(tokenProvider.getUserId()) }
        .shareIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            replay = 0
        )

    override suspend fun getMessages(meetingId: Long, page: Int): NetworkResult<List<ChatMessage>> {
        return try {
            val response = chatApiService.getMessages(meetingId, page)
            val userId = tokenProvider.getUserId()
            NetworkResult.Success(response.content.map { it.toDomain(userId) })
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override fun sendMessage(meetingId: Long, content: String) {
        webSocketManager.sendChatMessage(meetingId, content)
    }

    override fun subscribeToChat(meetingId: Long) {
        webSocketManager.subscribeToChat(meetingId)
    }

    override fun unsubscribeFromChat(meetingId: Long) {
        webSocketManager.unsubscribeFromChat(meetingId)
    }
}
