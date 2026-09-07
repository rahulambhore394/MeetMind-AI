package com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MeetingReportSummaryDto(
    val meetingId: Long,
    val title: String,
    val description: String? = null,
    val meetingCode: String? = null,
    val hostName: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val meetingType: String = "NORMAL", // "NORMAL" or "AI_REPRESENTATIVE"
    val status: String = "ENDED",
    val summarySnippet: String? = null,
    val actionItemCount: Int = 0,
    val myTaskCount: Int = 0,
    val hasAiRepresentative: Boolean = false
)

@Serializable
data class ComprehensiveReportDto(
    val meetingId: Long,
    val title: String,
    val description: String? = null,
    val meetingCode: String? = null,
    val hostName: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val meetingType: String = "NORMAL",
    val status: String = "ENDED",
    val executiveSummary: String? = null,
    val keyPoints: List<String> = emptyList(),
    val decisions: List<String> = emptyList(),
    val topics: List<String> = emptyList(),
    val questions: List<String> = emptyList(),
    val allWorkAssignments: List<ActionItemDto> = emptyList(),
    val myAssignedTasks: List<ActionItemDto> = emptyList(),
    val userTaskBreakdown: Map<String, List<ActionItemDto>> = emptyMap(),
    val representativeId: Long? = null,
    val ownerRelevantQuestions: List<String> = emptyList(),
    val monitoredTopicsFound: List<String> = emptyList()
)

@Serializable
data class ActionItemDto(
    val id: Long,
    val meetingId: Long,
    val description: String,
    val assignedUser: String? = null,
    val dueDate: String? = null,
    val confidence: Double = 0.9,
    val status: String = "OPEN"
)
