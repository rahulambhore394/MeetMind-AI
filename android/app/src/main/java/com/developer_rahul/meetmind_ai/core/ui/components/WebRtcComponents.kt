package com.developer_rahul.meetmind_ai.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun VideoRenderer(
    modifier: Modifier = Modifier,
    videoTrack: VideoTrack? = null,
    onSurfaceReady: (SurfaceViewRenderer) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(com.developer_rahul.meetmind_ai.MeetMindApplication.instance.container.webRtcManager.eglBaseContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
                onSurfaceReady(this)
            }
        },
        modifier = modifier,
        update = { view ->
            videoTrack?.addSink(view)
        },
        onRelease = { view ->
            videoTrack?.removeSink(view)
            view.release()
        }
    )
}
