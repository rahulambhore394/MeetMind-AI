package com.developer_rahul.meetmind_ai.feature.meetings.data.remote

import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.BatchInviteRequestDto
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

    @POST("api/meetings/{meetingId}/participants/batch")
    suspend fun batchInviteParticipants(
        @Path("meetingId") meetingId: Long,
        @Body request: BatchInviteRequestDto
    ): com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.BatchInviteResponseDto

    @POST("api/meetings/{meetingId}/participants/join")
    suspend fun joinMeeting(@Path("meetingId") meetingId: Long): ParticipantResponseDto

    @POST("api/meetings/{meetingId}/participants/leave")
    suspend fun leaveMeeting(@Path("meetingId") meetingId: Long): ParticipantResponseDto

    @GET("api/meetings/reports/all")
    suspend fun getAllMeetingReports(): List<com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.MeetingReportSummaryDto>

    @GET("api/meetings/{meetingId}/comprehensive-report")
    suspend fun getComprehensiveReport(@Path("meetingId") meetingId: Long): com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.ComprehensiveReportDto

    @POST("api/meetings/{meetingId}/media-token")
    suspend fun getMediaToken(@Path("meetingId") meetingId: Long): com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.MediaTokenResponseDto

    @PATCH("api/meetings/{meetingId}/lock")
    suspend fun lockMeeting(
        @Path("meetingId") meetingId: Long,
        @Query("locked") locked: Boolean
    ): MeetingResponseDto

    @DELETE("api/meetings/{meetingId}/participants/{userId}")
    suspend fun removeParticipant(
        @Path("meetingId") meetingId: Long,
        @Path("userId") userId: Long
    )

    @PATCH("api/meetings/{meetingId}/participants/{userId}/role")
    suspend fun updateParticipantRole(
        @Path("meetingId") meetingId: Long,
        @Path("userId") userId: Long,
        @Query("role") role: String
    ): ParticipantResponseDto

    @GET("api/invitations/my-invitations")
    suspend fun getMyInvitations(): List<com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.InvitationDto>

    @GET("api/invitations/code/{code}")
    suspend fun getInvitationByCode(@Path("code") code: String): com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.InvitationDto

    @POST("api/invitations/{meetingId}/accept")
    suspend fun acceptInvitation(@Path("meetingId") meetingId: Long): com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.InvitationDto

    @POST("api/invitations/{meetingId}/decline")
    suspend fun declineInvitation(@Path("meetingId") meetingId: Long): com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.InvitationDto

    @POST("api/invitations/code/{code}/accept")
    suspend fun acceptInvitationByCode(@Path("code") code: String): com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.InvitationDto
}
