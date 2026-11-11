package com.developer_rahul.meetmind_ai.feature.transcript.data.remote

import com.developer_rahul.meetmind_ai.feature.transcript.data.remote.dto.MeetingTranscriptDto
import com.developer_rahul.meetmind_ai.feature.transcript.data.remote.dto.TranscriptDetailDto
import retrofit2.http.*

interface TranscriptApiService {
    @GET("api/meetings/{meetingId}/transcripts")
    suspend fun listTranscripts(@Path("meetingId") meetingId: Long): List<MeetingTranscriptDto>

    @GET("api/meetings/{meetingId}/transcripts/{transcriptId}")
    suspend fun getTranscript(
        @Path("meetingId") meetingId: Long,
        @Path("transcriptId") transcriptId: Long
    ): TranscriptDetailDto

    @GET("api/meetings/{meetingId}/transcripts/latest")
    suspend fun getLatestTranscript(
        @Path("meetingId") meetingId: Long
    ): TranscriptDetailDto

    @GET("api/meetings/{meetingId}/transcripts/export")
    suspend fun exportTranscript(
        @Path("meetingId") meetingId: Long
    ): okhttp3.ResponseBody

    @POST("api/meetings/{meetingId}/transcripts/trigger")
    suspend fun triggerTranscription(
        @Path("meetingId") meetingId: Long,
        @Body request: Map<String, Long>
    ): MeetingTranscriptDto
}
