package com.developer_rahul.meetmind_ai.core.webrtc

import android.content.Context
import android.util.Log
import com.developer_rahul.meetmind_ai.core.webrtc.data.remote.dto.IceServerDto
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.*
import kotlinx.coroutines.*

class WebRtcManager(
    private val context: Context,
    val eglBaseContext: EglBase.Context
) {
    private val TAG = "WebRtcManager"
    
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var audioDeviceModule: AudioDeviceModule? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var cameraTextureHelper: SurfaceTextureHelper? = null

    private var screenVideoSource: VideoSource? = null
    private var screenVideoTrack: VideoTrack? = null
    private var screenCapturer: VideoCapturer? = null
    private var screenTextureHelper: SurfaceTextureHelper? = null

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

        val adm = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(false)
            .setUseHardwareNoiseSuppressor(false)
            .setAudioSource(android.media.MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            .setAudioRecordErrorCallback(object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                override fun onWebRtcAudioRecordInitError(errorMessage: String?) {
                    Log.e(TAG, "AudioRecord init error: $errorMessage")
                }
                override fun onWebRtcAudioRecordStartError(errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode?, errorMessage: String?) {
                    Log.e(TAG, "AudioRecord start error: $errorCode, $errorMessage")
                }
                override fun onWebRtcAudioRecordError(errorMessage: String?) {
                    Log.e(TAG, "AudioRecord error: $errorMessage")
                }
            })
            .setAudioTrackErrorCallback(object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                override fun onWebRtcAudioTrackInitError(errorMessage: String?) {
                    Log.e(TAG, "AudioTrack init error: $errorMessage")
                }
                override fun onWebRtcAudioTrackStartError(errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode?, errorMessage: String?) {
                    Log.e(TAG, "AudioTrack start error: $errorCode, $errorMessage")
                }
                override fun onWebRtcAudioTrackError(errorMessage: String?) {
                    Log.e(TAG, "AudioTrack error: $errorMessage")
                }
            })
            .createAudioDeviceModule()
        audioDeviceModule = adm

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(factoryOptions)
            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()
    }

    fun createLocalStream(forceRecreate: Boolean = false) {
        if (!forceRecreate && localAudioTrack != null && localVideoTrack != null) {
            Log.d(TAG, "Local audio and video tracks already active, reusing existing stream")
            return
        }
        if (peerConnectionFactory == null) {
            initPeerConnectionFactory(context)
        }
        stopLocalStream()

        val hasAudioPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasAudioPermission && !hasCameraPermission) {
            Log.w(TAG, "Neither camera nor microphone permission granted yet")
            return
        }

        // 1. Audio creation - decoupled so camera failure never breaks microphone
        try {
            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            }
            localAudioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
            localAudioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", localAudioSource)?.apply {
                setEnabled(true)
                setVolume(1.0)
            }
            Log.d(TAG, "Local audio track created and enabled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating local audio track", e)
        }

        // 2. Video creation - with fallback resolution if 720p fails on budget devices
        try {
            localVideoSource = peerConnectionFactory?.createVideoSource(false)
            videoCapturer = createVideoCapturer(context)
            if (videoCapturer != null) {
                val textureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
                cameraTextureHelper = textureHelper
                videoCapturer?.initialize(textureHelper, context, localVideoSource?.capturerObserver)
                try {
                    videoCapturer?.startCapture(640, 480, 30)
                } catch (e: Exception) {
                    Log.w(TAG, "640x480 capture failed, falling back to 480x360: ${e.message}")
                    videoCapturer?.startCapture(480, 360, 24)
                }
                localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", localVideoSource)?.apply {
                    setEnabled(true)
                }
                Log.d(TAG, "Local video track created and enabled successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating local video stream", e)
        }
    }

    fun updateCaptureFormat(width: Int, height: Int, fps: Int) {
        try {
            videoCapturer?.changeCaptureFormat(width, height, fps)
            Log.d(TAG, "Dynamic capture format updated to ${width}x${height} @ ${fps}fps")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update capture format: ${e.message}")
        }
    }

    fun stopLocalStream() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capturer: ${e.message}")
        }
        cameraTextureHelper?.dispose()
        cameraTextureHelper = null
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
            val builder = PeerConnection.IceServer.builder(it.urls)
            if (!it.username.isNullOrBlank()) {
                builder.setUsername(it.username)
            }
            if (!it.credential.isNullOrBlank()) {
                builder.setPassword(it.credential)
            }
            builder.createIceServer()
        }

        val rtcConfig = PeerConnection.RTCConfiguration(rtcIceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
        }
        
        val pc = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        
        if (pc != null) {
            attachLocalTracks(pc)
        }
        
        return pc
    }

    fun attachLocalTracks(pc: PeerConnection) {
        val audio = localAudioTrack
        val video = localVideoTrack

        val transceivers = pc.transceivers
        val audioTransceiver = transceivers.firstOrNull { it.mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO }
        val videoTransceiver = transceivers.firstOrNull { it.mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO }

        if (audio != null) {
            if (audioTransceiver != null) {
                audioTransceiver.direction = RtpTransceiver.RtpTransceiverDirection.SEND_RECV
                if (audioTransceiver.sender.track() == null) {
                    audioTransceiver.sender.setTrack(audio, true)
                }
            } else {
                pc.addTrack(audio, listOf("ARDAMS"))
            }
        } else if (audioTransceiver == null) {
            pc.addTransceiver(
                MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
                RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_RECV)
            )
        }

        if (video != null) {
            if (videoTransceiver != null) {
                videoTransceiver.direction = RtpTransceiver.RtpTransceiverDirection.SEND_RECV
                if (videoTransceiver.sender.track() == null) {
                    videoTransceiver.sender.setTrack(video, true)
                }
            } else {
                pc.addTrack(video, listOf("ARDAMS"))
            }
        } else if (videoTransceiver == null) {
            pc.addTransceiver(
                MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
                RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_RECV)
            )
        }
    }

    fun getLocalAudioTrack(): AudioTrack? = localAudioTrack

    private fun createVideoCapturer(context: Context): VideoCapturer? {
        val enumerator: CameraEnumerator = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(true)
        }
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

            val textureHelper = SurfaceTextureHelper.create("ScreenCaptureThread", eglBaseContext)
            screenTextureHelper = textureHelper
            screenCapturer?.initialize(textureHelper, context, screenVideoSource?.capturerObserver)
            screenCapturer?.startCapture(width, height, 15)
            screenVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSs0", screenVideoSource)
            Log.d(TAG, "Screen capture started successfully ($width x $height)")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting screen capture: ${e.message}", e)
            stopScreenCapture()
        }
    }

    fun stopScreenCapture(immediate: Boolean = false) {
        try {
            screenVideoTrack?.setEnabled(false)
            screenCapturer?.stopCapture()
            screenCapturer?.dispose()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping screen capturer: ${e.message}")
        }
        screenCapturer = null
        screenTextureHelper?.dispose()
        screenTextureHelper = null

        val trackToDispose = screenVideoTrack
        val sourceToDispose = screenVideoSource
        screenVideoTrack = null
        screenVideoSource = null

        if (immediate) {
            try {
                trackToDispose?.dispose()
                sourceToDispose?.dispose()
            } catch (e: Exception) {
                Log.e(TAG, "Error disposing screen video track/source immediately: ${e.message}")
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    delay(500)
                    trackToDispose?.dispose()
                    sourceToDispose?.dispose()
                } catch (e: Exception) {
                    Log.e(TAG, "Error disposing screen video track/source asynchronously: ${e.message}")
                }
            }
        }
    }

    fun switchCamera() {
        val cameraCapturer = videoCapturer as? CameraVideoCapturer
        cameraCapturer?.switchCamera(null)
    }

    fun getLocalVideoTrack(): VideoTrack? = localVideoTrack
    fun getLocalScreenTrack(): VideoTrack? = screenVideoTrack

    fun dispose() {
        stopScreenCapture(immediate = true)
        stopLocalStream()
        try {
            peerConnectionFactory?.dispose()
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing factory: ${e.message}")
        }
        peerConnectionFactory = null
        try {
            audioDeviceModule?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audioDeviceModule: ${e.message}")
        }
        audioDeviceModule = null
    }
}
