# 📡 **MeetMind_AI — Android & Backend Integration Audit**
## **Section 9: End-to-End API Contracts, DTO Mapping & Real-Time Sync**

---

### **1. Complete Screen-to-Endpoint Integration Mapping**

| Screen ID | Feature / UI Action | Trigger / Function | Repository Method | Retrofit API Endpoint / WebSocket | Request DTO | Response DTO / Entity |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| **SCR-05** | Login Submit | `AuthViewModel.login` | `RealAuthRepository.login` | `POST api/auth/login` | `LoginRequest` | `LoginResponse` |
| **SCR-06** | Register Submit | `AuthViewModel.register` | `RealAuthRepository.register` | `POST api/auth/register` | `RegisterRequest` | `RegisterResponse` |
| **SCR-12** | Load Dashboard | `MeetingViewModel.loadMeetings` | `RealMeetingRepository.getAllMeetings` | `GET api/meetings` | None | `List<MeetingResponseDto>` |
| **SCR-14** | Join by Code | `MeetingViewModel.joinByCode` | `RealMeetingRepository.getMeetingByCode` | `GET api/meetings/code/{code}` | None | `MeetingResponseDto` |
| **SCR-15** | Create Meeting | `MeetingViewModel.createMeeting` | `RealMeetingRepository.createMeeting` | `POST api/meetings` | `CreateMeetingRequestDto` | `MeetingResponseDto` |
| **SCR-17** | Meeting Details | `MeetingViewModel.loadMeetingDetails`| `RealMeetingRepository.getMeeting` | `GET api/meetings/{id}` | None | `MeetingResponseDto` |
| **SCR-17** | Get Participants | `MeetingViewModel.getParticipants` | `RealMeetingRepository.getParticipants` | `GET api/meetings/{id}/participants` | None | `List<ParticipantResponseDto>` |
| **SCR-17** | Get Recordings | `MeetingViewModel.loadMeetingDetails`| `RealRecordingRepository.listRecordings` | `GET api/meetings/{id}/recordings` | None | `List<RecordingResponseDto>` |
| **SCR-18** | Join Call Pre-Check | `MeetingViewModel.joinMeeting` | `RealMeetingRepository.joinMeeting` | `POST api/meetings/{id}/participants/join` | None | `ParticipantResponseDto` |
| **SCR-19** | Start Call / WebRTC | `LiveMeetingViewModel.joinMeeting` | `MeetingCallRepository.joinMeeting` | `GET api/webrtc/ice-servers` | None | `List<IceServerDto>` |
| **SCR-19** | Signaling Join/Offer | `MeetingCallRepository` | `MeetingWebSocketManager.sendSignalingMessage` | STOMP `/app/meetings/{id}/signaling` | `SignalingMessageDto` | `SignalingMessageDto` |
| **SCR-19** | Live Subtitles | `LiveTranslationViewModel` | `MeetingWebSocketManager.sendTranslationRequest` | STOMP `/app/meetings/{id}/translate` | `LiveTranslateRequestDto` | `LiveTranslationDto` |
| **SCR-19** | AI Proxy Query | `LiveMeetingViewModel` | `MeetingWebSocketManager.sendAiProxyQuery` | STOMP `/app/meetings/{id}/ai-proxy/ask` | JsonObject | `AiProxySpeechDto` |
| **SCR-22** | Comprehensive Report| `ComprehensiveReportScreen` | `RealMeetingRepository.getComprehensiveReport` | `GET api/meetings/{id}/comprehensive-report` | None | `ComprehensiveReportDto` |
| **SCR-24** | Deploy AI Rep | `ConfigureAiRepViewModel` | `RealLiveRepresentativeRepository` | `POST api/representatives` | `CreateRepresentativeRequestDto` | `AiRepresentativeDto` |
| **SCR-26** | Send Chat Message | `ChatViewModel.sendMessage` | `RealChatRepository.sendMessage` | STOMP `/app/meetings/{id}/chat` | JsonObject | `ChatMessageResponseDto` |
| **SCR-27** | Load Transcript | `TranscriptViewModel.loadTranscript`| `RealTranscriptRepository.getTranscript` | `GET api/meetings/{id}/transcript` | None | `MeetingTranscriptDto` |
| **SCR-29** | Upload Recording | `RecordingUploadWorker` | `RealRecordingRepository.uploadRecordingFile` | `POST api/meetings/{id}/recordings/{rId}/upload` | Multipart File | `RecordingResponseDto` |

---

### **2. Network Resiliency & Error Mapping Evaluation**

1. **`FallbackNetworkInterceptor` Failover**:
   * Inspects host of outgoing HTTP requests.
   * If primary endpoint (e.g. LAN IP `http://10.70.43.223:8080/`) fails due to timeout or unreachability, automatically attempts fallback candidates:
     1. `127.0.0.1` (USB adb reverse, ultra-fast local)
     2. `10.0.2.2` (Android Emulator loopback)
     3. `meetmind-backend-s3yy.onrender.com` (Cloud Production SSL fallback)
   * **Evaluation**: Zero-delay failover mechanism guarantees backend connectivity under local development, emulator, and remote cloud environments.

2. **Error Handling (`ErrorMapper.kt`)**:
   * Maps HTTP 401 Unauthorized to `MeetMindError(ErrorType.UNAUTHORIZED)`.
   * Maps HTTP 404 Not Found to `MeetMindError(ErrorType.NOT_FOUND)`.
   * Maps IOException / SocketTimeoutException to `MeetMindError(ErrorType.NETWORK_ERROR)`.
   * **Evaluation**: Clean error categorization across repositories.

3. **STOMP WebSocket Reconnection**:
   * `MeetingWebSocketManager` tracks `reconnectAttempt` and applies exponential backoff delays (`1s`, `2s`, `4s`, `8s`, up to `30s` max).
   * Upon successful reconnection, automatically re-subscribes to all active meeting, chat, signaling, translation, and notification channels.

---
*End of Section 9: Android & Backend Integration Audit.*
