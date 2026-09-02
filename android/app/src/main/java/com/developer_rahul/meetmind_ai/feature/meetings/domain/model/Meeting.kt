package com.developer_rahul.meetmind_ai.feature.meetings.domain.model

data class Meeting(
    val id: Long,
    val title: String,
    val description: String?,
    val hostId: Long,
    val hostName: String,
    val hostEmail: String,
    val scheduledAt: String,
    val startedAt: String?,
    val endedAt: String?,
    val createdAt: String,
    val status: MeetingStatus,
    val meetingCode: String? = null
)

enum class MeetingStatus {
    SCHEDULED,
    LIVE,
    ENDED,
    CANCELLED,
    UNKNOWN
}
