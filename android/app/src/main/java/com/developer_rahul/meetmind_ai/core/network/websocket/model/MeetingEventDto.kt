package com.developer_rahul.meetmind_ai.core.network.websocket.model

import kotlinx.serialization.Serializable

@Serializable
data class MeetingEventDto(
    val type: String,
    val meetingId: Long,
    val userId: Long? = null,
    val userName: String? = null,
    val message: String? = null
)
