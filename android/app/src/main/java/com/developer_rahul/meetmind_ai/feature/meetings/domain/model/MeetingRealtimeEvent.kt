package com.developer_rahul.meetmind_ai.feature.meetings.domain.model

sealed interface MeetingRealtimeEvent {
    val meetingId: Long
    
    data class MeetingStarted(override val meetingId: Long) : MeetingRealtimeEvent
    data class MeetingEnded(override val meetingId: Long) : MeetingRealtimeEvent
    data class ParticipantJoined(override val meetingId: Long, val userId: Long, val userName: String) : MeetingRealtimeEvent
    data class ParticipantLeft(override val meetingId: Long, val userId: Long, val userName: String) : MeetingRealtimeEvent
    data class ParticipantAccepted(override val meetingId: Long, val userId: Long, val userName: String) : MeetingRealtimeEvent
    data class ParticipantDeclined(override val meetingId: Long, val userId: Long, val userName: String) : MeetingRealtimeEvent
}
