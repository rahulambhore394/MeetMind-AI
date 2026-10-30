package com.developer_rahul.meetmind_ai.core.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun VideoRenderer(
    modifier: Modifier = Modifier,
    videoTrack: VideoTrack? = null,
    isOverlay: Boolean = false,
    onSurfaceReady: (SurfaceViewRenderer) -> Unit = {}
) {
    var renderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    DisposableEffect(videoTrack, renderer) {
        val currentRenderer = renderer
        val currentTrack = videoTrack
        if (currentRenderer != null && currentTrack != null) {
            currentTrack.addSink(currentRenderer)
        }
        onDispose {
            if (currentRenderer != null && currentTrack != null) {
                try {
                    currentTrack.removeSink(currentRenderer)
                } catch (e: Exception) {
                    // Ignored if track is disposed
                }
            }
        }
    }

    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(com.developer_rahul.meetmind_ai.MeetMindApplication.instance.container.webRtcManager.eglBaseContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
                if (isOverlay) {
                    setZOrderMediaOverlay(true)
                }
                renderer = this
                onSurfaceReady(this)
            }
        },
        update = { view ->
            if (renderer !== view) {
                renderer = view
            }
            view.setZOrderMediaOverlay(isOverlay)
        },
        modifier = modifier,
        onRelease = { view ->
            renderer = null
            try {
                view.release()
            } catch (e: Exception) {
                // Ignored
            }
        }
    )
}
