package com.developer_rahul.meetmind_ai.feature.meetings.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.ComprehensiveReportDto
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.MeetingReportSummaryDto
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Participant

interface MeetingRepository {
    suspend fun getAllMeetings(): NetworkResult<List<Meeting>>
    suspend fun createMeeting(title: String, description: String, scheduledAt: String, invitedEmails: List<String>? = null): NetworkResult<Meeting>
    suspend fun getMeeting(meetingId: Long): NetworkResult<Meeting>
    suspend fun getMeetingByCode(code: String): NetworkResult<Meeting>
    suspend fun deleteMeeting(meetingId: Long): NetworkResult<Unit>
    suspend fun startMeeting(meetingId: Long): NetworkResult<Meeting>
    suspend fun endMeeting(meetingId: Long): NetworkResult<Meeting>
    suspend fun getParticipants(meetingId: Long): NetworkResult<List<Participant>>
    suspend fun joinMeeting(meetingId: Long): NetworkResult<Participant>
    suspend fun leaveMeeting(meetingId: Long): NetworkResult<Participant>
    suspend fun inviteParticipant(meetingId: Long, email: String): NetworkResult<Participant>
    suspend fun batchInviteParticipants(meetingId: Long, emails: List<String>): NetworkResult<Unit>

    suspend fun getAllMeetingReports(): NetworkResult<List<MeetingReportSummaryDto>>
    suspend fun getComprehensiveReport(meetingId: Long): NetworkResult<ComprehensiveReportDto>
}

