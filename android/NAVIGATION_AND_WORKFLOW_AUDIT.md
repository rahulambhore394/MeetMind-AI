# 🗺️ **MeetMind_AI — Navigation & Workflow Audit**
## **Section 8: Navigation Architecture & User Journeys**

---

### **1. Navigation Structure & Routes**

The application uses **Navigation Compose** (`NavHostController`) declared in `AppNavigation.kt` with a centralized route configuration in `Screen.kt`.

```
Screen.Splash ("splash")
├── Screen.Onboarding ("onboarding")
│   └── Screen.Permissions ("permissions")
│       └── Screen.Welcome ("welcome")
│           ├── Screen.Login ("login") ──> Screen.Dashboard
│           └── Screen.Register ("register") ──> Screen.Dashboard
└── Screen.Dashboard ("dashboard") -> MainScreen BottomNav Container
    ├── Home Tab (SCR-12: DashboardScreen)
    │   ├── Screen.CreateMeeting ("create_meeting")
    │   ├── Screen.SearchMeetings ("search_meetings")
    │   ├── Screen.MeetingDetails ("meeting_details/{meetingId}")
    │   │   ├── Screen.PreJoinMeeting ("pre_join/{meetingId}")
    │   │   │   └── Screen.LiveMeeting ("live_meeting/{meetingId}")
    │   │   │       ├── Screen.Chat ("chat/{meetingId}")
    │   │   │       ├── Screen.Participants ("participants/{meetingId}")
    │   │   │       ├── Screen.Transcript ("transcript/{meetingId}")
    │   │   │       ├── Screen.MeetingIntelligence ("meeting_intelligence/{meetingId}")
    │   │   │       └── Screen.MeetingEnded ("meeting_ended/{meetingId}/{duration}")
    │   │   │           ├── Screen.ComprehensiveReport ("comprehensive_report/{meetingId}")
    │   │   │           └── Screen.MeetingIntelligence ("meeting_intelligence/{meetingId}")
    │   │   ├── Screen.AiRepresentativeConfig ("ai_rep_config/{meetingId}")
    │   │   └── Screen.RecordingPlayer ("recording_player/{meetingId}/{recordingId}")
    │   └── Screen.Notifications ("notifications")
    ├── Meetings Tab (SCR-13: MeetingsScreen)
    ├── AI Rep Tab (SCR-23: AiRepDashboardScreen)
    └── Profile Tab (ProfileScreen)
```

---

### **2. Complete Core User Journeys**

#### **Journey A: Quick Instant Meeting Start**
1. User taps "New Meeting" FAB or Dashboard Quick Action.
2. Screen navigates to `CreateMeetingScreen` (`create_meeting`).
3. User verifies Title & Agenda, then taps "Start Instant Meeting Now".
4. `MeetingViewModel.createMeeting()` posts to `POST api/meetings`.
5. Success Modal appears displaying meeting code and direct join link.
6. User taps "Start Now 🟢" -> Navigates to `PreJoinMeetingScreen` (`pre_join/{id}`).
7. User verifies camera/mic preview, taps "Join Meeting Now" -> Navigates to `LiveMeetingScreen` (`live_meeting/{id}`).
8. WebRTC P2P mesh and STOMP signaling connect -> Real-time video/audio call starts!

#### **Journey B: Scheduled Meeting & Comprehensive AI Report**
1. Host creates a scheduled meeting in `CreateMeetingScreen` with title, date, time, and optional invitee emails.
2. Meeting appears in `MeetingsScreen` list.
3. Upon scheduled time, Host and Participants tap meeting card -> `MeetingDetailsScreen`.
4. Tap "Start Meeting" / "Join Meeting" -> `PreJoinMeetingScreen` -> `LiveMeetingScreen`.
5. Host or Participant records call or uses AI Representative.
6. Host taps "End For Everyone" -> `LiveMeetingScreen` triggers `meetingViewModel.endMeeting(id)` -> Backend broadcasts `MEETING_ENDED` STOMP signal.
7. All participants' screens auto-exit live room and navigate to `MeetingEndedScreen` (`meeting_ended/{id}/{duration}`).
8. Tapping "View Comprehensive AI Report 📊" navigates to `ComprehensiveReportScreen` (`comprehensive_report/{id}`).

---

### **3. Navigation Deficiencies & Edge Cases Identified**

1. **PopUpTo Backstack Cleanup on Login/Logout**:
   * On successful login in `LoginScreen.kt`:
     `navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Welcome.route) { inclusive = true } }`
   * **Evaluation**: Correctly clears login/welcome screens from the backstack so pressing Android system Back from Dashboard does not return to Login.
2. **Deep-Link Intent Handling in `MainActivity.kt`**:
   * `MainActivity` observes `intent` extras (`meetingId`, `type`).
   * When `type == "MEETING_STARTED"`, navigates to `LiveMeeting`.
   * When `type == "AI_SUMMARY_READY"`, navigates to `MeetingIntelligence`.
   * When `type == "TRANSCRIPT_READY"`, navigates to `Transcript`.
   * **Evaluation**: Fully functional deep-linking handling for FCM push notifications.

---
*End of Section 8: Navigation & Workflow Audit.*
