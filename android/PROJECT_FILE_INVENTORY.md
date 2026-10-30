# 📁 **MeetMind_AI — Project File Inventory**
## **Section 2: Repository Structure & File Inventory**

---

### **1. Module & Directory Structure**

The **MeetMind_AI** project is structured as a single top-level Android application module (`:app`) using a modern **Feature-by-Package** domain architecture:

```
D:/MeetMind_AI/android/
├── build.gradle.kts (Root Build Configuration)
├── settings.gradle.kts (Gradle Subproject Inclusions)
├── app/
│   ├── build.gradle.kts (Module Build Configuration & Dependencies)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── res/
│       │   │   └── values/ (colors.xml, strings.xml, themes.xml)
│       │   └── java/com/developer_rahul/meetmind_ai/
│       │       ├── MainActivity.kt
│       │       ├── MainViewModel.kt
│       │       ├── MeetMindApplication.kt
│       │       ├── core/
│       │       │   ├── designsystem/ (Color, Theme, Type, Shapes, Spacing, Modifiers)
│       │       │   ├── di/ (AppContainer)
│       │       │   ├── media/ (audio, recording, speech, tts)
│       │       │   ├── navigation/ (AppNavigation, Screen)
│       │       │   ├── network/ (di, error, fcm, interceptor, model, token, websocket, stomp)
│       │       │   ├── ui/ (MeetMindViewModel, SystemStates, ViewModelFactory, components)
│       │       │   └── webrtc/ (WebRtcManager, WebRtcApiService, dto)
│       │       └── feature/
│       │           ├── ai/ (presentation)
│       │           ├── auth/ (data, domain, presentation)
│       │           ├── chat/ (data, domain, presentation)
│       │           ├── home/ (presentation)
│       │           ├── intelligence/ (data, domain, presentation)
│       │           ├── meetingroom/ (data, domain, presentation)
│       │           ├── meetings/ (data, domain, presentation)
│       │           ├── notifications/ (data, domain, presentation)
│       │           ├── onboarding/ (presentation)
│       │           ├── participants/ (presentation)
│       │           ├── permissions/ (presentation)
│       │           ├── profile/ (data, remote, presentation)
│       │           ├── recording/ (data, domain, presentation)
│       │           ├── representative/ (data, domain, presentation)
│       │           ├── settings/ (presentation)
│       │           ├── splash/ (presentation)
│       │           ├── transcript/ (data, domain, presentation)
│       │           └── translation/ (data, domain, presentation)
│       └── test/ (Unit Tests & Integration Tests)
```

---

### **2. Complete Source File Inventory**

| Category | File Path | Lines | Purpose / Description | Inspection Status |
| :--- | :--- | :---: | :--- | :---: |
| **Root Gradle** | `build.gradle.kts` | 12 | Top-level build script, Android application & Kotlin Compose plugins | **Inspected** |
| **Settings** | `settings.gradle.kts` | 24 | Plugin repositories and `:app` module inclusion | **Inspected** |
| **App Gradle** | `app/build.gradle.kts` | 102 | App dependencies, SDK targets (35/37), BASE_URL configuration, WebRTC, Compose, Retrofit, Firebase BOM | **Inspected** |
| **Manifest** | `app/src/main/AndroidManifest.xml` | 65 | App entry points, permissions (`CAMERA`, `RECORD_AUDIO`, `FOREGROUND_SERVICE`), services (`RecordingService`, `MeetMindFcmService`) | **Inspected** |
| **Application** | `com/.../MeetMindApplication.kt` | 20 | Singleton application initialization, `AppContainer` instantiation | **Inspected** |
| **Activity** | `com/.../MainActivity.kt` | 55 | Edge-to-edge root activity, FCM intent deep-linking, unauthorized session listener | **Inspected** |
| **ViewModel** | `com/.../MainViewModel.kt` | 25 | App-level ViewModel monitoring token provider events and WebSocket lifecycle | **Inspected** |

#### **A. Core Infrastructure (`com/developer_rahul/meetmind_ai/core`)**

