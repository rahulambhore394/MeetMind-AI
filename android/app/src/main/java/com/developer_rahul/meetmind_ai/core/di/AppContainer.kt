package com.developer_rahul.meetmind_ai.core.di

import android.content.Context
import com.developer_rahul.meetmind_ai.core.network.di.NetworkModule
import com.developer_rahul.meetmind_ai.core.network.token.SecureTokenProvider
import com.developer_rahul.meetmind_ai.core.network.token.TokenProvider
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import com.developer_rahul.meetmind_ai.core.webrtc.WebRtcManager
import com.developer_rahul.meetmind_ai.core.webrtc.data.remote.WebRtcApiService
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.AuthApiService
import com.developer_rahul.meetmind_ai.feature.auth.data.repository.AuthRepository
import com.developer_rahul.meetmind_ai.feature.auth.data.repository.RealAuthRepository
import com.developer_rahul.meetmind_ai.feature.chat.data.remote.ChatApiService
import com.developer_rahul.meetmind_ai.feature.chat.data.repository.ChatRepository
import com.developer_rahul.meetmind_ai.feature.chat.data.repository.RealChatRepository
import com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.IntelligenceApiService
import com.developer_rahul.meetmind_ai.feature.intelligence.data.repository.IntelligenceRepository
import com.developer_rahul.meetmind_ai.feature.intelligence.data.repository.RealIntelligenceRepository
import com.developer_rahul.meetmind_ai.feature.meetingroom.data.repository.MeetingCallRepository
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.MeetingApiService
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.MeetingRepository
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.PresenceRepository
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.RealMeetingRepository
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.RealPresenceRepository
import com.developer_rahul.meetmind_ai.core.media.recording.RealRecordingManager
import com.developer_rahul.meetmind_ai.core.media.recording.RecordingManager
import com.developer_rahul.meetmind_ai.core.media.recording.RecordingUploadManager
import com.developer_rahul.meetmind_ai.feature.recording.data.remote.RecordingApiService
import com.developer_rahul.meetmind_ai.feature.recording.data.repository.RecordingRepository
import com.developer_rahul.meetmind_ai.feature.recording.data.repository.RealRecordingRepository
import com.developer_rahul.meetmind_ai.feature.transcript.data.remote.TranscriptApiService
import com.developer_rahul.meetmind_ai.feature.transcript.data.repository.RealTranscriptRepository
import com.developer_rahul.meetmind_ai.feature.transcript.domain.repository.TranscriptRepository
import com.developer_rahul.meetmind_ai.feature.translation.data.repository.RealLiveTranslationRepository
import com.developer_rahul.meetmind_ai.feature.translation.domain.repository.LiveTranslationRepository
import com.developer_rahul.meetmind_ai.feature.representative.data.remote.RepresentativeApiService
import com.developer_rahul.meetmind_ai.feature.representative.data.repository.RealLiveRepresentativeRepository
import com.developer_rahul.meetmind_ai.feature.representative.domain.repository.LiveRepresentativeRepository
import com.developer_rahul.meetmind_ai.feature.notifications.data.remote.NotificationApiService
import com.developer_rahul.meetmind_ai.feature.notifications.data.repository.RealNotificationRepository
import com.developer_rahul.meetmind_ai.feature.notifications.domain.repository.NotificationRepository
import com.developer_rahul.meetmind_ai.core.media.speech.LiveSpeechProvider
import okhttp3.OkHttpClient
import org.webrtc.EglBase
import retrofit2.Retrofit

class AppContainer(context: Context) {

    private val eglBase: EglBase by lazy {
        EglBase.create()
    }

    val tokenProvider: TokenProvider by lazy {
        SecureTokenProvider(context)
    }

    private val okHttpClient: OkHttpClient by lazy {
        NetworkModule.provideOkHttpClient(tokenProvider)
    }

    val retrofit: Retrofit by lazy {
        NetworkModule.provideRetrofit(okHttpClient)
    }

    private val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val authRepository: AuthRepository by lazy {
        RealAuthRepository(authApiService, tokenProvider)
    }

    private val meetingApiService: MeetingApiService by lazy {
        retrofit.create(MeetingApiService::class.java)
    }

    val meetingRepository: MeetingRepository by lazy {
        RealMeetingRepository(meetingApiService)
    }

    val meetingWebSocketManager: MeetingWebSocketManager by lazy {
        MeetingWebSocketManager(okHttpClient, tokenProvider)
    }

    private val chatApiService: ChatApiService by lazy {
        retrofit.create(ChatApiService::class.java)
    }

    val chatRepository: ChatRepository by lazy {
        RealChatRepository(chatApiService, meetingWebSocketManager, tokenProvider)
    }

    val presenceRepository: PresenceRepository by lazy {
        RealPresenceRepository(meetingWebSocketManager)
    }

    private val recordingApiService: RecordingApiService by lazy {
        retrofit.create(RecordingApiService::class.java)
    }

    val recordingRepository: RecordingRepository by lazy {
        RealRecordingRepository(recordingApiService, meetingWebSocketManager)
    }

    val recordingManager: RecordingManager by lazy {
        RealRecordingManager(context)
    }

    val recordingUploadManager: RecordingUploadManager by lazy {
        RecordingUploadManager(context)
    }

    private val intelligenceApiService: IntelligenceApiService by lazy {
        retrofit.create(IntelligenceApiService::class.java)
    }

    val intelligenceRepository: IntelligenceRepository by lazy {
        RealIntelligenceRepository(intelligenceApiService, meetingWebSocketManager)
    }

    private val transcriptApiService: TranscriptApiService by lazy {
        retrofit.create(TranscriptApiService::class.java)
    }

    val transcriptRepository: TranscriptRepository by lazy {
        RealTranscriptRepository(transcriptApiService)
    }

    val liveTranslationRepository: LiveTranslationRepository by lazy {
        RealLiveTranslationRepository(meetingWebSocketManager)
    }

    private val representativeApiService: RepresentativeApiService by lazy {
        retrofit.create(RepresentativeApiService::class.java)
    }

    val liveRepresentativeRepository: LiveRepresentativeRepository by lazy {
        RealLiveRepresentativeRepository(representativeApiService, meetingWebSocketManager)
    }

    private val notificationApiService: NotificationApiService by lazy {
        retrofit.create(NotificationApiService::class.java)
    }

    val notificationRepository: NotificationRepository by lazy {
        RealNotificationRepository(notificationApiService, meetingWebSocketManager)
    }

    val liveSpeechProvider: LiveSpeechProvider by lazy {
        LiveSpeechProvider(context)
    }

    private val webRtcApiService: WebRtcApiService by lazy {
        retrofit.create(WebRtcApiService::class.java)
    }

    val webRtcManager: WebRtcManager by lazy {
        WebRtcManager(context, eglBase.eglBaseContext)
    }

    val meetingCallRepository: MeetingCallRepository by lazy {
        MeetingCallRepository(webRtcApiService, meetingWebSocketManager, webRtcManager)
    }
}
