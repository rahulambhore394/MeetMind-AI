package com.developer_rahul.meetmind_ai.feature.meetingroom.data.repository

import android.util.Log
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
    private val webRtcManager: WebRtcManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val peerConnections = ConcurrentHashMap<Long, PeerConnection>()
    
    private val _events = MutableSharedFlow<MeetingCallEvent>()
    val events: SharedFlow<MeetingCallEvent> = _events.asSharedFlow()

    private var currentMeetingId: Long? = null
    private var iceServers: List<com.developer_rahul.meetmind_ai.core.webrtc.data.remote.dto.IceServerDto> = emptyList()
    private var statsJob: Job? = null

    init {
        scope.launch {
            webSocketManager.signalingMessages.collect { message ->
                handleSignalingMessage(message)
            }
        }
        startStatsCollection()
    }

    private fun startStatsCollection() {
        statsJob?.cancel()
        statsJob = scope.launch {
            while (isActive) {
                delay(3000)
                for (id in peerConnections.keys()) {
                    val pc = peerConnections[id]
                    if (pc != null && pc.iceConnectionState() == PeerConnection.IceConnectionState.CONNECTED) {
                        // Connection is good
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
        currentMeetingId = meetingId
        webSocketManager.connect()
        
        iceServers = try {
            val fetched = webRtcApiService?.getIceServers() ?: emptyList()
            if (fetched.isEmpty()) fallbackIceServers() else fetched
        } catch (e: Exception) {
            fallbackIceServers()
        }
        
        webRtcManager.createLocalStream()
        webSocketManager.subscribeToSignaling(meetingId)
        
        _events.emit(MeetingCallEvent.LocalStreamReady(webRtcManager.getLocalVideoTrack()))
    }

    private fun handleSignalingMessage(message: com.developer_rahul.meetmind_ai.core.network.websocket.model.SignalingMessageDto) {
        if (message.type == "PEER_LIST") {
            val payload = message.payload
            if (!payload.isNullOrEmpty()) {
                val peerIds = payload.split(",").mapNotNull { 
                    it.split(":").firstOrNull()?.toLongOrNull() 
                }
                for (id in peerIds) {
                    if (id > 0L && !peerConnections.containsKey(id)) {
                        Log.d("MeetingCallRepo", "PEER_LIST: Creating offer connection to peer $id")
                        createPeerConnection(id, true)
                    }
                }
            }
            return
        }

        val peerId = message.senderId ?: return
        if (peerId <= 0L) return 

        when (message.type) {
            "JOIN" -> {
                Log.d("MeetingCallRepo", "Peer $peerId joined meeting")
                if (!peerConnections.containsKey(peerId)) {
                    createPeerConnection(peerId, true)
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
        }
    }

    private fun createPeerConnection(peerId: Long, isInitiator: Boolean): PeerConnection? {
        val pc = webRtcManager.createPeerConnection(iceServers, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                val payload = "${candidate.sdpMid}|${candidate.sdpMLineIndex}|${candidate.sdp}"
                webSocketManager.sendSignalingMessage(currentMeetingId ?: 0, "ICE_CANDIDATE", payload, peerId)
            }

            override fun onTrack(transceiver: RtpTransceiver) {
                val track = transceiver.receiver.track()
                if (track is VideoTrack) {
                    scope.launch {
                        _events.emit(MeetingCallEvent.RemoteStreamReady(peerId, track))
                    }
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                Log.d("MeetingCallRepo", "Peer $peerId ConnectionState: $newState")
                scope.launch {
                    _events.emit(MeetingCallEvent.ConnectionStateChanged(peerId, newState.name))
                }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                Log.d("MeetingCallRepo", "Peer $peerId IceConnectionState: $state")
            }

            override fun onRenegotiationNeeded() {
                if (isInitiator) {
                    createOffer(peerId)
                }
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onRemoveStream(stream: MediaStream) {
                 scope.launch { 
                    _events.emit(MeetingCallEvent.RemoteStreamRemoved(peerId)) 
                 }
            }
            override fun onDataChannel(dataChannel: DataChannel) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {}
        })

        if (pc != null) {
            peerConnections[peerId] = pc
            webRtcManager.getLocalScreenTrack()?.let { 
                pc.addTrack(it, listOf("ARDAMS_SCREEN")) 
            }
            
            if (isInitiator) {
                createOffer(peerId)
            }
        }
        return pc
    }

    private fun createOffer(peerId: Long) {
        val pc = peerConnections[peerId] ?: return
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription) {
                pc.setLocalDescription(this, description)
                webSocketManager.sendSignalingMessage(currentMeetingId ?: 0, "OFFER", description.description, peerId)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(s: String) {
                Log.e("MeetingCallRepo", "Failed to create offer for $peerId: $s")
            }
            override fun onSetFailure(s: String) {}
        }, MediaConstraints())
    }

    private fun handleOffer(peerId: Long, sdp: String) {
        val pc = createPeerConnection(peerId, false) ?: return
        pc.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                pc.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(description: SessionDescription) {
                        pc.setLocalDescription(this, description)
                        webSocketManager.sendSignalingMessage(currentMeetingId ?: 0, "ANSWER", description.description, peerId)
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(s: String) {}
                    override fun onSetFailure(s: String) {}
                }, MediaConstraints())
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, SessionDescription(SessionDescription.Type.OFFER, sdp))
    }

    private fun handleAnswer(peerId: Long, sdp: String) {
        val pc = peerConnections[peerId] ?: return
        pc.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    private fun handleIceCandidate(peerId: Long, payload: String) {
        val parts = payload.split("|")
        if (parts.size < 3) return
        val candidate = IceCandidate(parts[0], parts[1].toInt(), parts[2])
        peerConnections[peerId]?.addIceCandidate(candidate)
    }

    private fun removePeerConnection(peerId: Long) {
        peerConnections.remove(peerId)?.dispose()
        scope.launch {
            _events.emit(MeetingCallEvent.RemoteStreamRemoved(peerId))
        }
    }

    fun leaveMeeting() {
        webSocketManager.unsubscribeFromSignaling(currentMeetingId ?: 0)
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
            for (pc in peerConnections.values) {
                pc.addTrack(screenTrack, listOf("ARDAMS_SCREEN"))
            }
        }
    }

    fun stopScreenSharing() {
        webRtcManager.stopScreenCapture()
        scope.launch {
            _events.emit(MeetingCallEvent.LocalScreenStreamReady(null))
        }
        for (pc in peerConnections.values) {
            val senders = pc.senders
            val screenSender = senders.find { sender ->
                sender.track()?.id()?.contains("SCREEN") == true || sender.track()?.id()?.contains("ARDAMSs") == true
            }
            if (screenSender != null) {
                pc.removeTrack(screenSender)
            }
        }
    }

    fun getLocalScreenTrack(): VideoTrack? = webRtcManager.getLocalScreenTrack()

    private fun sendMediaUpdate(
        audioEnabled: Boolean? = null,
        videoEnabled: Boolean? = null,
        isSpeaking: Boolean? = null
    ) {
        val meetingId = currentMeetingId ?: return
        val payload = buildString {
            if (audioEnabled != null) append("audioEnabled:$audioEnabled,")
            if (videoEnabled != null) append("videoEnabled:$videoEnabled,")
            if (isSpeaking != null) append("isSpeaking:$isSpeaking,")
        }.removeSuffix(",")
        
        if (payload.isNotEmpty()) {
            webSocketManager.sendSignalingMessage(meetingId, "MEDIA_UPDATE", payload)
        }
    }
}
