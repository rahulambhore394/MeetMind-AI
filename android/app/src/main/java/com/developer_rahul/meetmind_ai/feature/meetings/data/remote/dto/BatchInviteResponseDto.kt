package com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BatchInviteResponseDto(
    val totalSubmitted: Int = 0,
    val successCount: Int = 0,
    val invitedEmails: List<String> = emptyList(),
    val message: String = ""
)
