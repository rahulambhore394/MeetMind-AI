package com.developer_rahul.meetmind_ai.feature.chat.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.chat.domain.model.ChatMessage
import com.developer_rahul.meetmind_ai.feature.chat.domain.model.MessageStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class StandaloneChatRepository : ChatRepository {

    private val _messages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    override val messages: SharedFlow<ChatMessage> = _messages.asSharedFlow()

    private val sampleChatMap = mutableMapOf<Long, MutableList<ChatMessage>>(
        101L to mutableListOf(
            ChatMessage(id = 1L, meetingId = 101L, senderId = 102L, senderName = "Sarah Connor", text = "Hello everyone! Excited for today's AI architecture review.", timestamp = "10:00 AM", isMe = false, status = MessageStatus.SENT),
            ChatMessage(id = 2L, meetingId = 101L, senderId = 103L, senderName = "Alex Rivera", text = "Hey Sarah, I've updated the Jetpack Compose specs.", timestamp = "10:02 AM", isMe = false, status = MessageStatus.SENT),
            ChatMessage(id = 3L, meetingId = 101L, senderId = 101L, senderName = "Rahul Ambhore", text = "Awesome! We are running completely standalone now.", timestamp = "10:05 AM", isMe = true, status = MessageStatus.SENT)
        )
    )

    override suspend fun getMessages(meetingId: Long, page: Int): NetworkResult<List<ChatMessage>> {
        val list = sampleChatMap.getOrPut(meetingId) {
            mutableListOf(
                ChatMessage(id = 1L, meetingId = meetingId, senderId = 101L, senderName = "Rahul Ambhore", text = "Welcome to meeting chat!", timestamp = "10:00 AM", isMe = true, status = MessageStatus.SENT)
            )
        }
        return NetworkResult.Success(list.toList())
    }

    override fun sendMessage(meetingId: Long, content: String) {
        val list = sampleChatMap.getOrPut(meetingId) { mutableListOf() }
        val newMsg = ChatMessage(
            id = System.currentTimeMillis(),
            meetingId = meetingId,
            senderId = 101L,
            senderName = "Rahul Ambhore",
            text = content,
            timestamp = "Just now",
            isMe = true,
            status = MessageStatus.SENT
        )
        list.add(newMsg)
        _messages.tryEmit(newMsg)
    }

    override fun subscribeToChat(meetingId: Long) {
        // No-op for standalone offline mode
    }

    override fun unsubscribeFromChat(meetingId: Long) {
        // No-op for standalone offline mode
    }
}
