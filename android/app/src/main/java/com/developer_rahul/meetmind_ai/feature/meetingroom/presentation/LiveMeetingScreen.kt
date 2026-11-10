package com.developer_rahul.meetmind_ai.feature.meetingroom.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.MeetingStatus
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.media.audio.AudioDeviceManager
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
import com.developer_rahul.meetmind_ai.core.media.recording.RecordingService
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.ParticipantRole
import com.developer_rahul.meetmind_ai.feature.meetings.presentation.MeetingViewModel
import com.developer_rahul.meetmind_ai.feature.translation.domain.model.Subtitle
import com.developer_rahul.meetmind_ai.feature.translation.presentation.LiveTranslationViewModel
import org.webrtc.VideoTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMeetingScreen(
    meetingId: String,
    onLeaveMeeting: (meetingId: String, duration: String) -> Unit,
    onToggleChat: (meetingId: String) -> Unit,
    onToggleParticipants: (meetingId: String) -> Unit,
    onViewTranscript: (meetingId: String) -> Unit = {},
    onViewIntelligence: (meetingId: String) -> Unit = {},
    meetingViewModel: MeetingViewModel = viewModel(factory = ViewModelFactory),
    callViewModel: LiveMeetingViewModel = viewModel(factory = provideLiveMeetingViewModelFactory(meetingId.toLongOrNull() ?: -1L)),
    translationViewModel: LiveTranslationViewModel = viewModel(factory = provideLiveTranslationViewModelFactory(meetingId.toLongOrNull() ?: -1L))
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val uiState by meetingViewModel.uiState.collectAsState()
    val callState by callViewModel.uiState.collectAsState()
    val translationState by translationViewModel.uiState.collectAsState()
    val recentSubtitles by translationViewModel.subtitles.collectAsState()

    val meeting = uiState.selectedMeeting
    val isRecording = uiState.isRecording

    val effectiveMeetingId = remember(meeting?.id, meetingId) {
        meeting?.id?.takeIf { it > 0L } ?: meetingId.toLongOrNull() ?: -1L
    }
    val effectiveMeetingIdString = remember(meeting?.id, meetingId) {
        (meeting?.id?.takeIf { it > 0L } ?: meetingId.toLongOrNull())?.toString() ?: meetingId
    }

    LaunchedEffect(effectiveMeetingId) {
        if (effectiveMeetingId > 0L) {
            callViewModel.updateMeetingId(effectiveMeetingId)
            translationViewModel.updateMeetingId(effectiveMeetingId)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (cameraGranted && audioGranted) {
            AudioDeviceManager.selectAudioDevice(context, "Speaker")
            callViewModel.startCall(effectiveMeetingId.takeIf { it > 0L })
        } else {
            Toast.makeText(
                context,
                "Camera and microphone permissions are required for live meetings.",
                Toast.LENGTH_LONG
            ).show()
        }
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
        AudioDeviceManager.selectAudioDevice(context, "Speaker")

        if (!hasCamera || !hasAudio) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        } else {
            callViewModel.startCall(effectiveMeetingId.takeIf { it > 0L })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AudioDeviceManager.resetToNormalMode(context)
        }
    }

    var isMuted by remember { mutableStateOf(false) }
    var isVideoOff by remember { mutableStateOf(false) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isSwappedByPip by remember { mutableStateOf(false) }
    
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val startMs = System.currentTimeMillis()
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - startMs) / 1000L
            kotlinx.coroutines.delay(1000L)
        }
    }
    val formattedDuration = remember(elapsedSeconds) {
        val mins = elapsedSeconds / 60
        val secs = elapsedSeconds % 60
        "%02d:%02d".format(mins, secs)
    }

    // Auto-exit all participants when host ends the meeting for everyone
    LaunchedEffect(meeting?.status) {
        if (meeting?.status == MeetingStatus.ENDED) {
            Toast.makeText(context, "The host has ended this meeting for everyone.", Toast.LENGTH_LONG).show()
            callViewModel.leaveMeeting()
            onLeaveMeeting(effectiveMeetingIdString, formattedDuration)
        }
    }

    var showMoreSheet by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showRecordingConsent by remember { mutableStateOf(false) }
    var showTranslationSettings by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showAudioDeviceSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val recordingLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val id = effectiveMeetingId.takeIf { it > 0L } ?: meeting?.id
            if (data != null && id != null && id > 0L) {
                meetingViewModel.startRecording(id, data)
            } else {
                RecordingService.stopService(context)
            }
        } else {
            RecordingService.stopService(context)
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
                        label = { Text("Invite via Email Broadcast") },
                        placeholder = { Text("user1@example.com, user2@example.com") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricIndigo,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    if (inviteMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            inviteMessage!!, 
                            style = Typography.bodySmall, 
                            color = if (inviteMessage!!.startsWith("Failed") || inviteMessage!!.startsWith("Please")) RoseRed else EmeraldGreen, 
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inviteEmailInput.isNotBlank()) {
                            val id = meetingId.toLongOrNull() ?: meeting.id
                            if (id > 0L) {
                                meetingViewModel.batchInviteParticipants(id, inviteEmailInput) { success, msg ->
                                    inviteMessage = msg
                                    if (success) {
                                        inviteEmailInput = ""
                                    }
                                }
                            } else {
                                inviteMessage = "Failed: Invalid meeting ID"
                            }
                        }
                    },
                    modifier = Modifier.bounceClick()
                ) {
                    Text("Send Broadcast", color = NeonCyan, fontWeight = FontWeight.Bold)
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
        val numericMeetingId = meetingId.toLongOrNull() ?: effectiveMeeting?.id ?: effectiveMeetingId.takeIf { it > 0L }

        if (isHost) {
            HostLeaveOrEndDialog(
                onDismiss = { showLeaveDialog = false },
                onLeave = {
                    showLeaveDialog = false
                    if (numericMeetingId != null) {
                        meetingViewModel.leaveMeeting(numericMeetingId)
                    }
                    callViewModel.leaveMeeting()
                    onLeaveMeeting(effectiveMeetingIdString, formattedDuration)
                },
                onEndForEveryone = {
                    showLeaveDialog = false
                    if (numericMeetingId != null) {
                        meetingViewModel.endMeeting(numericMeetingId)
                    }
                    callViewModel.leaveMeeting()
                    onLeaveMeeting(effectiveMeetingIdString, formattedDuration)
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
                    onLeaveMeeting(effectiveMeetingIdString, formattedDuration)
                }
            )
        }
    }

    if (showRecordingConsent) {
        val id = meetingId.toLongOrNull() ?: meeting?.id ?: effectiveMeetingId.takeIf { it > 0L }
        RecordingConsentDialog(
            onDismiss = { showRecordingConsent = false },
            onConfirmScreenAndAudio = {
                showRecordingConsent = false
                RecordingService.startService(context, isAudioOnly = false)
                val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                recordingLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
            },
            onConfirmAudioOnly = {
                showRecordingConsent = false
                if (id != null && id > 0L) {
                    meetingViewModel.startAudioOnlyRecording(id)
                    Toast.makeText(context, "Audio-only recording started", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Meeting not ready yet", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showAudioDeviceSheet) {
        val availableDevices = remember(context) {
            AudioDeviceManager.getAvailableAudioDevices(context)
        }
        ModalBottomSheet(
            onDismissRequest = { showAudioDeviceSheet = false },
            containerColor = BackgroundSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextDisabled) }
        ) {
            AudioDeviceSelectorSheet(
                availableDevices = availableDevices,
                selectedDevice = callState.selectedAudioDevice,
                onSelectDevice = { device ->
                    callViewModel.selectAudioDevice(context, device)
                    showAudioDeviceSheet = false
                    Toast.makeText(context, "Audio routed to $device", Toast.LENGTH_SHORT).show()
                }
            )
        }
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
                selectedAudioDevice = callState.selectedAudioDevice,
                onToggleAudioDevice = {
                    showMoreSheet = false
                    showAudioDeviceSheet = true
                },
                onToggleRecording = { 
                    val id = meetingId.toLongOrNull() ?: meeting?.id
                    if (id != null && id > 0L) {
                        if (isRecording) {
                            meetingViewModel.stopRecording(id)
                            Toast.makeText(context, "Stopping recording & uploading...", Toast.LENGTH_SHORT).show()
                        } else {
                            showRecordingConsent = true
                        }
                    } else {
                        Toast.makeText(context, "Meeting not ready yet", Toast.LENGTH_SHORT).show()
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
                    onViewTranscript(effectiveMeetingIdString)
                },
                onAiInsights = { 
                    showMoreSheet = false 
                    onViewIntelligence(effectiveMeetingIdString)
                },
                onToggleChat = {
                    showMoreSheet = false
                    showChatSheet = true
                },
                onToggleParticipants = {
                    showMoreSheet = false
                    onToggleParticipants(effectiveMeetingIdString)
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
                meetingId = effectiveMeetingIdString,
                onFullScreenChat = {
                    showChatSheet = false
                    onToggleChat(effectiveMeetingIdString)
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
        val knownUserIds = uiState.participants.mapNotNull { it.userId }.toSet()
        val extraFromWebRtc: List<com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Participant> = callState.participants.keys
            .filter { it !in knownUserIds && it > 0L }
            .map { peerId: Long ->
                com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Participant(
                    id = peerId,
                    userId = peerId,
                    name = "Participant $peerId",
                    email = "",
                    role = com.developer_rahul.meetmind_ai.feature.meetings.domain.model.ParticipantRole.PARTICIPANT,
                    status = com.developer_rahul.meetmind_ai.feature.meetings.domain.model.ParticipantStatus.JOINED,
                    isOnline = true
                )
            }
        val allParticipants = uiState.participants + extraFromWebRtc
        val activeParticipants = allParticipants.filter { participant ->
            val isLocal = (currentUserId > 0L && participant.userId == currentUserId) ||
                          (participant.name.endsWith("(You)", ignoreCase = true) || participant.name.equals("You", ignoreCase = true))
            val isAiAgent = participant.role == ParticipantRole.AI_REPRESENTATIVE || participant.role == ParticipantRole.AUTOMATED_AGENT
            val inWebRtc = participant.userId != null && callState.participants.containsKey(participant.userId)
            val isJoined = participant.status == com.developer_rahul.meetmind_ai.feature.meetings.domain.model.ParticipantStatus.JOINED || participant.isOnline
            
            isLocal || inWebRtc || isJoined || isAiAgent
        }.ifEmpty {
            allParticipants.filter {
                it.status == com.developer_rahul.meetmind_ai.feature.meetings.domain.model.ParticipantStatus.JOINED ||
                (currentUserId > 0L && it.userId == currentUserId)
            }
        }
        val participantsList = activeParticipants.distinctBy { it.userId ?: it.id }
        val totalParticipants = participantsList.size.coerceAtLeast(1)

        val localP2pScreen = if (callState.isScreenSharing) callState.localScreenTrack else null
        val localSfuScreen = if (callState.isScreenSharing) callState.localSfuScreenTrack else null
        val remoteScreenTrack = callState.participants.values.mapNotNull { it.screenTrack }.firstOrNull()
        val remoteSfuScreenTrack = callState.participants.values.mapNotNull { it.sfuScreenTrack }.firstOrNull()
        val activeP2pScreenTrack = localP2pScreen ?: remoteScreenTrack
        val activeSfuScreenTrack = localSfuScreen ?: remoteSfuScreenTrack
        val hasActiveScreenShare = activeP2pScreenTrack != null || activeSfuScreenTrack != null

        val remoteParticipants = participantsList.filter { participant ->
            val isLocal = (currentUserId > 0L && participant.userId == currentUserId) ||
                          (participant.name.endsWith("(You)", ignoreCase = true) || participant.name.equals("You", ignoreCase = true))
            !isLocal
        }

        val isHostUser = ((meeting ?: uiState.meetings.find { it.id.toString() == meetingId || it.meetingCode == meetingId })?.hostId == currentUserId && currentUserId > 0L) ||
                         (currentUserId > 0L && uiState.participants.any { it.userId == currentUserId && it.role == ParticipantRole.HOST })

        val localUiParticipant = Participant(
            id = "local_user_$currentUserId",
            name = "You",
            isActiveSpeaker = !isMuted,
            isOnline = true,
            isHost = isHostUser,
            videoTrack = if (!isVideoOff) callState.localVideoTrack else null,
            sfuVideoTrack = if (!isVideoOff) callState.localSfuVideoTrack else null,
            screenTrack = if (callState.isScreenSharing) callState.localScreenTrack else null,
            sfuScreenTrack = if (callState.isScreenSharing) callState.localSfuScreenTrack else null,
            isMicOn = !isMuted,
            isVideoOn = !isVideoOff
        )

        fun toUiParticipant(participant: com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Participant): Participant {
            val isLocalUser = (currentUserId > 0L && participant.userId == currentUserId) ||
                              (participant.name.endsWith("(You)", ignoreCase = true) || participant.name.equals("You", ignoreCase = true))
            if (isLocalUser) return localUiParticipant

            val mediaState = callState.participants[participant.userId] ?: callState.participants[participant.id]
            val isAiAgent = participant.role == ParticipantRole.AI_REPRESENTATIVE || participant.role == ParticipantRole.AUTOMATED_AGENT
            val currentAiSpeech = callState.activeAiSpeech
            val isAiCurrentlySpeaking = isAiAgent && currentAiSpeech != null && (currentAiSpeech.ownerId == participant.userId || participant.name.contains(currentAiSpeech.ownerName, ignoreCase = true))

            return Participant(
                id = participant.id.toString(),
                name = participant.name,
                isAiRep = isAiAgent,
                isHost = participant.role == ParticipantRole.HOST,
                isOnline = true,
                videoTrack = mediaState?.videoTrack,
                sfuVideoTrack = mediaState?.sfuVideoTrack,
                isMicOn = mediaState?.audioEnabled ?: true,
                isVideoOn = mediaState?.videoEnabled ?: (mediaState?.videoTrack != null || mediaState?.sfuVideoTrack != null),
                isActiveSpeaker = isAiCurrentlySpeaking || (mediaState?.isSpeaking ?: false),
                screenTrack = mediaState?.screenTrack,
                sfuScreenTrack = mediaState?.sfuScreenTrack
            )
        }

        val allUiParticipants = if (remoteParticipants.isEmpty()) {
            listOf(localUiParticipant)
        } else {
            listOf(localUiParticipant) + remoteParticipants.map { toUiParticipant(it) }
        }

        // Central Adaptive Video Stage
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    areControlsVisible = !areControlsVisible
                }
        ) {
            val stageTopPadding by animateDpAsState(
                targetValue = if (areControlsVisible) 68.dp else 0.dp,
                animationSpec = tween(250),
                label = "stageTopPadding"
            )
            val stageBottomPadding by animateDpAsState(
                targetValue = if (areControlsVisible) 84.dp else 0.dp,
                animationSpec = tween(250),
                label = "stageBottomPadding"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = stageTopPadding, bottom = stageBottomPadding)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                if (hasActiveScreenShare) {
                    // SCREEN SHARE HERO VIEW + PARTICIPANT ROW
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.5f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black)
                                .border(1.5.dp, NeonCyan, RoundedCornerShape(20.dp))
                        ) {
                            VideoRenderer(
                                modifier = Modifier.fillMaxSize(),
                                videoTrack = activeP2pScreenTrack,
                                sfuVideoTrack = activeSfuScreenTrack,
                                isOverlay = false
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

                        Spacer(Modifier.height(8.dp))

                        // Strip of participants
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(allUiParticipants) { p ->
                                ParticipantVideoTile(
                                    participant = p,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(130.dp),
                                    mirror = (p.id == localUiParticipant.id)
                                )
                            }
                        }
                    }
                } else if (remoteParticipants.isEmpty()) {
                    // SOLO MODE (Only local user)
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ParticipantVideoTile(
                            participant = localUiParticipant,
                            modifier = Modifier.fillMaxSize(),
                            mirror = true
                        )

                        SoloMeetingInvitationCard(
                            meetingTitle = meeting?.title ?: "Meeting",
                            meetingCode = meeting?.meetingCode ?: "mm-$meetingId",
                            onCopyCode = {
                                val code = meeting?.meetingCode ?: "mm-$meetingId"
                                clipboardManager.setText(AnnotatedString(code))
                                Toast.makeText(context, "Code copied: $code", Toast.LENGTH_SHORT).show()
                            },
                            onShareLink = {
                                val shareIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Join my MeetMind AI live meeting!\nTitle: ${meeting?.title ?: "Meeting"}\nMeeting Code: ${meeting?.meetingCode ?: meetingId}")
                                    type = "text/plain"
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Meeting Code"))
                            },
                            onInviteEmail = { showInviteDialog = true },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } else if (remoteParticipants.size == 1) {
                    // DUET MODE (Google Meet Standard: Remote full stage, local in bottom-right PIP)
                    val remoteUi = toUiParticipant(remoteParticipants.first())
                    val fullScreenParticipant = if (isSwappedByPip) localUiParticipant else remoteUi
                    val pipParticipant = if (isSwappedByPip) remoteUi else localUiParticipant

                    Box(modifier = Modifier.fillMaxSize()) {
                        ParticipantVideoTile(
                            participant = fullScreenParticipant,
                            modifier = Modifier.fillMaxSize(),
                            mirror = (fullScreenParticipant.id == localUiParticipant.id)
                        )

                        FloatingPipTile(
                            participant = pipParticipant,
                            isVideoOff = if (isSwappedByPip) !remoteUi.isVideoOn else isVideoOff,
                            onFlipCamera = { callViewModel.switchCamera() },
                            onSwap = { isSwappedByPip = !isSwappedByPip },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 8.dp, bottom = 8.dp)
                        )
                    }
                } else if (remoteParticipants.size == 2) {
                    // 2 REMOTE PARTICIPANTS: Split screen in equal halves, local in PIP
                    val remote0 = toUiParticipant(remoteParticipants[0])
                    val remote1 = toUiParticipant(remoteParticipants[1])

                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ParticipantVideoTile(
                                participant = remote0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                mirror = false
                            )
                            ParticipantVideoTile(
                                participant = remote1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                mirror = false
                            )
                        }

                        FloatingPipTile(
                            participant = localUiParticipant,
                            isVideoOff = isVideoOff,
                            onFlipCamera = { callViewModel.switchCamera() },
                            onSwap = {},
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 8.dp, bottom = 8.dp)
                        )
                    }
                } else if (remoteParticipants.size in 3..4) {
                    // 3-4 PARTICIPANTS: 2x2 Balanced Grid
                    val gridList = if (remoteParticipants.size == 3) {
                        listOf(
                            toUiParticipant(remoteParticipants[0]),
                            toUiParticipant(remoteParticipants[1]),
                            toUiParticipant(remoteParticipants[2]),
                            localUiParticipant
                        )
                    } else {
                        remoteParticipants.take(4).map { toUiParticipant(it) }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ParticipantVideoTile(
                                    participant = gridList[0],
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f),
                                    mirror = (gridList[0].id == localUiParticipant.id)
                                )
                                ParticipantVideoTile(
                                    participant = gridList[1],
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f),
                                    mirror = (gridList[1].id == localUiParticipant.id)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ParticipantVideoTile(
                                    participant = gridList[2],
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f),
                                    mirror = (gridList[2].id == localUiParticipant.id)
                                )
                                ParticipantVideoTile(
                                    participant = gridList[3],
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f),
                                    mirror = (gridList[3].id == localUiParticipant.id)
                                )
                            }
                        }

                        if (remoteParticipants.size == 4) {
                            FloatingPipTile(
                                participant = localUiParticipant,
                                isVideoOff = isVideoOff,
                                onFlipCamera = { callViewModel.switchCamera() },
                                onSwap = {},
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                } else {
                    // 5+ PARTICIPANTS: Scrollable 2-Column Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allUiParticipants) { p ->
                            ParticipantVideoTile(
                                participant = p,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                mirror = (p.id == localUiParticipant.id)
                            )
                        }
                    }
                }
            }
        }

        // Top Floating Bar (Slides smoothly with AnimatedVisibility)
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { -it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopMeetingBar(
                meetingTitle = meeting?.title ?: "Live Meeting",
                meetingCode = meeting?.meetingCode ?: "mm-$meetingId",
                formattedDuration = formattedDuration,
                isRecording = isRecording,
                recordingDurationMs = uiState.localRecordingState.durationMs,
                selectedAudioDevice = callState.selectedAudioDevice,
                totalParticipants = totalParticipants,
                onCopyMeetingCode = {
                    val code = meeting?.meetingCode ?: "mm-$meetingId"
                    clipboardManager.setText(AnnotatedString(code))
                    Toast.makeText(context, "Code copied: $code", Toast.LENGTH_SHORT).show()
                },
                onToggleAudioDevice = { showAudioDeviceSheet = true },
                onFlipCamera = { callViewModel.switchCamera() },
                onOpenChat = { showChatSheet = true },
                onOpenParticipants = { onToggleParticipants(effectiveMeetingIdString) },
                onStopRecording = {
                    val id = meetingId.toLongOrNull() ?: meeting?.id ?: effectiveMeetingId.takeIf { it > 0L }
                    if (id != null && id > 0L) {
                        meetingViewModel.stopRecording(id)
                        com.developer_rahul.meetmind_ai.core.media.recording.RecordingService.stopService(context)
                        Toast.makeText(context, "Stopping recording & uploading...", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Active AI Speaking Banner
        val activeSpeech = callState.activeAiSpeech
        if (activeSpeech != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = if (areControlsVisible) 70.dp else 12.dp, start = 14.dp, end = 14.dp)
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0D1117).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(NeonCyan.copy(alpha = 0.15f), CircleShape)
                            .pulse(color = NeonCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = "Speaking",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "AI Proxy for ${activeSpeech.ownerName}",
                                style = Typography.labelMedium,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusChip(text = "SPEAKING", color = EmeraldGreen)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "\"${activeSpeech.spokenText}\"",
                            style = Typography.bodySmall,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Subtitles Overlay
        if (translationState.subtitlesEnabled) {
            val subtitleBottomOffset by animateDpAsState(
                targetValue = if (areControlsVisible) 92.dp else 20.dp,
                animationSpec = tween(250),
                label = "subtitleBottomOffset"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = subtitleBottomOffset)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                SubtitleOverlay(
                    originalText = translationState.currentOriginalText,
                    subtitles = recentSubtitles,
                    errorMessage = translationState.error
                )
            }
        }

        // Bottom Controls Dock (Slides smoothly with AnimatedVisibility)
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            MeetingBottomDock(
                isMuted = isMuted,
                isVideoOff = isVideoOff,
                isScreenSharing = callState.isScreenSharing,
                selectedAudioDevice = callState.selectedAudioDevice,
                onToggleMic = {
                    isMuted = !isMuted
                    callViewModel.setAudioEnabled(!isMuted)
                    Toast.makeText(context, if (isMuted) "Microphone Muted" else "Microphone Unmuted", Toast.LENGTH_SHORT).show()
                },
                onToggleVideo = {
                    isVideoOff = !isVideoOff
                    callViewModel.setVideoEnabled(!isVideoOff)
                    Toast.makeText(context, if (isVideoOff) "Camera Turned Off" else "Camera Turned On", Toast.LENGTH_SHORT).show()
                },
                onToggleScreenShare = {
                    if (callState.isScreenSharing) {
                        callViewModel.stopScreenSharing()
                        com.developer_rahul.meetmind_ai.core.media.recording.RecordingService.stopService(context)
                        Toast.makeText(context, "Screen Sharing Stopped", Toast.LENGTH_SHORT).show()
                    } else {
                        com.developer_rahul.meetmind_ai.core.media.recording.RecordingService.startService(context)
                        val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        screenShareLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                    }
                },
                onToggleAudioDevice = { showAudioDeviceSheet = true },
                onOpenMore = { showMoreSheet = true },
                onEndCall = { showLeaveDialog = true }
            )
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
fun AudioDeviceSelectorSheet(
    availableDevices: List<String>,
    selectedDevice: String,
    onSelectDevice: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Text("Audio Output Device", style = Typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Select output device for meeting audio", style = Typography.bodySmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(20.dp))

        availableDevices.forEach { device ->
            val isSelected = device.equals(selectedDevice, ignoreCase = true)
            val icon = when (device) {
                "Bluetooth" -> Icons.Default.BluetoothAudio
                "Headset" -> Icons.Default.Headset
                "Earpiece" -> Icons.Default.PhoneInTalk
                else -> Icons.Default.VolumeUp
            }

            Surface(
                onClick = { onSelectDevice(device) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) ElectricIndigo.copy(alpha = 0.2f) else CardSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) ElectricIndigo else BorderColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .bounceClick()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isSelected) ElectricIndigo else GlassWhite,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = device,
                            tint = if (isSelected) Color.White else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        device,
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) ElectricIndigo else TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = ElectricIndigo,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoreControlsContent(
    isRecording: Boolean,
    isSharing: Boolean,
    selectedAudioDevice: String = "Speaker",
    onToggleAudioDevice: () -> Unit = {},
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
            icon = when (selectedAudioDevice) {
                "Bluetooth" -> Icons.Default.BluetoothAudio
                "Headset" -> Icons.Default.Headset
                "Earpiece" -> Icons.Default.PhoneInTalk
                else -> Icons.Default.VolumeUp
            },
            title = "Audio Output ($selectedAudioDevice)",
            color = NeonCyan,
            onClick = onToggleAudioDevice
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
fun ParticipantVideoTile(
    participant: Participant,
    modifier: Modifier = Modifier,
    isOverlay: Boolean = false,
    mirror: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .then(
                if (participant.isActiveSpeaker && participant.isOnline) {
                    val speakerBorderColor = if (participant.isAiRep) NeonCyan else EmeraldGreen
                    Modifier.border(2.5.dp, speakerBorderColor, RoundedCornerShape(16.dp))
                } else if (participant.isAiRep) {
                    Modifier.border(1.5.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                }
            )
    ) {
        val cameraTrack = participant.videoTrack
        val sfuCameraTrack = participant.sfuVideoTrack
        if ((cameraTrack != null || sfuCameraTrack != null) && participant.isVideoOn) {
            VideoRenderer(
                modifier = Modifier.fillMaxSize(),
                videoTrack = cameraTrack,
                sfuVideoTrack = sfuCameraTrack,
                isOverlay = isOverlay,
                mirror = mirror
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AvatarPlaceholder(
                    name = participant.name, 
                    isActiveSpeaker = participant.isActiveSpeaker && participant.isOnline, 
                    sizeDp = 68
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (!participant.isOnline) Icons.Default.CloudOff else if (!participant.isMicOn) Icons.Default.MicOff else Icons.Default.Mic,
                    null, 
                    tint = if (!participant.isOnline || !participant.isMicOn) RoseRed else EmeraldGreen, 
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    participant.name + if (participant.isOnline) "" else " (Offline)",
                    color = if (participant.isOnline) Color.White else TextDisabled,
                    style = Typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (participant.isHost) {
                    Spacer(modifier = Modifier.width(4.dp))
                    StatusChip(text = "HOST", color = ElectricIndigo)
                }
                if (participant.isAiRep) {
                    Spacer(modifier = Modifier.width(4.dp))
                    StatusChip(text = "AI", color = NeonCyan)
                }
                if (participant.screenTrack != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.PresentToAll, null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                }
            }
        }
        
        if (participant.isActiveSpeaker && participant.isOnline) {
            val speakerColor = if (participant.isAiRep) NeonCyan else EmeraldGreen
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(speakerColor.copy(alpha = 0.25f), CircleShape)
                    .padding(5.dp)
            ) {
                Icon(
                    if (participant.isAiRep) Icons.Default.GraphicEq else Icons.Default.VolumeUp,
                    null,
                    tint = speakerColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun TopMeetingBar(
    meetingTitle: String,
    meetingCode: String,
    formattedDuration: String,
    isRecording: Boolean,
    recordingDurationMs: Long,
    selectedAudioDevice: String,
    totalParticipants: Int,
    onCopyMeetingCode: () -> Unit,
    onToggleAudioDevice: () -> Unit,
    onFlipCamera: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenParticipants: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .glassmorphism(),
        shape = RoundedCornerShape(24.dp),
        color = GlassBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Title + Code Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(EmeraldGreen, CircleShape)
                        .pulse(color = EmeraldGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = meetingTitle,
                    style = Typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    onClick = onCopyMeetingCode,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.bounceClick()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            meetingCode,
                            style = Typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ContentCopy, null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                    }
                }
            }

            // Right Action Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Duration & REC Pill
                if (isRecording) {
                    Surface(
                        onClick = onStopRecording,
                        color = RoseRed.copy(alpha = 0.9f),
                        shape = CircleShape,
                        modifier = Modifier.bounceClick()
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
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                "REC ${formatDuration(recordingDurationMs)}",
                                color = Color.White,
                                style = Typography.labelSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = formattedDuration,
                            style = Typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Audio Route Quick Switcher
                val audioIcon = when (selectedAudioDevice) {
                    "Bluetooth" -> Icons.Default.BluetoothAudio
                    "Headset" -> Icons.Default.Headset
                    "Earpiece" -> Icons.Default.PhoneInTalk
                    else -> Icons.Default.VolumeUp
                }
                IconButton(
                    onClick = onToggleAudioDevice,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        .bounceClick()
                ) {
                    Icon(audioIcon, contentDescription = "Audio Output", tint = Color.White, modifier = Modifier.size(17.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Camera Flip Quick Switcher
                IconButton(
                    onClick = onFlipCamera,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        .bounceClick()
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White, modifier = Modifier.size(17.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Chat Shortcut
                IconButton(
                    onClick = onOpenChat,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        .bounceClick()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = Color.White, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Participants Badge Button
                Surface(
                    onClick = onOpenParticipants,
                    shape = CircleShape,
                    color = ElectricIndigo.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.5f)),
                    modifier = Modifier.bounceClick()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.People, null, tint = ElectricIndigo, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "$totalParticipants",
                            color = TextPrimary,
                            style = Typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingBottomDock(
    isMuted: Boolean,
    isVideoOff: Boolean,
    isScreenSharing: Boolean,
    selectedAudioDevice: String,
    onToggleMic: () -> Unit,
    onToggleVideo: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onToggleAudioDevice: () -> Unit,
    onOpenMore: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 14.dp, start = 14.dp, end = 14.dp)
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xEE1E222D),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1. Mic Button (52dp)
            MeetingControlButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                isActive = !isMuted,
                activeColor = Color(0xFF3C4043),
                inactiveColor = RoseRed,
                size = 52.dp,
                iconSize = 24.dp,
                onClick = onToggleMic
            )

            // 2. Camera Button (52dp)
            MeetingControlButton(
                icon = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                isActive = !isVideoOff,
                activeColor = Color(0xFF3C4043),
                inactiveColor = RoseRed,
                size = 52.dp,
                iconSize = 24.dp,
                onClick = onToggleVideo
            )

            // 3. Screen Share (48dp)
            MeetingControlButton(
                icon = if (isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.PresentToAll,
                isActive = isScreenSharing,
                activeColor = NeonCyan,
                inactiveColor = Color(0xFF3C4043),
                contentColor = if (isScreenSharing) Color.Black else Color.White,
                size = 48.dp,
                iconSize = 22.dp,
                onClick = onToggleScreenShare
            )

            // 4. Audio Output Selector (48dp)
            val audioDeviceIcon = when (selectedAudioDevice) {
                "Bluetooth" -> Icons.Default.BluetoothAudio
                "Headset" -> Icons.Default.Headset
                "Earpiece" -> Icons.Default.PhoneInTalk
                else -> Icons.Default.VolumeUp
            }
            MeetingControlButton(
                icon = audioDeviceIcon,
                isActive = selectedAudioDevice != "Speaker",
                activeColor = NeonCyan,
                inactiveColor = Color(0xFF3C4043),
                contentColor = if (selectedAudioDevice != "Speaker") Color.Black else Color.White,
                size = 48.dp,
                iconSize = 22.dp,
                onClick = onToggleAudioDevice
            )

            // 5. More Options (48dp)
            MeetingControlButton(
                icon = Icons.Default.MoreHoriz,
                isActive = false,
                inactiveColor = Color(0xFF3C4043),
                size = 48.dp,
                iconSize = 22.dp,
                onClick = onOpenMore
            )

            // 6. End Call Button (52dp)
            Surface(
                onClick = onEndCall,
                shape = CircleShape,
                color = RoseRed,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(52.dp)
                    .bounceClick()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SoloMeetingInvitationCard(
    meetingTitle: String,
    meetingCode: String,
    onCopyCode: () -> Unit,
    onShareLink: () -> Unit,
    onInviteEmail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth()
            .shadow(20.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xE61A1E29),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(ElectricIndigo.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = ElectricIndigo,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "You're the only one here",
                style = Typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Share this meeting link with others you want in the call",
                style = Typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                onClick = onCopyCode,
                color = CardSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        meetingCode,
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Copy", style = Typography.labelMedium, color = TextSecondary)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ContentCopy, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onShareLink,
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share Link", fontWeight = FontWeight.Bold, style = Typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onInviteEmail,
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.PersonAdd, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Invite Email", color = NeonCyan, fontWeight = FontWeight.Bold, style = Typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun FloatingPipTile(
    participant: Participant,
    isVideoOff: Boolean,
    onFlipCamera: () -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onSwap,
        modifier = modifier
            .size(width = 96.dp, height = 144.dp)
            .shadow(16.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassWhite)
    ) {
        Box(contentAlignment = Alignment.Center) {
            val cameraTrack = participant.videoTrack
            val sfuCameraTrack = participant.sfuVideoTrack
            if ((cameraTrack != null || sfuCameraTrack != null) && participant.isVideoOn) {
                VideoRenderer(
                    modifier = Modifier.fillMaxSize(),
                    videoTrack = cameraTrack,
                    sfuVideoTrack = sfuCameraTrack,
                    isOverlay = true,
                    mirror = true
                )
            } else {
                AvatarPlaceholder(name = participant.name, sizeDp = 42)
            }

            // Camera flip shortcut
            if (participant.isVideoOn && !isVideoOff) {
                IconButton(
                    onClick = onFlipCamera,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(26.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .bounceClick()
                ) {
                    Icon(
                        Icons.Default.Cameraswitch,
                        contentDescription = "Flip Camera",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Name & mic tag
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (!participant.isMicOn) Icons.Default.MicOff else Icons.Default.Mic,
                    null,
                    tint = if (!participant.isMicOn) RoseRed else EmeraldGreen,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    participant.name.take(6),
                    style = Typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MeetingControlButton(
    icon: ImageVector,
    isActive: Boolean,
    activeColor: Color = Color(0xFF3C4043),
    inactiveColor: Color = RoseRed,
    contentColor: Color = Color.White,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isActive) activeColor else inactiveColor,
        shadowElevation = 4.dp,
        modifier = Modifier
            .size(size)
            .bounceClick()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
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
    val sfuVideoTrack: io.livekit.android.room.track.VideoTrack? = null,
    val screenTrack: VideoTrack? = null,
    val sfuScreenTrack: io.livekit.android.room.track.VideoTrack? = null
)

@Composable
fun SubtitleOverlay(
    originalText: String,
    subtitles: List<Subtitle>,
    errorMessage: String? = null
) {
    if (subtitles.isEmpty() && originalText.isBlank() && errorMessage.isNullOrBlank()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A).copy(alpha = 0.88f), RoundedCornerShape(16.dp))
            .border(1.dp, GlassWhite, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!errorMessage.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RoseRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RoseRed,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = errorMessage,
                    color = RoseRed,
                    style = Typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        subtitles.takeLast(3).forEach { subtitle ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(ElectricIndigo.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = subtitle.speaker,
                            color = NeonCyan,
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(EmeraldGreen.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = subtitle.targetLanguage.uppercase(),
                            color = EmeraldGreen,
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle.translatedText,
                    color = Color.White,
                    style = Typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle.originalText.isNotBlank() && !subtitle.originalText.equals(subtitle.translatedText, ignoreCase = true)) {
                    Text(
                        text = "\"${subtitle.originalText}\"",
                        color = TextSecondary,
                        style = Typography.labelSmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }

        if (originalText.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "$originalText...",
                    color = TextPrimary.copy(alpha = 0.85f),
                    style = Typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
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
        "hi" to "Hindi (हिंदी)",
        "mr" to "Marathi (मराठी)",
        "en" to "English",
        "es" to "Spanish (Español)",
        "gu" to "Gujarati (ગુજરાતી)",
        "fr" to "French (Français)",
        "de" to "German (Deutsch)",
        "ja" to "Japanese (日本語)"
    )

    val spokenLanguages = listOf(
        "en-US" to "English (US / India)",
        "hi-IN" to "Hindi (हिंदी)",
        "mr-IN" to "Marathi (मराठी)",
        "es-ES" to "Spanish (Español)",
        "gu-IN" to "Gujarati (ગુજરાતી)",
        "fr-FR" to "French (Français)",
        "de-DE" to "German (Deutsch)",
        "ja-JP" to "Japanese (日本語)"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(ElectricIndigo.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Translate, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Real-Time Translation Settings", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
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

        Spacer(modifier = Modifier.height(24.dp))
        Text("Your Spoken Language", style = Typography.titleMedium, color = TextSecondary, fontWeight = FontWeight.SemiBold)
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
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Your Preferred Subtitle Language", style = Typography.titleMedium, color = TextSecondary, fontWeight = FontWeight.SemiBold)
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
    chatViewModel: ChatViewModel = viewModel(
        key = "InMeetingChat_$meetingId",
        factory = provideChatViewModelFactory(meetingId.toLongOrNull() ?: -1L)
    )
) {
    val uiState by chatViewModel.uiState.collectAsState()

    LaunchedEffect(meetingId) {
        val numId = meetingId.toLongOrNull() ?: -1L
        if (numId > 0L) {
            chatViewModel.updateMeetingId(numId)
        }
    }

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
