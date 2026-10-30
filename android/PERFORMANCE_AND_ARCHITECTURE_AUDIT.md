# ⚡ **MeetMind_AI — Performance & Architecture Audit**
## **Section 11: Performance Optimization, Memory & Architecture**

---

### **1. Performance & Recomposition Analysis**

1. **Jetpack Compose Recomposition in Live Video Call Room (`LiveMeetingScreen.kt`)**:
   * **Observation**: `LiveMeetingScreen` observes `callViewModel.uiState` (`LiveMeetingUiState`), `meetingViewModel.uiState`, `translationViewModel.uiState`, and `translationViewModel.subtitles`.
   * **Optimization Verified**:
     * `ParticipantVideoTile` uses `derivedStateOf` and stable `Participant` data keys inside `LazyVerticalGrid` (`itemsIndexed(participantsList)`).
     * `VideoRenderer` wraps native `SurfaceViewRenderer` using `DisposableEffect(videoTrack, renderer)`, ensuring hardware EGL video sinks are added and removed cleanly without leaking native C++ WebRTC pointers.

2. **Hardware Video Encoder & Decoder Pooling**:
   * `WebRtcManager` instantiates `DefaultVideoEncoderFactory` and `DefaultVideoDecoderFactory` with shared `eglBaseContext`.
   * Video capture formats are dynamically scaled down (720p -> 480p -> 360p) as participant count $N$ increases via `applyAdaptiveScalabilityControls()`.
   * **Performance Result**: Prevents GPU/CPU thermal throttling and eliminates frame drops in multi-participant calls.

3. **Background Media Upload Efficiency (`RecordingUploadWorker`)**:
   * Offloads video/audio file uploads to Android `WorkManager` using `OneTimeWorkRequestBuilder` with `NetworkType.CONNECTED` constraints and exponential backoff retries.
   * Prevents UI thread blocking or HTTP socket timeouts during large media transfers.

---

### **2. Memory & Coroutine Lifecycle Management Audit**

1. **Coroutine Scope Cleanup**:
   * `MeetingCallRepository`: Uses `CoroutineScope(Dispatchers.IO + SupervisorJob())`. Clears `peerConnections`, `pendingIceCandidates`, `screenSenders`, and disposes `webRtcManager` on `leaveMeeting()`.
   * `MeetingWebSocketManager`: Disconnects `stompClient` and clears subscription maps on `disconnect()`.
   * `LiveMeetingViewModel`: Stops `TextToSpeechManager` and unsubscribes from AI proxy speech in `onCleared()`.
   * `MeetMindFcmService`: Cancels background `SupervisorJob()` in `onDestroy()`.

2. **ViewModel Memory Retention**:
   * ViewModels are tied to Navigation backstack entries via `BaseViewModelFactory`.
   * When navigating back from a meeting room, `LiveMeetingViewModel.onCleared()` executes automatically, preventing WebRTC memory leaks.

---
*End of Section 11: Performance & Architecture Audit.*
