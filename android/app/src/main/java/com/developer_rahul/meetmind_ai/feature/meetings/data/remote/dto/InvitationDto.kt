package com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto

import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Invitation
import kotlinx.serialization.Serializable

@Serializable
data class InvitationDto(
    val id: Long? = null,
    val meetingId: Long,
    val meetingCode: String? = null,
    val meetingTitle: String? = null,
    val meetingDescription: String? = null,
    val hostId: Long? = null,
    val hostName: String? = null,
    val hostEmail: String? = null,
    val status: String? = null,
    val scheduledAt: String? = null,
    val isLive: Boolean? = null
) {
    fun toDomain(): Invitation = Invitation(
        id = id,
        meetingId = meetingId,
        meetingCode = meetingCode ?: "mm-$meetingId",
        meetingTitle = meetingTitle ?: "Meeting Session",
        meetingDescription = meetingDescription ?: "",
        hostId = hostId,
        hostName = hostName ?: "Host",
        hostEmail = hostEmail ?: "",
        status = status ?: "INVITED",
        scheduledAt = scheduledAt,
        isLive = isLive ?: false
    )
}

