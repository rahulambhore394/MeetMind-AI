package com.developer_rahul.meetmind_ai.feature.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageRequestDto(
    val meetingId: Long,
    val message: String
)
