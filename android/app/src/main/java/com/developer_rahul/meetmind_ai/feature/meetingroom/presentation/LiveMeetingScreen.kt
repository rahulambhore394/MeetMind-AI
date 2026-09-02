package com.developer_rahul.meetmind_ai.feature.meetingroom.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.LeaveMeetingDialog
import com.developer_rahul.meetmind_ai.core.ui.components.RecordingConsentDialog
import com.developer_rahul.meetmind_ai.core.ui.components.StatusChip
import com.developer_rahul.meetmind_ai.core.ui.components.VideoRenderer
import com.developer_rahul.meetmind_ai.core.ui.provideLiveMeetingViewModelFactory
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.MeetingStatus
import com.developer_rahul.meetmind_ai.feature.meetings.presentation.MeetingViewModel
import com.developer_rahul.meetmind_ai.feature.translation.presentation.LiveTranslationViewModel
import com.developer_rahul.meetmind_ai.feature.translation.domain.model.Subtitle
import com.developer_rahul.meetmind_ai.core.ui.provideLiveTranslationViewModelFactory
import org.webrtc.VideoTrack
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMeetingScreen(
    meetingId: String,
    onLeaveMeeting: () -> Unit,
    onToggleChat: () -> Unit,
    onToggleParticipants: () -> Unit,
    meetingViewModel: MeetingViewModel = viewModel(factory = ViewModelFactory),
    callViewModel: LiveMeetingViewModel = viewModel(factory = provideLiveMeetingViewModelFactory(meetingId.toLongOrNull() ?: -1L)),
    translationViewModel: LiveTranslationViewModel = viewModel(factory = provideLiveTranslationViewModelFactory(meetingId.toLongOrNull() ?: -1L))
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (cameraGranted && audioGranted) {
            // Permissions granted
        }
    }

    val screenShareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { callViewModel.startScreenSharing(it) }
        }
    }

    LaunchedEffect(Unit) {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        
        if (!hasCamera || !hasAudio) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }
    val uiState by meetingViewModel.uiState.collectAsState()
    val callState by callViewModel.uiState.collectAsState()
    val translationState by translationViewModel.uiState.collectAsState()
    val recentSubtitles by translationViewModel.subtitles.collectAsState()

    val meeting = uiState.selectedMeeting
    val isRecording = uiState.isRecording
    
    var isMuted by remember { mutableStateOf(false) }
    var isVideoOff by remember { mutableStateOf(false) }
    
    var showMoreSheet by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showRecordingConsent by remember { mutableStateOf(false) }
    var showTranslationSettings by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val recordingLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val id = meetingId.toLongOrNull()
            if (data != null && id != null) {
                meetingViewModel.startRecording(id, data)
            }
        }
    }

    LaunchedEffect(meetingId) {
        meetingViewModel.loadMeetingDetails(meetingId)
    }

    if (showInviteDialog && meeting != null) {
        var inviteEmailInput by remember { mutableStateOf("") }
        var inviteMessage by remember { mutableStateOf<String?>(null) }
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            containerColor = BackgroundSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, null, tint = ElectricIndigo)
                    Spacer(Modifier.width(8.dp))
                    Text("Invite People to Meeting", style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text("Meeting Code", style = Typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = CardSurface,
                        shape = MaterialTheme.shapes.small,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                meeting.meetingCode ?: "mm-${meeting.id}",
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            IconButton(onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(meeting.meetingCode ?: "mm-${meeting.id}"))
                                inviteMessage = "Code copied to clipboard!"
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextPrimary)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "Join my MeetMind AI live meeting!\nTitle: ${meeting.title}\nMeeting Code: ${meeting.meetingCode ?: meeting.id}")
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Meeting Code"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share Invitation Link & Code")
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inviteEmailInput,
                        onValueChange = { inviteEmailInput = it },
                        label = { Text("Invite via Email") },
                        placeholder = { Text("user@example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricIndigo)
                    )

                    if (inviteMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(inviteMessage!!, style = Typography.bodySmall, color = EmeraldGreen)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (inviteEmailInput.isNotBlank()) {
                        val id = meetingId.toLongOrNull()
                        if (id != null) {
                            meetingViewModel.inviteParticipant(id, inviteEmailInput.trim()) { success, msg ->
                                inviteMessage = msg
                            }
                        }
                    }
                }) {
                    Text("Send Invite", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    if (showLeaveDialog) {
        LeaveMeetingDialog(
            onDismiss = { showLeaveDialog = false },
            onLeave = {
                showLeaveDialog = false
                callViewModel.leaveMeeting()
                onLeaveMeeting()
            }
        )
    }

    if (showRecordingConsent) {
        RecordingConsentDialog(
            onDismiss = { showRecordingConsent = false },
            onConfirm = {
                showRecordingConsent = false
                val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                recordingLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
            }
        )
    }

    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            sheetState = sheetState,
            containerColor = BackgroundSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextDisabled) }
        ) {
            MoreControlsContent(
                isRecording = isRecording,
                isSharing = callState.isScreenSharing,
                onToggleRecording = { 
                    val id = meetingId.toLongOrNull()
                    if (id != null) {
                        if (isRecording) {
                            val activeRecording = uiState.recordings.find { it.status == "STARTED" }
                            if (activeRecording != null) {
                                meetingViewModel.stopRecording(id, activeRecording.id)
                            }
                        } else {
                            showRecordingConsent = true
                        }
                    }
                    showMoreSheet = false 
                },
                onInvitePeople = {
                    showInviteDialog = true
                    showMoreSheet = false
                },
                onToggleScreenShare = {
                    if (callState.isScreenSharing) {
                        callViewModel.stopScreenSharing()
                    } else {
                        val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        screenShareLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                    }
                    showMoreSheet = false
                },
                onToggleTranslationSettings = {
                    showTranslationSettings = true
                    showMoreSheet = false
                },
                onViewTranscript = { /* Navigate */ showMoreSheet = false },
                onAiInsights = { /* Navigate */ showMoreSheet = false }
            )
        }
    }

    if (showTranslationSettings) {
        ModalBottomSheet(
            onDismissRequest = { showTranslationSettings = false },
            containerColor = BackgroundSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextDisabled) }
        ) {
            TranslationSettingsContent(
                enabled = translationState.subtitlesEnabled,
                onToggle = { translationViewModel.toggleSubtitles(it) },
                selectedLanguage = translationState.selectedTargetLanguage,
                onLanguageSelect = { translationViewModel.setTargetLanguage(it) }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Immersive black background
    ) {
        // ... (existing code remains the same)
        // Participant Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.participants) { participant ->
                val mediaState = callState.participants[participant.userId]
                val uiParticipant = Participant(
                    id = participant.id.toString(),
                    name = participant.name,
                    isAiRep = participant.role == com.developer_rahul.meetmind_ai.feature.meetings.domain.model.ParticipantRole.AI_REPRESENTATIVE,
                    isOnline = participant.isOnline,
                    videoTrack = mediaState?.videoTrack,
                    isMicOn = mediaState?.audioEnabled ?: true,
                    isVideoOn = mediaState?.videoEnabled ?: true,
                    isActiveSpeaker = mediaState?.isSpeaking ?: false,
                    screenTrack = mediaState?.screenTrack
                )
                ParticipantVideoTile(uiParticipant)
            }
            
            // If empty, show current user
            if (uiState.participants.isEmpty()) {
                item {
                    ParticipantVideoTile(Participant("1", "You", isActiveSpeaker = true, isOnline = true, videoTrack = callState.localVideoTrack))
                }
            }
        }

        // Top Status Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRecording) {
                    Surface(
                        color = RoseRed.copy(alpha = 0.8f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.White, CircleShape)
                                    .pulse(color = Color.White)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "REC ${formatDuration(uiState.localRecordingState.durationMs)}",
                                color = Color.White,
                                style = Typography.labelLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                Surface(
                    color = GlassBackground,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SmartToy, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI Rep Active",
                            color = NeonCyan,
                            style = Typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (uiState.uploadStatus != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        color = ElectricIndigo.copy(alpha = 0.8f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.uploadStatus == "RUNNING") {
                                CircularProgressIndicator(
                                    progress = { (uiState.uploadProgress ?: 0) / 100f },
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    if (uiState.uploadStatus == "SUCCEEDED") Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (uiState.uploadStatus == "RUNNING") "Uploading ${uiState.uploadProgress}%" else "Upload ${uiState.uploadStatus}",
                                color = Color.White,
                                style = Typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // Floating Local Preview (You)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 20.dp)
                .size(width = 110.dp, height = 150.dp),
            shape = MaterialTheme.shapes.large,
            color = Color.DarkGray,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (callState.localVideoTrack != null && !isVideoOff) {
                    VideoRenderer(
                        modifier = Modifier.fillMaxSize(),
                        videoTrack = callState.localVideoTrack
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("You", style = Typography.labelSmall, color = Color.White)
                }
            }
        }

        // Subtitle Overlay
        if (translationState.subtitlesEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
            ) {
                SubtitleOverlay(
                    originalText = translationState.currentOriginalText,
                    subtitles = recentSubtitles
                )
            }
        }

        // Main Bottom Control Bar (Glassmorphic)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .glassmorphism()
                .padding(vertical = 12.dp, horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MeetingControlButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    isActive = !isMuted,
                    activeColor = GlassWhite,
                    inactiveColor = RoseRed,
                    onClick = { 
                        isMuted = !isMuted
                        callViewModel.setAudioEnabled(!isMuted)
                    }
                )
                MeetingControlButton(
                    icon = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                    isActive = !isVideoOff,
                    onClick = { 
                        isVideoOff = !isVideoOff
                        callViewModel.setVideoEnabled(!isVideoOff)
                    }
                )
                MeetingControlButton(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    isActive = false,
                    onClick = onToggleChat
                )
                MeetingControlButton(
                    icon = Icons.Default.MoreHoriz,
                    isActive = false,
                    onClick = { showMoreSheet = true }
                )
                Surface(
                    onClick = { showLeaveDialog = true },
                    shape = CircleShape,
                    color = RoseRed,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CallEnd, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60)) % 24
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
fun MoreControlsContent(
    isRecording: Boolean,
    isSharing: Boolean,
    onToggleRecording: () -> Unit,
    onInvitePeople: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onToggleTranslationSettings: () -> Unit,
    onViewTranscript: () -> Unit,
    onAiInsights: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Text("Meeting Controls", style = Typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        ControlItem(
            icon = if (isSharing) Icons.Default.StopScreenShare else Icons.Default.PresentToAll,
            title = if (isSharing) "Stop Screen Share" else "Share Screen",
            color = if (isSharing) RoseRed else TextPrimary,
            onClick = onToggleScreenShare
        )
        ControlItem(
            icon = Icons.Default.FiberManualRecord,
            title = if (isRecording) "Stop Recording" else "Start Recording",
            color = if (isRecording) RoseRed else TextPrimary,
            onClick = onToggleRecording
        )
        ControlItem(
            icon = Icons.Default.PersonAdd,
            title = "Invite People ➕",
            color = NeonCyan,
            onClick = onInvitePeople
        )
        ControlItem(
            icon = Icons.Default.Description,
            title = "View Live Transcript",
            onClick = onViewTranscript
        )
        ControlItem(
            icon = Icons.Default.AutoAwesome,
            title = "AI Meeting Insights",
            onClick = onAiInsights
        )
        ControlItem(
            icon = Icons.Default.Translate,
            title = "Translation Settings",
            onClick = onToggleTranslationSettings
        )
        ControlItem(
            icon = Icons.Default.Settings,
            title = "Meeting Settings",
            onClick = { }
        )
    }
}

@Composable
fun ControlItem(
    icon: ImageVector,
    title: String,
    color: Color = TextPrimary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (color == TextPrimary) ElectricIndigo else color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(20.dp))
            Text(title, style = Typography.bodyLarge, color = color)
        }
    }
}

