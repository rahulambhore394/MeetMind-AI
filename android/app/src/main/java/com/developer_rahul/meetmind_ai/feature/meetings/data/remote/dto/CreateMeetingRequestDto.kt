package com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateMeetingRequestDto(
    val title: String,
    val description: String?,
    val scheduledAt: String, // ISO-8601 string
    val invitedEmails: List<String>? = null
)
