# 📱 **MeetMind_AI — Screen-by-Screen UI Audit**
## **Section 3 & Section 4: Screen Inventory & Detailed Per-Screen Audit**

---

### **1. Screen Inventory & Coverage Matrix**

The application contains **30 distinct user-accessible UI destinations** declared in `Screen.kt` and `AppNavigation.kt`.

| Screen ID | Screen Name | Route Pattern | Source File Path | Entry Point / Type | ViewModel | API / WebSocket Dependencies | Audit Status |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| **SCR-01** | Splash | `splash` | `feature/splash/presentation/SplashScreen.kt` | Composable | `SplashViewModel` | Auth token check | **Audited** |
| **SCR-02** | Onboarding | `onboarding` | `feature/onboarding/presentation/OnboardingScreen.kt` | Composable | N/A | None | **Audited** |
| **SCR-03** | Permissions | `permissions` | `feature/permissions/presentation/PermissionScreen.kt` | Composable | N/A | System permissions | **Audited** |
| **SCR-04** | Welcome | `welcome` | `feature/auth/presentation/WelcomeScreen.kt` | Composable | N/A | None | **Audited** |
| **SCR-05** | Login | `login` | `feature/auth/presentation/LoginScreen.kt` | Composable | `AuthViewModel` | `POST api/auth/login` | **Audited** |
| **SCR-06** | Register | `register` | `feature/auth/presentation/RegisterScreen.kt` | Composable | `AuthViewModel` | `POST api/auth/register` | **Audited** |
| **SCR-07** | Email Verification | `email_verification` | `feature/auth/presentation/EmailVerificationScreen.kt` | Composable | `AuthViewModel` | `POST api/auth/verify-email` | **Audited** |
| **SCR-08** | Forgot Password | `forgot_password` | `feature/auth/presentation/ForgotPasswordScreen.kt` | Composable | `AuthViewModel` | `POST api/auth/forgot-password` | **Audited** |
| **SCR-09** | Reset Password | `reset_password` | `feature/auth/presentation/ResetPasswordScreen.kt` | Composable | `AuthViewModel` | `POST api/auth/reset-password` | **Audited** |
| **SCR-10** | Session Expired | `session_expired` | `feature/auth/presentation/SessionExpiredScreen.kt` | Composable | `AuthViewModel` | None | **Audited** |
| **SCR-11** | Main Container | `dashboard` | `feature/home/presentation/MainScreen.kt` | Composable | `MainViewModel` | Navigation & STOMP WS | **Audited** |
| **SCR-12** | Dashboard | Home Tab | `feature/home/presentation/DashboardScreen.kt` | Composable | `MeetingViewModel` | `GET api/meetings` | **Audited** |
| **SCR-13** | Meetings List | Meetings Tab | `feature/meetings/presentation/MeetingsScreen.kt` | Composable | `MeetingViewModel` | `GET api/meetings` | **Audited** |
| **SCR-14** | Search Meetings | `search_meetings` | `feature/meetings/presentation/SearchMeetingsScreen.kt` | Composable | `MeetingViewModel` | `GET api/meetings/code/{code}` | **Audited** |
| **SCR-15** | Create Meeting | `create_meeting` | `feature/meetings/presentation/CreateMeetingScreen.kt` | Composable | `MeetingViewModel` | `POST api/meetings` | **Audited** |
| **SCR-16** | Edit Meeting | `edit_meeting/{id}` | `feature/meetings/presentation/EditMeetingScreen.kt` | Composable | `MeetingViewModel` | `PUT api/meetings/{id}` | **Audited** |
| **SCR-17** | Meeting Details | `meeting_details/{id}` | `feature/meetings/presentation/MeetingDetailsScreen.kt` | Composable | `MeetingViewModel` | `GET api/meetings/{id}` | **Audited** |
| **SCR-18** | Pre-Join Meeting | `pre_join/{id}` | `feature/meetings/presentation/PreJoinMeetingScreen.kt` | Composable | `MeetingViewModel` | `GET api/meetings/{id}` | **Audited** |
| **SCR-19** | Live Meeting Room | `live_meeting/{id}` | `feature/meetingroom/presentation/LiveMeetingScreen.kt` | Composable | `LiveMeetingViewModel` | WebRTC + STOMP WebSocket | **Audited** |
| **SCR-20** | Meeting Ended | `meeting_ended/{id}/{dur}`| `feature/meetings/presentation/MeetingEndedScreen.kt` | Composable | N/A | Post-meeting summary | **Audited** |
| **SCR-21** | Meeting Summaries | `meeting_summaries` | `feature/meetings/presentation/MeetingSummariesScreen.kt` | Composable | `MeetingViewModel` | `GET api/meetings/reports/all` | **Audited** |
| **SCR-22** | Comprehensive Report| `comprehensive_report/{id}`| `feature/meetings/presentation/ComprehensiveReportScreen.kt`| Composable | N/A | `GET api/meetings/{id}/report` | **Audited** |
| **SCR-23** | AI Representative | `ai_rep_dashboard` | `feature/representative/presentation/AiRepDashboardScreen.kt` | Composable | `AiRepDashboardViewModel`| `GET api/representatives` | **Audited** |
| **SCR-24** | AI Rep Config | `ai_rep_config/{id}` | `feature/representative/presentation/AiRepConfigScreen.kt` | Composable | `ConfigureAiRepViewModel`| `POST api/representatives` | **Audited** |
| **SCR-25** | AI Rep Status | `ai_rep_details/{id}` | `feature/representative/presentation/AiRepStatusScreen.kt` | Composable | `AiRepStatusViewModel` | STOMP AI speech WS | **Audited** |
| **SCR-26** | In-Meeting Chat | `chat/{id}` | `feature/chat/presentation/ChatScreen.kt` | Composable | `ChatViewModel` | STOMP Chat WS + REST | **Audited** |
| **SCR-27** | Live Transcript | `transcript/{id}` | `feature/transcript/presentation/TranscriptScreen.kt` | Composable | `TranscriptViewModel` | REST + STOMP Transcript | **Audited** |
| **SCR-28** | Live Translation | `live_translation/{id}` | `feature/translation/presentation/LiveTranslationScreen.kt` | Composable | `LiveTranslationViewModel`| STOMP Translation WS + TTS | **Audited** |
| **SCR-29** | Meeting Recordings | `recordings` | `feature/recording/presentation/MeetingRecordingsScreen.kt` | Composable | `MeetingViewModel` | `GET api/meetings/{id}/recordings` | **Audited** |
| **SCR-30** | Recording Player | `recording_player/{mId}/{rId}`| `feature/recording/presentation/RecordingPlayerScreen.kt` | Composable | N/A | Recording media player | **Audited** |

