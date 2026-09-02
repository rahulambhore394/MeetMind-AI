package com.developer_rahul.meetmind_ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
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

                LaunchedEffect(intent) {
                    val meetingId = intent.getStringExtra("meetingId")
                    val type = intent.getStringExtra("type")
                    if (meetingId != null) {
                        when (type) {
                            "MEETING_STARTED" -> navController.navigate(Screen.LiveMeeting.createRoute(meetingId))
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
                        }
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
}
