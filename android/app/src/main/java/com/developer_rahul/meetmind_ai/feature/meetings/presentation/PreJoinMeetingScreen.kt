package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*

import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.meetings.presentation.MeetingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreJoinMeetingScreen(
    meetingId: String,
    onBack: () -> Unit,
    onJoin: () -> Unit,
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val meeting = uiState.selectedMeeting

    LaunchedEffect(meetingId) {
        viewModel.loadMeetingDetails(meetingId)
    }

    var isMicOn by remember { mutableStateOf(true) }
    var isCameraOn by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface,
        bottomBar = {
            Box(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                MeetMindButton(
                    text = "Join Meeting",
                    onClick = onJoin
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Premium Video Preview Placeholder
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = MaterialTheme.shapes.extraLarge,
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCameraOn) {
                        // Background gradient to simulate video feel
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF202020), Color(0xFF101010))
                                    )
                                )
                        )
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(120.dp), tint = Color.White.copy(alpha = 0.1f))
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VideocamOff, null, tint = TextDisabled, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Camera is Off", color = TextSecondary)
                        }
                    }
                    
                    // Controls Overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .background(GlassBackground.copy(alpha = 0.8f), CircleShape)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        PreJoinControlButton(
                            icon = if (isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                            isActive = isMicOn,
                            onClick = { isMicOn = !isMicOn }
                        )
                        PreJoinControlButton(
                            icon = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            isActive = isCameraOn,
                            onClick = { isCameraOn = !isCameraOn }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text("Ready to join?", style = Typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(meeting?.title ?: "Intelligent Meeting", style = Typography.bodyLarge, color = TextSecondary)
            
            if (uiState.participants.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${uiState.participants.size} people are already in this meeting", style = Typography.bodyMedium, color = EmeraldGreen)
                }
            }
        }
    }
}

@Composable
fun PreJoinControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = if (isActive) Color.White.copy(alpha = 0.15f) else RoseRed.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) Color.White.copy(alpha = 0.2f) else RoseRed.copy(alpha = 0.4f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (isActive) Color.White else RoseRed, modifier = Modifier.size(22.dp))
        }
    }
}
