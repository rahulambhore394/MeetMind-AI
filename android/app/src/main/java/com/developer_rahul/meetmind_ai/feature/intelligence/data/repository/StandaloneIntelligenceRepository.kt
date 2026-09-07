package com.developer_rahul.meetmind_ai.feature.intelligence.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ActionItemResponseDto
import com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.SummaryDetailResponseDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.JsonPrimitive

class StandaloneIntelligenceRepository : IntelligenceRepository {

    private val _intelligenceEvents = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 16)
    override val intelligenceEvents: SharedFlow<Map<String, String>> = _intelligenceEvents.asSharedFlow()

    override suspend fun getSummary(meetingId: Long): NetworkResult<SummaryDetailResponseDto> {
        val mockSummary = SummaryDetailResponseDto(
            id = 1L,
            meetingId = meetingId,
            transcriptId = 1L,
            summary = "The team discussed the MeetMind AI architecture and successfully disconnected the Android application from remote Spring Boot backend dependency. All APIs now operate seamlessly offline with local mock providers.",
            keyPoints = listOf(
                "Backend services disconnected for pure offline operation.",
                "Jetpack Compose UI components populated with rich local sample data.",
                "Android app build validated with zero network latency or server errors."
            ),
            decisions = listOf(
                "Adopt Standalone Repositories for full client-side independence.",
                "Maintain WebRTC native client capability for local peer-to-peer testing."
            ),
            topics = listOf("Offline Architecture", "Jetpack Compose", "Standalone Mode", "Local Data"),
            questions = listOf("How does offline mode affect real-time sync?", "Are AI summaries generated locally?"),
            analysisMetrics = mapOf("wordCount" to JsonPrimitive(120), "speakerCount" to JsonPrimitive(3)),
            actionItems = listOf(
                ActionItemResponseDto(id = 1L, description = "Verify offline Android build via Gradle", assignedUser = "Rahul Ambhore", dueDate = "Today", confidence = 0.98, status = "COMPLETED"),
                ActionItemResponseDto(id = 2L, description = "Test full offline navigation flow across screens", assignedUser = "Alex Rivera", dueDate = "Tomorrow", confidence = 0.95, status = "OPEN")
            ),
            status = "COMPLETED",
            createdAt = "2026-09-04 23:30:00"
        )
        return NetworkResult.Success(mockSummary)
    }

    override suspend fun triggerAnalysis(
        meetingId: Long,
        transcriptId: Long
    ): NetworkResult<SummaryDetailResponseDto> {
        return getSummary(meetingId)
    }

    override suspend fun updateActionItemStatus(
        meetingId: Long,
        actionItemId: Long,
        status: String
    ): NetworkResult<ActionItemResponseDto> {
        val updated = ActionItemResponseDto(
            id = actionItemId,
            description = "Verify offline Android build via Gradle",
            assignedUser = "Rahul Ambhore",
            dueDate = "Today",
            confidence = 0.99,
            status = status
        )
        return NetworkResult.Success(updated)
    }

    override fun subscribeToIntelligence(meetingId: Long) {
        // No-op for standalone offline mode
    }

    override fun unsubscribeFromIntelligence(meetingId: Long) {
        // No-op for standalone offline mode
    }
}
