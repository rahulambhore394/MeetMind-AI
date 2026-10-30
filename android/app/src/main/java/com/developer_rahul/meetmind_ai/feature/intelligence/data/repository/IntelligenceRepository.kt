package com.developer_rahul.meetmind_ai.feature.intelligence.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.SummaryDetailResponseDto
import kotlinx.coroutines.flow.SharedFlow

interface IntelligenceRepository {
    val intelligenceEvents: SharedFlow<Map<String, String>>
    suspend fun getSummary(meetingId: Long): NetworkResult<SummaryDetailResponseDto>
    suspend fun triggerAnalysis(meetingId: Long, transcriptId: Long): NetworkResult<SummaryDetailResponseDto>
    suspend fun updateActionItemStatus(meetingId: Long, actionItemId: Long, status: String): NetworkResult<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ActionItemResponseDto>
    suspend fun getDecisions(meetingId: Long): NetworkResult<List<String>>
    suspend fun addDecision(meetingId: Long, decision: String): NetworkResult<List<String>>
    suspend fun deleteDecision(meetingId: Long, index: Int): NetworkResult<List<String>>
    suspend fun getChapters(meetingId: Long): NetworkResult<List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.TopicChapterDto>>
    suspend fun getParkingLot(meetingId: Long): NetworkResult<List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ParkingQuestionDto>>
    suspend fun addParkingQuestion(meetingId: Long, question: String, askedBy: String = "Team", status: String = "UNANSWERED"): NetworkResult<List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ParkingQuestionDto>>
    suspend fun updateParkingQuestionStatus(meetingId: Long, index: Int, status: String, notes: String? = null): NetworkResult<List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ParkingQuestionDto>>
    suspend fun deleteParkingQuestion(meetingId: Long, index: Int): NetworkResult<List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ParkingQuestionDto>>
    fun subscribeToIntelligence(meetingId: Long)
    fun unsubscribeFromIntelligence(meetingId: Long)
}