---

### **2. Screen-by-Screen Detailed Evaluation**

#### **SCR-19: Live Meeting Room (`LiveMeetingScreen.kt`)**
* **Source File**: `D:/MeetMind_AI/android/app/src/main/java/com/developer_rahul/meetmind_ai/feature/meetingroom/presentation/LiveMeetingScreen.kt`
* **Entry Point**: Navigated from `PreJoinMeetingScreen` or Notification Deep Link (`Screen.LiveMeeting.createRoute(meetingId)`).
* **ViewModel**: `LiveMeetingViewModel` (factored via `provideLiveMeetingViewModelFactory(meetingId)`).
* **API / Realtime Dependencies**: WebRTC PeerConnections (`MeetingCallRepository`), STOMP Signaling (`/topic/meetings/$id/signaling`), AI Speech Stream (`/topic/meetings/$id/ai-proxy/speech`), Live Subtitles (`LiveTranslationViewModel`).
* **Layout Structure**:
  * Root `Box` container (`#0A0C14` background) with status-bar insets handling.
  * Top Bar: Glassmorphism surface displaying Meeting Title, Code copy button, REC timer badge, Chat button, and Participant count chip.
  * Main Content Column:
    * Screen Share Hero Box (240dp height, bounded in 20dp rounded corners) when screen share active.
    * Weighted `LazyVerticalGrid` (2 columns) displaying active participant camera video tiles (`ParticipantVideoTile`).
  * Floating Local Preview ("You"): Picture-in-Picture card (100dp x 140dp) anchored at bottom right with camera flip button.
  * Bottom Dock: Glassmorphism control bar containing Microphone Mute, Camera Mute, Audio Device Output Selector, Screen Share, More Controls, and End Call CTA buttons.
