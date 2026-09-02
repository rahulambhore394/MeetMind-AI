package com.developer_rahul.meetmind_ai.feature.chat.domain.model

data class ChatMessage(
    val id: Long,
    val meetingId: Long,
    val senderId: Long,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isMe: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT
)

enum class MessageStatus {
    SENDING, SENT, FAILED
}
