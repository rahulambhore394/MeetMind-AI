package com.developer_rahul.meetmind_ai.feature.meetings.domain.model

data class Participant(
    val id: Long? = null,
    val userId: Long? = null,
    val name: String,
    val email: String,
    val role: ParticipantRole,
    val status: ParticipantStatus,
    val joinedAt: String? = null,
    val leftAt: String? = null,
    val isOnline: Boolean = false
)

enum class ParticipantRole {
    HOST,
    PARTICIPANT,
    AUTOMATED_AGENT,
    AI_REPRESENTATIVE,
    UNKNOWN
}

enum class ParticipantStatus {
    INVITED,
    ACCEPTED,
    DECLINED,
    JOINED,
    LEFT,
    REMOVED,
    UNKNOWN
}
