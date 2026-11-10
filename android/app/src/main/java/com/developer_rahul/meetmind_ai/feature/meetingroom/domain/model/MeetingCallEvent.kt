package com.developer_rahul.meetmind_ai.feature.meetingroom.domain.model

import org.webrtc.VideoTrack

sealed interface MeetingCallEvent {
    data class LocalStreamReady(
        val videoTrack: VideoTrack? = null,
        val sfuVideoTrack: io.livekit.android.room.track.VideoTrack? = null
    ) : MeetingCallEvent

    data class LocalScreenStreamReady(
        val videoTrack: VideoTrack? = null,
        val sfuVideoTrack: io.livekit.android.room.track.VideoTrack? = null
    ) : MeetingCallEvent

    data class RemoteStreamReady(
        val userId: Long,
        val videoTrack: VideoTrack? = null,
        val sfuVideoTrack: io.livekit.android.room.track.VideoTrack? = null
    ) : MeetingCallEvent

    data class RemoteStreamRemoved(val userId: Long) : MeetingCallEvent

    data class MediaStateChanged(
        val userId: Long,
        val audioEnabled: Boolean? = null,
        val videoEnabled: Boolean? = null,
        val isSpeaking: Boolean? = null
    ) : MeetingCallEvent

    data class ConnectionStateChanged(val userId: Long, val state: String) : MeetingCallEvent

    data class ScreenShareStarted(
        val userId: Long,
        val videoTrack: VideoTrack? = null,
        val sfuVideoTrack: io.livekit.android.room.track.VideoTrack? = null
    ) : MeetingCallEvent

    data class ScreenShareStopped(val userId: Long) : MeetingCallEvent

    data class Error(val message: String) : MeetingCallEvent

    // Host Control Events
    data class HostMuted(val targetUserId: Long, val isMuteAll: Boolean) : MeetingCallEvent
    data class HostVideoStopped(val targetUserId: Long) : MeetingCallEvent
    data class HostRemoved(val targetUserId: Long) : MeetingCallEvent
    data class HostLockChanged(val isLocked: Boolean) : MeetingCallEvent
    data class HostPermissionsUpdated(
        val allowScreenShare: Boolean,
        val allowChat: Boolean,
        val allowUnmute: Boolean
    ) : MeetingCallEvent
}