| File Path | Lines | Category | Purpose | Status |
| :--- | :---: | :--- | :--- | :---: |
| `designsystem/Color.kt` | 75 | Design Tokens | Color definitions, dynamic light/dark theme colors, glassmorphism tokens, gradients | **Inspected** |
| `designsystem/Theme.kt` | 60 | Design Tokens | `MeetMindTheme`, Material 3 Dark/Light color schemes, status/navigation bar inset colors | **Inspected** |
| `designsystem/Type.kt` | 65 | Design Tokens | Typography scale (`displayLarge`, `titleLarge`, `bodyLarge`, `labelSmall`) | **Inspected** |
| `designsystem/Shapes.kt` | 20 | Design Tokens | Corner shapes (`Small`, `Medium`, `Large`, `ExtraLarge`) | **Inspected** |
| `designsystem/Spacing.kt` | 25 | Design Tokens | Padding and margin spacing constants (`xs`, `s`, `m`, `l`, `xl`) | **Inspected** |
| `designsystem/Modifiers.kt` | 45 | Custom Modifiers | `bounceClick()`, `glassmorphism()`, `aiGlow()`, `pulse()` animations | **Inspected** |
| `di/AppContainer.kt` | 185 | Dependency Injection | Manual Service Locator creating Retrofit, WebSocket manager, WebRTC, TTS, STT, and Repositories | **Inspected** |
| `media/audio/AudioDeviceManager.kt` | 70 | Audio Routing | Speaker, Earpiece, Wired Headset, and Bluetooth SCO audio output switcher | **Inspected** |
| `media/recording/AudioExtractor.kt` | 50 | Media Utility | Audio extraction from video recordings | **Inspected** |
| `media/recording/RecordingManager.kt` | 25 | Interface | Contract for local media recording lifecycle | **Inspected** |
| `media/recording/RealRecordingManager.kt` | 120 | Media Engine | Android `MediaRecorder` wrapper for high-definition meeting audio capture | **Inspected** |
| `media/recording/RecordingService.kt` | 75 | Foreground Service | Foreground service (`mediaProjection|microphone`) for screen capture & recording | **Inspected** |
| `media/recording/RecordingUploadManager.kt` | 65 | Upload Engine | Manages enqueuing upload tasks to `WorkManager` | **Inspected** |
| `media/speech/LiveSpeechProvider.kt` | 90 | Speech Recognition | On-device STT using Android `SpeechRecognizer` | **Inspected** |
| `media/tts/TextToSpeechManager.kt` | 115 | Speech Synthesis | Android `TextToSpeech` engine wrapper for multilingual voice output | **Inspected** |
| `navigation/AppNavigation.kt` | 315 | Navigation Graph | `NavHost` containing all 30 screen routes and argument transitions | **Inspected** |
| `navigation/Screen.kt` | 85 | Sealed Class | Route definitions and argument builder functions for all 30 screens | **Inspected** |
| `network/di/NetworkModule.kt` | 50 | Network Setup | Retrofit, OkHttpClient, KotlinX Serialization converter factory setup | **Inspected** |
| `network/error/ErrorMapper.kt` | 40 | Network Error | Converts HTTP exceptions and network timeouts into `MeetMindError` domain objects | **Inspected** |
| `network/fcm/MeetMindFcmService.kt` | 85 | Push Service | Handles FCM device tokens and incoming push notifications with deep-links | **Inspected** |
| `network/interceptor/AuthInterceptor.kt` | 35 | Interceptor | Injects `Bearer <JWT>` tokens into OkHttp requests and triggers 401 events | **Inspected** |
| `network/interceptor/FallbackNetworkInterceptor.kt` | 75 | Interceptor | Zero-delay failover switching between local LAN/USB endpoints and Cloud backend | **Inspected** |
| `network/interceptor/HostManager.kt` | 25 | State Holder | Active backend host state manager | **Inspected** |
| `network/model/MeetMindError.kt` | 30 | Domain Model | Standardized error types (`UNAUTHORIZED`, `NETWORK_ERROR`, `SERVER_ERROR`, `NOT_FOUND`) | **Inspected** |
| `network/model/NetworkResult.kt` | 20 | Sealed Class | Generic result wrapper (`Success`, `Error`, `Loading`) | **Inspected** |
| `network/token/TokenProvider.kt` | 20 | Interface | Token storage contract | **Inspected** |
| `network/token/SecureTokenProvider.kt` | 65 | Security | Encrypted storage using `EncryptedSharedPreferences` | **Inspected** |
| `network/websocket/MeetingWebSocketManager.kt` | 460 | Realtime Manager | Full-duplex STOMP WebSocket manager for signaling, chat, AI, translation, notifications | **Inspected** |
| `network/websocket/model/MeetingEventDto.kt` | 20 | DTO | WebSocket event DTO (`MEETING_STARTED`, `MEETING_ENDED`, `PARTICIPANT_JOINED`) | **Inspected** |
| `network/websocket/model/SignalingMessageDto.kt` | 20 | DTO | WebRTC signaling DTO (`JOIN`, `LEAVE`, `OFFER`, `ANSWER`, `ICE_CANDIDATE`, `MEDIA_UPDATE`) | **Inspected** |
| `network/websocket/stomp/StompClient.kt` | 135 | STOMP Client | Custom WebSocket STOMP frame framing, subscribing, sending, and queueing client | **Inspected** |
| `network/websocket/stomp/StompFrame.kt` | 45 | STOMP Parser | STOMP protocol frame string builder and parser | **Inspected** |
| `ui/MeetMindViewModel.kt` | 30 | Base Class | Standardized ViewModel base class | **Inspected** |
| `ui/SystemStates.kt` | 25 | UI Component | Standardized full-screen loading, error, and empty state composables | **Inspected** |
| `ui/ViewModelFactory.kt` | 170 | Factory | Factory creating ViewModel instances with dependency injection parameters | **Inspected** |
| `ui/components/CoreComponents.kt` | 215 | Reusable UI | `MeetMindButton`, `MeetMindTextField`, `MeetMindCard`, `StatusChip`, `SectionHeader`, `GradientFloatingActionButton` | **Inspected** |
| `ui/components/DialogComponents.kt` | 120 | Reusable UI | `LeaveMeetingDialog`, `HostLeaveOrEndDialog`, `RecordingConsentDialog` | **Inspected** |
| `ui/components/SelectionComponents.kt` | 80 | Reusable UI | Custom radio buttons, checkboxes, and segmented selectors | **Inspected** |
| `ui/components/WebRtcComponents.kt` | 60 | Reusable UI | Compose `VideoRenderer` wrapper bridging native WebRTC `SurfaceViewRenderer` | **Inspected** |
| `webrtc/WebRtcManager.kt` | 210 | WebRTC Engine | Native `PeerConnectionFactory`, camera capturer, screen capturer, EGL context manager | **Inspected** |
| `webrtc/data/remote/WebRtcApiService.kt` | 15 | Retrofit API | `GET api/webrtc/ice-servers` for fetching TURN/STUN credentials | **Inspected** |
| `webrtc/data/remote/dto/IceServerDto.kt` | 15 | DTO | ICE server configuration DTO | **Inspected** |

