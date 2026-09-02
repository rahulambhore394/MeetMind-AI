package com.developer_rahul.meetmind_ai.feature.intelligence.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.SummaryDetailResponseDto
import kotlinx.coroutines.flow.SharedFlow

interface IntelligenceRepository {
    val intelligenceEvents: SharedFlow<Map<String, String>>
    suspend fun getSummary(meetingId: Long): NetworkResult<SummaryDetailResponseDto>
    suspend fun triggerAnalysis(meetingId: Long, transcriptId: Long): NetworkResult<SummaryDetailResponseDto>
    suspend fun updateActionItemStatus(meetingId: Long, actionItemId: Long, status: String): NetworkResult<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ActionItemResponseDto>
    fun subscribeToIntelligence(meetingId: Long)
    fun unsubscribeFromIntelligence(meetingId: Long)
}
