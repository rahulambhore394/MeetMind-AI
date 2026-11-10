package com.developer_rahul.meetmind_ai.core.webrtc.sfu

import android.content.Context
import android.content.Intent
import android.util.Log
import io.livekit.android.ConnectOptions
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class LiveKitMediaManager(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var room: Room? = null

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private val _remoteVideoTracks = MutableStateFlow<Map<Long, VideoTrack>>(emptyMap())
    val remoteVideoTracks: StateFlow<Map<Long, VideoTrack>> = _remoteVideoTracks.asStateFlow()

    private val _activeSpeakers = MutableStateFlow<Set<Long>>(emptySet())
    val activeSpeakers: StateFlow<Set<Long>> = _activeSpeakers.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    suspend fun connect(
        url: String,
        token: String,
        enableAudio: Boolean = true,
        enableVideo: Boolean = true
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                disconnect()

                Log.d("LiveKitManager", "Connecting to LiveKit SFU: $url")
                val newRoom = LiveKit.create(
                    appContext = context.applicationContext,
                    options = RoomOptions(adaptiveStream = true, dynacast = true)
                )
                room = newRoom

                // Collect room events using onEach + launchIn on SharedFlow
                newRoom.events.events.onEach { event ->
                    handleRoomEvent(event)
                }.launchIn(scope)

                kotlinx.coroutines.withTimeout(3000) {
                    newRoom.connect(
                        url = url,
                        token = token,
                        options = ConnectOptions(autoSubscribe = true)
                    )
                }

                // Publish local tracks
                if (enableAudio) {
                    newRoom.localParticipant.setMicrophoneEnabled(true)
                }
                if (enableVideo) {
                    newRoom.localParticipant.setCameraEnabled(true)
                    val camPub = newRoom.localParticipant.getTrackPublication(Track.Source.CAMERA)
                    val localTrack = camPub?.track as? VideoTrack
                    _localVideoTrack.value = localTrack
                }

                _isConnected.value = true
                Log.i("LiveKitManager", "Connected successfully to LiveKit SFU room: ${newRoom.name}")
                true
            } catch (e: Exception) {
                Log.w("LiveKitManager", "LiveKit SFU not reachable (${e.message}), cleanly falling back to WebRTC mesh")
                disconnect()
                false
            }
        }
    }

    private fun handleRoomEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.TrackSubscribed -> {
                val participant = event.participant
                val track = event.track
                val userId = participant.identity?.value?.toLongOrNull() ?: participant.identity?.hashCode()?.toLong() ?: 0L

                if (track is VideoTrack) {
                    Log.d("LiveKitManager", "Remote video track subscribed for user $userId (source: ${event.publication.source})")
                    val current = _remoteVideoTracks.value.toMutableMap()
                    current[userId] = track
                    _remoteVideoTracks.value = current
                }
            }

            is RoomEvent.TrackUnsubscribed -> {
                val participant = event.participant
                val track = event.track
                val userId = participant.identity?.value?.toLongOrNull() ?: participant.identity?.hashCode()?.toLong() ?: 0L

                if (track is VideoTrack) {
                    Log.d("LiveKitManager", "Remote video track unsubscribed for user $userId")
                    val current = _remoteVideoTracks.value.toMutableMap()
                    current.remove(userId)
                    _remoteVideoTracks.value = current
                }
            }

            is RoomEvent.ActiveSpeakersChanged -> {
                val speakerIds = event.speakers.mapNotNull {
                    it.identity?.value?.toLongOrNull() ?: it.identity?.hashCode()?.toLong()
                }.toSet()
                _activeSpeakers.value = speakerIds
            }

            is RoomEvent.Disconnected -> {
                Log.w("LiveKitManager", "Disconnected from LiveKit SFU: ${event.reason}")
                _isConnected.value = false
                _remoteVideoTracks.value = emptyMap()
                _localVideoTrack.value = null
            }

            is RoomEvent.Reconnecting -> {
                Log.i("LiveKitManager", "Reconnecting to LiveKit SFU...")
            }

            is RoomEvent.Reconnected -> {
                Log.i("LiveKitManager", "Reconnected to LiveKit SFU!")
                _isConnected.value = true
            }

            else -> {
                // TrackMuted, TrackUnmuted, etc.
            }
        }
    }

    suspend fun setMicrophoneEnabled(enabled: Boolean) {
        try {
            room?.localParticipant?.setMicrophoneEnabled(enabled)
            Log.d("LiveKitManager", "Set microphone enabled: $enabled")
        } catch (e: Exception) {
            Log.e("LiveKitManager", "Failed to set microphone enabled: ${e.message}")
        }
    }

    suspend fun setCameraEnabled(enabled: Boolean) {
        try {
            room?.localParticipant?.setCameraEnabled(enabled)
            val camPub = room?.localParticipant?.getTrackPublication(Track.Source.CAMERA)
            _localVideoTrack.value = if (enabled) camPub?.track as? VideoTrack else null
            Log.d("LiveKitManager", "Set camera enabled: $enabled")
        } catch (e: Exception) {
            Log.e("LiveKitManager", "Failed to set camera enabled: ${e.message}")
        }
    }

    suspend fun switchCamera() {
        try {
            val camPub = room?.localParticipant?.getTrackPublication(Track.Source.CAMERA)
            val track = camPub?.track as? LocalVideoTrack
            track?.switchCamera()
            Log.d("LiveKitManager", "Switched camera position")
        } catch (e: Exception) {
            Log.e("LiveKitManager", "Failed to switch camera: ${e.message}")
        }
    }

    suspend fun setScreenShareEnabled(enabled: Boolean, mediaProjectionIntent: Intent? = null) {
        try {
            val params = if (mediaProjectionIntent != null) {
                ScreenCaptureParams(mediaProjectionPermissionResultData = mediaProjectionIntent)
            } else null
            room?.localParticipant?.setScreenShareEnabled(enabled, params)
            Log.d("LiveKitManager", "Set screen share enabled: $enabled")
        } catch (e: Exception) {
            Log.e("LiveKitManager", "Failed to toggle screen share: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            room?.disconnect()
            room?.release()
            room = null
            _isConnected.value = false
            _remoteVideoTracks.value = emptyMap()
            _localVideoTrack.value = null
            _activeSpeakers.value = emptySet()
            Log.i("LiveKitManager", "LiveKit SFU disconnected and released")
        } catch (e: Exception) {
            Log.w("LiveKitManager", "Error while disconnecting LiveKit: ${e.message}")
        }
    }
}
