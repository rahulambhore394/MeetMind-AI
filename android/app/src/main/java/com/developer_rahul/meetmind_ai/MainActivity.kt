package com.developer_rahul.meetmind_ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.developer_rahul.meetmind_ai.core.navigation.AppNavigation
import com.developer_rahul.meetmind_ai.core.navigation.Screen
import com.developer_rahul.meetmind_ai.core.designsystem.MeetMindTheme
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeetMindTheme {
                val navController = rememberNavController()
                val viewModel: MainViewModel = viewModel(factory = ViewModelFactory)

                // Android 13+ Notification Permission Request
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        // Permission result handled
                    }
                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Handle Notification & Deep Link Intents
                LaunchedEffect(intent) {
                    // 1. Deep Link URI processing
                    val uri = intent.data
                    if (uri != null) {
                        val scheme = uri.scheme
                        val host = uri.host
                        val path = uri.path

                        var extractedCode: String? = null
                        if (scheme == "meetmind") {
                            if (host == "invite" || host == "join") {
                                extractedCode = uri.pathSegments?.firstOrNull() ?: uri.lastPathSegment
                            } else if (path != null) {
                                extractedCode = uri.lastPathSegment
                            }
                        } else if (scheme == "https" || scheme == "http") {
                            if (path != null && (path.contains("/invite/") || path.contains("/join/"))) {
                                extractedCode = uri.lastPathSegment
                            } else {
                                extractedCode = uri.getQueryParameter("code")
                            }
                        }

                        if (!extractedCode.isNullOrBlank()) {
                            navController.navigate(Screen.MeetingInvitation.createRoute(extractedCode))
                            intent.data = null
                            return@LaunchedEffect
                        }
                    }

                    // 2. Push Notification Extras
                    val meetingId = intent.getStringExtra("meetingId")
                    val type = intent.getStringExtra("type")
                    if (meetingId != null && meetingId.isNotBlank()) {
                        when (type) {
                            "MEETING_STARTED" -> navController.navigate(Screen.LiveMeeting.createRoute(meetingId))
                            "MEETING_INVITATION" -> navController.navigate(Screen.MeetingInvitation.createRoute(meetingId))
                            "MEETING_REMINDER" -> navController.navigate(Screen.MeetingDetails.createRoute(meetingId))
                            "AI_SUMMARY_READY" -> navController.navigate(Screen.MeetingIntelligence.createRoute(meetingId))
                            "TRANSCRIPT_READY" -> navController.navigate(Screen.Transcript.createRoute(meetingId))
                            "REPRESENTATIVE_REPORT_READY" -> {
                                val repId = intent.getStringExtra("representativeId")
                                if (repId != null) {
                                    val mId = try { java.lang.Long.parseLong(meetingId) } catch (e: Exception) { -1L }
                                    val rId = try { java.lang.Long.parseLong(repId) } catch (e: Exception) { -1L }
                                    navController.navigate(Screen.RepresentativeReport.createRoute(mId, rId))
                                } else {
                                    navController.navigate(Screen.AiRepresentativeDetails.createRoute(meetingId))
                                }
                            }
                            else -> navController.navigate(Screen.Notifications.route)
                        }
                    } else if (type != null) {
                        navController.navigate(Screen.Notifications.route)
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.unauthorizedEvent.collect {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0)
                        }
                    }
                }

                AppNavigation(navController = navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
