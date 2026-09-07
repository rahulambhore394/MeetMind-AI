package com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MeetingResponseDto(
    val id: Long,
    val title: String,
    val description: String?,
    val hostId: Long,
    val hostName: String,
    val hostEmail: String,
    val scheduledAt: String, // ISO-8601 string
    val startedAt: String? = null,
    val endedAt: String? = null,
    val createdAt: String,
    val status: String,
    val meetingCode: String? = null,
    val hasJoinedBefore: Boolean? = false,
    val userParticipantStatus: String? = null
)
