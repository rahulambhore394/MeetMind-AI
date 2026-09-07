package com.developer_rahul.meetmind_ai.core.webrtc

import android.content.Context
import android.util.Log
import com.developer_rahul.meetmind_ai.core.webrtc.data.remote.dto.IceServerDto
import org.webrtc.*
import java.util.*

class WebRtcManager(
    private val context: Context,
    val eglBaseContext: EglBase.Context
) {
    private val TAG = "WebRtcManager"
    
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null

    private var screenVideoSource: VideoSource? = null
    private var screenVideoTrack: VideoTrack? = null
    private var screenCapturer: VideoCapturer? = null

    init {
        initPeerConnectionFactory(context)
    }

    private fun initPeerConnectionFactory(context: Context) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val factoryOptions = PeerConnectionFactory.Options()
        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(eglBaseContext, true, true)
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(factoryOptions)
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()
    }

    fun createLocalStream() {
        if (peerConnectionFactory == null) {
            initPeerConnectionFactory(context)
        }
        stopLocalStream()

        try {
            // Audio
            localAudioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
            localAudioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", localAudioSource)

            // Video
            localVideoSource = peerConnectionFactory?.createVideoSource(false)
            videoCapturer = createVideoCapturer(context)
            videoCapturer?.initialize(SurfaceTextureHelper.create("CaptureThread", eglBaseContext), context, localVideoSource?.capturerObserver)
            videoCapturer?.startCapture(1280, 720, 30)
            localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", localVideoSource)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating local stream", e)
        }
    }

    fun stopLocalStream() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capturer: ${e.message}")
        }
        videoCapturer = null
        localVideoTrack?.dispose()
        localVideoTrack = null
        localVideoSource?.dispose()
        localVideoSource = null
        localAudioTrack?.dispose()
        localAudioTrack = null
        localAudioSource?.dispose()
        localAudioSource = null
    }

    fun createPeerConnection(
        iceServers: List<IceServerDto>,
        observer: PeerConnection.Observer
    ): PeerConnection? {
        if (peerConnectionFactory == null) {
            initPeerConnectionFactory(context)
        }

        val rtcIceServers = iceServers.map {
            PeerConnection.IceServer.builder(it.urls)
                .setUsername(it.username)
                .setPassword(it.credential)
                .createIceServer()
        }

        val rtcConfig = PeerConnection.RTCConfiguration(rtcIceServers)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        
        val pc = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        
        localAudioTrack?.let { pc?.addTrack(it, listOf("ARDAMS")) }
        localVideoTrack?.let { pc?.addTrack(it, listOf("ARDAMS")) }
        
        return pc
    }

    private fun createVideoCapturer(context: Context): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }

        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }

    fun setVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun setAudioEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun startScreenCapture(mediaProjectionData: android.content.Intent) {
        try {
            if (peerConnectionFactory == null) {
                initPeerConnectionFactory(context)
            }
            stopScreenCapture()
            screenVideoSource = peerConnectionFactory?.createVideoSource(true)
            screenCapturer = ScreenCapturerAndroid(mediaProjectionData, object : android.media.projection.MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "Screen capture stopped by system/user")
                    stopScreenCapture()
                }
            })
            val displayMetrics = context.resources.displayMetrics
            val width = if (displayMetrics.widthPixels > 0) displayMetrics.widthPixels else 1280
            val height = if (displayMetrics.heightPixels > 0) displayMetrics.heightPixels else 720

            screenCapturer?.initialize(SurfaceTextureHelper.create("ScreenCaptureThread", eglBaseContext), context, screenVideoSource?.capturerObserver)
            screenCapturer?.startCapture(width, height, 15)
            screenVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSs0", screenVideoSource)
            Log.d(TAG, "Screen capture started successfully ($width x $height)")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting screen capture: ${e.message}", e)
            stopScreenCapture()
        }
    }

    fun stopScreenCapture() {
        try {
            screenCapturer?.stopCapture()
            screenCapturer?.dispose()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping screen capturer: ${e.message}")
        }
        screenCapturer = null
        screenVideoTrack?.dispose()
        screenVideoTrack = null
        screenVideoSource?.dispose()
        screenVideoSource = null
    }

    fun switchCamera() {
        val cameraCapturer = videoCapturer as? CameraVideoCapturer
        cameraCapturer?.switchCamera(null)
    }

    fun getLocalVideoTrack(): VideoTrack? = localVideoTrack
    fun getLocalScreenTrack(): VideoTrack? = screenVideoTrack

    fun dispose() {
        stopScreenCapture()
        stopLocalStream()
        try {
            peerConnectionFactory?.dispose()
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing factory: ${e.message}")
        }
        peerConnectionFactory = null
    }
}
