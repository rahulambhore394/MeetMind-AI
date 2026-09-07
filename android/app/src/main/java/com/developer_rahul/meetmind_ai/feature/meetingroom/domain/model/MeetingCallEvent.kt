package com.developer_rahul.meetmind_ai.feature.meetingroom.domain.model

import org.webrtc.VideoTrack

sealed interface MeetingCallEvent {
    data class LocalStreamReady(val videoTrack: VideoTrack?) : MeetingCallEvent
    data class LocalScreenStreamReady(val videoTrack: VideoTrack?) : MeetingCallEvent
    data class RemoteStreamReady(val userId: Long, val videoTrack: VideoTrack) : MeetingCallEvent
    data class RemoteStreamRemoved(val userId: Long) : MeetingCallEvent
    data class MediaStateChanged(
        val userId: Long,
        val audioEnabled: Boolean? = null,
        val videoEnabled: Boolean? = null,
        val isSpeaking: Boolean? = null
    ) : MeetingCallEvent
    data class ConnectionStateChanged(val userId: Long, val state: String) : MeetingCallEvent
    data class ScreenShareStarted(val userId: Long, val videoTrack: VideoTrack) : MeetingCallEvent
    data class ScreenShareStopped(val userId: Long) : MeetingCallEvent
    data class Error(val message: String) : MeetingCallEvent
}
