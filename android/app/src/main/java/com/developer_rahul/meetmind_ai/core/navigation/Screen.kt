package com.developer_rahul.meetmind_ai.core.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Permissions : Screen("permissions")
    
    // Auth
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object ResetPassword : Screen("reset_password")
    object EmailVerification : Screen("email_verification")
    object SessionExpired : Screen("session_expired")
    
    // Main
    object Dashboard : Screen("dashboard")
    object Meetings : Screen("meetings")
    object SearchMeetings : Screen("search_meetings")
    object CreateMeeting : Screen("create_meeting")
    object EditMeeting : Screen("edit_meeting/{meetingId}") {
        fun createRoute(id: String) = "edit_meeting/$id"
    }
    object MeetingPermissions : Screen("meeting_permissions/{meetingId}") {
        fun createRoute(id: String) = "meeting_permissions/$id"
    }
    object AddParticipant : Screen("add_participant/{meetingId}") {
        fun createRoute(id: String) = "add_participant/$id"
    }
    object MeetingDetails : Screen("meeting_details/{meetingId}") {
        fun createRoute(id: String) = "meeting_details/$id"
    }
    object LiveMeeting : Screen("live_meeting/{meetingId}") {
        fun createRoute(id: String) = "live_meeting/$id"
    }
    object PreJoinMeeting : Screen("pre_join/{meetingId}") {
        fun createRoute(id: String) = "pre_join/$id"
    }
    object MeetingEnded : Screen("meeting_ended/{meetingId}/{duration}") {
        fun createRoute(meetingId: String, duration: String) = "meeting_ended/$meetingId/$duration"
    }
    object MeetingInvitation : Screen("meeting_invitation")
    object Participants : Screen("participants/{meetingId}") {
        fun createRoute(id: String) = "participants/$id"
    }
    object Chat : Screen("chat/{meetingId}") {
        fun createRoute(id: String) = "chat/$id"
    }
    object Transcript : Screen("transcript/{meetingId}") {
        fun createRoute(id: String) = "transcript/$id"
    }
    object LiveTranslation : Screen("live_translation/{meetingId}") {
        fun createRoute(id: String) = "live_translation/$id"
    }
    
    // AI & Rep
    object MeetingIntelligence : Screen("meeting_intelligence/{meetingId}") {
        fun createRoute(id: String) = "meeting_intelligence/$id"
    }
    object AiRepresentativeConfig : Screen("ai_rep_config/{meetingId}") {
        fun createRoute(id: String) = "ai_rep_config/$id"
    }
    object AiRepresentativeDashboard : Screen("ai_rep_dashboard")
    object AiRepresentativeActive : Screen("ai_rep_active")
    object AiRepresentativeDetails : Screen("ai_rep_details/{id}") {
        fun createRoute(id: String) = "ai_rep_details/$id"
    }
    object RepresentativeReport : Screen("rep_report/{meetingId}/{repId}") {
        fun createRoute(meetingId: Long, repId: Long) = "rep_report/$meetingId/$repId"
    }
    
    // Reports & Summaries
    object MeetingSummaries : Screen("meeting_summaries")
    object ComprehensiveReport : Screen("comprehensive_report/{meetingId}") {
        fun createRoute(id: String) = "comprehensive_report/$id"
    }

    // Others
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Recordings : Screen("recordings")
}
