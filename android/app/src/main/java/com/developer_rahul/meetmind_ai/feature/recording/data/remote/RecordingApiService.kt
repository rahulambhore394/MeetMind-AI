package com.developer_rahul.meetmind_ai.feature.recording.data.remote

import com.developer_rahul.meetmind_ai.feature.recording.data.remote.dto.RecordingResponseDto
import okhttp3.MultipartBody
import retrofit2.http.*

interface RecordingApiService {
    @POST("api/meetings/{meetingId}/recordings/start")
    suspend fun startRecording(@Path("meetingId") meetingId: Long): RecordingResponseDto

    @POST("api/meetings/{meetingId}/recordings/{recordingId}/stop")
    suspend fun stopRecording(
        @Path("meetingId") meetingId: Long,
        @Path("recordingId") recordingId: Long
    ): RecordingResponseDto

    @Multipart
    @POST("api/meetings/{meetingId}/recordings/{recordingId}/upload")
    suspend fun uploadRecordingFile(
        @Path("meetingId") meetingId: Long,
        @Path("recordingId") recordingId: Long,
        @Part file: MultipartBody.Part
    ): RecordingResponseDto

    @GET("api/meetings/{meetingId}/recordings")
    suspend fun listRecordings(@Path("meetingId") meetingId: Long): List<RecordingResponseDto>
}
