package com.developer_rahul.meetmind_ai.feature.meetings.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.MeetingStatus
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Participant
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.ParticipantRole
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.ParticipantStatus

class StandaloneMeetingRepository : MeetingRepository {

    private val sampleParticipants = listOf(
        Participant(
            id = 1L,
            userId = 101L,
            name = "Rahul Ambhore",
            email = "rahul@meetmind.ai",
            role = ParticipantRole.HOST,
            status = ParticipantStatus.JOINED,
            joinedAt = "2026-09-04 10:00:00",
            isOnline = true
        ),
        Participant(
            id = 2L,
            userId = 102L,
            name = "Sarah Connor",
            email = "sarah@meetmind.ai",
            role = ParticipantRole.PARTICIPANT,
            status = ParticipantStatus.ACCEPTED,
            isOnline = true
        ),
        Participant(
            id = 3L,
            userId = 103L,
            name = "Alex Rivera",
            email = "alex@meetmind.ai",
            role = ParticipantRole.PARTICIPANT,
            status = ParticipantStatus.INVITED,
            isOnline = false
        )
    )

    private val sampleMeetings = mutableListOf(
        Meeting(
            id = 101L,
            title = "AI Architecture & Strategy Review",
            description = "Monthly roadmap sync for MeetMind AI offline intelligence engine and WebRTC integration.",
            hostId = 101L,
            hostName = "Rahul Ambhore",
            hostEmail = "rahul@meetmind.ai",
            scheduledAt = "2026-09-04 10:00:00",
            startedAt = "2026-09-04 10:05:00",
            endedAt = null,
            createdAt = "2026-09-04 09:00:00",
            status = MeetingStatus.LIVE,
            meetingCode = "MM-884-AI"
        ),
        Meeting(
            id = 102L,
            title = "Mobile UX & Jetpack Compose Review",
            description = "Design discussion regarding dark theme, bottom navigation, and live transcription view.",
            hostId = 102L,
            hostName = "Sarah Connor",
            hostEmail = "sarah@meetmind.ai",
            scheduledAt = "2026-09-04 08:00:00",
            startedAt = "2026-09-04 08:00:00",
            endedAt = "2026-09-04 09:00:00",
            createdAt = "2026-09-03 18:00:00",
            status = MeetingStatus.ENDED,
            meetingCode = "MM-992-UX"
        ),
        Meeting(
            id = 103L,
            title = "Weekly Product Standup",
            description = "Quick 15-min sync on current sprint progress and upcoming AI release candidate.",
            hostId = 101L,
            hostName = "Rahul Ambhore",
            hostEmail = "rahul@meetmind.ai",
            scheduledAt = "2026-09-05 10:00:00",
            startedAt = null,
            endedAt = null,
            createdAt = "2026-09-04 09:30:00",
            status = MeetingStatus.SCHEDULED,
            meetingCode = "MM-103-SU"
        )
    )

    override suspend fun getAllMeetings(): NetworkResult<List<Meeting>> {
        return NetworkResult.Success(sampleMeetings.toList())
    }

    override suspend fun createMeeting(
        title: String,
        description: String,
        scheduledAt: String,
        invitedEmails: List<String>?
    ): NetworkResult<Meeting> {
        val newId = (sampleMeetings.maxOfOrNull { it.id } ?: 100L) + 1L
        val newCode = "MM-${(100..999).random()}-AI"
        val newMeeting = Meeting(
            id = newId,
            title = title,
            description = description,
            hostId = 101L,
            hostName = "Rahul Ambhore",
            hostEmail = "rahul@meetmind.ai",
            scheduledAt = scheduledAt,
            startedAt = null,
            endedAt = null,
            createdAt = "Just now",
            status = MeetingStatus.SCHEDULED,
            meetingCode = newCode
        )
        sampleMeetings.add(0, newMeeting)
        return NetworkResult.Success(newMeeting)
    }

    override suspend fun getMeeting(meetingId: Long): NetworkResult<Meeting> {
        val meeting = sampleMeetings.find { it.id == meetingId } ?: sampleMeetings.first()
        return NetworkResult.Success(meeting)
    }

    override suspend fun getMeetingByCode(code: String): NetworkResult<Meeting> {
        val meeting = sampleMeetings.find { it.meetingCode.equals(code, ignoreCase = true) } ?: sampleMeetings.first()
        return NetworkResult.Success(meeting)
    }

    override suspend fun deleteMeeting(meetingId: Long): NetworkResult<Unit> {
        sampleMeetings.removeAll { it.id == meetingId }
        return NetworkResult.Success(Unit)
    }

    override suspend fun startMeeting(meetingId: Long): NetworkResult<Meeting> {
        val index = sampleMeetings.indexOfFirst { it.id == meetingId }
        if (index != -1) {
            val updated = sampleMeetings[index].copy(
                status = MeetingStatus.LIVE,
                startedAt = "Just now"
            )
            sampleMeetings[index] = updated
            return NetworkResult.Success(updated)
        }
        return NetworkResult.Success(sampleMeetings.first().copy(status = MeetingStatus.LIVE))
    }

    override suspend fun endMeeting(meetingId: Long): NetworkResult<Meeting> {
        val index = sampleMeetings.indexOfFirst { it.id == meetingId }
        if (index != -1) {
            val updated = sampleMeetings[index].copy(
                status = MeetingStatus.ENDED,
                endedAt = "Just now"
            )
            sampleMeetings[index] = updated
            return NetworkResult.Success(updated)
        }
        return NetworkResult.Success(sampleMeetings.first().copy(status = MeetingStatus.ENDED))
    }

    override suspend fun getParticipants(meetingId: Long): NetworkResult<List<Participant>> {
        return NetworkResult.Success(sampleParticipants)
    }

    override suspend fun joinMeeting(meetingId: Long): NetworkResult<Participant> {
        val participant = Participant(
            id = System.currentTimeMillis(),
            userId = 101L,
            name = "Rahul Ambhore",
            email = "rahul@meetmind.ai",
            role = ParticipantRole.PARTICIPANT,
            status = ParticipantStatus.JOINED,
            joinedAt = "Just now",
            isOnline = true
        )
        return NetworkResult.Success(participant)
    }

    override suspend fun leaveMeeting(meetingId: Long): NetworkResult<Participant> {
        val participant = Participant(
            id = System.currentTimeMillis(),
            userId = 101L,
            name = "Rahul Ambhore",
            email = "rahul@meetmind.ai",
            role = ParticipantRole.PARTICIPANT,
            status = ParticipantStatus.LEFT,
            leftAt = "Just now",
            isOnline = false
        )
        return NetworkResult.Success(participant)
    }

    override suspend fun inviteParticipant(
        meetingId: Long,
        email: String
    ): NetworkResult<Participant> {
        val participant = Participant(
            id = System.currentTimeMillis(),
            userId = (104..999).random().toLong(),
            name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
            email = email,
            role = ParticipantRole.PARTICIPANT,
            status = ParticipantStatus.INVITED,
            isOnline = false
        )
        return NetworkResult.Success(participant)
    }

    override suspend fun getAllMeetingReports(): NetworkResult<List<com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.MeetingReportSummaryDto>> {
        return NetworkResult.Success(emptyList())
    }

    override suspend fun getComprehensiveReport(meetingId: Long): NetworkResult<com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.ComprehensiveReportDto> {
        return NetworkResult.Success(
            com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.ComprehensiveReportDto(
                meetingId = meetingId,
                title = "Sample Meeting Report",
                executiveSummary = "Sample report summary"
            )
        )
    }
}
