package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*

import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.MeetingStatus
import com.developer_rahul.meetmind_ai.feature.recording.presentation.RecordingItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailsScreen(
    meetingId: String,
    onBack: () -> Unit,
    onJoin: () -> Unit,
    onConfigureRep: () -> Unit,
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val meeting = uiState.selectedMeeting

    LaunchedEffect(meetingId) {
        viewModel.loadMeetingDetails(meetingId)
    }

    LaunchedEffect(uiState.joinSuccess) {
        if (uiState.joinSuccess) {
            viewModel.resetJoinState()
            onJoin()
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && meeting != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = BackgroundSurface,
            title = { Text("Delete Meeting?", style = Typography.titleMedium, color = TextPrimary) },
            text = { Text("Are you sure you want to delete '${meeting.title}'? This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMeeting(meeting.id) {
                            showDeleteConfirm = false
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(GlassWhite, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    if (meeting != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Meeting", tint = RoseRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface,
        bottomBar = {
            if (meeting != null) {
                Surface(
                    modifier = Modifier.navigationBarsPadding(),
                    color = BackgroundSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onConfigureRep,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = MaterialTheme.shapes.medium,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricIndigo)
                        ) {
                            Icon(Icons.Default.SmartToy, null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("AI Rep", color = ElectricIndigo, fontWeight = FontWeight.Bold)
                        }
                        
                        val buttonText = when (meeting.status) {
                            MeetingStatus.SCHEDULED -> "Start Meeting"
                            MeetingStatus.LIVE -> "Join Meeting"
                            else -> "Replay"
                        }
                        
                        MeetMindButton(
                            text = buttonText,
                            onClick = {
                                if (meeting.status == MeetingStatus.SCHEDULED) {
                                    viewModel.startMeeting(meeting.id)
                                } else if (meeting.status == MeetingStatus.LIVE) {
                                    viewModel.joinMeeting(meeting.id)
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            isLoading = uiState.actionInProgress
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoadingDetails) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricIndigo)
            }
        } else if (uiState.detailsError != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.detailsError!!, color = RoseRed)
                    Button(onClick = { viewModel.loadMeetingDetails(meetingId) }) {
                        Text("Retry")
                    }
                }
            }
        } else if (meeting == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Meeting not found", color = TextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                StatusChip(
                    text = meeting.status.name, 
                    color = if (meeting.status == MeetingStatus.LIVE) RoseRed else ElectricIndigo
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    meeting.title,
                    style = Typography.displayMedium,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                MeetMindCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailRow(icon = Icons.Default.Event, text = meeting.scheduledAt, label = "Scheduled Time")
                        DetailRow(icon = Icons.Default.Person, text = meeting.hostName, label = "Host")
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                SectionHeader(title = "Description")
                Text(
                    meeting.description ?: "No description provided.",
                    style = Typography.bodyLarge,
                    color = TextSecondary,
                    lineHeight = 26.sp
                )
                
                if (uiState.participants.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    SectionHeader(title = "Participants", action = "Invite")
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.participants.forEach { participant ->
                            ParticipantRow(
                                name = participant.name, 
                                status = participant.status.name,
                                isOnline = participant.isOnline
                            )
                        }
                    }
                }

                if (uiState.recordings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    SectionHeader(title = "Recordings")
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.recordings.forEach { recording ->
                            RecordingItemCard(
                                title = "Recording #${recording.id}",
                                date = recording.createdAt.take(10),
                                duration = recording.duration?.let { "${it / 60}:${String.format("%02d", it % 60)}" } ?: "Processing",
                                status = recording.status,
                                onClick = { /* Navigate to player if ready */ }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = MaterialTheme.shapes.medium,
            color = ElectricIndigo.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = Typography.labelSmall, color = TextSecondary)
            Text(text, style = Typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ParticipantRow(name: String, status: String, isOnline: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = Color.DarkGray
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(name.take(1), style = Typography.titleMedium, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = Typography.bodyLarge, color = TextPrimary)
                if (isOnline) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).background(EmeraldGreen, CircleShape))
                }
            }
            Text(status, style = Typography.labelSmall, color = TextSecondary)
        }
        if (name.contains("(You)")) {
            StatusChip(text = "Host", color = TextDisabled)
        }
    }
}
