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

    override suspend fun getDecisions(meetingId: Long): NetworkResult<List<String>> {
        return try {
            val response = intelligenceApiService.getDecisions(meetingId)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun addDecision(meetingId: Long, decision: String): NetworkResult<List<String>> {
        return try {
            val response = intelligenceApiService.addDecision(meetingId, mapOf("decision" to decision))
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun deleteDecision(meetingId: Long, index: Int): NetworkResult<List<String>> {
        return try {
            val response = intelligenceApiService.deleteDecision(meetingId, index)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun getChapters(meetingId: Long): NetworkResult<List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.TopicChapterDto>> {
        return try {
            val response = intelligenceApiService.getChapters(meetingId)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun getParkingLot(meetingId: Long): NetworkResult<List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ParkingQuestionDto>> {
        return try {
            val response = intelligenceApiService.getParkingLot(meetingId)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun addParkingQuestion(meetingId: Long, question: String, askedBy: String, status: String): NetworkResult<List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ParkingQuestionDto>> {
        return try {
            val body = mapOf("question" to question, "askedBy" to askedBy, "status" to status)
            val response = intelligenceApiService.addParkingQuestion(meetingId, body)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun updateParkingQuestionStatus(meetingId: Long, index: Int, status: String, notes: String?): NetworkResult<List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ParkingQuestionDto>> {
        return try {
            val body = mutableMapOf("status" to status)
            if (notes != null) body["notes"] = notes
            val response = intelligenceApiService.updateParkingQuestion(meetingId, index, body)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun deleteParkingQuestion(meetingId: Long, index: Int): NetworkResult<List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ParkingQuestionDto>> {
        return try {
            val response = intelligenceApiService.deleteParkingQuestion(meetingId, index)
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
