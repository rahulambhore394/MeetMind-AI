package com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SummaryDetailResponseDto(
    val id: Long,
    val meetingId: Long,
    val transcriptId: Long,
    val summary: String,
    val keyPoints: List<String>,
    val decisions: List<String>,
    val topics: List<String>,
    val questions: List<String>,
    val analysisMetrics: Map<String, kotlinx.serialization.json.JsonElement>,
    val actionItems: List<ActionItemResponseDto> = emptyList(),
    val status: String,
    val createdAt: String
)

@Serializable
data class ActionItemResponseDto(
    val id: Long,
    val description: String,
    val assignedUser: String? = null,
    val dueDate: String? = null,
    val confidence: Double,
    val status: String
)
