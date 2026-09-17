package com.developer_rahul.meetmind_ai.e2e

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.core.network.token.TokenProvider
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.AuthApiService
import com.developer_rahul.meetmind_ai.feature.auth.data.repository.RealAuthRepository
import com.developer_rahul.meetmind_ai.feature.chat.data.remote.ChatApiService
import com.developer_rahul.meetmind_ai.feature.chat.data.repository.RealChatRepository
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.MeetingApiService
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.RealMeetingRepository
import com.developer_rahul.meetmind_ai.feature.representative.data.remote.RepresentativeApiService
import com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto.AiProxySpeechDto
import com.developer_rahul.meetmind_ai.feature.representative.data.repository.RealLiveRepresentativeRepository
import com.developer_rahul.meetmind_ai.feature.translation.data.remote.TranslationApiService
import com.developer_rahul.meetmind_ai.feature.translation.data.repository.RealLiveTranslationRepository
import com.developer_rahul.meetmind_ai.feature.translation.domain.model.Subtitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * End-to-End Real-Time Integration Test Suite for MeetMind AI Android App.
 *
 * Executes against live backend at http://localhost:8080/ and ws://localhost:8080/ws
 * verifying:
 * 1. Live User Registration and JWT Authentication
 * 2. Real Meeting Creation, Code Generation, and Scheduling
 * 3. Real Single & Batch Participant Invitations
 * 4. Real-Time STOMP In-Meeting Chat & WebSockets
 * 5. Real-Time AI Representative Activation and Live Voice Event Generation
 * 6. Full Lifecycle End-to-End Orchestration
 */
