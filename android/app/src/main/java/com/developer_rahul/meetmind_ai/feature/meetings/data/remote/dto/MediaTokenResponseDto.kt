package com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MediaTokenResponseDto(
    val meetingId: String,
    val rtcUrl: String,
    val token: String,
    val participantId: String,
    val permissions: MediaPermissionsDto? = null,
    val expiresAt: String? = null
)

@Serializable
data class MediaPermissionsDto(
    val canPublishAudio: Boolean = true,
    val canPublishVideo: Boolean = true,
    val canPublishScreen: Boolean = false,
    val canSubscribe: Boolean = true
)

