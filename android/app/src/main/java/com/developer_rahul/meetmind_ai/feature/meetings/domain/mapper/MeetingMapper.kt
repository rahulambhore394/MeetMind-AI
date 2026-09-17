package com.developer_rahul.meetmind_ai.feature.meetings.domain.mapper

import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.MeetingResponseDto
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.ParticipantResponseDto
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.*

fun MeetingResponseDto.toDomain(): Meeting {
    return Meeting(
        id = id,
        title = title,
        description = description,
        hostId = hostId,
        hostName = hostName,
        hostEmail = hostEmail,
        scheduledAt = scheduledAt,
        startedAt = startedAt,
        endedAt = endedAt,
        createdAt = createdAt,
        status = try {
            MeetingStatus.valueOf(status.uppercase())
        } catch (e: Exception) {
            MeetingStatus.UNKNOWN
        },
        meetingCode = meetingCode,
        hasJoinedBefore = hasJoinedBefore ?: false,
        userParticipantStatus = userParticipantStatus
    )
}

fun ParticipantResponseDto.toDomain(): Participant {
    return Participant(
        id = id,
        userId = userId,
        name = name,
        email = email,
        role = try {
            val upperRole = role.uppercase()
            if (upperRole == "AUTOMATED_AGENT") ParticipantRole.AI_REPRESENTATIVE
            else ParticipantRole.valueOf(upperRole)
        } catch (e: Exception) {
            ParticipantRole.UNKNOWN
        },
        status = try {
            ParticipantStatus.valueOf(status.uppercase())
        } catch (e: Exception) {
            ParticipantStatus.UNKNOWN
        },
        joinedAt = joinedAt,
        leftAt = leftAt
    )
}
