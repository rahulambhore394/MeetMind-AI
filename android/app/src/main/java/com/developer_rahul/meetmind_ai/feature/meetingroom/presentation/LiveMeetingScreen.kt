package com.developer_rahul.meetmind_ai.feature.meetingroom.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import com.developer_rahul.meetmind_ai.core.ui.components.VideoRenderer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.HostLeaveOrEndDialog
import com.developer_rahul.meetmind_ai.core.ui.components.LeaveMeetingDialog
import com.developer_rahul.meetmind_ai.core.ui.components.RecordingConsentDialog
import com.developer_rahul.meetmind_ai.core.ui.components.StatusChip
import com.developer_rahul.meetmind_ai.core.ui.provideChatViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.provideLiveMeetingViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.provideLiveTranslationViewModelFactory
import com.developer_rahul.meetmind_ai.feature.chat.presentation.ChatBubble
import com.developer_rahul.meetmind_ai.feature.chat.presentation.ChatViewModel
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.ParticipantRole
import com.developer_rahul.meetmind_ai.feature.meetings.presentation.MeetingViewModel
import com.developer_rahul.meetmind_ai.feature.translation.domain.model.Subtitle
import com.developer_rahul.meetmind_ai.feature.translation.presentation.LiveTranslationViewModel
import org.webrtc.VideoTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMeetingScreen(
    meetingId: String,
    onLeaveMeeting: () -> Unit,
    onToggleChat: () -> Unit,
    onToggleParticipants: () -> Unit,
    onViewTranscript: () -> Unit = {},
    onViewIntelligence: () -> Unit = {},
    meetingViewModel: MeetingViewModel = viewModel(factory = ViewModelFactory),
    callViewModel: LiveMeetingViewModel = viewModel(factory = provideLiveMeetingViewModelFactory(meetingId.toLongOrNull() ?: -1L)),
    translationViewModel: LiveTranslationViewModel = viewModel(factory = provideLiveTranslationViewModelFactory(meetingId.toLongOrNull() ?: -1L))
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
    }

    val screenShareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intentData ->
                try {
                    callViewModel.startScreenSharing(intentData)
                    Toast.makeText(context, "Screen Sharing Started", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    com.developer_rahul.meetmind_ai.core.media.recording.RecordingService.stopService(context)
                    Toast.makeText(context, "Screen share error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            com.developer_rahul.meetmind_ai.core.media.recording.RecordingService.stopService(context)
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
    var showChatSheet by remember { mutableStateOf(false) }
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

        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            containerColor = BackgroundSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ElectricIndigo.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PersonAdd, null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Invite to Meeting", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text("Meeting Code", style = Typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = CardSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                meeting.meetingCode ?: "mm-${meeting.id}",
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(meeting.meetingCode ?: "mm-${meeting.id}"))
                                    inviteMessage = "Meeting code copied!"
                                    Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextPrimary, modifier = Modifier.size(18.dp))
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share Invitation Link", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inviteEmailInput,
                        onValueChange = { inviteEmailInput = it },
                        label = { Text("Invite via Email") },
                        placeholder = { Text("colleague@example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricIndigo,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    if (inviteMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(inviteMessage!!, style = Typography.bodySmall, color = EmeraldGreen, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inviteEmailInput.isNotBlank()) {
                            val id = meetingId.toLongOrNull()
                            if (id != null) {
                                meetingViewModel.inviteParticipant(id, inviteEmailInput.trim()) { _, msg ->
                                    inviteMessage = msg
                                }
                            }
                        }
                    },
                    modifier = Modifier.bounceClick()
                ) {
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
        val currentUserId = meetingViewModel.currentUserId
        val meetingFromList = uiState.meetings.find { it.id.toString() == meetingId || it.meetingCode == meetingId }
        val effectiveMeeting = meeting ?: meetingFromList
        
        val isHostByMeeting = effectiveMeeting != null && currentUserId > 0L && effectiveMeeting.hostId == currentUserId
        val isHostByParticipants = currentUserId > 0L && uiState.participants.any { 
            it.userId == currentUserId && it.role == ParticipantRole.HOST 
        }
        val isHost = isHostByMeeting || isHostByParticipants
        val numericMeetingId = meetingId.toLongOrNull() ?: effectiveMeeting?.id

        if (isHost) {
            HostLeaveOrEndDialog(
                onDismiss = { showLeaveDialog = false },
                onLeave = {
                    showLeaveDialog = false
                    if (numericMeetingId != null) {
                        meetingViewModel.leaveMeeting(numericMeetingId)
                    }
                    callViewModel.leaveMeeting()
                    onLeaveMeeting()
                },
                onEndForEveryone = {
                    showLeaveDialog = false
                    if (numericMeetingId != null) {
                        meetingViewModel.endMeeting(numericMeetingId)
                    }
                    callViewModel.leaveMeeting()
                    onLeaveMeeting()
                }
            )
        } else {
            LeaveMeetingDialog(
                onDismiss = { showLeaveDialog = false },
                onLeave = {
                    showLeaveDialog = false
                    if (numericMeetingId != null) {
                        meetingViewModel.leaveMeeting(numericMeetingId)
                    }
                    callViewModel.leaveMeeting()
                    onLeaveMeeting()
                }
            )
        }
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
                        com.developer_rahul.meetmind_ai.core.media.recording.RecordingService.stopService(context)
                    } else {
                        com.developer_rahul.meetmind_ai.core.media.recording.RecordingService.startService(context)
                        val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        screenShareLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                    }
                    showMoreSheet = false
                },
                onToggleTranslationSettings = {
                    showTranslationSettings = true
                    showMoreSheet = false
                },
                onViewTranscript = { 
                    showMoreSheet = false 
                    onViewTranscript()
                },
                onAiInsights = { 
                    showMoreSheet = false 
                    onViewIntelligence()
                },
                onToggleChat = {
                    showMoreSheet = false
                    showChatSheet = true
                },
                onToggleParticipants = {
                    showMoreSheet = false
                    onToggleParticipants()
                }
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
                audioTranslationEnabled = translationState.audioTranslationEnabled,
                onToggleSubtitles = { translationViewModel.toggleSubtitles(it) },
                onToggleAudio = { translationViewModel.toggleAudioTranslation(it) },
                sourceLanguage = translationState.sourceLanguage,
                onSourceLanguageSelect = { translationViewModel.setSourceLanguage(it) },
                selectedLanguage = translationState.selectedTargetLanguage,
                onTargetLanguageSelect = { translationViewModel.setTargetLanguage(it) }
            )
        }
    }

    if (showChatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChatSheet = false },
            containerColor = BackgroundSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextDisabled) }
        ) {
            InMeetingChatSheetContent(
                meetingId = meetingId,
                onFullScreenChat = {
                    showChatSheet = false
                    onToggleChat()
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C14)) 
    ) {
        val currentUserId = meetingViewModel.currentUserId
        val participantsList = uiState.participants
        val totalParticipants = participantsList.size.coerceAtLeast(1)

        val localScreenTrack = if (callState.isScreenSharing) callState.localScreenTrack else null
        val remoteScreenTrack = callState.participants.values.mapNotNull { it.screenTrack }.firstOrNull()
        val activeScreenShareTrack = localScreenTrack ?: remoteScreenTrack

        Column(modifier = Modifier.fillMaxSize()) {
            if (activeScreenShareTrack != null) {
                // Prominent Screen Share Hero View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(top = 80.dp, start = 12.dp, end = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black)
                        .border(1.5.dp, NeonCyan, RoundedCornerShape(20.dp))
                ) {
                    VideoRenderer(
                        modifier = Modifier.fillMaxSize(),
                        videoTrack = activeScreenShareTrack
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PresentToAll, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (callState.isScreenSharing) "Sharing Your Screen" else "Screen Share",
                                style = Typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(if (totalParticipants <= 1) 1 else 2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = if (activeScreenShareTrack != null) 12.dp else 115.dp,
                    bottom = 120.dp,
                    start = 12.dp,
                    end = 12.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(participantsList) { index, participant ->
                    val mediaState = callState.participants[participant.userId]
                    val isLocalUser = (currentUserId > 0L && participant.userId == currentUserId) ||
                                      (participant.name.endsWith("(You)", ignoreCase = true) || participant.name.equals("You", ignoreCase = true))

                    val localVideo = if (!isVideoOff) callState.localVideoTrack else null
                    val effectiveVideoTrack = if (isLocalUser) localVideo else mediaState?.videoTrack
                    val effectiveScreenTrack = if (isLocalUser) (if (callState.isScreenSharing) callState.localScreenTrack else null) else mediaState?.screenTrack

                    val uiParticipant = Participant(
                        id = participant.id.toString(),
                        name = if (isLocalUser && !participant.name.contains("(You)")) "${participant.name} (You)" else participant.name,
                        isAiRep = participant.role == ParticipantRole.AI_REPRESENTATIVE,
                        isHost = participant.role == ParticipantRole.HOST,
                        isOnline = true,
                        videoTrack = effectiveVideoTrack,
                        isMicOn = if (isLocalUser) !isMuted else (mediaState?.audioEnabled ?: true),
                        isVideoOn = if (isLocalUser) !isVideoOff else (mediaState?.videoEnabled ?: (mediaState?.videoTrack != null)),
                        isActiveSpeaker = if (isLocalUser) (!isMuted) else (mediaState?.isSpeaking ?: false),
                        screenTrack = effectiveScreenTrack
                    )
                    ParticipantVideoTile(uiParticipant)
                }
                
                if (participantsList.isEmpty()) {
                    item {
                        ParticipantVideoTile(
                            Participant(
                                id = "1",
                                name = "You",
                                isActiveSpeaker = !isMuted,
                                isOnline = true,
                                isHost = ((meeting ?: uiState.meetings.find { it.id.toString() == meetingId || it.meetingCode == meetingId })?.hostId == currentUserId && currentUserId > 0L) ||
                                         (currentUserId > 0L && uiState.participants.any { it.userId == currentUserId && it.role == ParticipantRole.HOST }),
                                videoTrack = if (!isVideoOff) callState.localVideoTrack else null,
                                screenTrack = if (callState.isScreenSharing) callState.localScreenTrack else null,
                                isMicOn = !isMuted,
                                isVideoOn = !isVideoOff
                            )
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .glassmorphism(),
            shape = RoundedCornerShape(24.dp),
            color = GlassBackground
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(EmeraldGreen, CircleShape)
                            .pulse(color = EmeraldGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = meeting?.title ?: "Live Meeting",
                        style = Typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Surface(
                        onClick = {
                            val code = meeting?.meetingCode ?: "mm-${meetingId}"
                            clipboardManager.setText(AnnotatedString(code))
                            Toast.makeText(context, "Code copied: $code", Toast.LENGTH_SHORT).show()
                        },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                meeting?.meetingCode ?: "mm-${meetingId}",
                                style = Typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ContentCopy, null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRecording) {
                        Surface(
                            color = RoseRed.copy(alpha = 0.85f),
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color.White, CircleShape)
                                        .pulse(color = Color.White)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "REC ${formatDuration(uiState.localRecordingState.durationMs)}",
                                    color = Color.White,
                                    style = Typography.labelSmall,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Chat Button in Top Bar
                    IconButton(
                        onClick = { showChatSheet = true },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            .bounceClick()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Participants Counter Button in Top Bar
                    Surface(
                        onClick = onToggleParticipants,
                        shape = CircleShape,
                        color = ElectricIndigo.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.5f)),
                        modifier = Modifier.bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.People, null, tint = ElectricIndigo, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${uiState.participants.size.coerceAtLeast(1)}",
                                color = TextPrimary,
                                style = Typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Floating Local Preview (You) - Positioned cleanly above Bottom Controls
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 16.dp)
                .size(width = 100.dp, height = 140.dp)
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = CardSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassWhite)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (callState.localVideoTrack != null && !isVideoOff) {
                    VideoRenderer(
                        modifier = Modifier.fillMaxSize(),
                        videoTrack = callState.localVideoTrack
                    )
                } else {
                    AvatarPlaceholder(name = "You", sizeDp = 48)
                }

                // Camera Flip Quick Control
                if (!isVideoOff) {
                    IconButton(
                        onClick = { callViewModel.switchCamera() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .bounceClick()
                    ) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        null,
                        tint = if (isMuted) RoseRed else EmeraldGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("You", style = Typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (translationState.subtitlesEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
                    .padding(start = 16.dp, end = 125.dp)
                    .fillMaxWidth()
            ) {
                SubtitleOverlay(
                    originalText = translationState.currentOriginalText,
                    subtitles = recentSubtitles
                )
            }
        }

        // Standard Bottom Glass Dock Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .glassmorphism(borderColor = GlassWhite)
                .padding(vertical = 10.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Mic Button (Green ON / RoseRed OFF)
                MeetingControlButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    isActive = !isMuted,
                    activeColor = EmeraldGreen.copy(alpha = 0.85f),
                    inactiveColor = RoseRed.copy(alpha = 0.85f),
                    onClick = { 
                        isMuted = !isMuted
                        callViewModel.setAudioEnabled(!isMuted)
                        Toast.makeText(context, if (isMuted) "Microphone Muted" else "Microphone Unmuted", Toast.LENGTH_SHORT).show()
                    }
                )

                // Camera Button (Indigo ON / RoseRed OFF)
                MeetingControlButton(
                    icon = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                    isActive = !isVideoOff,
                    activeColor = ElectricIndigo.copy(alpha = 0.85f),
                    inactiveColor = RoseRed.copy(alpha = 0.85f),
                    onClick = { 
                        isVideoOff = !isVideoOff
                        callViewModel.setVideoEnabled(!isVideoOff)
                        Toast.makeText(context, if (isVideoOff) "Camera Turned Off" else "Camera Turned On", Toast.LENGTH_SHORT).show()
                    }
                )

                // Screen Share Button (Cyan ON / Translucent OFF)
                MeetingControlButton(
                    icon = if (callState.isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.PresentToAll,
                    isActive = callState.isScreenSharing,
                    activeColor = NeonCyan.copy(alpha = 0.85f),
                    inactiveColor = Color.White.copy(alpha = 0.12f),
                    onClick = {
                        if (callState.isScreenSharing) {
                            callViewModel.stopScreenSharing()
                            com.developer_rahul.meetmind_ai.core.media.recording.RecordingService.stopService(context)
                            Toast.makeText(context, "Screen Sharing Stopped", Toast.LENGTH_SHORT).show()
                        } else {
                            com.developer_rahul.meetmind_ai.core.media.recording.RecordingService.startService(context)
                            val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            screenShareLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                        }
                    }
                )

                // More Controls Sheet Button
                MeetingControlButton(
                    icon = Icons.Default.MoreHoriz,
                    isActive = false,
                    onClick = { showMoreSheet = true }
                )
                
                // End Call CTA Button
                Surface(
                    onClick = { showLeaveDialog = true },
                    shape = CircleShape,
                    color = RoseRed,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(52.dp)
                        .bounceClick()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(24.dp))
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
    onAiInsights: () -> Unit,
    onToggleChat: () -> Unit = {},
    onToggleParticipants: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Text("Meeting Controls", style = Typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        
        ControlItem(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = "In-Meeting Chat",
            color = NeonCyan,
            onClick = onToggleChat
        )
        ControlItem(
            icon = Icons.Default.People,
            title = "Participants List",
            color = ElectricIndigo,
            onClick = onToggleParticipants
        )
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
            title = "Invite People to Join",
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
            title = "AI Meeting Summary & Action Items",
            color = ElectricIndigo,
            onClick = onAiInsights
        )
        ControlItem(
            icon = Icons.Default.Translate,
            title = "Live Translation & Subtitles",
            onClick = onToggleTranslationSettings
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
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background((if (color == TextPrimary) ElectricIndigo else color).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (color == TextPrimary) ElectricIndigo else color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = Typography.bodyLarge, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ParticipantVideoTile(participant: Participant) {
    Box(
        modifier = Modifier
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .then(
                if (participant.isActiveSpeaker && participant.isOnline) {
                    Modifier.border(2.5.dp, EmeraldGreen, RoundedCornerShape(20.dp))
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                }
            )
    ) {
        val activeVideoTrack = participant.screenTrack ?: participant.videoTrack
        if (activeVideoTrack != null && (participant.isVideoOn || participant.screenTrack != null)) {
            VideoRenderer(
                modifier = Modifier.fillMaxSize(),
                videoTrack = activeVideoTrack
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AvatarPlaceholder(
                    name = participant.name, 
                    isActiveSpeaker = participant.isActiveSpeaker && participant.isOnline, 
                    sizeDp = 72
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (!participant.isOnline) Icons.Default.CloudOff else if (!participant.isMicOn) Icons.Default.MicOff else Icons.Default.Mic,
                null, 
                tint = if (!participant.isOnline || !participant.isMicOn) RoseRed else EmeraldGreen, 
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                participant.name + if (participant.isOnline) "" else " (Offline)",
                color = if (participant.isOnline) Color.White else TextDisabled,
                style = Typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (participant.isHost) {
                Spacer(modifier = Modifier.width(6.dp))
                StatusChip(text = "HOST", color = ElectricIndigo)
            }
            if (participant.isAiRep) {
                Spacer(modifier = Modifier.width(6.dp))
                StatusChip(text = "AI", color = NeonCyan)
            }
            if (participant.screenTrack != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.PresentToAll, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
            }
        }
        
        if (participant.isActiveSpeaker && participant.isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(EmeraldGreen.copy(alpha = 0.2f), CircleShape)
                    .padding(6.dp)
            ) {
                Icon(Icons.Default.VolumeUp, null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
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
        modifier = Modifier
            .size(48.dp)
            .bounceClick()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
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
    val isHost: Boolean = false,
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
            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
            .border(1.dp, GlassWhite, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        subtitles.forEach { subtitle ->
            Text(
                text = "${subtitle.speaker}: ${subtitle.translatedText}",
                color = Color.White,
                style = Typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
        if (originalText.isNotBlank()) {
            Text(
                text = originalText,
                color = Color.White.copy(alpha = 0.7f),
                style = Typography.bodyMedium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TranslationSettingsContent(
    enabled: Boolean,
    audioTranslationEnabled: Boolean,
    onToggleSubtitles: (Boolean) -> Unit,
    onToggleAudio: (Boolean) -> Unit,
    sourceLanguage: String,
    onSourceLanguageSelect: (String) -> Unit,
    selectedLanguage: String,
    onTargetLanguageSelect: (String) -> Unit
) {
    val targetLanguages = listOf(
        "en" to "English",
        "hi" to "Hindi (हिंदी)",
        "mr" to "Marathi (मराठी)"
    )

    val spokenLanguages = listOf(
        "en-US" to "English (US)",
        "hi-IN" to "Hindi (हिंदी)",
        "mr-IN" to "Marathi (मराठी)"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Text("Real-Time Translation Settings", style = Typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Enable Live Subtitles", style = Typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text("Show captions on screen in real time", style = Typography.bodySmall, color = TextSecondary)
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggleSubtitles,
                colors = SwitchDefaults.colors(checkedThumbColor = ElectricIndigo)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Audio Voice Output (TTS)", style = Typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text("Listen to translated speech out loud", style = Typography.bodySmall, color = TextSecondary)
            }
            Switch(
                checked = audioTranslationEnabled,
                onCheckedChange = onToggleAudio,
                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Your Spoken Language", style = Typography.titleMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(modifier = Modifier.selectableGroup()) {
            spokenLanguages.forEach { (code, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .selectable(
                            selected = (code == sourceLanguage),
                            onClick = { onSourceLanguageSelect(code) },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (code == sourceLanguage),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricIndigo)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = name, style = Typography.bodyLarge, color = TextPrimary)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        Text("Your Preferred Listening Language", style = Typography.titleMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(modifier = Modifier.selectableGroup()) {
            targetLanguages.forEach { (code, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .selectable(
                            selected = (code == selectedLanguage),
                            onClick = { onTargetLanguageSelect(code) },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (code == selectedLanguage),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = name, style = Typography.bodyLarge, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun AvatarPlaceholder(name: String, isActiveSpeaker: Boolean = false, sizeDp: Int = 72) {
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifEmpty { name.take(1).uppercase() }

    val gradients = listOf(
        listOf(Color(0xFF6366F1), Color(0xFFA855F7)),
        listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)),
        listOf(Color(0xFF10B981), Color(0xFF059669)),
        listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
        listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
    )
    val colorIndex = Math.abs(name.hashCode()) % gradients.size
    val gradientColors = gradients[colorIndex]

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActiveSpeaker) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
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
                style = if (sizeDp > 50) Typography.headlineMedium else Typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun InMeetingChatSheetContent(
    meetingId: String,
    onFullScreenChat: () -> Unit,
    chatViewModel: ChatViewModel = viewModel(factory = provideChatViewModelFactory(meetingId.toLongOrNull() ?: -1L))
) {
    val uiState by chatViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("In-Meeting Live Chat", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Real-time communication", style = Typography.labelSmall, color = EmeraldGreen)
                }
            }

            IconButton(onClick = onFullScreenChat) {
                Icon(Icons.Default.OpenInNew, contentDescription = "Expand Fullscreen", tint = TextPrimary)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (uiState.isLoading && uiState.messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                reverseLayout = true
            ) {
                items(uiState.messages, key = { it.id }) { chatMessage ->
                    ChatBubble(chatMessage)
                }
                if (uiState.messages.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No messages yet. Start the conversation!", color = TextSecondary, style = Typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.pendingMessage,
                onValueChange = chatViewModel::onMessageChange,
                placeholder = { Text("Send message to everyone...", color = TextDisabled) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                onClick = chatViewModel::sendMessage,
                enabled = uiState.pendingMessage.isNotBlank(),
                shape = CircleShape,
                color = if (uiState.pendingMessage.isNotBlank()) NeonCyan else GlassWhite,
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (uiState.pendingMessage.isNotBlank()) Color.Black else TextDisabled,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

