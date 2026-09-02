package com.developer_rahul.meetmind_ai.feature.chat.domain.mapper

import com.developer_rahul.meetmind_ai.feature.chat.data.remote.dto.ChatMessageResponseDto
import com.developer_rahul.meetmind_ai.feature.chat.domain.model.ChatMessage

fun ChatMessageResponseDto.toDomain(currentUserId: Long): ChatMessage {
    return ChatMessage(
        id = messageId,
        meetingId = meetingId,
        senderId = senderId,
        senderName = senderName,
        text = message,
        timestamp = sentAt,
        isMe = senderId == currentUserId
    )
}
