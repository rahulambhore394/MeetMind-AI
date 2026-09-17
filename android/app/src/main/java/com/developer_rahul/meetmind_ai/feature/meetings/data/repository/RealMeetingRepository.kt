package com.developer_rahul.meetmind_ai.feature.meetings.data.repository

import com.developer_rahul.meetmind_ai.core.network.error.ErrorMapper
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.MeetingApiService
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.BatchInviteRequestDto
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.ComprehensiveReportDto
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.CreateMeetingRequestDto
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.InviteParticipantRequestDto
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.MeetingReportSummaryDto
import com.developer_rahul.meetmind_ai.feature.meetings.domain.mapper.toDomain
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Participant

class RealMeetingRepository(
    private val meetingApiService: MeetingApiService
) : MeetingRepository {

    override suspend fun getAllMeetings(): NetworkResult<List<Meeting>> {
        return try {
            val response = meetingApiService.getAllMeetings()
            NetworkResult.Success(response.map { it.toDomain() })
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun createMeeting(
        title: String,
        description: String,
        scheduledAt: String,
        invitedEmails: List<String>?
    ): NetworkResult<Meeting> {
        return try {
            val request = CreateMeetingRequestDto(title, description, scheduledAt, invitedEmails)
            val response = meetingApiService.createMeeting(request)
            NetworkResult.Success(response.toDomain())
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun getMeeting(meetingId: Long): NetworkResult<Meeting> {
        return try {
            val response = meetingApiService.getMeeting(meetingId)
            NetworkResult.Success(response.toDomain())
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun getMeetingByCode(code: String): NetworkResult<Meeting> {
        return try {
            val response = meetingApiService.getMeetingByCode(code)
            NetworkResult.Success(response.toDomain())
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun deleteMeeting(meetingId: Long): NetworkResult<Unit> {
        return try {
            meetingApiService.deleteMeeting(meetingId)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun startMeeting(meetingId: Long): NetworkResult<Meeting> {
        return try {
            val response = meetingApiService.startMeeting(meetingId)
            NetworkResult.Success(response.toDomain())
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun endMeeting(meetingId: Long): NetworkResult<Meeting> {
        return try {
            val response = meetingApiService.endMeeting(meetingId)
            NetworkResult.Success(response.toDomain())
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun getParticipants(meetingId: Long): NetworkResult<List<Participant>> {
        return try {
            val response = meetingApiService.getParticipants(meetingId)
            NetworkResult.Success(response.map { it.toDomain() })
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun joinMeeting(meetingId: Long): NetworkResult<Participant> {
        return try {
            val response = meetingApiService.joinMeeting(meetingId)
            NetworkResult.Success(response.toDomain())
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun leaveMeeting(meetingId: Long): NetworkResult<Participant> {
        return try {
            val response = meetingApiService.leaveMeeting(meetingId)
            NetworkResult.Success(response.toDomain())
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun inviteParticipant(
        meetingId: Long,
        email: String
    ): NetworkResult<Participant> {
        return try {
            val response = meetingApiService.inviteParticipant(meetingId, InviteParticipantRequestDto(email))
            NetworkResult.Success(response.toDomain())
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun batchInviteParticipants(
        meetingId: Long,
        emails: List<String>
    ): NetworkResult<Unit> {
        return try {
            meetingApiService.batchInviteParticipants(meetingId, BatchInviteRequestDto(emails))
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun getAllMeetingReports(): NetworkResult<List<MeetingReportSummaryDto>> {
        return try {
            val response = meetingApiService.getAllMeetingReports()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun getComprehensiveReport(meetingId: Long): NetworkResult<ComprehensiveReportDto> {
        return try {
            val response = meetingApiService.getComprehensiveReport(meetingId)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }
}
