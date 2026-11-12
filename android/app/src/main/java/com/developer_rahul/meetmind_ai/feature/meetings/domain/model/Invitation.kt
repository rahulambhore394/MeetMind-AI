package com.developer_rahul.meetmind_ai.feature.meetings.domain.model

data class Invitation(
    val id: Long?,
    val meetingId: Long,
    val meetingCode: String,
    val meetingTitle: String,
    val meetingDescription: String,
    val hostId: Long?,
    val hostName: String,
    val hostEmail: String,
    val status: String, // "INVITED", "ACCEPTED", "DECLINED"
    val scheduledAt: String?,
    val isLive: Boolean
)

