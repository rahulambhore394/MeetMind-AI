package com.developer_rahul.meetmind_ai.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.developer_rahul.meetmind_ai.feature.splash.presentation.SplashScreen
import com.developer_rahul.meetmind_ai.feature.onboarding.presentation.OnboardingScreen
import com.developer_rahul.meetmind_ai.feature.permissions.presentation.PermissionScreen
import com.developer_rahul.meetmind_ai.feature.auth.presentation.*
import com.developer_rahul.meetmind_ai.feature.home.presentation.DashboardScreen
import com.developer_rahul.meetmind_ai.feature.home.presentation.MainScreen
import com.developer_rahul.meetmind_ai.feature.meetings.presentation.*
import com.developer_rahul.meetmind_ai.feature.meetingroom.presentation.LiveMeetingScreen
import com.developer_rahul.meetmind_ai.feature.chat.presentation.ChatScreen
import com.developer_rahul.meetmind_ai.feature.transcript.presentation.TranscriptScreen
import com.developer_rahul.meetmind_ai.feature.translation.presentation.LiveTranslationScreen
import com.developer_rahul.meetmind_ai.feature.ai.presentation.AiDashboardScreen
import com.developer_rahul.meetmind_ai.feature.representative.presentation.*
import com.developer_rahul.meetmind_ai.feature.notifications.presentation.NotificationsScreen
import com.developer_rahul.meetmind_ai.feature.profile.presentation.*
import com.developer_rahul.meetmind_ai.feature.settings.presentation.SettingsScreen
import com.developer_rahul.meetmind_ai.feature.participants.presentation.*
import com.developer_rahul.meetmind_ai.feature.recording.presentation.MeetingRecordingsScreen
import com.developer_rahul.meetmind_ai.feature.recording.presentation.RecordingPlayerScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = { navController.navigate(Screen.Onboarding.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                onNavigateToAuth = { navController.navigate(Screen.Welcome.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Splash.route) { inclusive = true } } }
            )
        }
        
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = { navController.navigate(Screen.Permissions.route) }
            )
        }
        
        composable(Screen.Permissions.route) {
            PermissionScreen(
                onContinue = { navController.navigate(Screen.Welcome.route) }
            )
        }
        
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onLogin = { navController.navigate(Screen.Login.route) },
                onRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Welcome.route) { inclusive = true } } },
                onBack = { navController.popBackStack() },
                onForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                onRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) } },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.EmailVerification.route) {
            EmailVerificationScreen(
                onContinue = { navController.navigate(Screen.Login.route) },
                onResend = { }
            )
        }
        
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                onResetSent = { navController.navigate(Screen.ResetPassword.route) }
            )
        }
        
        composable(Screen.ResetPassword.route) {
            ResetPasswordScreen(
                onResetSuccess = { navController.navigate(Screen.Login.route) }
            )
        }
        
        composable(Screen.Dashboard.route) {
            MainScreen(
                onNewMeeting = { navController.navigate(Screen.CreateMeeting.route) },
                onJoinMeeting = { navController.navigate(Screen.SearchMeetings.route) },
                onAiRepConfig = { navController.navigate(Screen.AiRepresentativeDashboard.route) },
                onMeetingClick = { id -> navController.navigate(Screen.MeetingDetails.createRoute(id)) },
                onNotifications = { navController.navigate(Screen.Notifications.route) },
                onProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = { navController.navigate(Screen.Welcome.route) { popUpTo(0) } },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onAddParticipant = { id -> navController.navigate(Screen.AddParticipant.createRoute(id)) },
                onSummaries = { navController.navigate(Screen.MeetingSummaries.route) }
            )
        }

        composable(Screen.MeetingSummaries.route) {
            MeetingSummariesScreen(
                onBack = { navController.popBackStack() },
                onReportClick = { meetingId -> navController.navigate(Screen.ComprehensiveReport.createRoute(meetingId.toString())) }
            )
        }

        composable(
            Screen.ComprehensiveReport.route,
            arguments = listOf(navArgument("meetingId") { defaultValue = "" })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("meetingId") ?: ""
            ComprehensiveReportScreen(
                meetingId = id,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SearchMeetings.route) {
            SearchMeetingsScreen(
                onBack = { navController.popBackStack() },
                onMeetingClick = { id -> navController.navigate(Screen.MeetingDetails.createRoute(id)) }
            )
        }

        composable(Screen.CreateMeeting.route) {
            CreateMeetingScreen(
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() },
                onStartMeeting = { id ->
                    navController.popBackStack()
                    navController.navigate(Screen.PreJoinMeeting.createRoute(id))
                }
            )
        }

        composable(
            Screen.EditMeeting.route,
            arguments = listOf(navArgument("meetingId") { defaultValue = "" })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("meetingId") ?: ""
            EditMeetingScreen(
                meetingId = id,
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() }
            )
        }

        composable(
            Screen.MeetingDetails.route,
            arguments = listOf(navArgument("meetingId") { defaultValue = "" })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("meetingId") ?: ""
            MeetingDetailsScreen(
                meetingId = id,
                onBack = { navController.popBackStack() },
                onJoin = { navController.navigate(Screen.PreJoinMeeting.createRoute(id)) },
                onConfigureRep = { navController.navigate(Screen.AiRepresentativeConfig.createRoute(id)) },
                onPlayRecording = { recId, meetId ->
                    navController.navigate(Screen.RecordingPlayer.createRoute(meetId, recId))
                },
                onViewIntelligence = { mId ->
                    navController.navigate(Screen.MeetingIntelligence.createRoute(mId))
                },
                onViewReport = { mId ->
                    navController.navigate(Screen.ComprehensiveReport.createRoute(mId))
                }
            )
        }

        composable(
            Screen.PreJoinMeeting.route,
            arguments = listOf(navArgument("meetingId") { defaultValue = "" })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("meetingId") ?: ""
            PreJoinMeetingScreen(
                meetingId = id,
                onBack = { navController.popBackStack() },
                onJoin = { navController.navigate(Screen.LiveMeeting.createRoute(id)) }
            )
        }

        composable(
            Screen.LiveMeeting.route,
            arguments = listOf(navArgument("meetingId") { defaultValue = "" })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("meetingId") ?: ""
            LiveMeetingScreen(
                meetingId = id,
                onLeaveMeeting = { navController.navigate(Screen.MeetingEnded.createRoute(id, "45:20")) },
                onToggleChat = { navController.navigate(Screen.Chat.createRoute(id)) },
                onToggleParticipants = { navController.navigate(Screen.Participants.createRoute(id)) },
                onViewTranscript = { navController.navigate(Screen.Transcript.createRoute(id)) },
                onViewIntelligence = { navController.navigate(Screen.MeetingIntelligence.createRoute(id)) }
            )
        }

        composable(
            Screen.Participants.route,
            arguments = listOf(navArgument("meetingId") { defaultValue = "" })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("meetingId") ?: ""
            ParticipantsScreen(
                meetingId = id,
                onBack = { navController.popBackStack() },
                onAddParticipant = { navController.navigate(Screen.AddParticipant.createRoute(id)) }
            )
        }

        composable(
            Screen.AddParticipant.route,
            arguments = listOf(navArgument("meetingId") { defaultValue = "" })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("meetingId") ?: ""
            AddParticipantScreen(
                meetingId = id,
                onBack = { navController.popBackStack() },
                onInviteSent = { navController.popBackStack() }
            )
        }

        composable(
            Screen.MeetingPermissions.route,
            arguments = listOf(navArgument("meetingId") { defaultValue = "" })
        ) {
            MeetingPermissionsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Screen.Chat.route,
            arguments = listOf(navArgument("meetingId") { defaultValue = "" })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("meetingId") ?: ""
            ChatScreen(meetingId = id, onBack = { navController.popBackStack() })
        }

        composable(Screen.Transcript.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("meetingId") ?: ""
            TranscriptScreen(meetingId = id, onBack = { navController.popBackStack() })
        }

        composable(
            Screen.LiveTranslation.route,
            arguments = listOf(navArgument("meetingId") { defaultValue = "" })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("meetingId") ?: ""
            LiveTranslationScreen(meetingId = id, onBack = { navController.popBackStack() })
        }

        composable(
            Screen.MeetingEnded.route,
            arguments = listOf(
                navArgument("meetingId") { defaultValue = "" },
                navArgument("duration") { defaultValue = "00:00" }
            )
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString("meetingId") ?: ""
            val duration = backStackEntry.arguments?.getString("duration") ?: "00:00"
            MeetingEndedScreen(
                duration = duration,
                onViewSummary = { navController.navigate(Screen.MeetingIntelligence.createRoute(meetingId)) },
                onBackToHome = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) } }
            )
        }

        composable(
            Screen.AiRepresentativeConfig.route,
            arguments = listOf(navArgument("meetingId") { defaultValue = "" })
        ) {
            AiRepConfigScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(Screen.AiRepresentativeDashboard.route) {
            AiRepDashboardScreen(
                onBack = { navController.popBackStack() },
                onConfigureRep = { id -> navController.navigate(Screen.AiRepresentativeConfig.createRoute(id.toString())) },
                onViewReport = { id -> navController.navigate(Screen.AiRepresentativeDetails.createRoute(id.toString())) }
            )
        }

        composable(Screen.AiRepresentativeActive.route) {
            AiRepActiveScreen(onCancel = { navController.popBackStack() })
        }

        composable(
            Screen.AiRepresentativeDetails.route,
            arguments = listOf(navArgument("id") { defaultValue = "" })
        ) { backStackEntry ->
            val idStr = backStackEntry.arguments?.getString("id") ?: "-1"
            val id = idStr.toLongOrNull() ?: -1L
            AiRepStatusScreen(
                meetingId = id,
                onBack = { navController.popBackStack() },
                onViewReport = { mid, rid -> navController.navigate(Screen.RepresentativeReport.createRoute(mid, rid)) }
            )
        }

        composable(
            Screen.RepresentativeReport.route,
            arguments = listOf(
                navArgument("meetingId") { defaultValue = "" },
                navArgument("repId") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val midStr = backStackEntry.arguments?.getString("meetingId") ?: "-1"
            val ridStr = backStackEntry.arguments?.getString("repId") ?: "-1"
            val mid = midStr.toLongOrNull() ?: -1L
            val rid = ridStr.toLongOrNull() ?: -1L
            RepresentativeReportScreen(
                meetingId = mid,
                representativeId = rid,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = { navController.navigate(Screen.Welcome.route) { popUpTo(0) } },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Screen.MeetingIntelligence.route,
            arguments = listOf(navArgument("meetingId") { defaultValue = "" })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("meetingId") ?: ""
            AiDashboardScreen(
                meetingId = id,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.ComprehensiveReport.route,
            arguments = listOf(navArgument("meetingId") { defaultValue = "" })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("meetingId") ?: ""
            ComprehensiveReportScreen(
                meetingId = id,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Recordings.route) {
            MeetingRecordingsScreen(
                onBack = { navController.popBackStack() },
                onPlayRecording = { id -> navController.navigate("recording_player/0/$id") }
            )
        }

        composable(
            Screen.RecordingPlayer.route,
            arguments = listOf(
                navArgument("meetingId") { defaultValue = "" },
                navArgument("recordingId") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val mId = backStackEntry.arguments?.getString("meetingId") ?: ""
            val rId = backStackEntry.arguments?.getString("recordingId") ?: ""
            RecordingPlayerScreen(
                recordingId = rId,
                meetingId = mId,
                onBack = { navController.popBackStack() },
                onViewSummary = {
                    if (mId.isNotBlank() && mId != "0") {
                        navController.navigate(Screen.MeetingIntelligence.createRoute(mId))
                    }
                }
            )
        }

        composable(
            "recording_player/{recordingId}",
            arguments = listOf(navArgument("recordingId") { defaultValue = "" })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("recordingId") ?: ""
            RecordingPlayerScreen(
                recordingId = id,
                meetingId = "",
                onBack = { navController.popBackStack() }
            )
        }
    }
}