* **Loading / Error / Empty States**:
  * Loading state displays `CircularProgressIndicator` during WebRTC initialization.
  * Empty grid state falls back gracefully to a single local user preview tile.
  * Network disconnects or WebRTC failure states trigger automatic `triggerIceRestart()` recovery.
* **Findings & Deficiencies**:
  1. `[P1] Accessibility`: Top bar icon buttons (`Chat`, `Copy`) have touch targets smaller than `48dp` (`34dp`).
  2. `[P1] Hardcoded Text`: Strings like `"Live Meeting"`, `"Sharing Your Screen"`, `"Microphone Muted"`, `"Microphone Unmuted"`, `"The host has ended this meeting for everyone"` are hardcoded in Kotlin code without `strings.xml` resource keys.
  3. `[P2] Contrast`: Subtitle overlay text on black translucent background lacks high-contrast outline for low-vision accessibility.
  4. `[P2] Inset Padding`: On devices with gesture navigation bars, the bottom glass dock floats close to the bottom screen edge (`20.dp` bottom padding), requiring `navigationBarsPadding()` adjustment on extra-tall phones.

---

#### **SCR-12: Dashboard (`DashboardScreen.kt`)**
* **Source File**: `D:/MeetMind_AI/android/app/src/main/java/com/developer_rahul/meetmind_ai/feature/home/presentation/DashboardScreen.kt`
* **Entry Point**: Default home tab of `MainScreen`.
* **ViewModel**: `MeetingViewModel`.
* **API / Data Dependencies**: `GET api/meetings`.
* **Layout Structure**:
  * Top App Bar: User Greeting ("Welcome back 👋"), Profile Avatar button, Notifications bell icon with unread count badge.
  * Quick Actions Row: "New Meeting" (Gradient button) and "Join by Code" (Outlined button).
  * Meeting Stats Cards: 2x2 Grid displaying Total Meetings, Hours Spent, AI Reports Generated, and AI Rep Usage.
  * Upcoming Meetings Section: Filter chips (All, Scheduled, Live, Completed) + `LazyColumn` of meeting cards.
  * Floating Action Button: `GradientFloatingActionButton` ("New Meeting").
* **Findings & Deficiencies**:
  1. `[P1] Hardcoded Text`: Greeting `"Welcome back"`, section headers `"Quick Actions"`, `"Meeting Statistics"`, `"Upcoming Meetings"`, and empty state `"No meetings scheduled"` are hardcoded.
  2. `[P2] Touch Target`: Notification bell icon in top bar lacks `48dp` minimum touch bounding box padding.
  3. `[P2] Empty State CTA`: Empty meeting list displays static text without an interactive "Schedule Meeting" button inside the card.

---

#### **SCR-15: Create Meeting (`CreateMeetingScreen.kt`)**
* **Source File**: `D:/MeetMind_AI/android/app/src/main/java/com/developer_rahul/meetmind_ai/feature/meetings/presentation/CreateMeetingScreen.kt`
* **Entry Point**: Tapping "New Meeting" on Dashboard or FAB (`Screen.CreateMeeting.route`).
* **ViewModel**: `MeetingViewModel`.
* **API Dependencies**: `POST api/meetings`.
* **Layout Structure**:
  * Mode Switcher Segmented Control: "⚡ Instant Meeting" vs "📅 Scheduled Meeting".
  * Form Fields: Title input, Agenda / Description input, Invite Participants (Optional) email broadcast field.
  * AI Co-Host Expandable Switcher: Focus mode selection (Action Items, Full Summary, Executive).
  * Date & Time Pickers for Scheduled mode with quick preset chips ("+30 Mins", "Tomorrow 10 AM").
  * Success Dialog Modal with meeting code, direct link copy, share invitation CTA, and instant "Start Now" button.