@Composable
fun ParticipantVideoTile(participant: Participant) {
    val alpha = if (participant.isOnline) 1f else 0.5f
    Box(
        modifier = Modifier
            .aspectRatio(0.82f)
            .clip(MaterialTheme.shapes.large)
            .background(CardSurface)
            .then(
                if (participant.isActiveSpeaker && participant.isOnline) {
                    Modifier.border(2.5.dp, EmeraldGreen, MaterialTheme.shapes.large)
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), MaterialTheme.shapes.large)
                }
            )
    ) {
        // Video Renderer / Avatar
        val activeVideoTrack = participant.screenTrack ?: participant.videoTrack
        if (activeVideoTrack != null && participant.isOnline && (participant.isVideoOn || participant.screenTrack != null)) {
            VideoRenderer(
                modifier = Modifier.fillMaxSize(),
                videoTrack = activeVideoTrack
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AvatarPlaceholder(name = participant.name, isActiveSpeaker = participant.isActiveSpeaker && participant.isOnline, sizeDp = 72)
            }
        }

        // Overlay Info
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!participant.isMicOn || !participant.isOnline) {
                Icon(
                    if (!participant.isOnline) Icons.Default.CloudOff else Icons.Default.MicOff,
                    null, 
                    tint = RoseRed, 
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                participant.name + if (participant.isOnline) "" else " (Offline)",
                color = if (participant.isOnline) Color.White else TextDisabled,
                style = Typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            if (participant.isAiRep) {
                Spacer(modifier = Modifier.width(6.dp))
                StatusChip(text = "AI", color = NeonCyan)
            }
            if (participant.screenTrack != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.PresentToAll, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
            }
        }
        
        if (participant.isActiveSpeaker) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Icon(Icons.Default.VolumeUp, null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun MeetingControlButton(
    icon: ImageVector,
    isActive: Boolean,
    activeColor: Color = GlassWhite,
    inactiveColor: Color = GlassWhite,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isActive) activeColor else inactiveColor,
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

data class Participant(
    val id: String,
    val name: String,
    val isVideoOn: Boolean = true,
    val isMicOn: Boolean = true,
    val isActiveSpeaker: Boolean = false,
    val isAiRep: Boolean = false,
    val isOnline: Boolean = true,
    val videoTrack: VideoTrack? = null,
    val screenTrack: VideoTrack? = null
)

@Composable
fun SubtitleOverlay(
    originalText: String,
    subtitles: List<Subtitle>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        subtitles.forEach { subtitle ->
            Text(
                text = "${subtitle.speaker}: ${subtitle.translatedText}",
                color = Color.White,
                style = Typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (originalText.isNotBlank()) {
            Text(
                text = originalText,
                color = Color.White.copy(alpha = 0.7f),
                style = Typography.bodyMedium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun TranslationSettingsContent(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    selectedLanguage: String,
    onLanguageSelect: (String) -> Unit
) {
    val languages = listOf(
        "en" to "English",
        "hi" to "Hindi (हिंदी)",
        "mr" to "Marathi (मराठी)"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Text("Live Translation Settings", style = Typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Enable Subtitles", style = Typography.bodyLarge, color = TextPrimary)
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = ElectricIndigo)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Target Language", style = Typography.titleMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(modifier = Modifier.selectableGroup()) {
            languages.forEach { (code, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (code == selectedLanguage),
                            onClick = { onLanguageSelect(code) },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (code == selectedLanguage),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricIndigo)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = name, style = Typography.bodyLarge, color = TextPrimary)
                }
            }
        }
    }
}

val sampleParticipants = listOf(
    Participant("2", "Alice Chen", isActiveSpeaker = true),
    Participant("3", "Bob Wilson", isVideoOn = false, isMicOn = false),
    Participant("4", "Charlie Day"),
    Participant("5", "David Miller", isMicOn = false),
    Participant("6", "AI Representative", isAiRep = true, isVideoOn = false)
)

@Composable
fun AvatarPlaceholder(name: String, isActiveSpeaker: Boolean = false, sizeDp: Int = 72) {
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifEmpty { name.take(1).uppercase() }

    val gradients = listOf(
        listOf(Color(0xFF6366F1), Color(0xFFA855F7)), // Electric Indigo & Purple
        listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)), // Cyan & Blue
        listOf(Color(0xFF10B981), Color(0xFF059669)), // Emerald Green
        listOf(Color(0xFFF59E0B), Color(0xFFEF4444)), // Amber & Sunset Red
        listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))  // Pink & Violet
    )
    val colorIndex = Math.abs(name.hashCode()) % gradients.size
    val gradientColors = gradients[colorIndex]

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActiveSpeaker) 1.15f else 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween<Float>(800, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(contentAlignment = Alignment.Center) {
        if (isActiveSpeaker) {
            Box(
                modifier = Modifier
                    .size((sizeDp * pulseScale).dp)
                    .clip(CircleShape)
                    .background(EmeraldGreen.copy(alpha = 0.35f))
            )
        }
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = Typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
        }
    }
}
