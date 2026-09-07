package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
                        
                        val isRejoin = meeting.hasJoinedBefore || meeting.userParticipantStatus == "LEFT"
                        val buttonText = when (meeting.status) {
                            MeetingStatus.SCHEDULED -> "Start Meeting"
                            MeetingStatus.LIVE -> if (isRejoin) "Re-join Meeting" else "Join Meeting"
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
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(
                        text = if (meeting.status == MeetingStatus.LIVE) "LIVE NOW 🟢" else meeting.status.name, 
                        color = if (meeting.status == MeetingStatus.LIVE) EmeraldGreen else ElectricIndigo
                    )
                    
                    if (meeting.meetingCode != null) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = NeonCyan.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = meeting.meetingCode!!,
                                color = NeonCyan,
                                style = Typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    meeting.title,
                    style = Typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Meeting Code Copy & Share Hero Card
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                val context = androidx.compose.ui.platform.LocalContext.current
                var copiedNotice by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Meeting Invitation Code", style = Typography.labelSmall, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                meeting.meetingCode ?: "mm-${meeting.id}",
                                style = Typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = NeonCyan
                            )
                            Row {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(meeting.meetingCode ?: "mm-${meeting.id}"))
                                        copiedNotice = true
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = TextPrimary)
                                }
                                IconButton(
                                    onClick = {
                                        val shareIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, "Join my MeetMind AI meeting!\nTitle: ${meeting.title}\nCode: ${meeting.meetingCode ?: meeting.id}")
                                            type = "text/plain"
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Meeting Code"))
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Link", tint = ElectricIndigo)
                                }
                            }
                        }
                        if (copiedNotice) {
                            Text("Meeting code copied to clipboard!", style = Typography.labelSmall, color = EmeraldGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                MeetMindCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailRow(icon = Icons.Default.Event, text = meeting.scheduledAt, label = "Scheduled Time")
                        DetailRow(icon = Icons.Default.Person, text = meeting.hostName, label = "Host")
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                SectionHeader(title = "Description")
                Text(
                    meeting.description ?: "No description provided.",
                    style = Typography.bodyLarge,
                    color = TextSecondary,
                    lineHeight = 24.sp
                )
                
                if (uiState.participants.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(28.dp))
                    SectionHeader(title = "Participants (${uiState.participants.size})")
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.participants.forEach { participant ->
                            ParticipantRow(
                                name = participant.name, 
                                status = participant.status.name,
                                isOnline = participant.isOnline,
                                isHost = participant.userId == meeting.hostId
                            )
                        }
                    }
                }

                if (uiState.recordings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(28.dp))
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
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.medium,
            color = ElectricIndigo.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.25f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = Typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(2.dp))
            Text(text, style = Typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ParticipantRow(name: String, status: String, isOnline: Boolean = false, isHost: Boolean = false) {
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifEmpty { name.take(1).uppercase() }

    MeetMindCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = ElectricIndigo.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(initials, style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = Typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    if (isOnline) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(8.dp).background(EmeraldGreen, CircleShape))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(status.lowercase().replaceFirstChar { it.uppercase() }, style = Typography.labelSmall, color = TextSecondary)
            }
            if (isHost) {
                StatusChip(text = "Host 👑", color = AmberGold)
            } else {
                StatusChip(text = "Participant", color = TextSecondary)
            }
        }
    }
}
