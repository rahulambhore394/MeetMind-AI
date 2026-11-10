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

    private val _currentRoom = MutableStateFlow<Room?>(null)
    val currentRoom: StateFlow<Room?> = _currentRoom.asStateFlow()

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private val _localScreenTrack = MutableStateFlow<VideoTrack?>(null)
    val localScreenTrack: StateFlow<VideoTrack?> = _localScreenTrack.asStateFlow()

    private val _remoteVideoTracks = MutableStateFlow<Map<Long, VideoTrack>>(emptyMap())
    val remoteVideoTracks: StateFlow<Map<Long, VideoTrack>> = _remoteVideoTracks.asStateFlow()

    private val _remoteScreenTracks = MutableStateFlow<Map<Long, VideoTrack>>(emptyMap())
    val remoteScreenTracks: StateFlow<Map<Long, VideoTrack>> = _remoteScreenTracks.asStateFlow()

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

                _currentRoom.value = newRoom
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
                    if (event.publication.source == Track.Source.SCREEN_SHARE) {
                        Log.d("LiveKitManager", "Remote screen share track subscribed for user $userId")
                        val current = _remoteScreenTracks.value.toMutableMap()
                        current[userId] = track
                        _remoteScreenTracks.value = current
                    } else {
                        Log.d("LiveKitManager", "Remote video track subscribed for user $userId (source: ${event.publication.source})")
                        val current = _remoteVideoTracks.value.toMutableMap()
                        current[userId] = track
                        _remoteVideoTracks.value = current
                    }
                }
            }

            is RoomEvent.TrackUnsubscribed -> {
                val participant = event.participant
                val track = event.track
                val userId = participant.identity?.value?.toLongOrNull() ?: participant.identity?.hashCode()?.toLong() ?: 0L

                if (track is VideoTrack) {
                    val screenMap = _remoteScreenTracks.value.toMutableMap()
                    if (screenMap[userId] == track) {
                        screenMap.remove(userId)
                        _remoteScreenTracks.value = screenMap
                        Log.d("LiveKitManager", "Remote screen share track unsubscribed for user $userId")
                    }
                    val videoMap = _remoteVideoTracks.value.toMutableMap()
                    if (videoMap[userId] == track) {
                        videoMap.remove(userId)
                        _remoteVideoTracks.value = videoMap
                        Log.d("LiveKitManager", "Remote video track unsubscribed for user $userId")
                    }
                }
            }

            is RoomEvent.TrackPublished -> {
                val participant = event.participant
                val track = event.publication.track
                if (participant is io.livekit.android.room.participant.LocalParticipant) {
                    if (event.publication.source == Track.Source.CAMERA) {
                        _localVideoTrack.value = track as? VideoTrack
                        Log.d("LiveKitManager", "Local camera track published: $track")
                    } else if (event.publication.source == Track.Source.SCREEN_SHARE) {
                        _localScreenTrack.value = track as? VideoTrack
                        Log.d("LiveKitManager", "Local screen share track published: $track")
                    }
                }
            }

            is RoomEvent.TrackUnpublished -> {
                val participant = event.participant
                if (participant is io.livekit.android.room.participant.LocalParticipant) {
                    if (event.publication.source == Track.Source.CAMERA) {
                        _localVideoTrack.value = null
                        Log.d("LiveKitManager", "Local camera track unpublished")
                    } else if (event.publication.source == Track.Source.SCREEN_SHARE) {
                        _localScreenTrack.value = null
                        Log.d("LiveKitManager", "Local screen share track unpublished")
                    }
                }
            }

            is RoomEvent.TrackMuted -> {
                val participant = event.participant
                val pub = event.publication
                val userId = participant.identity?.value?.toLongOrNull() ?: participant.identity?.hashCode()?.toLong() ?: 0L
                if (pub.source == Track.Source.CAMERA) {
                    if (participant is io.livekit.android.room.participant.LocalParticipant) {
                        _localVideoTrack.value = null
                    } else {
                        val current = _remoteVideoTracks.value.toMutableMap()
                        current.remove(userId)
                        _remoteVideoTracks.value = current
                    }
                }
            }

            is RoomEvent.TrackUnmuted -> {
                val participant = event.participant
                val pub = event.publication
                val userId = participant.identity?.value?.toLongOrNull() ?: participant.identity?.hashCode()?.toLong() ?: 0L
                if (pub.source == Track.Source.CAMERA) {
                    val track = pub.track as? VideoTrack
                    if (participant is io.livekit.android.room.participant.LocalParticipant) {
                        _localVideoTrack.value = track
                    } else if (track != null) {
                        val current = _remoteVideoTracks.value.toMutableMap()
                        current[userId] = track
                        _remoteVideoTracks.value = current
                    }
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
            val localTrack = if (enabled) {
                camPub?.track as? VideoTrack 
                    ?: room?.localParticipant?.videoTrackPublications?.firstOrNull { it.first.source == Track.Source.CAMERA }?.first?.track as? VideoTrack
            } else null
            _localVideoTrack.value = localTrack
            Log.d("LiveKitManager", "Set camera enabled: $enabled, track: $localTrack")
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
            if (enabled) {
                val pub = room?.localParticipant?.getTrackPublication(Track.Source.SCREEN_SHARE)
                _localScreenTrack.value = pub?.track as? VideoTrack
            } else {
                _localScreenTrack.value = null
            }
            Log.d("LiveKitManager", "Set screen share enabled: $enabled")
        } catch (e: Exception) {
            Log.e("LiveKitManager", "Failed to toggle screen share: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            _currentRoom.value = null
            room?.disconnect()
            room?.release()
            room = null
            _isConnected.value = false
            _remoteVideoTracks.value = emptyMap()
            _remoteScreenTracks.value = emptyMap()
            _localVideoTrack.value = null
            _localScreenTrack.value = null
            _activeSpeakers.value = emptySet()
            Log.i("LiveKitManager", "LiveKit SFU disconnected and released")
        } catch (e: Exception) {
            Log.w("LiveKitManager", "Error while disconnecting LiveKit: ${e.message}")
        }
    }
}
