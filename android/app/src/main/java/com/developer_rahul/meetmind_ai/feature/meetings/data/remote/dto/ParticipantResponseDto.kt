package com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ParticipantResponseDto(
    val id: Long? = null,
    val userId: Long? = null,
    val name: String,
    val email: String,
    val role: String,
    val status: String,
    val joinedAt: String? = null,
    val leftAt: String? = null
)