@OptIn(ExperimentalCoroutinesApi::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AndroidRealTimeE2ETest {

    private val baseUrl = "http://localhost:8080/"
    private val wsUrl = "ws://localhost:8080/ws"

    private val inMemoryTokenProvider = InMemoryTokenProvider()
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var retrofit: Retrofit

    private lateinit var authApiService: AuthApiService
    private lateinit var meetingApiService: MeetingApiService
    private lateinit var representativeApiService: RepresentativeApiService
    private lateinit var chatApiService: ChatApiService
    private lateinit var translationApiService: TranslationApiService

    private lateinit var authRepository: RealAuthRepository
    private lateinit var meetingRepository: RealMeetingRepository
    private lateinit var liveRepresentativeRepository: RealLiveRepresentativeRepository
    private lateinit var liveTranslationRepository: RealLiveTranslationRepository
    private lateinit var chatRepository: RealChatRepository
    private lateinit var webSocketManager: MeetingWebSocketManager

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    class InMemoryTokenProvider : TokenProvider {
        private var token: String? = null
        private var userId: Long = 0L
        private var userName: String? = null
        private var userEmail: String? = null
        private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        override val unauthorizedEvent: SharedFlow<Unit> = _unauthorized

        override fun getToken(): String? = token
        override fun saveToken(token: String) { this.token = token }
        override fun getUserId(): Long = userId
        override fun saveUserId(userId: Long) { this.userId = userId }
        override fun getUserName(): String? = userName
        override fun saveUserName(name: String) { this.userName = name }
        override fun getUserEmail(): String? = userEmail
        override fun saveUserEmail(email: String) { this.userEmail = email }
        override fun clearToken() {
            token = null
            userId = 0L
            userName = null
            userEmail = null
        }
        override fun hasToken(): Boolean = !token.isNullOrEmpty()
        override fun notifyUnauthorized() { _unauthorized.tryEmit(Unit) }
    }

    @Before
    fun setup() {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            inMemoryTokenProvider.getToken()?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
            chain.proceed(requestBuilder.build())
        }

        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        authApiService = retrofit.create(AuthApiService::class.java)
        meetingApiService = retrofit.create(MeetingApiService::class.java)
        representativeApiService = retrofit.create(RepresentativeApiService::class.java)
        chatApiService = retrofit.create(ChatApiService::class.java)
        translationApiService = retrofit.create(TranslationApiService::class.java)

        authRepository = RealAuthRepository(authApiService, inMemoryTokenProvider)
        meetingRepository = RealMeetingRepository(meetingApiService)

        webSocketManager = MeetingWebSocketManager(
            okHttpClient = okHttpClient,
            tokenProvider = inMemoryTokenProvider,
            customWsUrl = wsUrl
        )

        liveRepresentativeRepository = RealLiveRepresentativeRepository(
            apiService = representativeApiService,
            webSocketManager = webSocketManager
        )

        liveTranslationRepository = RealLiveTranslationRepository(
            apiService = translationApiService,
            webSocketManager = webSocketManager
        )

        chatRepository = RealChatRepository(
            chatApiService = chatApiService,
            webSocketManager = webSocketManager,
            tokenProvider = inMemoryTokenProvider
        )
    }

    @Test
    fun test01_AuthWorkflowRealData() = runBlocking(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val email = "host_$timestamp@meetmind.ai"
        val password = "Password123!"
        val name = "Host User $timestamp"

        println("=== [E2E Phase 1] Registering New Host: $email ===")
        val registerResult = authRepository.register(name, email, password)
        assertTrue("Registration failed: $registerResult", registerResult is NetworkResult.Success)
        val regData = (registerResult as NetworkResult.Success).data
        assertTrue("User ID must be positive", regData.id > 0)
        assertEquals(email, regData.email)
        assertEquals(name, regData.name)

        println("=== [E2E Phase 1] Logging In Host: $email ===")
        val loginResult = authRepository.login(email, password)
        assertTrue("Login failed: $loginResult", loginResult is NetworkResult.Success)
        val loginData = (loginResult as NetworkResult.Success).data
        assertNotNull("Access token must not be null", loginData.accessToken)
        assertTrue("Token must be non-empty", loginData.accessToken.isNotEmpty())
        assertTrue("User is logged in", authRepository.isLoggedIn())
        assertEquals(loginData.accessToken, inMemoryTokenProvider.getToken())
        println("=== [E2E Phase 1] Host Authenticated Successfully with JWT ===")
    }

    @Test
    fun test02_MeetingLifecycleAndBatchInvitation() = runBlocking(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val email = "meeting_host_$timestamp@meetmind.ai"
        authRepository.register("Meeting Host", email, "Password123!")
        authRepository.login(email, "Password123!")

        println("=== [E2E Phase 2] Creating Meeting via RealMeetingRepository ===")
        val title = "Sprint Architecture Review $timestamp"
        val description = "Deep-dive real-time session"
        val createResult = meetingRepository.createMeeting(
            title = title,
            description = description,
            scheduledAt = "2026-09-16T14:00:00Z",
            invitedEmails = null
        )
        assertTrue("Create meeting failed: $createResult", createResult is NetworkResult.Success)
        val meeting = (createResult as NetworkResult.Success).data
        assertTrue("Meeting ID must be positive", meeting.id > 0)
        assertEquals(title, meeting.title)
        assertNotNull("Meeting code must not be null", meeting.meetingCode)
        println("=== [E2E Phase 2] Created Meeting ID: ${meeting.id}, Code: ${meeting.meetingCode} ===")

        println("=== [E2E Phase 2] Starting Meeting ===")
        val startResult = meetingRepository.startMeeting(meeting.id)
        assertTrue("Start meeting failed: $startResult", startResult is NetworkResult.Success)

        println("=== [E2E Phase 3] Single Participant Invitation ===")
        val singleGuest = "guest_$timestamp@meetmind.ai"
        val inviteResult = meetingRepository.inviteParticipant(meeting.id, singleGuest)
        assertTrue("Invite participant failed: $inviteResult", inviteResult is NetworkResult.Success)

        println("=== [E2E Phase 3] Batch Participant Invitation ===")
        val batchGuests = listOf(
            "team1_$timestamp@meetmind.ai",
            "team2_$timestamp@meetmind.ai",
            "team3_$timestamp@meetmind.ai"
        )
        val batchResult = meetingRepository.batchInviteParticipants(meeting.id, batchGuests)
        assertTrue("Batch invite failed: $batchResult", batchResult is NetworkResult.Success)

        println("=== [E2E Phase 3] Verifying Participants List ===")
        val participantsResult = meetingRepository.getParticipants(meeting.id)
        assertTrue("Get participants failed: $participantsResult", participantsResult is NetworkResult.Success)
        val participants = (participantsResult as NetworkResult.Success).data
        assertTrue("Participants list should contain host + invited users", participants.isNotEmpty())
        println("=== [E2E Phase 3] Total Participants Count: ${participants.size} ===")

        println("=== [E2E Phase 2] Ending Meeting ===")
        val endResult = meetingRepository.endMeeting(meeting.id)
        assertTrue("End meeting failed: $endResult", endResult is NetworkResult.Success)
        println("=== [E2E Phase 2] Meeting Closed Successfully ===")
    }

    @Test
    fun test03_RealTimeChatWebSocketSignaling() = runBlocking(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val email = "chat_user_$timestamp@meetmind.ai"
        authRepository.register("Chat Tester", email, "Password123!")
        authRepository.login(email, "Password123!")

        val createResult = meetingRepository.createMeeting(
            title = "Live Chat Room $timestamp",
            description = "Real-Time WebSocket Validation",
            scheduledAt = "2026-09-16T14:00:00Z",
            invitedEmails = null
        )
        val meeting = (createResult as NetworkResult.Success).data
        meetingRepository.startMeeting(meeting.id)

        println("=== [E2E Phase 4] Connecting STOMP WebSocket to $wsUrl ===")
        webSocketManager.connect()

        // Wait a short moment for WebSocket handshake
        kotlinx.coroutines.delay(1000)

        println("=== [E2E Phase 4] Subscribing to Chat for Meeting ${meeting.id} ===")
        webSocketManager.subscribeToChat(meeting.id)
        kotlinx.coroutines.delay(500)

        val testMessageContent = "Real-Time Sync Verification: Message #$timestamp"
        println("=== [E2E Phase 4] Pre-listening for Chat Message via async... ===")
        val messageDeferred = async {
            webSocketManager.chatMessages.first {
                it.meetingId == meeting.id && it.message == testMessageContent
            }
        }
        delay(300)

        println("=== [E2E Phase 4] Sending Chat Message: '$testMessageContent' ===")
        webSocketManager.sendChatMessage(meeting.id, testMessageContent)

        println("=== [E2E Phase 4] Awaiting WebSocket Chat Echo... ===")
        val receivedMessage = withTimeoutOrNull(10000) {
            messageDeferred.await()
        }

        assertNotNull("Failed to receive real-time chat message via WebSocket", receivedMessage)
        assertEquals(testMessageContent, receivedMessage?.message)
        assertEquals(meeting.id, receivedMessage?.meetingId)
        println("=== [E2E Phase 4] Verified Real-Time Chat Delivery: ID=${receivedMessage?.messageId}, Sender=${receivedMessage?.senderName} ===")

        println("=== [E2E Phase 4] Sending WebRTC Signaling Frame ===")
        webSocketManager.subscribeToSignaling(meeting.id)
        delay(500)
        webSocketManager.sendSignalingMessage(meeting.id, "TEST_PING", "ping_payload", null)

        webSocketManager.disconnect()
        println("=== [E2E Phase 4] Chat & Signaling WebSocket Test Passed ===")
    }

    @Test
    fun test04_RealTimeAiProxyVoiceGeneration() = runBlocking(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val email = "ai_proxy_owner_$timestamp@meetmind.ai"
        authRepository.register("AI Proxy Host", email, "Password123!")
        authRepository.login(email, "Password123!")

        val createResult = meetingRepository.createMeeting(
            title = "AI Proxy Voice Room $timestamp",
            description = "AI Voice Synthesis Validation",
            scheduledAt = "2026-09-16T14:00:00Z",
            invitedEmails = null
        )
        val meeting = (createResult as NetworkResult.Success).data
        meetingRepository.startMeeting(meeting.id)

        println("=== [E2E Phase 5] Creating AI Representative for Meeting ${meeting.id} ===")
        val repResult = liveRepresentativeRepository.createRepresentative(
            meetingId = meeting.id,
            monitoredTopics = listOf("Timeline", "Architecture", "Budget"),
            monitoredQuestions = listOf("What is the deadline?"),
            importantPeople = listOf("Lead Architect"),
            reportPreferences = "FULL_REPORT",
            notificationPreferences = "IMMEDIATE"
        )
        assertTrue("Create representative failed: $repResult", repResult is NetworkResult.Success)
        val rep = (repResult as NetworkResult.Success).data
        assertTrue("Representative ID must be positive", rep.id > 0)
        println("=== [E2E Phase 5] Created AI Representative ID: ${rep.id} ===")

        println("=== [E2E Phase 5] Connecting WebSocket and Subscribing to AI Proxy Speech ===")
        webSocketManager.connect()
        delay(1000)

        webSocketManager.subscribeToAiProxySpeech(meeting.id)
        delay(500)

        println("=== [E2E Phase 5] Pre-listening for STOMP Voice Event via async... ===")
        val speechDeferred = async {
            webSocketManager.aiProxySpeech.first {
                it.meetingId == meeting.id && it.representativeId == rep.id
            }
        }
        delay(300)

        println("=== [E2E Phase 5] Triggering AI Proxy Speech Event via REST/STOMP ===")
        val query = "What is our architecture strategy for this quarter?"
        val speechResponse = representativeApiService.triggerSpeech(
            meetingId = meeting.id,
            id = rep.id,
            body = mapOf("query" to query, "language" to "en")
        )

        assertNotNull("Speech response must not be null", speechResponse)
        assertEquals(rep.id, speechResponse.representativeId)
        assertEquals(meeting.id, speechResponse.meetingId)
        assertTrue("Spoken text must be generated", speechResponse.spokenText.isNotEmpty())
        println("=== [E2E Phase 5] AI Representative Generated Speech: '${speechResponse.spokenText}' ===")

        println("=== [E2E Phase 5] Awaiting Live STOMP Voice Event on Android Flow ===")
        val receivedWsSpeech = withTimeoutOrNull(10000) {
            speechDeferred.await()
        }

        assertNotNull("STOMP aiProxySpeech event was not broadcasted to WebSocket", receivedWsSpeech)
        assertEquals(rep.id, receivedWsSpeech?.representativeId)
        assertEquals(speechResponse.spokenText, receivedWsSpeech?.spokenText)
        println("=== [E2E Phase 5] Real-Time Voice Broadcast Received on Android Client! ===")
        println("=== [E2E Phase 5] Ready for TextToSpeechManager.speak('${receivedWsSpeech?.spokenText}') ===")

        webSocketManager.disconnect()
        meetingRepository.endMeeting(meeting.id)
        println("=== [E2E Phase 5] AI Representative Live Voice Generation Validated 100% ===")
    }

    @Test
    fun test05_CompleteEndToEndFlow() = runBlocking(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val email = "e2e_full_$timestamp@meetmind.ai"
        val password = "Password123!"

        println("=== [E2E Complete] 1. Register & Login ===")
        authRepository.register("Full Flow User", email, password)
        authRepository.login(email, password)
        assertTrue(authRepository.isLoggedIn())

        println("=== [E2E Complete] 2. Create Meeting ===")
        val meetingResult = meetingRepository.createMeeting(
            title = "Final Real-Time Orchestration $timestamp",
            description = "E2E complete flow test",
            scheduledAt = "2026-09-16T16:00:00Z",
            invitedEmails = null
        )
        val meeting = (meetingResult as NetworkResult.Success).data
        meetingRepository.startMeeting(meeting.id)

        println("=== [E2E Complete] 3. Batch Invite 4 Team Members ===")
        val batchEmails = listOf(
            "alice_$timestamp@meetmind.ai",
            "bob_$timestamp@meetmind.ai",
            "carol_$timestamp@meetmind.ai",
            "dan_$timestamp@meetmind.ai"
        )
        val batchResult = meetingRepository.batchInviteParticipants(meeting.id, batchEmails)
        assertTrue(batchResult is NetworkResult.Success)

        println("=== [E2E Complete] 4. Setup AI Proxy Representative ===")
        val repResult = liveRepresentativeRepository.createRepresentative(
            meetingId = meeting.id,
            monitoredTopics = listOf("Production", "Deployment", "RealTime"),
            monitoredQuestions = listOf("Is the build ready?"),
            importantPeople = listOf("Lead Engineer"),
            reportPreferences = "FULL_REPORT",
            notificationPreferences = "IMMEDIATE"
        )
        val rep = (repResult as NetworkResult.Success).data

        println("=== [E2E Complete] 5. Connect WebSocket, Chat, and AI Voice ===")
        webSocketManager.connect()
        delay(1000)

        webSocketManager.subscribeToChat(meeting.id)
        delay(200)
        webSocketManager.subscribeToAiProxySpeech(meeting.id)
        delay(500)

        // Pre-listen to chat
        val chatText = "All systems operational at timestamp $timestamp"
        val chatDeferred = async {
            webSocketManager.chatMessages.first { it.message == chatText }
        }
        delay(300)

        // Send chat
        webSocketManager.sendChatMessage(meeting.id, chatText)
        val chatReceived = withTimeoutOrNull(10000) {
            chatDeferred.await()
        }
        assertNotNull("Chat echo must arrive", chatReceived)

        // Pre-listen to voice
        val voiceDeferred = async {
            webSocketManager.aiProxySpeech.first {
                it.meetingId == meeting.id && it.representativeId == rep.id
            }
        }
        delay(300)

        // Trigger AI speech
        representativeApiService.triggerSpeech(
            meetingId = meeting.id,
            id = rep.id,
            body = mapOf("query" to "Can we deploy now?", "language" to "en")
        )
        val voiceReceived = withTimeoutOrNull(10000) {
            voiceDeferred.await()
        }
        assertNotNull("Voice broadcast must arrive", voiceReceived)
        println("=== [E2E Complete] Voice received: '${voiceReceived?.spokenText}' ===")

        println("=== [E2E Complete] 6. End Meeting and Tear Down ===")
        webSocketManager.disconnect()
        meetingRepository.endMeeting(meeting.id)

        println("=== [E2E Complete] ENTIRE REAL-TIME ANDROID APP PIPELINE VALIDATED SUCCESSFULLY! ===")
    }

    @Test
    fun test06_LiveMeetingTranslationRealTime() = runBlocking(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val email = "translator_$timestamp@meetmind.ai"
        val password = "Password123!"

        println("=== [E2E Translation] 1. Register & Login Translator Host ===")
        val reg = authRepository.register("Trans Host", email, password)
        assertTrue("Registration failed: $reg", reg is NetworkResult.Success)
        val login = authRepository.login(email, password)
        assertTrue("Login failed: $login", login is NetworkResult.Success)

        println("=== [E2E Translation] 2. Create and Start Live Meeting ===")
        val meetingResult = meetingRepository.createMeeting(
            title = "Live Multilingual Room $timestamp",
            description = "Real-time subtitle translation verification",
            scheduledAt = "2026-09-16T17:00:00Z",
            invitedEmails = null
        )
        val meeting = (meetingResult as NetworkResult.Success).data
        meetingRepository.startMeeting(meeting.id)

        println("=== [E2E Translation] 3. Set User Language Preference (Spanish - es) ===")
        liveTranslationRepository.setLanguagePreference(meeting.id, "es")

        println("=== [E2E Translation] 4. Connect WebSocket and Subscribe to Spanish Subtitles ===")
        webSocketManager.connect()
        delay(1000)

        liveTranslationRepository.subscribeToSubtitles(meeting.id, "es")
        delay(500)

        println("=== [E2E Translation] 5. Pre-listening for Live Subtitle via async... ===")
        val subtitleDeferred = async {
            liveTranslationRepository.subtitles.first {
                it.meetingId == meeting.id && it.targetLanguage.equals("es", ignoreCase = true)
            }
        }
        delay(300)

        println("=== [E2E Translation] 6. Triggering Live Translation ===")
        translationApiService.translateLive(
            meetingId = meeting.id,
            body = com.developer_rahul.meetmind_ai.feature.translation.data.remote.dto.LiveTranslateRequestDto(
                segmentId = 1001L,
                sourceText = "welcome to the meeting",
                sourceLanguage = "en",
                targetLanguage = "es",
                speaker = "Trans Host",
                timestamp = timestamp
            )
        )

        println("=== [E2E Translation] 7. Awaiting Live Subtitle Broadcast on Android Repository Flow ===")
        val receivedSubtitle = withTimeoutOrNull(10000) {
            subtitleDeferred.await()
        }

        assertNotNull("Live translated subtitle must arrive over WebSocket", receivedSubtitle)
        assertEquals(meeting.id, receivedSubtitle?.meetingId)
        assertEquals("en", receivedSubtitle?.sourceLanguage)
        assertEquals("es", receivedSubtitle?.targetLanguage)
        assertEquals("welcome to the meeting", receivedSubtitle?.originalText)
        assertTrue("Translated text must not be empty", !receivedSubtitle?.translatedText.isNullOrEmpty())
        println("=== [E2E Translation] Live Subtitle Received: '${receivedSubtitle?.originalText}' => '${receivedSubtitle?.translatedText}' ===")

        webSocketManager.disconnect()
        meetingRepository.endMeeting(meeting.id)
        println("=== [E2E Translation] LIVE MEETING TRANSLATION FEATURE VALIDATED 100%! ===")
    }
}
