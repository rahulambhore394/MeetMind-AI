package com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AiProxySpeechDto(
    val meetingId: Long,
    val representativeId: Long,
    val ownerId: Long,
    val ownerName: String,
    val query: String,
    val spokenText: String,
    val language: String = "en",
    val timestamp: Long = 0L
)
