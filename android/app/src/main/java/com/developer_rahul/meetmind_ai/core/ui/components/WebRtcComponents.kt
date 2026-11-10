package com.developer_rahul.meetmind_ai.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.developer_rahul.meetmind_ai.MeetMindApplication
import io.getstream.webrtc.android.compose.VideoRenderer as StreamVideoRenderer
import io.getstream.webrtc.android.compose.VideoScalingType
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import io.livekit.android.compose.ui.ScaleType
import io.livekit.android.compose.ui.VideoTrackView as LiveKitVideoTrackView
import org.webrtc.RendererCommon
import org.webrtc.VideoTrack

@Composable
fun VideoRenderer(
    modifier: Modifier = Modifier,
    videoTrack: VideoTrack? = null,
    sfuVideoTrack: io.livekit.android.room.track.VideoTrack? = null,
    isOverlay: Boolean = false,
    mirror: Boolean = false,
    onSurfaceReady: (VideoTextureViewRenderer) -> Unit = {}
) {
    val eglBaseContext = remember {
        MeetMindApplication.instance.container.webRtcManager.eglBaseContext
    }
    val currentRoom by MeetMindApplication.instance.container.liveKitMediaManager.currentRoom.collectAsState()

    Box(modifier = modifier.background(Color.Black)) {
        if (sfuVideoTrack != null && currentRoom != null) {
            LiveKitVideoTrackView(
                videoTrack = sfuVideoTrack,
                passedRoom = currentRoom,
                mirror = mirror || isOverlay,
                scaleType = ScaleType.Fill,
                modifier = Modifier.fillMaxSize()
            )
        } else if (videoTrack != null) {
            StreamVideoRenderer(
                modifier = Modifier.fillMaxSize(),
                videoTrack = videoTrack,
                eglBaseContext = eglBaseContext,
                videoScalingType = VideoScalingType.SCALE_ASPECT_FILL,
                onTextureViewCreated = { renderer ->
                    onSurfaceReady(renderer)
                },
                rendererEvents = object : RendererCommon.RendererEvents {
                    override fun onFirstFrameRendered() {}
                    override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {}
                }
            )
        }
    }
}
