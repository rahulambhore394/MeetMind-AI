package com.developer_rahul.meetmind_ai.feature.meetings.data.remote

import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.CreateMeetingRequestDto
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.InviteParticipantRequestDto
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.MeetingResponseDto
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.ParticipantResponseDto
import retrofit2.http.*

interface MeetingApiService {
    @GET("api/meetings")
    suspend fun getAllMeetings(): List<MeetingResponseDto>

    @POST("api/meetings")
    suspend fun createMeeting(@Body request: CreateMeetingRequestDto): MeetingResponseDto

    @GET("api/meetings/{meetingId}")
    suspend fun getMeeting(@Path("meetingId") meetingId: Long): MeetingResponseDto

    @GET("api/meetings/code/{code}")
    suspend fun getMeetingByCode(@Path("code") code: String): MeetingResponseDto

    @POST("api/meetings/code/{code}/join")
    suspend fun joinMeetingByCode(@Path("code") code: String): MeetingResponseDto

    @DELETE("api/meetings/{meetingId}")
    suspend fun deleteMeeting(@Path("meetingId") meetingId: Long)

    @POST("api/meetings/{meetingId}/start")
    suspend fun startMeeting(@Path("meetingId") meetingId: Long): MeetingResponseDto

    @POST("api/meetings/{meetingId}/end")
    suspend fun endMeeting(@Path("meetingId") meetingId: Long): MeetingResponseDto

    @GET("api/meetings/{meetingId}/participants")
    suspend fun getParticipants(@Path("meetingId") meetingId: Long): List<ParticipantResponseDto>

    @POST("api/meetings/{meetingId}/participants")
    suspend fun inviteParticipant(
        @Path("meetingId") meetingId: Long,
        @Body request: InviteParticipantRequestDto
    ): ParticipantResponseDto

    @POST("api/meetings/{meetingId}/participants/join")
    suspend fun joinMeeting(@Path("meetingId") meetingId: Long): ParticipantResponseDto

    @POST("api/meetings/{meetingId}/participants/leave")
    suspend fun leaveMeeting(@Path("meetingId") meetingId: Long): ParticipantResponseDto
}
