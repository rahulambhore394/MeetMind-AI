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
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.encodeToString
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

    suspend fun awaitConnection(timeoutMs: Long = 5000L) {
        if (stompClient == null) {
            connect()
        }
        try {
            withTimeout(timeoutMs) {
                stompClient?.connectionState?.first { it is StompClient.ConnectionState.Connected }
            }
        } catch (e: Exception) {
            Log.w("MeetingWS", "Timeout waiting for WebSocket STOMP connection: ${e.message}")
        }
    }

    private fun doConnect() {
        val token = tokenProvider.getToken() ?: return
        val wsUrl = if (customWsUrl != null) {
            customWsUrl
        } else {
            val activeHost = com.developer_rahul.meetmind_ai.core.network.interceptor.HostManager.activeHost
            val base = BuildConfig.BASE_URL.trimEnd('/')
            if (activeHost != null && !base.contains("onrender.com")) {
                val scheme = if (activeHost.contains("onrender.com")) "wss" else "ws"
                val portSuffix = if (activeHost.contains("onrender.com")) "" else ":8080"
                "$scheme://$activeHost$portSuffix/ws"
            } else {
                val wsBase = if (base.startsWith("https://")) {
                    base.replaceFirst("https://", "wss://")
                } else if (base.startsWith("http://")) {
                    base.replaceFirst("http://", "ws://")
                } else {
                    "ws://$base"
                }
                "$wsBase/ws"
            }
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
                        signalingSubscriptions.toList().forEach { resubscribeToSignaling(it) }
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
            val element = json.parseToJsonElement(body).jsonObject
            val messageId = element["messageId"]?.jsonPrimitive?.longOrNull
                ?: element["id"]?.jsonPrimitive?.longOrNull
                ?: System.currentTimeMillis()
            val meetingId = element["meetingId"]?.jsonPrimitive?.longOrNull ?: 0L
            val senderId = element["senderId"]?.jsonPrimitive?.longOrNull
                ?: element["userId"]?.jsonPrimitive?.longOrNull
                ?: 0L
            val senderName = element["senderName"]?.jsonPrimitive?.contentOrNull
                ?: element["userName"]?.jsonPrimitive?.contentOrNull
                ?: "User $senderId"
            val messageText = element["message"]?.jsonPrimitive?.contentOrNull
                ?: element["content"]?.jsonPrimitive?.contentOrNull
                ?: element["text"]?.jsonPrimitive?.contentOrNull
                ?: ""
            val messageType = element["messageType"]?.jsonPrimitive?.contentOrNull ?: "CHAT"
            val sentAt = element["sentAt"]?.jsonPrimitive?.contentOrNull
                ?: element["timestamp"]?.jsonPrimitive?.contentOrNull
                ?: java.time.Instant.now().toString()

            if (messageText.isNotBlank()) {
                val dto = ChatMessageResponseDto(
                    messageId = messageId,
                    meetingId = meetingId,
                    senderId = senderId,
                    senderName = senderName,
                    message = messageText,
                    messageType = messageType,
                    sentAt = sentAt
                )
                _chatMessages.tryEmit(dto)
            }
        } catch (e: Exception) {
            Log.e("MeetingWS", "Failed to parse chat message: ${e.message}, body=$body")
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
        if (meetingSubscriptions.add(meetingId)) {
            stompClient?.subscribe("/topic/meetings/$meetingId")
        }
    }

    fun unsubscribeFromMeeting(meetingId: Long) {
        meetingSubscriptions.remove(meetingId)
        stompClient?.unsubscribe("/topic/meetings/$meetingId")
    }

    fun subscribeToChat(meetingId: Long) {
        if (chatSubscriptions.add(meetingId)) {
            stompClient?.subscribe("/topic/meetings/$meetingId/chat")
        }
    }

    fun unsubscribeFromChat(meetingId: Long) {
        chatSubscriptions.remove(meetingId)
        stompClient?.unsubscribe("/topic/meetings/$meetingId/chat")
    }

    fun subscribeToSignaling(meetingId: Long) {
        if (signalingSubscriptions.add(meetingId)) {
            stompClient?.subscribe("/topic/meetings/$meetingId/signaling")
            stompClient?.subscribe("/user/queue/meetings/$meetingId/signaling")
            sendSignalingMessage(meetingId, "JOIN")
        }
    }

    private fun resubscribeToSignaling(meetingId: Long) {
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
        if (recordingSubscriptions.add(meetingId)) {
            stompClient?.subscribe("/topic/meetings/$meetingId/recordings")
        }
    }

    fun unsubscribeFromRecordings(meetingId: Long) {
        recordingSubscriptions.remove(meetingId)
        stompClient?.unsubscribe("/topic/meetings/$meetingId/recordings")
    }

    fun subscribeToIntelligence(meetingId: Long) {
        if (intelligenceSubscriptions.add(meetingId)) {
            stompClient?.subscribe("/topic/meetings/$meetingId/intelligence")
        }
    }

    fun unsubscribeFromIntelligence(meetingId: Long) {
        intelligenceSubscriptions.remove(meetingId)
        stompClient?.unsubscribe("/topic/meetings/$meetingId/intelligence")
    }

    fun subscribeToTranslation(meetingId: Long, targetLanguage: String) {
        if (translationSubscriptions.add("$meetingId:$targetLanguage")) {
            stompClient?.subscribe("/topic/meetings/$meetingId/translations/$targetLanguage")
        }
    }

    fun unsubscribeFromTranslation(meetingId: Long, targetLanguage: String) {
        translationSubscriptions.remove("$meetingId:$targetLanguage")
        stompClient?.unsubscribe("/topic/meetings/$meetingId/translations/$targetLanguage")
    }

    fun subscribeToUserNotifications(userId: Long) {
        if (userSubscriptions.add(userId)) {
            stompClient?.subscribe("/topic/users/$userId/notifications")
        }
    }

    fun unsubscribeFromUserNotifications(userId: Long) {
        userSubscriptions.remove(userId)
        stompClient?.unsubscribe("/topic/users/$userId/notifications")
    }

    fun sendTranslationRequest(meetingId: Long, sourceText: String, sourceLanguage: String, targetLanguage: String? = null) {
        val dto = LiveTranslateRequestDto(sourceText, sourceLanguage, targetLanguage ?: "", System.currentTimeMillis())
        val body = json.encodeToString(LiveTranslateRequestDto.serializer(), dto)
        stompClient?.send("/app/meetings/$meetingId/translate", body)
    }

    fun sendLanguagePreference(meetingId: Long, targetLanguage: String) {
        val dto = LanguagePreferenceRequestDto(targetLanguage)
        val body = json.encodeToString(LanguagePreferenceRequestDto.serializer(), dto)
        stompClient?.send("/app/meetings/$meetingId/language-preference", body)
    }

    fun sendSignalingMessage(meetingId: Long, type: String, payload: String? = null, receiverId: Long? = null) {
        val currentUserId = tokenProvider.getUserId()
        val currentUserName = tokenProvider.getUserName()
        val dto = SignalingMessageDto(
            meetingId = meetingId,
            senderId = if (currentUserId > 0L) currentUserId else null,
            senderName = currentUserName,
            type = type,
            payload = payload,
            receiverId = receiverId
        )
        val body = json.encodeToString(SignalingMessageDto.serializer(), dto)
        stompClient?.send("/app/meetings/$meetingId/signaling", body)
    }

    fun sendChatMessage(meetingId: Long, message: String) {
        val currentUserId = tokenProvider.getUserId()
        val currentUserName = tokenProvider.getUserName() ?: "User"
        val jsonElement = buildJsonObject {
            put("meetingId", meetingId)
            put("message", message)
            put("content", message)
            if (currentUserId > 0L) {
                put("senderId", currentUserId)
                put("userId", currentUserId)
            }
            put("senderName", currentUserName)
            put("userName", currentUserName)
            put("sentAt", java.time.Instant.now().toString())
        }
        val body = json.encodeToString(jsonElement)
        stompClient?.send("/app/meetings/$meetingId/chat", body)
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
        val jsonElement = buildJsonObject {
            put("representativeId", representativeId)
            put("query", query)
            put("language", language)
        }
        val body = json.encodeToString(jsonElement)
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
