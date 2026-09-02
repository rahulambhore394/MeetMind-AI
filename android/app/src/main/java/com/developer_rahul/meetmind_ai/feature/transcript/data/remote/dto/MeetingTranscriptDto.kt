package com.developer_rahul.meetmind_ai.feature.transcript.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MeetingTranscriptDto(
    val id: Long,
    val meetingId: Long,
    val recordingId: Long,
    val language: String,
    val fullText: String? = null,
    val status: String,
    val providerName: String? = null,
    val errorMessage: String? = null,
    val completedAt: String? = null,
    val createdAt: String
)
