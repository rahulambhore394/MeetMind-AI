package com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BatchInviteRequestDto(
    val emails: List<String>
)
