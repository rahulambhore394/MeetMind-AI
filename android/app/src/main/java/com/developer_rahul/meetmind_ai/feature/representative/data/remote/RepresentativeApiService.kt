package com.developer_rahul.meetmind_ai.feature.representative.data.remote

import com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto.AiProxySpeechDto
import com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto.AiRepresentativeDto
import com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto.CreateRepresentativeRequestDto
import com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto.RepresentativeReportDto
import retrofit2.http.*

interface RepresentativeApiService {
    @POST("api/meetings/{meetingId}/representatives")
    suspend fun createRepresentative(
        @Path("meetingId") meetingId: Long,
        @Body request: CreateRepresentativeRequestDto
    ): AiRepresentativeDto

    @GET("api/meetings/{meetingId}/representatives")
    suspend fun getRepresentative(@Path("meetingId") meetingId: Long): AiRepresentativeDto

    @POST("api/meetings/{meetingId}/representatives/{id}/media")
    suspend fun uploadMedia(
        @Path("meetingId") meetingId: Long,
        @Path("id") id: Long,
        @Body body: Map<String, String>
    ): AiRepresentativeDto

    @POST("api/meetings/{meetingId}/representatives/{id}/cancel")
    suspend fun cancelRepresentative(
        @Path("meetingId") meetingId: Long,
        @Path("id") id: Long
    ): AiRepresentativeDto

    @GET("api/meetings/{meetingId}/representatives/{id}/report")
    suspend fun getReport(
        @Path("meetingId") meetingId: Long,
        @Path("id") id: Long
    ): RepresentativeReportDto

    @POST("api/meetings/{meetingId}/representatives/{id}/speak")
    suspend fun triggerSpeech(
        @Path("meetingId") meetingId: Long,
        @Path("id") id: Long,
        @Body body: Map<String, String> = emptyMap()
    ): AiProxySpeechDto
}
