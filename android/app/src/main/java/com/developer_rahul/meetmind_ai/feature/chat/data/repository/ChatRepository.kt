package com.developer_rahul.meetmind_ai.feature.chat.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.chat.domain.model.ChatMessage
import kotlinx.coroutines.flow.SharedFlow

interface ChatRepository {
    val messages: SharedFlow<ChatMessage>
    suspend fun getMessages(meetingId: Long, page: Int = 0): NetworkResult<List<ChatMessage>>
    fun sendMessage(meetingId: Long, content: String)
    fun subscribeToChat(meetingId: Long)
    fun unsubscribeFromChat(meetingId: Long)
}
