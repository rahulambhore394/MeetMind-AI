package com.developer_rahul.meetmind_ai.feature.recording.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecordingResponseDto(
    val id: Long,
    val meetingId: Long,
    val ownerId: Long,
    val startedAt: String,
    val endedAt: String? = null,
    val duration: Long? = null,
    val status: String,
    val createdAt: String
)
