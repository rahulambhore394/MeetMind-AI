package com.developer_rahul.meetmind_ai.feature.meetingroom.data.repository

import android.util.Log
import com.developer_rahul.meetmind_ai.core.network.token.TokenProvider
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import com.developer_rahul.meetmind_ai.core.webrtc.WebRtcManager
import com.developer_rahul.meetmind_ai.core.webrtc.data.remote.WebRtcApiService
import com.developer_rahul.meetmind_ai.feature.meetingroom.domain.model.MeetingCallEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.webrtc.*
import java.util.concurrent.ConcurrentHashMap

class MeetingCallRepository(
    private val webRtcApiService: WebRtcApiService?,
    private val webSocketManager: MeetingWebSocketManager,
    private val webRtcManager: WebRtcManager,
    private val tokenProvider: TokenProvider? = null
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val peerConnections = ConcurrentHashMap<Long, PeerConnection>()
    private val pendingIceCandidates = ConcurrentHashMap<Long, MutableList<IceCandidate>>()
    
    // Track remote screen tracks per peer
    private val remoteScreenTracks = ConcurrentHashMap<Long, VideoTrack>()

    // Perfect Negotiator state tracking maps for SDP glare resolution
    private val makingOfferMap = ConcurrentHashMap<Long, Boolean>()
    private val ignoreOfferMap = ConcurrentHashMap<Long, Boolean>()
    private val isInitiatorMap = ConcurrentHashMap<Long, Boolean>()
    private val initialHandshakeComplete = ConcurrentHashMap<Long, Boolean>()

    // Screen sharing RtpSender tracking map per peer
    private val screenSenders = ConcurrentHashMap<Long, RtpSender>()

    // SDP offer/answer deduplication caches
    private val lastHandledOfferSdp = ConcurrentHashMap<Long, String>()
    private val lastHandledAnswerSdp = ConcurrentHashMap<Long, String>()

    // ICE restart recovery attempt counter per peer
    private val iceRestartAttempts = ConcurrentHashMap<Long, Int>()
    private val maxIceRestartAttempts = 3

    private val _events = MutableSharedFlow<MeetingCallEvent>()
    val events: SharedFlow<MeetingCallEvent> = _events.asSharedFlow()

    private var currentMeetingId: Long? = null
    private var localUserId: Long = 0L
    private var iceServers: List<com.developer_rahul.meetmind_ai.core.webrtc.data.remote.dto.IceServerDto> = emptyList()
    private var statsJob: Job? = null

    // Flag indicating SFU media server mode vs P2P Mesh mode
    private var isSfuMode = false

    init {
        scope.launch {
            webSocketManager.signalingMessages.collect { message ->
                handleSignalingMessage(message)
            }
        }
        startStatsCollection()
    }

    private fun getCurrentUserId(): Long {
        return tokenProvider?.getUserId() ?: 0L
    }

    /**
     * Perfect Negotiator politeness rule:
     * Deterministically designates one peer as "polite" and the other as "impolite".
     * Here, a peer is polite relative to [peerId] if its user ID is less than [peerId].
     */
    private fun isPolite(peerId: Long): Boolean {
        val myUserId = if (localUserId > 0L) localUserId else getCurrentUserId()
        return if (myUserId > 0L && peerId > 0L) {
            myUserId < peerId
        } else {
            true
        }
    }

    /**
     * Triggers automatic ICE restart for [peerId] when connection breaks (e.g. Wi-Fi <-> 5G switch).
     * Generates a new SDP Offer with `IceRestart=true` to negotiate new ICE candidates.
     */
    private fun triggerIceRestart(peerId: Long, reason: String) {
        val meetingId = currentMeetingId ?: return
        val attempts = iceRestartAttempts.getOrDefault(peerId, 0)
        if (attempts >= maxIceRestartAttempts) {
            Log.w("MeetingCallRepo", "Max ICE restart attempts ($maxIceRestartAttempts) reached for peer $peerId. Aborting restart.")
            return
        }

        iceRestartAttempts[peerId] = attempts + 1
        Log.i("MeetingCallRepo", "Triggering ICE restart for peer $peerId (Attempt ${attempts + 1}/$maxIceRestartAttempts). Reason: $reason")

        val pc = peerConnections[peerId] ?: return

        try {
            pc.restartIce()
        } catch (e: Exception) {
            Log.d("MeetingCallRepo", "Native restartIce() call ignored/unsupported: ${e.message}")
        }

        webRtcManager.attachLocalTracks(pc)
        makingOfferMap[peerId] = true

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
        }

        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription) {
                pc.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        makingOfferMap[peerId] = false
                        Log.i("MeetingCallRepo", "ICE restart SDP offer created & set locally for peer $peerId. Dispatching via signaling.")
                        if (currentMeetingId == meetingId && peerConnections[peerId] === pc) {
                            webSocketManager.sendSignalingMessage(meetingId, "OFFER", description.description, peerId)
                        }
                    }
                    override fun onCreateFailure(s: String?) {
                        makingOfferMap[peerId] = false
                        Log.e("MeetingCallRepo", "Failed to set local description during ICE restart for peer $peerId: $s")
                    }
                    override fun onSetFailure(s: String?) {
                        makingOfferMap[peerId] = false
                        Log.e("MeetingCallRepo", "Failed to set local description during ICE restart for peer $peerId: $s")
                    }
                }, description)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(s: String) {
                makingOfferMap[peerId] = false
                Log.e("MeetingCallRepo", "Failed to create offer during ICE restart for peer $peerId: $s")
            }
            override fun onSetFailure(s: String) {
                makingOfferMap[peerId] = false
                Log.e("MeetingCallRepo", "Failed to set local offer during ICE restart for peer $peerId: $s")
            }
        }, constraints)
    }

    /**
     * Dynamically scales camera capture resolution, frame rate, and RtpSender maximum bitrate
     * based on total participant count (N).
     *
     * Prevents CPU overheating, battery drain, and network congestion in WebRTC Mesh calls:
     * - N <= 2: 720p @ 30fps (1.5 Mbps max)
     * - N = 3 or 4: 480p @ 24fps (500 Kbps max)
     * - N >= 5: 360p @ 15fps (180 Kbps max)
     */
    private fun applyAdaptiveScalabilityControls() {
        if (isSfuMode) return

        val activePeersCount = peerConnections.size
        val totalParticipants = activePeersCount + 1

        val (targetWidth, targetHeight, targetFps, maxBitrateBps, scaleDownFactor) = when {
            totalParticipants <= 2 -> {
                ScalabilityParams(1280, 720, 30, 1_500_000, 1.0)
            }
            totalParticipants in 3..4 -> {
                ScalabilityParams(640, 480, 24, 500_000, 1.5)
            }
            else -> {
                ScalabilityParams(480, 360, 15, 180_000, 2.0)
            }
        }

        Log.i(
            "MeetingCallRepo",
            "Adaptive Scalability: totalParticipants=$totalParticipants, capture=${targetWidth}x${targetHeight}@${targetFps}fps, maxBitrate=$maxBitrateBps bps"
        )

        webRtcManager.updateCaptureFormat(targetWidth, targetHeight, targetFps)

        for ((peerId, pc) in peerConnections) {
            val senders = pc.senders
            for (sender in senders) {
                val track = sender.track()
                if (track is VideoTrack && !track.id().contains("SCREEN")) {
                    val parameters = sender.parameters
                    var updated = false
                    for (encoding in parameters.encodings) {
                        encoding.maxBitrateBps = maxBitrateBps
                        encoding.maxFramerate = targetFps
                        encoding.scaleResolutionDownBy = scaleDownFactor
                        updated = true
                    }
                    if (updated) {
                        val success = sender.setParameters(parameters)
                        Log.d("MeetingCallRepo", "Applied RtpSender parameters for peer $peerId: success=$success, bitrate=$maxBitrateBps bps")
                    }
                }
            }
        }
    }

    private data class ScalabilityParams(
        val width: Int,
        val height: Int,
        val fps: Int,
        val maxBitrateBps: Int,
        val scaleDownFactor: Double
    )

    private fun startStatsCollection() {
        statsJob?.cancel()
        statsJob = scope.launch {
            while (isActive) {
                delay(1200)
                for (peerId in peerConnections.keys()) {
                    val pc = peerConnections[peerId] ?: continue
                    pc.getStats { report ->
                        for (stats in report.statsMap.values) {
                            if (stats.type == "inbound-rtp" || stats.type == "track") {
                                val audioLevelObj = stats.members["audioLevel"] ?: stats.members["audioOutputLevel"]
                                val audioLevel = when (audioLevelObj) {
                                    is Double -> audioLevelObj.toFloat()
                                    is Float -> audioLevelObj
                                    is Number -> audioLevelObj.toFloat()
                                    else -> 0f
                                }
                                val isSpeakingNow = audioLevel > 0.015f
                                scope.launch {
                                    _events.emit(MeetingCallEvent.MediaStateChanged(
                                        userId = peerId,
                                        isSpeaking = isSpeakingNow
                                    ))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fallbackIceServers(): List<com.developer_rahul.meetmind_ai.core.webrtc.data.remote.dto.IceServerDto> {
        return listOf(
            com.developer_rahul.meetmind_ai.core.webrtc.data.remote.dto.IceServerDto(urls = listOf("stun:stun.l.google.com:19302")),
            com.developer_rahul.meetmind_ai.core.webrtc.data.remote.dto.IceServerDto(urls = listOf("stun:stun1.l.google.com:19302")),
            com.developer_rahul.meetmind_ai.core.webrtc.data.remote.dto.IceServerDto(urls = listOf("stun:stun2.l.google.com:19302"))
        )
    }

    suspend fun joinMeeting(meetingId: Long) {
        if (meetingId <= 0L) {
            throw IllegalArgumentException("Invalid meeting ID")
        }
        if (currentMeetingId != null || peerConnections.isNotEmpty()) {
            leaveMeeting()
        }
        currentMeetingId = meetingId
        localUserId = getCurrentUserId()
        isSfuMode = false
        webSocketManager.connect()
        
        iceServers = try {
            val fetched = webRtcApiService?.getIceServers() ?: emptyList()
            if (fetched.isEmpty()) fallbackIceServers() else fetched
        } catch (e: Exception) {
            fallbackIceServers()
        }
        
        webRtcManager.createLocalStream()
        
        webSocketManager.awaitConnection()
        webSocketManager.subscribeToSignaling(meetingId)
        
        _events.emit(MeetingCallEvent.LocalStreamReady(webRtcManager.getLocalVideoTrack()))
    }

    private fun drainPendingIceCandidates(peerId: Long, pc: PeerConnection) {
        val candidates = pendingIceCandidates.remove(peerId) ?: return
        Log.d("MeetingCallRepo", "Draining ${candidates.size} queued ICE candidates for peer $peerId")
        for (candidate in candidates) {
            pc.addIceCandidate(candidate)
        }
    }

    private fun handleSignalingMessage(message: com.developer_rahul.meetmind_ai.core.network.websocket.model.SignalingMessageDto) {
        Log.d(
            "MeetingCallRepo",
            "Handling signaling type=${message.type}, meetingId=${message.meetingId}, " +
                "senderId=${message.senderId}, receiverId=${message.receiverId}"
        )
        if (message.type == "SFU_MODE") {
            Log.i("MeetingCallRepo", "Signaling indicated SFU Mode available - enabling SFU single-publisher connection model")
            isSfuMode = true
            return
        }

        if (message.type == "PEER_LIST") {
            val payload = message.payload
            if (!payload.isNullOrEmpty()) {
                val peerIds = payload.split(",").mapNotNull { 
                    it.split(":").firstOrNull()?.toLongOrNull() 
                }
                for (id in peerIds) {
                    if (id > 0L && id != localUserId && !peerConnections.containsKey(id)) {
                        Log.d("MeetingCallRepo", "PEER_LIST: Preparing peer connection for $id")
                        createPeerConnection(id, isInitiator = false)
                    }
                }
            }
            return
        }

        val peerId = message.senderId ?: return
        if (peerId <= 0L || peerId == localUserId) return 

        val myUserId = if (localUserId > 0L) localUserId else getCurrentUserId()
        val targetReceiver = message.receiverId
        if (targetReceiver != null && targetReceiver > 0L && targetReceiver != myUserId) {
            return
        }

        when (message.type) {
            "JOIN" -> {
                Log.d("MeetingCallRepo", "Peer $peerId joined meeting, creating offer as initiator")
                if (!peerConnections.containsKey(peerId)) {
                    createPeerConnection(peerId, isInitiator = true)
                }
            }
            "OFFER" -> {
                handleOffer(peerId, message.payload ?: "")
            }
            "ANSWER" -> {
                handleAnswer(peerId, message.payload ?: "")
            }
            "ICE_CANDIDATE" -> {
                handleIceCandidate(peerId, message.payload ?: "")
            }
            "MEDIA_UPDATE" -> {
                handleMediaUpdate(peerId, message.payload ?: "")
            }
            "SCREEN_SHARE_STOPPED" -> {
                Log.d("MeetingCallRepo", "Peer $peerId stopped screen sharing via signaling")
                remoteScreenTracks.remove(peerId)
                scope.launch {
                    _events.emit(MeetingCallEvent.ScreenShareStopped(peerId))
                }
            }
            "LEAVE" -> {
                removePeerConnection(peerId)
            }
        }
    }

    private fun handleMediaUpdate(peerId: Long, payload: String) {
        val updates = payload.split(",").associate {
            val parts = it.split(":")
            val key = parts[0]
            val value = parts.getOrNull(1)?.toBoolean() ?: false
            key to value
        }
        scope.launch {
            _events.emit(MeetingCallEvent.MediaStateChanged(
                userId = peerId,
                audioEnabled = updates["audioEnabled"],
                videoEnabled = updates["videoEnabled"],
                isSpeaking = updates["isSpeaking"]
            ))
            if (updates.containsKey("isScreenSharing")) {
                val isSharing = updates["isScreenSharing"] ?: false
                if (!isSharing) {
                    Log.d("MeetingCallRepo", "MediaUpdate: Remote peer $peerId stopped screen sharing")
                    remoteScreenTracks.remove(peerId)
                    _events.emit(MeetingCallEvent.ScreenShareStopped(peerId))
                }
            }
        }
    }

    private fun createPeerConnection(peerId: Long, isInitiator: Boolean): PeerConnection? {
        val meetingId = currentMeetingId ?: return null
        val existing = peerConnections[peerId]
        if (existing != null) {
            if (isInitiator) {
                createOffer(peerId)
            }
            return existing
        }

        isInitiatorMap[peerId] = isInitiator
        initialHandshakeComplete[peerId] = false

        val pc = webRtcManager.createPeerConnection(iceServers, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                val mid = candidate.sdpMid ?: ""
                val payload = "$mid|${candidate.sdpMLineIndex}|${candidate.sdp}"
                if (currentMeetingId == meetingId && peerConnections[peerId] != null) {
                    webSocketManager.sendSignalingMessage(meetingId, "ICE_CANDIDATE", payload, peerId)
                }
            }

            override fun onTrack(transceiver: RtpTransceiver) {
                val track = transceiver.receiver.track()
                if (track is AudioTrack) {
                    track.setEnabled(true)
                    track.setVolume(1.0)
                    Log.d("MeetingCallRepo", "onTrack: remote audio track received & enabled for peer $peerId (id=${track.id()})")
                } else if (track is VideoTrack) {
                    track.setEnabled(true)
                    val trackId = track.id() ?: ""
                    Log.d("MeetingCallRepo", "onTrack: remote video track received & enabled for peer $peerId, trackId=$trackId")
                    if (trackId.contains("SCREEN") || trackId.contains("ARDAMSs")) {
                        remoteScreenTracks[peerId] = track
                        scope.launch {
                            _events.emit(MeetingCallEvent.ScreenShareStarted(peerId, track))
                        }
                    } else {
                        scope.launch {
                            _events.emit(MeetingCallEvent.RemoteStreamReady(peerId, track))
                        }
                    }
                }
            }

            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {
                val track = receiver.track()
                if (track is AudioTrack) {
                    track.setEnabled(true)
                    track.setVolume(1.0)
                    Log.d("MeetingCallRepo", "onAddTrack: remote audio track received & enabled for peer $peerId (id=${track.id()})")
                } else if (track is VideoTrack) {
                    track.setEnabled(true)
                    val trackId = track.id() ?: ""
                    val streamId = mediaStreams.firstOrNull()?.id ?: ""
                    Log.d("MeetingCallRepo", "onAddTrack: remote video track received & enabled for peer $peerId, trackId=$trackId, streamId=$streamId")
                    if (trackId.contains("SCREEN") || trackId.contains("ARDAMSs") || streamId.contains("SCREEN")) {
                        remoteScreenTracks[peerId] = track
                        scope.launch {
                            _events.emit(MeetingCallEvent.ScreenShareStarted(peerId, track))
                        }
                    } else {
                        scope.launch {
                            _events.emit(MeetingCallEvent.RemoteStreamReady(peerId, track))
                        }
                    }
                }
            }

            override fun onAddStream(stream: MediaStream) {
                Log.d("MeetingCallRepo", "onAddStream: remote stream received for peer $peerId, videoTracks=${stream.videoTracks.size}, audioTracks=${stream.audioTracks.size}")
                for (track in stream.audioTracks) {
                    track.setEnabled(true)
                    track.setVolume(1.0)
                }
                for (track in stream.videoTracks) {
                    track.setEnabled(true)
                    val trackId = track.id() ?: ""
                    val streamId = stream.id ?: ""
                    if (trackId.contains("SCREEN") || trackId.contains("ARDAMSs") || streamId.contains("SCREEN")) {
                        remoteScreenTracks[peerId] = track
                        scope.launch {
                            _events.emit(MeetingCallEvent.ScreenShareStarted(peerId, track))
                        }
                    } else {
                        scope.launch {
                            _events.emit(MeetingCallEvent.RemoteStreamReady(peerId, track))
                        }
                    }
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                Log.d("MeetingCallRepo", "Peer $peerId ConnectionState: $newState")
                scope.launch {
                    _events.emit(MeetingCallEvent.ConnectionStateChanged(peerId, newState.name))
                }
                if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    initialHandshakeComplete[peerId] = true
                } else if (newState == PeerConnection.PeerConnectionState.FAILED || 
                    newState == PeerConnection.PeerConnectionState.DISCONNECTED) {
                    triggerIceRestart(peerId, "ConnectionState: $newState")
                }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                Log.d("MeetingCallRepo", "Peer $peerId IceConnectionState: $state")
                if (state == PeerConnection.IceConnectionState.FAILED || 
                    state == PeerConnection.IceConnectionState.DISCONNECTED) {
                    triggerIceRestart(peerId, "IceConnectionState: $state")
                } else if (state == PeerConnection.IceConnectionState.CONNECTED || 
                           state == PeerConnection.IceConnectionState.COMPLETED) {
                    initialHandshakeComplete[peerId] = true
                    iceRestartAttempts.remove(peerId)
                }
            }

            override fun onRenegotiationNeeded() {
                val wasInitiator = isInitiatorMap[peerId] ?: isInitiator
                val isHandshakeDone = initialHandshakeComplete[peerId] == true
                if (wasInitiator || isHandshakeDone) {
                    createOffer(peerId)
                }
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onRemoveStream(stream: MediaStream) {
                 remoteScreenTracks.remove(peerId)
                 scope.launch { 
                    _events.emit(MeetingCallEvent.RemoteStreamRemoved(peerId))
                    _events.emit(MeetingCallEvent.ScreenShareStopped(peerId))
                 }
            }
            override fun onDataChannel(dataChannel: DataChannel) {}
        })

        if (pc != null) {
            peerConnections[peerId] = pc
            webRtcManager.getLocalScreenTrack()?.let { screenTrack ->
                val sender = pc.addTrack(screenTrack, listOf("ARDAMS_SCREEN"))
                if (sender != null) {
                    screenSenders[peerId] = sender
                }
            }
            
            applyAdaptiveScalabilityControls()

            if (isInitiator) {
                createOffer(peerId)
            }
        }
        return pc
    }

    private fun createOffer(peerId: Long) {
        val meetingId = currentMeetingId ?: return
        val pc = peerConnections[peerId] ?: return
        webRtcManager.attachLocalTracks(pc)
        makingOfferMap[peerId] = true

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription) {
                pc.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        makingOfferMap[peerId] = false
                        Log.d("MeetingCallRepo", "Offer setLocalDescription success for peer $peerId")
                        if (currentMeetingId == meetingId && peerConnections[peerId] === pc) {
                            webSocketManager.sendSignalingMessage(meetingId, "OFFER", description.description, peerId)
                        }
                    }
                    override fun onCreateFailure(s: String?) {
                        makingOfferMap[peerId] = false
                        Log.e("MeetingCallRepo", "Failed to set local description for offer $peerId: $s")
                    }
                    override fun onSetFailure(s: String?) {
                        makingOfferMap[peerId] = false
                        Log.e("MeetingCallRepo", "Failed to set local description for offer $peerId: $s")
                    }
                }, description)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(s: String) {
                makingOfferMap[peerId] = false
                Log.e("MeetingCallRepo", "Failed to create offer for $peerId: $s")
            }
            override fun onSetFailure(s: String) {
                makingOfferMap[peerId] = false
                Log.e("MeetingCallRepo", "Failed to set local offer for $peerId: $s")
            }
        }, constraints)
    }

    private fun handleOffer(peerId: Long, sdp: String) {
        if (currentMeetingId == null) return
        if (lastHandledOfferSdp[peerId] == sdp) {
            Log.d("MeetingCallRepo", "Duplicate offer from peer $peerId, ignoring")
            return
        }
        val pc = peerConnections[peerId] ?: createPeerConnection(peerId, false) ?: return

        val isMakingOffer = makingOfferMap[peerId] == true
        val isStable = pc.signalingState() == PeerConnection.SignalingState.STABLE
        val isOfferCollision = isMakingOffer || !isStable
        val polite = isPolite(peerId)

        ignoreOfferMap[peerId] = !polite && isOfferCollision

        if (ignoreOfferMap[peerId] == true) {
            Log.w("MeetingCallRepo", "SDP Glare detected with peer $peerId: Impolite peer ignoring incoming offer")
            return
        }

        lastHandledOfferSdp[peerId] = sdp
        Log.d("MeetingCallRepo", "Handling offer from peer $peerId (collision=$isOfferCollision, polite=$polite)")

        if (isOfferCollision && polite) {
            Log.i("MeetingCallRepo", "SDP Glare detected with peer $peerId: Polite peer rolling back local offer")
            pc.setLocalDescription(object : SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onSetSuccess() {
                    applyRemoteOfferAndAnswer(peerId, pc, sdp)
                }
                override fun onCreateFailure(p0: String?) {
                    applyRemoteOfferAndAnswer(peerId, pc, sdp)
                }
                override fun onSetFailure(p0: String?) {
                    applyRemoteOfferAndAnswer(peerId, pc, sdp)
                }
            }, SessionDescription(SessionDescription.Type.ROLLBACK, ""))
        } else {
            applyRemoteOfferAndAnswer(peerId, pc, sdp)
        }
    }

    private fun applyRemoteOfferAndAnswer(peerId: Long, pc: PeerConnection, sdp: String) {
        val meetingId = currentMeetingId ?: return
        webRtcManager.attachLocalTracks(pc)
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        pc.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d("MeetingCallRepo", "Remote offer set successfully for peer $peerId, draining ICE and creating answer")
                drainPendingIceCandidates(peerId, pc)
                webRtcManager.attachLocalTracks(pc)
                pc.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(description: SessionDescription) {
                        pc.setLocalDescription(object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                Log.d("MeetingCallRepo", "Local answer set successfully for peer $peerId")
                                if (currentMeetingId == meetingId && peerConnections[peerId] === pc) {
                                    webSocketManager.sendSignalingMessage(meetingId, "ANSWER", description.description, peerId)
                                }
                            }
                            override fun onCreateFailure(s: String?) {
                                Log.e("MeetingCallRepo", "Failed to set local answer description for $peerId: $s")
                            }
                            override fun onSetFailure(s: String?) {
                                Log.e("MeetingCallRepo", "Failed to set local answer description for $peerId: $s")
                            }
                        }, description)
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(s: String) {
                        Log.e("MeetingCallRepo", "Failed to create answer for $peerId: $s")
                    }
                    override fun onSetFailure(s: String) {
                        Log.e("MeetingCallRepo", "Failed to set local answer for $peerId: $s")
                    }
                }, constraints)
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {
                Log.e("MeetingCallRepo", "Failed to set remote offer for $peerId: $p0")
            }
        }, SessionDescription(SessionDescription.Type.OFFER, sdp))
    }

    private fun handleAnswer(peerId: Long, sdp: String) {
        if (currentMeetingId == null) return
        if (lastHandledAnswerSdp[peerId] == sdp) {
            Log.d("MeetingCallRepo", "Duplicate answer from peer $peerId, ignoring")
            return
        }
        if (ignoreOfferMap[peerId] == true) {
            Log.d("MeetingCallRepo", "Ignoring answer from peer $peerId because we ignored its offer during glare")
            ignoreOfferMap[peerId] = false
            return
        }

        val pc = peerConnections[peerId] ?: return
        if (pc.signalingState() != PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
            Log.w("MeetingCallRepo", "Received answer from peer $peerId but signaling state is ${pc.signalingState()}, ignoring")
            return
        }

        lastHandledAnswerSdp[peerId] = sdp

        pc.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d("MeetingCallRepo", "Remote answer set successfully for peer $peerId, draining ICE")
                drainPendingIceCandidates(peerId, pc)
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {
                Log.e("MeetingCallRepo", "Failed to set remote answer for $peerId: $p0")
            }
        }, SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    private fun handleIceCandidate(peerId: Long, payload: String) {
        val parts = payload.split("|")
        if (parts.size < 3) {
            Log.w("MeetingCallRepo", "Ignoring malformed ICE candidate from peer $peerId")
            return
        }
        val mLineIndex = parts[1].toIntOrNull()
        if (mLineIndex == null) {
            Log.w("MeetingCallRepo", "Ignoring ICE candidate with invalid m-line index from peer $peerId")
            return
        }
        val candidate = IceCandidate(parts[0], mLineIndex, parts.drop(2).joinToString("|"))
        val pc = peerConnections[peerId]
        if (pc != null && pc.remoteDescription != null) {
            pc.addIceCandidate(candidate)
        } else {
            pendingIceCandidates.computeIfAbsent(peerId) { java.util.Collections.synchronizedList(mutableListOf()) }.add(candidate)
        }
    }

    private fun removePeerConnection(peerId: Long) {
        iceRestartAttempts.remove(peerId)
        screenSenders.remove(peerId)
        makingOfferMap.remove(peerId)
        ignoreOfferMap.remove(peerId)
        isInitiatorMap.remove(peerId)
        initialHandshakeComplete.remove(peerId)
        remoteScreenTracks.remove(peerId)
        pendingIceCandidates.remove(peerId)
        peerConnections.remove(peerId)?.dispose()
        
        applyAdaptiveScalabilityControls()

        scope.launch {
            _events.emit(MeetingCallEvent.RemoteStreamRemoved(peerId))
            _events.emit(MeetingCallEvent.ScreenShareStopped(peerId))
        }
    }

    fun leaveMeeting() {
        val meetingId = currentMeetingId
        if (meetingId != null) {
            webSocketManager.unsubscribeFromSignaling(meetingId)
        }
        iceRestartAttempts.clear()
        lastHandledOfferSdp.clear()
        lastHandledAnswerSdp.clear()
        screenSenders.clear()
        makingOfferMap.clear()
        ignoreOfferMap.clear()
        isInitiatorMap.clear()
        initialHandshakeComplete.clear()
        remoteScreenTracks.clear()
        pendingIceCandidates.clear()
        for (pc in peerConnections.values) {
            pc.dispose()
        }
        peerConnections.clear()
        webRtcManager.dispose()
        currentMeetingId = null
    }

    fun setAudioEnabled(enabled: Boolean) {
        webRtcManager.setAudioEnabled(enabled)
        sendMediaUpdate(audioEnabled = enabled)
    }

    fun setVideoEnabled(enabled: Boolean) {
        webRtcManager.setVideoEnabled(enabled)
        sendMediaUpdate(videoEnabled = enabled)
    }

    fun switchCamera() {
        webRtcManager.switchCamera()
    }

    fun startScreenSharing(mediaProjectionData: android.content.Intent) {
        webRtcManager.startScreenCapture(mediaProjectionData)
        val screenTrack = webRtcManager.getLocalScreenTrack()
        
        scope.launch {
            _events.emit(MeetingCallEvent.LocalScreenStreamReady(screenTrack))
        }

        if (screenTrack != null) {
            for ((peerId, pc) in peerConnections) {
                val sender = pc.addTrack(screenTrack, listOf("ARDAMS_SCREEN"))
                if (sender != null) {
                    screenSenders[peerId] = sender
                }
                createOffer(peerId)
            }
            sendMediaUpdate(isScreenSharing = true)
        }
    }

    fun stopScreenSharing() {
        for ((peerId, pc) in peerConnections) {
            val sender = screenSenders.remove(peerId) ?: pc.senders.find { s ->
                val trackId = s.track()?.id() ?: ""
                trackId.contains("SCREEN") || trackId.contains("ARDAMSs")
            }
            if (sender != null) {
                pc.removeTrack(sender)
            }
            createOffer(peerId)
        }

        webRtcManager.stopScreenCapture()
        sendMediaUpdate(isScreenSharing = false)
        webSocketManager.sendSignalingMessage(currentMeetingId ?: 0, "SCREEN_SHARE_STOPPED")

        scope.launch {
            _events.emit(MeetingCallEvent.LocalScreenStreamReady(null))
        }
    }

    fun getLocalScreenTrack(): VideoTrack? = webRtcManager.getLocalScreenTrack()

    private fun sendMediaUpdate(
        audioEnabled: Boolean? = null,
        videoEnabled: Boolean? = null,
        isSpeaking: Boolean? = null,
        isScreenSharing: Boolean? = null
    ) {
        val meetingId = currentMeetingId ?: return
        val payload = buildString {
            if (audioEnabled != null) append("audioEnabled:$audioEnabled,")
            if (videoEnabled != null) append("videoEnabled:$videoEnabled,")
            if (isSpeaking != null) append("isSpeaking:$isSpeaking,")
            if (isScreenSharing != null) append("isScreenSharing:$isScreenSharing,")
        }.removeSuffix(",")
        
        if (payload.isNotEmpty()) {
            webSocketManager.sendSignalingMessage(meetingId, "MEDIA_UPDATE", payload)
        }
    }
}