* **Findings & Deficiencies**:
  1. `[P1] Soft Keyboard Occlusion`: On small phones (e.g. 5.1" 720p), the bottom "Start Instant Meeting Now" button gets partially occluded by the soft keyboard when typing in the description field.
  2. `[P1] Hardcoded Text`: All form labels, placeholders, dialog titles, and preset chip texts are hardcoded.
  3. `[P2] Accessibility`: Date and Time picker surface cards lack explicit `contentDescription` or screen-reader role announcements.

---

#### **SCR-22: Comprehensive Report (`ComprehensiveReportScreen.kt`)**
* **Source File**: `D:/MeetMind_AI/android/app/src/main/java/com/developer_rahul/meetmind_ai/feature/meetings/presentation/ComprehensiveReportScreen.kt`
* **Entry Point**: Tapping "Full Report" on `MeetingDetailsScreen` or `MeetingEndedScreen` (`Screen.ComprehensiveReport.createRoute(meetingId)`).
* **API Dependencies**: `GET api/meetings/{meetingId}/comprehensive-report`.
* **Layout Structure**:
  * Top Bar: "Meeting Report" title, back navigation arrow.
  * Header Card: Meeting type badge ("AI Representative Attended" vs "Standard Live Meeting"), meeting code, topic, description, host name.
  * Executive Summary Card: Synthesized AI summary text.
  * My Assigned Tasks Section: Interactive task cards with status toggle checkboxes (OPEN <-> COMPLETED).
  * Work Assignments by Participant Section: Grouped tasks per participant.
  * Key Discussion Points & Decisions Made Sections: Bulleted list cards.
* **Findings & Deficiencies**:
  1. `[P1] Hardcoded Text`: All section headers ("Executive Summary", "My Assigned Tasks", "Work Assignments by Participant", "Key Discussion Points", "Decisions Made") are hardcoded.
  2. `[P2] Task Toggle Feedback`: Toggling a task status executes `onUpdateTaskStatus` but lacks an immediate haptic feedback or visual animation trigger.

---

#### **SCR-26: In-Meeting Chat (`ChatScreen.kt` & `InMeetingChatSheetContent`)**
* **Source File**: `D:/MeetMind_AI/android/app/src/main/java/com/developer_rahul/meetmind_ai/feature/chat/presentation/ChatScreen.kt`
* **Entry Point**: Tapping Chat icon on `LiveMeetingScreen` (opens BottomSheet) or full-screen route (`Screen.Chat.createRoute(meetingId)`).
* **ViewModel**: `ChatViewModel`.
* **API / WebSocket Dependencies**: `GET api/meetings/{meetingId}/messages`, STOMP WebSocket `/topic/meetings/$id/chat`.
* **Layout Structure**:
  * Top Bar: "In-Meeting Live Chat" title, fullscreen toggle icon button.
  * Reverse `LazyColumn`: Chat bubbles (`ChatBubble`), differentiating local user (right-aligned Indigo bubble) vs remote participants (left-aligned CardSurface bubble with avatar).
  * Bottom Input Bar: Text field (`MeetMindTextField`) + Send button (`NeonCyan` / `ElectricIndigo` surface).
* **Findings & Deficiencies**:
  1. `[P1] Hardcoded Text`: Placeholder `"Send message to everyone..."`, `"No messages yet. Say hello!"` are hardcoded.
  2. `[P2] Touch Target`: Fullscreen expand icon button in sheet top bar has a touch target of `32dp`.

---
*End of Section 3 & 4: Screen Inventory & Per-Screen Audit.*
