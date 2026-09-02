package com.developer_rahul.meetmind_ai.core.network.websocket.model

import kotlinx.serialization.Serializable

@Serializable
data class SignalingMessageDto(
    val meetingId: Long,
    val senderId: Long? = null,
    val senderName: String? = null,
    val receiverId: Long? = null,
    val type: String, // JOIN, LEAVE, OFFER, ANSWER, ICE_CANDIDATE, PEER_LIST
    val payload: String? = null
)
