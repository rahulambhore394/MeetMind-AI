package com.developer_rahul.meetmind_ai.feature.intelligence.data.remote

import com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.SummaryDetailResponseDto
import retrofit2.http.*

interface IntelligenceApiService {
    @GET("api/meetings/{meetingId}/intelligence")
    suspend fun getSummary(@Path("meetingId") meetingId: Long): SummaryDetailResponseDto

    @POST("api/meetings/{meetingId}/intelligence/generate")
    suspend fun triggerAnalysis(
        @Path("meetingId") meetingId: Long,
        @Body request: Map<String, Long>
    ): SummaryDetailResponseDto

    @PATCH("api/meetings/{meetingId}/action-items/{actionItemId}")
    suspend fun updateActionItemStatus(
        @Path("meetingId") meetingId: Long,
        @Path("actionItemId") actionItemId: Long,
        @Body request: Map<String, String>
    ): com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ActionItemResponseDto
}