#### **B. Feature Modules (`com/developer_rahul/meetmind_ai/feature`)**

| Feature | Key Source Files | Description | Status |
| :--- | :--- | :--- | :---: |
| **`ai`** | `AiDashboardScreen.kt`, `MeetingIntelligenceViewModel.kt` | Post-meeting AI intelligence studio, action items, speaker stats | **Inspected** |
| **`auth`** | `AuthApiService.kt`, `AuthRepository.kt`, `RealAuthRepository.kt`, `AuthViewModel.kt`, `LoginScreen.kt`, `RegisterScreen.kt`, `WelcomeScreen.kt`, `EmailVerificationScreen.kt`, `ForgotPasswordScreen.kt`, `ResetPasswordScreen.kt`, `SessionExpiredScreen.kt` | Authentication lifecycle, JWT token issuance, password recovery, session expiration | **Inspected** |
| **`chat`** | `ChatApiService.kt`, `ChatRepository.kt`, `RealChatRepository.kt`, `ChatMapper.kt`, `ChatMessage.kt`, `ChatScreen.kt`, `ChatViewModel.kt` | In-meeting live chat UI, message history, STOMP real-time messaging, optimistic send | **Inspected** |
| **`home`** | `DashboardScreen.kt`, `MainScreen.kt` | Main bottom-nav container, upcoming meetings, quick action cards, stats | **Inspected** |
| **`intelligence`** | `IntelligenceApiService.kt`, `IntelligenceRepository.kt`, `RealIntelligenceRepository.kt`, `SummaryDetailResponseDto.kt` | REST + WebSocket AI summary data layer | **Inspected** |
| **`meetingroom`**| `MeetingCallRepository.kt`, `LiveMeetingScreen.kt`, `LiveMeetingViewModel.kt`, `MeetingCallEvent.kt`, `ParticipantMediaState.kt` | WebRTC P2P video call room, screen share, active speaker highlighting, Perfect Negotiator | **Inspected** |
| **`meetings`** | `MeetingApiService.kt`, `MeetingRepository.kt`, `RealMeetingRepository.kt`, `MeetingViewModel.kt`, `MeetingsScreen.kt`, `CreateMeetingScreen.kt`, `EditMeetingScreen.kt`, `MeetingDetailsScreen.kt`, `PreJoinMeetingScreen.kt`, `CalendarScreen.kt`, `ComprehensiveReportScreen.kt`, `MeetingEndedScreen.kt`, `SearchMeetingsScreen.kt` | Scheduled & instant meeting CRUD, invitation code share, calendar view, comprehensive report | **Inspected** |
| **`notifications`**| `NotificationApiService.kt`, `NotificationRepository.kt`, `RealNotificationRepository.kt`, `NotificationsScreen.kt`, `NotificationViewModel.kt` | In-app notification center & push notification history | **Inspected** |
| **`onboarding`**| `OnboardingScreen.kt` | Introductory feature slides & permissions transition | **Inspected** |
| **`participants`**| `ParticipantsScreen.kt`, `AddParticipantScreen.kt` | Active call roster display, host privileges, broadcast email invitation | **Inspected** |
| **`permissions`** | `PermissionScreen.kt` | Camera, microphone, notification runtime permission requests | **Inspected** |
| **`profile`** | `UserApiService.kt`, `ProfileScreen.kt`, `EditProfileScreen.kt`, `ProfileViewModel.kt` | User profile avatar, details, meeting stats, account edit | **Inspected** |
| **`recording`** | `RecordingApiService.kt`, `RecordingRepository.kt`, `RealRecordingRepository.kt`, `RecordingUploadWorker.kt`, `MeetingRecordingsScreen.kt`, `RecordingPlayerScreen.kt` | Local meeting recording, background WorkManager cloud upload, video/audio player | **Inspected** |
| **`representative`**| `RepresentativeApiService.kt`, `LiveRepresentativeRepository.kt`, `RealLiveRepresentativeRepository.kt`, `AiRepConfigScreen.kt`, `AiRepDashboardScreen.kt`, `AiRepStatusScreen.kt`, `AiRepActiveScreen.kt`, `RepresentativeReportScreen.kt` | Autonomous AI Proxy Agent configuration, active status monitoring, representation reports | **Inspected** |
| **`settings`** | `SettingsScreen.kt` | App settings, theme toggle, notification preferences | **Inspected** |
| **`splash`** | `SplashScreen.kt`, `SplashViewModel.kt`, `SplashUiState.kt` | Initial splash screen, token presence check, destination auto-routing | **Inspected** |
| **`transcript`** | `TranscriptApiService.kt`, `TranscriptRepository.kt`, `RealTranscriptRepository.kt`, `TranscriptScreen.kt`, `TranscriptViewModel.kt` | Speech-to-text transcript timeline, speaker attribution, search | **Inspected** |
| **`translation`** | `TranslationApiService.kt`, `LiveTranslationRepository.kt`, `RealLiveTranslationRepository.kt`, `LiveTranslationScreen.kt`, `LiveTranslationViewModel.kt` | Real-time subtitle translation overlay, TTS spoken translation output | **Inspected** |

---

### **3. Third-Party Library Inventory**

* **Jetpack Compose BOM**: `2026.02.01` (`androidx.compose.material3`, `ui`, `ui-graphics`, `ui-tooling-preview`)
* **Navigation Compose**: `2.10.0` (`androidx.navigation:navigation-compose`)
* **WebRTC Engine**: `111.0.0` (`com.dafruits:webrtc`)
* **Retrofit**: `2.11.0` (`com.squareup.retrofit2:retrofit`, `converter-kotlinx-serialization`)
* **OkHttp3**: `4.12.0` (`com.squareup.okhttp3:okhttp`, `logging-interceptor`)
* **KotlinX Serialization**: `1.6.3` (`org.jetbrains.kotlinx:kotlinx-serialization-json`)
* **Security Crypto**: `1.1.0-alpha06` (`androidx.security:security-crypto`)
* **WorkManager**: `2.10.0` (`androidx.work:work-runtime-ktx`)
* **Firebase BOM**: `33.1.2` (`firebase-messaging-ktx`, `firebase-analytics-ktx`)

---
*End of Section 2: Repository Structure & File Inventory.*
