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

    @GET("api/meetings/{meetingId}/decisions")
    suspend fun getDecisions(@Path("meetingId") meetingId: Long): List<String>

    @POST("api/meetings/{meetingId}/decisions")
    suspend fun addDecision(
        @Path("meetingId") meetingId: Long,
        @Body request: Map<String, String>
    ): List<String>

    @DELETE("api/meetings/{meetingId}/decisions/{decisionIndex}")
    suspend fun deleteDecision(
        @Path("meetingId") meetingId: Long,
        @Path("decisionIndex") decisionIndex: Int
    ): List<String>

    @GET("api/meetings/{meetingId}/chapters")
    suspend fun getChapters(@Path("meetingId") meetingId: Long): List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.TopicChapterDto>

    @GET("api/meetings/{meetingId}/parking-lot")
    suspend fun getParkingLot(@Path("meetingId") meetingId: Long): List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ParkingQuestionDto>

    @POST("api/meetings/{meetingId}/parking-lot")
    suspend fun addParkingQuestion(
        @Path("meetingId") meetingId: Long,
        @Body request: Map<String, String>
    ): List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ParkingQuestionDto>

    @PATCH("api/meetings/{meetingId}/parking-lot/{questionIndex}")
    suspend fun updateParkingQuestion(
        @Path("meetingId") meetingId: Long,
        @Path("questionIndex") questionIndex: Int,
        @Body request: Map<String, String>
    ): List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ParkingQuestionDto>

    @DELETE("api/meetings/{meetingId}/parking-lot/{questionIndex}")
    suspend fun deleteParkingQuestion(
        @Path("meetingId") meetingId: Long,
        @Path("questionIndex") questionIndex: Int
    ): List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ParkingQuestionDto>
}
