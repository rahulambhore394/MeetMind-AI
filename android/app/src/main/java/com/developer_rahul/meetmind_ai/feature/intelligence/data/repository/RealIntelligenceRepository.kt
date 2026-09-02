package com.developer_rahul.meetmind_ai.feature.intelligence.data.repository

import com.developer_rahul.meetmind_ai.core.network.error.ErrorMapper
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.IntelligenceApiService
import com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.SummaryDetailResponseDto
import kotlinx.coroutines.flow.SharedFlow

class RealIntelligenceRepository(
    private val intelligenceApiService: IntelligenceApiService,
    private val webSocketManager: MeetingWebSocketManager
) : IntelligenceRepository {

    override val intelligenceEvents: SharedFlow<Map<String, String>> = webSocketManager.intelligenceEvents

    override suspend fun getSummary(meetingId: Long): NetworkResult<SummaryDetailResponseDto> {
        return try {
            val response = intelligenceApiService.getSummary(meetingId)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun triggerAnalysis(meetingId: Long, transcriptId: Long): NetworkResult<SummaryDetailResponseDto> {
        return try {
            val response = intelligenceApiService.triggerAnalysis(meetingId, mapOf("transcriptId" to transcriptId))
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun updateActionItemStatus(
        meetingId: Long,
        actionItemId: Long,
        status: String
    ): NetworkResult<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ActionItemResponseDto> {
        return try {
            val response = intelligenceApiService.updateActionItemStatus(meetingId, actionItemId, mapOf("status" to status))
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override fun subscribeToIntelligence(meetingId: Long) {
        webSocketManager.subscribeToIntelligence(meetingId)
    }

    override fun unsubscribeFromIntelligence(meetingId: Long) {
        webSocketManager.unsubscribeFromIntelligence(meetingId)
    }
}
