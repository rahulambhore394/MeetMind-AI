package com.developer_rahul.meetmind_ai.core.network.websocket

import android.util.Log
import com.developer_rahul.meetmind_ai.BuildConfig
import com.developer_rahul.meetmind_ai.core.network.token.TokenProvider
import com.developer_rahul.meetmind_ai.core.network.websocket.model.MeetingEventDto
import com.developer_rahul.meetmind_ai.core.network.websocket.model.SignalingMessageDto
import com.developer_rahul.meetmind_ai.feature.chat.data.remote.dto.ChatMessageResponseDto
import com.developer_rahul.meetmind_ai.feature.translation.data.remote.dto.LiveTranslationDto
import com.developer_rahul.meetmind_ai.feature.translation.data.remote.dto.LiveTranslateRequestDto
import com.developer_rahul.meetmind_ai.feature.translation.data.remote.dto.LanguagePreferenceRequestDto
import com.developer_rahul.meetmind_ai.core.network.websocket.stomp.StompClient
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.MeetingRealtimeEvent
import com.developer_rahul.meetmind_ai.feature.notifications.data.remote.dto.NotificationDto
import com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto.AiProxySpeechDto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient

class MeetingWebSocketManager(
    private val okHttpClient: OkHttpClient,
    private val tokenProvider: TokenProvider,
    private val customWsUrl: String? = null
) {
    private var stompClient: StompClient? = null
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null

    private val _events = MutableSharedFlow<MeetingRealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<MeetingRealtimeEvent> = _events.asSharedFlow()

    private val _chatMessages = MutableSharedFlow<ChatMessageResponseDto>(replay = 1, extraBufferCapacity = 64)
    val chatMessages: SharedFlow<ChatMessageResponseDto> = _chatMessages.asSharedFlow()

    private val _signalingMessages = MutableSharedFlow<SignalingMessageDto>(extraBufferCapacity = 64)
    val signalingMessages: SharedFlow<SignalingMessageDto> = _signalingMessages.asSharedFlow()

    private val _recordingEvents = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 64)
    val recordingEvents: SharedFlow<Map<String, String>> = _recordingEvents.asSharedFlow()

    private val _translations = MutableSharedFlow<LiveTranslationDto>(replay = 1, extraBufferCapacity = 64)
    val translations: SharedFlow<LiveTranslationDto> = _translations.asSharedFlow()

    private val _intelligenceEvents = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 64)
    val intelligenceEvents: SharedFlow<Map<String, String>> = _intelligenceEvents.asSharedFlow()

    private val _notifications = MutableSharedFlow<NotificationDto>(extraBufferCapacity = 64)
    val notifications: SharedFlow<NotificationDto> = _notifications.asSharedFlow()

    private val _aiProxySpeech = MutableSharedFlow<AiProxySpeechDto>(replay = 1, extraBufferCapacity = 64)
    val aiProxySpeech: SharedFlow<AiProxySpeechDto> = _aiProxySpeech.asSharedFlow()

    private val meetingSubscriptions = mutableSetOf<Long>()
    private val chatSubscriptions = mutableSetOf<Long>()
    private val signalingSubscriptions = mutableSetOf<Long>()
    private val recordingSubscriptions = mutableSetOf<Long>()
    private val translationSubscriptions = mutableSetOf<String>()
    private val intelligenceSubscriptions = mutableSetOf<Long>()
    private val userSubscriptions = mutableSetOf<Long>()
    private val aiProxySubscriptions = mutableSetOf<Long>()

    private var reconnectAttempt = 0
    private val maxReconnectDelay = 30000L // 30 seconds

    fun connect() {
        if (stompClient != null) return
        reconnectAttempt = 0
        doConnect()
    }

    private fun doConnect() {
        val token = tokenProvider.getToken() ?: return
        val wsUrl = if (customWsUrl != null) {
            customWsUrl
        } else if (BuildConfig.BASE_URL.contains("onrender.com")) {
            BuildConfig.BASE_URL.replace("http", "ws") + "ws"
        } else {
            val candidateHosts = listOf("127.0.0.1:8080", "10.70.43.145:8080", "10.0.2.2:8080")
            val chosenHost = candidateHosts[reconnectAttempt % candidateHosts.size]
            "ws://$chosenHost/ws"
        }
        
        Log.d("MeetingWS", "Connecting to $wsUrl (Attempt ${reconnectAttempt + 1})")
        
        stompClient = StompClient(
            client = okHttpClient,
            url = wsUrl,
            headers = mapOf("Authorization" to "Bearer $token")
        ).apply {
            connect()
            
            scope.launch {
                connectionState.collect { state ->
                    Log.d("MeetingWS", "Connection State: $state")
                    if (state is StompClient.ConnectionState.Connected) {
                        reconnectJob?.cancel()
                        reconnectJob = null
                        reconnectAttempt = 0
                        
                        meetingSubscriptions.toList().forEach { subscribeToMeeting(it) }
                        chatSubscriptions.toList().forEach { subscribeToChat(it) }
                        signalingSubscriptions.toList().forEach { subscribeToSignaling(it) }
                        recordingSubscriptions.toList().forEach { subscribeToRecordings(it) }
                        intelligenceSubscriptions.toList().forEach { subscribeToIntelligence(it) }
                        userSubscriptions.toList().forEach { subscribeToUserNotifications(it) }
                        aiProxySubscriptions.toList().forEach { subscribeToAiProxySpeech(it) }
                        translationSubscriptions.toList().forEach { sub ->
                            val parts = sub.split(":")
                            if (parts.size == 2) {
                                val mid = parts[0].toLongOrNull() ?: 0L
                                val lang = parts[1]
                                subscribeToTranslation(mid, lang)
                            }
                        }
                    } else if (state is StompClient.ConnectionState.Disconnected || state is StompClient.ConnectionState.Error) {
                        scheduleReconnect()
                    }
                }
            }

            scope.launch {
                events.collect { frame ->
                    if (frame.command == "MESSAGE") {
                        val destination = frame.headers["destination"]
                        val body = frame.body
                        if (body != null) {
                            if (destination?.contains("/chat") == true) {
                                parseAndEmitChatMessage(body)
                            } else if (destination?.contains("/signaling") == true) {
                                parseAndEmitSignalingMessage(body)
                            } else if (destination?.contains("/recordings") == true) {
                                parseAndEmitRecordingEvent(body)
                            } else if (destination?.contains("/intelligence") == true) {
                                parseAndEmitIntelligenceEvent(body)
                            } else if (destination?.contains("/translations") == true) {
                                parseAndEmitTranslation(body)
                            } else if (destination?.contains("/notifications") == true) {
                                parseAndEmitNotification(body)
                            } else if (destination?.contains("/ai-proxy/speech") == true) {
                                parseAndEmitAiProxySpeech(body)
                            } else {
                                parseAndEmitEvent(body)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (reconnectJob != null) return
        reconnectJob = scope.launch {
            val delayMs = if (reconnectAttempt < 3) 1000L else (Math.pow(2.0, (reconnectAttempt - 2).toDouble()).toLong() * 1000L).coerceAtMost(maxReconnectDelay)
            reconnectAttempt++
            
            Log.d("MeetingWS", "Attempting to reconnect in ${delayMs}ms (Attempt $reconnectAttempt)...")
            delay(delayMs)
            stompClient?.disconnect()
            stompClient = null
            doConnect()
        }
    }

    private fun parseAndEmitEvent(body: String) {
        try {
            val dto = json.decodeFromString<MeetingEventDto>(body)
            val event = when (dto.type) {
                "MEETING_STARTED" -> MeetingRealtimeEvent.MeetingStarted(dto.meetingId)
                "MEETING_ENDED" -> MeetingRealtimeEvent.MeetingEnded(dto.meetingId)
                "PARTICIPANT_JOINED" -> {
                    if (dto.userId != null && dto.userName != null) {
                        MeetingRealtimeEvent.ParticipantJoined(dto.meetingId, dto.userId, dto.userName)
                    } else null
                }
                "PARTICIPANT_LEFT" -> {
                    if (dto.userId != null && dto.userName != null) {
                        MeetingRealtimeEvent.ParticipantLeft(dto.meetingId, dto.userId, dto.userName)
                    } else null
                }
                "PARTICIPANT_ACCEPTED" -> {
                    if (dto.userId != null && dto.userName != null) {
                        MeetingRealtimeEvent.ParticipantAccepted(dto.meetingId, dto.userId, dto.userName)
                    } else null
                }
                "PARTICIPANT_DECLINED" -> {
                    if (dto.userId != null && dto.userName != null) {
                        MeetingRealtimeEvent.ParticipantDeclined(dto.meetingId, dto.userId, dto.userName)
                    } else null
                }
                else -> null
            }
            if (event != null) _events.tryEmit(event)
        } catch (e: Exception) {
            Log.e("MeetingWS", "Failed to parse event: ${e.message}")
        }
    }

    private fun parseAndEmitChatMessage(body: String) {
        try {
            val dto = json.decodeFromString<ChatMessageResponseDto>(body)
            _chatMessages.tryEmit(dto)
        } catch (e: Exception) {
            Log.e("MeetingWS", "Failed to parse chat message: ${e.message}")
        }
    }

    private fun parseAndEmitSignalingMessage(body: String) {
        try {
            val dto = json.decodeFromString<SignalingMessageDto>(body)
            _signalingMessages.tryEmit(dto)
        } catch (e: Exception) {
            Log.e("MeetingWS", "Failed to parse signaling message: ${e.message}")
        }
    }

    private fun parseAndEmitRecordingEvent(body: String) {
        try {
            // Backend sends numeric fields (ownerId, recordingId, meetingId, timestamp) as Long.
            // Deserialize as JsonObject first, then convert each entry to String.
            val jsonObj = json.parseToJsonElement(body).jsonObject
            val event = jsonObj.entries.associate { (k, v) -> k to v.jsonPrimitive.content }
            _recordingEvents.tryEmit(event)
        } catch (e: Exception) {
            Log.e("MeetingWS", "Failed to parse recording event: ${e.message}")
            Log.e("MeetingWS", "JSON input: $body")
        }
    }

    private fun parseAndEmitTranslation(body: String) {
        try {
            val dto = json.decodeFromString<LiveTranslationDto>(body)
            _translations.tryEmit(dto)
        } catch (e: Exception) {
            Log.e("MeetingWS", "Failed to parse translation: ${e.message}")
        }
    }

    private fun parseAndEmitIntelligenceEvent(body: String) {
        try {
            val event = json.decodeFromString<Map<String, String>>(body)
            _intelligenceEvents.tryEmit(event)
        } catch (e: Exception) {
            Log.e("MeetingWS", "Failed to parse intelligence event: ${e.message}")
        }
    }

    private fun parseAndEmitNotification(body: String) {
        try {
            val dto = json.decodeFromString<NotificationDto>(body)
            _notifications.tryEmit(dto)
        } catch (e: Exception) {
            Log.e("MeetingWS", "Failed to parse notification: ${e.message}")
        }
    }

    private fun parseAndEmitAiProxySpeech(body: String) {
        try {
            val dto = json.decodeFromString<AiProxySpeechDto>(body)
            _aiProxySpeech.tryEmit(dto)
        } catch (e: Exception) {
            Log.e("MeetingWS", "Failed to parse AI proxy speech: ${e.message}")
        }
    }

    fun subscribeToMeeting(meetingId: Long) {
        meetingSubscriptions.add(meetingId)
        stompClient?.subscribe("/topic/meetings/$meetingId")
    }

    fun unsubscribeFromMeeting(meetingId: Long) {
        meetingSubscriptions.remove(meetingId)
        stompClient?.unsubscribe("/topic/meetings/$meetingId")
    }

    fun subscribeToChat(meetingId: Long) {
        chatSubscriptions.add(meetingId)
        stompClient?.subscribe("/topic/meetings/$meetingId/chat")
    }

    fun unsubscribeFromChat(meetingId: Long) {
        chatSubscriptions.remove(meetingId)
        stompClient?.unsubscribe("/topic/meetings/$meetingId/chat")
    }

    fun subscribeToSignaling(meetingId: Long) {
        signalingSubscriptions.add(meetingId)
        stompClient?.subscribe("/topic/meetings/$meetingId/signaling")
        stompClient?.subscribe("/user/queue/meetings/$meetingId/signaling")
        sendSignalingMessage(meetingId, "JOIN")
    }

    fun unsubscribeFromSignaling(meetingId: Long) {
        sendSignalingMessage(meetingId, "LEAVE")
        signalingSubscriptions.remove(meetingId)
        stompClient?.unsubscribe("/topic/meetings/$meetingId/signaling")
        stompClient?.unsubscribe("/user/queue/meetings/$meetingId/signaling")
    }

    fun subscribeToRecordings(meetingId: Long) {
        recordingSubscriptions.add(meetingId)
        stompClient?.subscribe("/topic/meetings/$meetingId/recordings")
    }

    fun unsubscribeFromRecordings(meetingId: Long) {
        recordingSubscriptions.remove(meetingId)
        stompClient?.unsubscribe("/topic/meetings/$meetingId/recordings")
    }

    fun subscribeToIntelligence(meetingId: Long) {
        intelligenceSubscriptions.add(meetingId)
        stompClient?.subscribe("/topic/meetings/$meetingId/intelligence")
    }

    fun unsubscribeFromIntelligence(meetingId: Long) {
        intelligenceSubscriptions.remove(meetingId)
        stompClient?.unsubscribe("/topic/meetings/$meetingId/intelligence")
    }

    fun subscribeToTranslation(meetingId: Long, targetLanguage: String) {
        translationSubscriptions.add("$meetingId:$targetLanguage")
        stompClient?.subscribe("/topic/meetings/$meetingId/translations/$targetLanguage")
    }

    fun unsubscribeFromTranslation(meetingId: Long, targetLanguage: String) {
        translationSubscriptions.remove("$meetingId:$targetLanguage")
        stompClient?.unsubscribe("/topic/meetings/$meetingId/translations/$targetLanguage")
    }

    fun subscribeToUserNotifications(userId: Long) {
        userSubscriptions.add(userId)
        stompClient?.subscribe("/topic/users/$userId/notifications")
    }

    fun unsubscribeFromUserNotifications(userId: Long) {
        userSubscriptions.remove(userId)
        stompClient?.unsubscribe("/topic/users/$userId/notifications")
    }

    fun sendTranslationRequest(meetingId: Long, sourceText: String, sourceLanguage: String, targetLanguage: String? = null) {
        // Fallback to manually creating JSON string if encodeToString extension is acting up
        val body = "{\"sourceText\":\"$sourceText\",\"sourceLanguage\":\"$sourceLanguage\",\"targetLanguage\":\"${targetLanguage ?: ""}\",\"timestamp\":${System.currentTimeMillis()}}"
        stompClient?.send("/app/meetings/$meetingId/translate", body)
    }

    fun sendLanguagePreference(meetingId: Long, targetLanguage: String) {
        val body = "{\"targetLanguage\":\"$targetLanguage\"}"
        stompClient?.send("/app/meetings/$meetingId/language-preference", body)
    }

    fun sendSignalingMessage(meetingId: Long, type: String, payload: String? = null, receiverId: Long? = null) {
        val body = "{\"meetingId\":$meetingId,\"type\":\"$type\",\"payload\":${if (payload != null) "\"$payload\"" else "null"},\"receiverId\":${receiverId ?: "null"}}"
        stompClient?.send("/app/meetings/$meetingId/signaling", body)
    }

    fun sendChatMessage(meetingId: Long, message: String) {
        val body = "{\"meetingId\":$meetingId,\"message\":\"$message\"}"
        stompClient?.send("/app/chat.send", body)
    }

    fun subscribeToAiProxySpeech(meetingId: Long) {
        aiProxySubscriptions.add(meetingId)
        stompClient?.subscribe("/topic/meetings/$meetingId/ai-proxy/speech")
    }

    fun unsubscribeFromAiProxySpeech(meetingId: Long) {
        aiProxySubscriptions.remove(meetingId)
        stompClient?.unsubscribe("/topic/meetings/$meetingId/ai-proxy/speech")
    }

    fun sendAiProxyQuery(meetingId: Long, representativeId: Long, query: String, language: String = "en") {
        val body = "{\"representativeId\":$representativeId,\"query\":\"$query\",\"language\":\"$language\"}"
        stompClient?.send("/app/meetings/$meetingId/ai-proxy/ask", body)
    }

    fun disconnect() {
        stompClient?.disconnect()
        stompClient = null
        meetingSubscriptions.clear()
        chatSubscriptions.clear()
        signalingSubscriptions.clear()
        recordingSubscriptions.clear()
        translationSubscriptions.clear()
        intelligenceSubscriptions.clear()
        aiProxySubscriptions.clear()
    }
}
