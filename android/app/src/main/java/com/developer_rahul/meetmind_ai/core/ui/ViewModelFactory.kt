package com.developer_rahul.meetmind_ai.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.developer_rahul.meetmind_ai.MeetMindApplication
import com.developer_rahul.meetmind_ai.MainViewModel
import com.developer_rahul.meetmind_ai.feature.auth.presentation.AuthViewModel
import com.developer_rahul.meetmind_ai.feature.chat.presentation.ChatViewModel
import com.developer_rahul.meetmind_ai.feature.meetingroom.presentation.LiveMeetingViewModel
import com.developer_rahul.meetmind_ai.feature.meetings.presentation.MeetingViewModel
import com.developer_rahul.meetmind_ai.feature.splash.presentation.SplashViewModel
import com.developer_rahul.meetmind_ai.feature.transcript.presentation.TranscriptViewModel
import com.developer_rahul.meetmind_ai.feature.translation.presentation.LiveTranslationViewModel
import com.developer_rahul.meetmind_ai.feature.ai.presentation.MeetingIntelligenceViewModel
import com.developer_rahul.meetmind_ai.feature.representative.presentation.ConfigureAiRepViewModel
import com.developer_rahul.meetmind_ai.feature.representative.presentation.AiRepStatusViewModel
import com.developer_rahul.meetmind_ai.feature.representative.presentation.RepresentativeReportViewModel
import com.developer_rahul.meetmind_ai.feature.representative.presentation.AiRepDashboardViewModel
import com.developer_rahul.meetmind_ai.feature.notifications.presentation.NotificationViewModel

@Suppress("UNCHECKED_CAST")
class BaseViewModelFactory(
    private val application: MeetMindApplication,
    private val meetingId: Long? = null,
    private val representativeId: Long? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val container = application.container
        
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(container.authRepository) as T
            }
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> {
                SplashViewModel(container.authRepository) as T
            }
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(container.tokenProvider, container.meetingWebSocketManager) as T
            }
            modelClass.isAssignableFrom(MeetingViewModel::class.java) -> {
                MeetingViewModel(
                    container.meetingRepository, 
                    container.meetingWebSocketManager, 
                    container.presenceRepository,
                    container.recordingRepository,
                    container.intelligenceRepository,
                    container.recordingManager,
                    container.recordingUploadManager
                ) as T
            }
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> {
                ChatViewModel(container.chatRepository, meetingId ?: -1L) as T
            }
            modelClass.isAssignableFrom(LiveMeetingViewModel::class.java) -> {
                LiveMeetingViewModel(container.meetingCallRepository, meetingId ?: -1L) as T
            }
            modelClass.isAssignableFrom(TranscriptViewModel::class.java) -> {
                TranscriptViewModel(container.transcriptRepository, meetingId ?: -1L) as T
            }
            modelClass.isAssignableFrom(LiveTranslationViewModel::class.java) -> {
                LiveTranslationViewModel(
                    container.liveTranslationRepository,
                    container.liveSpeechProvider,
                    meetingId ?: -1L
                ) as T
            }
            modelClass.isAssignableFrom(MeetingIntelligenceViewModel::class.java) -> {
                MeetingIntelligenceViewModel(
                    container.intelligenceRepository,
                    meetingId ?: -1L
                ) as T
            }
            modelClass.isAssignableFrom(ConfigureAiRepViewModel::class.java) -> {
                ConfigureAiRepViewModel(
                    container.meetingRepository,
                    container.liveRepresentativeRepository
                ) as T
            }
            modelClass.isAssignableFrom(AiRepStatusViewModel::class.java) -> {
                AiRepStatusViewModel(
                    meetingId ?: -1L,
                    container.liveRepresentativeRepository
                ) as T
            }
            modelClass.isAssignableFrom(RepresentativeReportViewModel::class.java) -> {
                RepresentativeReportViewModel(
                    meetingId ?: -1L,
                    representativeId ?: -1L,
                    container.liveRepresentativeRepository
                ) as T
            }
            modelClass.isAssignableFrom(AiRepDashboardViewModel::class.java) -> {
                AiRepDashboardViewModel(
                    container.meetingRepository,
                    container.liveRepresentativeRepository
                ) as T
            }
            modelClass.isAssignableFrom(NotificationViewModel::class.java) -> {
                NotificationViewModel(container.notificationRepository) as T
            }
            modelClass.isAssignableFrom(com.developer_rahul.meetmind_ai.feature.profile.presentation.ProfileViewModel::class.java) -> {
                com.developer_rahul.meetmind_ai.feature.profile.presentation.ProfileViewModel(container.userApiService) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

val ViewModelFactory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MeetMindApplication
        return BaseViewModelFactory(application).create(modelClass, extras)
    }
}

fun provideChatViewModelFactory(meetingId: Long) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MeetMindApplication
        return BaseViewModelFactory(application, meetingId).create(modelClass, extras)
    }
}

fun provideLiveMeetingViewModelFactory(meetingId: Long) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MeetMindApplication
        return BaseViewModelFactory(application, meetingId).create(modelClass, extras)
    }
}

fun provideTranscriptViewModelFactory(meetingId: Long) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MeetMindApplication
        return BaseViewModelFactory(application, meetingId).create(modelClass, extras)
    }
}

fun provideLiveTranslationViewModelFactory(meetingId: Long) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MeetMindApplication
        return BaseViewModelFactory(application, meetingId).create(modelClass, extras)
    }
}

fun provideMeetingIntelligenceViewModelFactory(meetingId: Long) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MeetMindApplication
        return BaseViewModelFactory(application, meetingId).create(modelClass, extras)
    }
}

fun provideConfigureAiRepViewModelFactory() = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MeetMindApplication
        return BaseViewModelFactory(application).create(modelClass, extras)
    }
}

fun provideAiRepStatusViewModelFactory(meetingId: Long) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MeetMindApplication
        return BaseViewModelFactory(application, meetingId).create(modelClass, extras)
    }
}

fun provideRepresentativeReportViewModelFactory(meetingId: Long, representativeId: Long) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MeetMindApplication
        return BaseViewModelFactory(application, meetingId, representativeId).create(modelClass, extras)
    }
}

fun provideAiRepDashboardViewModelFactory() = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MeetMindApplication
        return BaseViewModelFactory(application).create(modelClass, extras)
    }
}
