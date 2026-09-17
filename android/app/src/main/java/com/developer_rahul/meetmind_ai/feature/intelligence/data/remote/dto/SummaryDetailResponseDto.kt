package com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SummaryDetailResponseDto(
    val id: Long = 0L,
    val meetingId: Long = 0L,
    val transcriptId: Long? = null,
    val summary: String = "",
    val keyPoints: List<String> = emptyList(),
    val decisions: List<String> = emptyList(),
    val topics: List<String> = emptyList(),
    val questions: List<String> = emptyList(),
    val analysisMetrics: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
    val actionItems: List<ActionItemResponseDto> = emptyList(),
    val status: String = "COMPLETED",
    val createdAt: String? = null
)

@Serializable
data class ActionItemResponseDto(
    val id: Long = 0L,
    val description: String = "",
    val assignedUser: String? = null,
    val dueDate: String? = null,
    val confidence: Double = 0.0,
    val status: String = "OPEN"
)
