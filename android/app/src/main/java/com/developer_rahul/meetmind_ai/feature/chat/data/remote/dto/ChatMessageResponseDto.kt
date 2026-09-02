package com.developer_rahul.meetmind_ai.feature.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageResponseDto(
    val messageId: Long,
    val meetingId: Long,
    val senderId: Long,
    val senderName: String,
    val message: String,
    val messageType: String,
    val sentAt: String // ISO-8601 string
)

@Serializable
data class ChatPageResponseDto(
    val content: List<ChatMessageResponseDto>,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int
)
