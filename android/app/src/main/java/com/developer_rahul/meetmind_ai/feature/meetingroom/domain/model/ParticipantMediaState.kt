package com.developer_rahul.meetmind_ai.feature.meetingroom.domain.model

import org.webrtc.VideoTrack

data class ParticipantMediaState(
    val userId: Long,
    val displayName: String? = null,
    val audioEnabled: Boolean = true,
    val videoEnabled: Boolean = true,
    val isSpeaking: Boolean = false,
    val connectionState: String = "CONNECTING",
    val videoTrack: VideoTrack? = null,
    val sfuVideoTrack: io.livekit.android.room.track.VideoTrack? = null,
    val screenTrack: VideoTrack? = null,
    val sfuScreenTrack: io.livekit.android.room.track.VideoTrack? = null,
    val isScreenSharing: Boolean = false
)
