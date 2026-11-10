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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.sp
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
import com.developer_rahul.meetmind_ai.core.ui.provideMeetingIntelligenceViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.provideTranscriptViewModelFactory
import com.developer_rahul.meetmind_ai.feature.transcript.presentation.TranscriptViewModel
import com.developer_rahul.meetmind_ai.feature.transcript.domain.model.TranscriptSegment
import android.content.Intent
import com.developer_rahul.meetmind_ai.feature.ai.presentation.MeetingIntelligenceViewModel
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

    val currentUserId = meetingViewModel.currentUserId
    val meetingFromList = uiState.meetings.find { it.id.toString() == meetingId || it.meetingCode == meetingId }
    val effectiveMeeting = meeting ?: meetingFromList
    val isHostByMeeting = effectiveMeeting != null && currentUserId > 0L && effectiveMeeting.hostId == currentUserId
    val isHostByParticipants = currentUserId > 0L && uiState.participants.any { 
        it.userId == currentUserId && it.role == ParticipantRole.HOST 
    }
    val isHost = isHostByMeeting || isHostByParticipants

    val currentUserName = remember(currentUserId, uiState.participants) {
        uiState.participants.find { it.userId == currentUserId }?.name ?: "You"
    }

    LaunchedEffect(currentUserName) {
        if (currentUserName.isNotBlank() && currentUserName != "You") {
            translationViewModel.setCurrentUserName(currentUserName)
        }
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

    LaunchedEffect(isMuted) {
        translationViewModel.onAudioMuteChanged(isMuted)
    }

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
    var showIntelligenceSheet by remember { mutableStateOf(false) }
    var showTranscriptSheet by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showAudioDeviceSheet by remember { mutableStateOf(false) }
    var showParticipantsSheet by remember { mutableStateOf(false) }
    var showHostControlsSheet by remember { mutableStateOf(false) }
    var showMuteAllConfirmDialog by remember { mutableStateOf(false) }
    var participantToRemove by remember { mutableStateOf<Participant?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // Host control reaction listeners
    val hostNotification = callState.hostNotification
    LaunchedEffect(hostNotification) {
        if (!hostNotification.isNullOrBlank()) {
            Toast.makeText(context, hostNotification, Toast.LENGTH_LONG).show()
            callViewModel.clearHostNotification()
        }
    }

    val isMutedByHost = callState.isMutedByHost
    LaunchedEffect(isMutedByHost) {
        if (isMutedByHost) {
            isMuted = true
        }
    }

    val isVideoStoppedByHost = callState.isVideoStoppedByHost
    LaunchedEffect(isVideoStoppedByHost) {
        if (isVideoStoppedByHost) {
            isVideoOff = true
        }
    }

    val wasRemovedByHost = callState.wasRemovedByHost
    LaunchedEffect(wasRemovedByHost) {
        if (wasRemovedByHost) {
            Toast.makeText(context, "You were removed from the meeting by the host.", Toast.LENGTH_LONG).show()
            onLeaveMeeting(effectiveMeetingIdString, formattedDuration)
        }
    }

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
                isHost = isHost,
                selectedAudioDevice = callState.selectedAudioDevice,
                onOpenHostControls = {
                    showMoreSheet = false
                    showHostControlsSheet = true
                },
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
                        if (!isHost && !callState.allowScreenShare) {
                            Toast.makeText(context, "Screen sharing has been disabled by the host", Toast.LENGTH_SHORT).show()
                        } else {
                            com.developer_rahul.meetmind_ai.core.media.recording.RecordingService.startService(context)
                            val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            screenShareLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                        }
                    }
                    showMoreSheet = false
                },
                onToggleTranslationSettings = {
                    showTranslationSettings = true
                    showMoreSheet = false
                },
                onViewTranscript = { 
                    showMoreSheet = false 
                    showTranscriptSheet = true
                },
                onAiInsights = { 
                    showMoreSheet = false 
                    showIntelligenceSheet = true
                },
                subtitlesActive = translationState.subtitlesEnabled,
                activeLanguageBadge = translationState.activeLanguageBadge,
                onToggleChat = {
                    showMoreSheet = false
                    showChatSheet = true
                },
                onToggleParticipants = {
                    showMoreSheet = false
                    showParticipantsSheet = true
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
                allowChat = callState.allowChat,
                isHost = isHost,
                onFullScreenChat = {
                    showChatSheet = false
                    onToggleChat(effectiveMeetingIdString)
                }
            )
        }
    }

    if (showIntelligenceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showIntelligenceSheet = false },
            containerColor = BackgroundSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextDisabled) }
        ) {
            InMeetingIntelligenceSheetContent(
                meetingId = effectiveMeetingIdString,
                onFullScreen = {
                    showIntelligenceSheet = false
                    onViewIntelligence(effectiveMeetingIdString)
                }
            )
        }
    }

    if (showTranscriptSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTranscriptSheet = false },
            containerColor = BackgroundSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextDisabled) }
        ) {
            InMeetingTranscriptSheetContent(
                meetingId = effectiveMeetingIdString,
                onClose = { showTranscriptSheet = false },
                onFullScreen = {
                    showTranscriptSheet = false
                    onViewTranscript(effectiveMeetingIdString)
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

        if (showMuteAllConfirmDialog) {
            MuteAllConfirmDialog(
                onDismiss = { showMuteAllConfirmDialog = false },
                onConfirm = {
                    showMuteAllConfirmDialog = false
                    callViewModel.muteAllParticipants()
                    Toast.makeText(context, "Muted all participants", Toast.LENGTH_SHORT).show()
                }
            )
        }

        participantToRemove?.let { target ->
            RemoveParticipantConfirmDialog(
                participantName = target.name,
                onDismiss = { participantToRemove = null },
                onConfirm = {
                    val targetId = target.id.toLongOrNull() ?: 0L
                    participantToRemove = null
                    if (targetId > 0L) {
                        callViewModel.removeParticipant(targetId)
                        Toast.makeText(context, "Removed ${target.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (showParticipantsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showParticipantsSheet = false },
                containerColor = BackgroundSurface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = TextDisabled) }
            ) {
                InMeetingParticipantsSheetContent(
                    participants = allUiParticipants,
                    currentUserId = currentUserId,
                    isHost = isHost,
                    onMuteAll = { showMuteAllConfirmDialog = true },
                    onMuteParticipant = { targetId ->
                        callViewModel.muteParticipant(targetId)
                        Toast.makeText(context, "Muted participant microphone", Toast.LENGTH_SHORT).show()
                    },
                    onStopParticipantVideo = { targetId ->
                        callViewModel.stopParticipantVideo(targetId)
                        Toast.makeText(context, "Turned off participant camera", Toast.LENGTH_SHORT).show()
                    },
                    onRemoveParticipant = { target ->
                        participantToRemove = target
                    },
                    onOpenHostControls = {
                        showParticipantsSheet = false
                        showHostControlsSheet = true
                    }
                )
            }
        }

        if (showHostControlsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showHostControlsSheet = false },
                containerColor = BackgroundSurface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = TextDisabled) }
            ) {
                HostControlsSheetContent(
                    isLocked = callState.isMeetingLocked,
                    allowScreenShare = callState.allowScreenShare,
                    allowChat = callState.allowChat,
                    allowUnmute = callState.allowUnmute,
                    onLockToggled = { locked ->
                        callViewModel.setMeetingLocked(locked)
                        Toast.makeText(context, if (locked) "Meeting locked" else "Meeting unlocked", Toast.LENGTH_SHORT).show()
                    },
                    onMuteAll = { showMuteAllConfirmDialog = true },
                    onPermissionsUpdated = { allowScreen, allowChat, allowUnmute ->
                        callViewModel.updateHostPermissions(allowScreen, allowChat, allowUnmute)
                        Toast.makeText(context, "Host permissions updated", Toast.LENGTH_SHORT).show()
                    }
                )
            }
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
                isLocked = callState.isMeetingLocked,
                isHost = isHost,
                isSubtitlesEnabled = translationState.subtitlesEnabled,
                activeLanguageBadge = translationState.activeLanguageBadge,
                isAudioTranslationEnabled = translationState.audioTranslationEnabled,
                onOpenHostControls = if (isHost) { { showHostControlsSheet = true } } else null,
                onCopyMeetingCode = {
                    val code = meeting?.meetingCode ?: "mm-$meetingId"
                    clipboardManager.setText(AnnotatedString(code))
                    Toast.makeText(context, "Code copied: $code", Toast.LENGTH_SHORT).show()
                },
                onToggleAudioDevice = { showAudioDeviceSheet = true },
                onFlipCamera = { callViewModel.switchCamera() },
                onOpenChat = { showChatSheet = true },
                onOpenParticipants = { showParticipantsSheet = true },
                onToggleSubtitles = {
                    val newEnabled = !translationState.subtitlesEnabled
                    translationViewModel.toggleSubtitles(newEnabled)
                    val msg = if (newEnabled) "Live Subtitles enabled (${translationState.activeLanguageBadge})" else "Live Subtitles disabled"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onOpenTranslationSettings = { showTranslationSettings = true },
                onOpenIntelligence = { showIntelligenceSheet = true },
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

        // Floating Subtitle / Language Status Indicator Chip
        if (translationState.subtitlesEnabled) {
            Surface(
                onClick = { showTranslationSettings = true },
                shape = CircleShape,
                color = Color(0xEE161B22),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = if (areControlsVisible) 58.dp else 12.dp)
                    .bounceClick()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(EmeraldGreen, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Subtitles: ${translationState.activeLanguageBadge}",
                        style = Typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (translationState.audioTranslationEnabled) {
                        Spacer(Modifier.width(6.dp))
                        Text("•", color = TextSecondary, style = Typography.labelSmall)
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "TTS ON",
                            color = EmeraldGreen,
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
                    if (isMuted && !isHost && !callState.allowUnmute) {
                        Toast.makeText(context, "The host has disabled unmuting for participants", Toast.LENGTH_SHORT).show()
                    } else {
                        isMuted = !isMuted
                        callViewModel.setAudioEnabled(!isMuted)
                        Toast.makeText(context, if (isMuted) "Microphone Muted" else "Microphone Unmuted", Toast.LENGTH_SHORT).show()
                    }
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
                        if (!isHost && !callState.allowScreenShare) {
                            Toast.makeText(context, "Screen sharing has been disabled by the host", Toast.LENGTH_SHORT).show()
                        } else {
                            com.developer_rahul.meetmind_ai.core.media.recording.RecordingService.startService(context)
                            val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            screenShareLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                        }
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
    isHost: Boolean = false,
    selectedAudioDevice: String = "Speaker",
    subtitlesActive: Boolean = false,
    activeLanguageBadge: String = "",
    onOpenHostControls: () -> Unit = {},
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

        if (isHost) {
            ControlItem(
                icon = Icons.Default.AdminPanelSettings,
                title = "Host Controls",
                color = Color(0xFFF59E0B),
                onClick = onOpenHostControls
            )
        }
        
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
            title = "Meeting Intelligence (Live AI Copilot)",
            color = ElectricIndigo,
            onClick = onAiInsights
        )
        ControlItem(
            icon = Icons.Default.Translate,
            title = if (subtitlesActive) "Live Translation ($activeLanguageBadge • Active)" else "Live Translation & Subtitles",
            color = if (subtitlesActive) EmeraldGreen else TextPrimary,
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
    isLocked: Boolean = false,
    isHost: Boolean = false,
    isSubtitlesEnabled: Boolean = false,
    activeLanguageBadge: String = "",
    isAudioTranslationEnabled: Boolean = false,
    onOpenHostControls: (() -> Unit)? = null,
    onCopyMeetingCode: () -> Unit,
    onToggleAudioDevice: () -> Unit,
    onFlipCamera: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenParticipants: () -> Unit,
    onToggleSubtitles: () -> Unit = {},
    onOpenTranslationSettings: () -> Unit = {},
    onOpenIntelligence: () -> Unit = {},
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

                if (isLocked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Locked", style = Typography.labelSmall, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                        }
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

                // CC Subtitles & Translation Quick Toggle
                IconButton(
                    onClick = onToggleSubtitles,
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (isSubtitlesEnabled) EmeraldGreen.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f),
                            CircleShape
                        )
                        .bounceClick()
                ) {
                    Icon(
                        Icons.Default.ClosedCaption,
                        contentDescription = "Live Subtitles & Translation",
                        tint = if (isSubtitlesEnabled) EmeraldGreen else Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // AI Meeting Intelligence Button
                IconButton(
                    onClick = onOpenIntelligence,
                    modifier = Modifier
                        .size(34.dp)
                        .background(ElectricIndigo.copy(alpha = 0.25f), CircleShape)
                        .bounceClick()
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI Meeting Intelligence",
                        tint = ElectricIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isHost && onOpenHostControls != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onOpenHostControls,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f), CircleShape)
                            .bounceClick()
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = "Host Controls", tint = Color(0xFFF59E0B), modifier = Modifier.size(17.dp))
                    }
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
    allowChat: Boolean = true,
    isHost: Boolean = false,
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

        val isChatDisabled = !isHost && !allowChat
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = if (isChatDisabled) "" else uiState.pendingMessage,
                onValueChange = { if (!isChatDisabled) chatViewModel.onMessageChange(it) },
                enabled = !isChatDisabled,
                placeholder = {
                    Text(
                        if (isChatDisabled) "Chat is disabled by the host" else "Send message to everyone...",
                        color = if (isChatDisabled) RoseRed else TextDisabled
                    )
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    disabledContainerColor = CardSurface.copy(alpha = 0.5f),
                    disabledBorderColor = RoseRed.copy(alpha = 0.3f)
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                onClick = chatViewModel::sendMessage,
                enabled = !isChatDisabled && uiState.pendingMessage.isNotBlank(),
                shape = CircleShape,
                color = if (!isChatDisabled && uiState.pendingMessage.isNotBlank()) NeonCyan else GlassWhite,
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (!isChatDisabled && uiState.pendingMessage.isNotBlank()) Color.Black else TextDisabled,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// HOST CONTROL DIALOGS & BOTTOM SHEETS
// =========================================================================

@Composable
fun MuteAllConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mute Everyone?", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "This will mute microphones for all participants in the meeting room.",
                style = Typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
            ) {
                Text("Mute all", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = BackgroundSurface
    )
}

@Composable
fun RemoveParticipantConfirmDialog(
    participantName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove Participant?", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Are you sure you want to remove $participantName from this meeting? They will be disconnected immediately.",
                style = Typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
            ) {
                Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = BackgroundSurface
    )
}

@Composable
fun HostControlsSheetContent(
    isLocked: Boolean,
    allowScreenShare: Boolean,
    allowChat: Boolean,
    allowUnmute: Boolean,
    onLockToggled: (Boolean) -> Unit,
    onMuteAll: () -> Unit,
    onPermissionsUpdated: (Boolean, Boolean, Boolean) -> Unit
) {
    var screenShareState by remember(allowScreenShare) { mutableStateOf(allowScreenShare) }
    var chatState by remember(allowChat) { mutableStateOf(allowChat) }
    var unmuteState by remember(allowUnmute) { mutableStateOf(allowUnmute) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Host Controls",
                    style = Typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "Manage meeting security and permissions",
                    style = Typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Action: Mute All
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161B22),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Mute all participants",
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        "Mute microphones for everyone in the room",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onMuteAll,
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.MicOff, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mute all", color = Color.White, style = Typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Meeting Security: Lock Meeting
        Text(
            "Meeting Security",
            style = Typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161B22),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isLocked) Color(0xFFF59E0B) else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Lock meeting",
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            if (isLocked) "Meeting locked. No new participants can join." else "When locked, new participants cannot join.",
                            style = Typography.bodySmall,
                            color = if (isLocked) Color(0xFFF59E0B) else TextSecondary
                        )
                    }
                }
                Switch(
                    checked = isLocked,
                    onCheckedChange = onLockToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFF59E0B)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Participant Permissions
        Text(
            "Let participants:",
            style = Typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161B22),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                // Share Screen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PresentToAll, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Share their screen", style = Typography.bodyMedium, color = TextPrimary)
                    }
                    Switch(
                        checked = screenShareState,
                        onCheckedChange = {
                            screenShareState = it
                            onPermissionsUpdated(screenShareState, chatState, unmuteState)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ElectricIndigo
                        )
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                // Send Chat Messages
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Send in-meeting chat messages", style = Typography.bodyMedium, color = TextPrimary)
                    }
                    Switch(
                        checked = chatState,
                        onCheckedChange = {
                            chatState = it
                            onPermissionsUpdated(screenShareState, chatState, unmuteState)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ElectricIndigo
                        )
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                // Turn on Microphone
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Turn on their microphone", style = Typography.bodyMedium, color = TextPrimary)
                    }
                    Switch(
                        checked = unmuteState,
                        onCheckedChange = {
                            unmuteState = it
                            onPermissionsUpdated(screenShareState, chatState, unmuteState)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ElectricIndigo
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun InMeetingIntelligenceSheetContent(
    meetingId: String,
    onFullScreen: () -> Unit,
    viewModel: MeetingIntelligenceViewModel = viewModel(
        key = "InMeetingIntelligence_$meetingId",
        factory = provideMeetingIntelligenceViewModelFactory(meetingId.toLongOrNull() ?: -1L)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val summary = uiState.summary
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Summary", "Tasks", "Decisions", "Topics")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            Brush.linearGradient(listOf(ElectricIndigo, NeonCyan)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Meeting Intelligence",
                        style = Typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Live AI Copilot & Real-Time Synthesis",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.loadIntelligence() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onFullScreen,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.OpenInFull,
                        contentDescription = "Full Screen",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = ElectricIndigo,
            divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.08f)) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            style = Typography.labelMedium,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) ElectricIndigo else TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ElectricIndigo, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Synthesizing meeting insights...", color = TextSecondary, style = Typography.bodySmall)
                }
            }
        } else if (summary == null || (summary.summary.isBlank() && summary.actionItems.isEmpty() && summary.decisions.isEmpty())) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = TextDisabled,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Listening to conversation...",
                        style = Typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "As participants speak, MeetMind AI will synthesize live summaries, action items, and key decisions.",
                        style = Typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadIntelligence() },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Generate AI Summary", style = Typography.labelMedium)
                    }
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> InMeetingSummaryTab(summary.summary, summary.keyPoints)
                    1 -> InMeetingTasksTab(summary.actionItems) { id, completed ->
                        viewModel.updateActionItemStatus(id, completed)
                    }
                    2 -> InMeetingDecisionsTab(summary.decisions)
                    3 -> InMeetingTopicsTab(summary.topics)
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { viewModel.loadIntelligence() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Refresh AI Insights", style = Typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InMeetingSummaryTab(summaryText: String, keyPoints: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (summaryText.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF161B22),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = ElectricIndigo, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Executive Brief", style = Typography.labelLarge, color = ElectricIndigo, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(summaryText, style = Typography.bodyMedium, color = TextPrimary, lineHeight = 22.sp)
                    }
                }
            }
        }

        if (keyPoints.isNotEmpty()) {
            item {
                Text("Key Discussion Points", style = Typography.titleSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
            }
            items(keyPoints) { point ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161B22),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(point, style = Typography.bodyMedium, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun InMeetingTasksTab(
    actionItems: List<com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.ActionItemResponseDto>,
    onToggleStatus: (Long, Boolean) -> Unit
) {
    if (actionItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No action items extracted yet.", color = TextSecondary, style = Typography.bodyMedium)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(actionItems) { item ->
                val isCompleted = item.status.equals("COMPLETED", ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161B22),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { onToggleStatus(item.id, it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = EmeraldGreen,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.description,
                                style = Typography.bodyMedium,
                                color = if (isCompleted) TextDisabled else TextPrimary,
                                textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )
                            if (!item.assignedUser.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(ElectricIndigo.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "@${item.assignedUser}",
                                            style = Typography.labelSmall,
                                            color = ElectricIndigo,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InMeetingDecisionsTab(decisions: List<String>) {
    if (decisions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No agreed decisions recorded yet.", color = TextSecondary, style = Typography.bodyMedium)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(decisions) { decision ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161B22),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(decision, style = Typography.bodyMedium, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun InMeetingTopicsTab(topics: List<String>) {
    if (topics.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No agenda topics identified yet.", color = TextSecondary, style = Typography.bodyMedium)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(topics) { topic ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161B22),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NeonCyan, CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(topic, style = Typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun InMeetingParticipantsSheetContent(
    participants: List<Participant>,
    currentUserId: Long,
    isHost: Boolean,
    onMuteAll: () -> Unit,
    onMuteParticipant: (Long) -> Unit,
    onStopParticipantVideo: (Long) -> Unit,
    onRemoveParticipant: (Participant) -> Unit,
    onOpenHostControls: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ElectricIndigo.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.People, null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Participants (${participants.size})", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("In this meeting", style = Typography.labelSmall, color = TextSecondary)
                }
            }

            if (isHost) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenHostControls) {
                        Icon(Icons.Default.AdminPanelSettings, "Host Controls", tint = Color(0xFFF59E0B))
                    }
                    TextButton(onClick = onMuteAll) {
                        Icon(Icons.Default.MicOff, null, tint = RoseRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mute all", color = RoseRed, style = Typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(participants, key = { it.id }) { participant ->
                val targetUserId = participant.id.toLongOrNull() ?: 0L
                val isMe = (currentUserId > 0L && targetUserId == currentUserId) || participant.name.contains("(You)")
                var menuExpanded by remember { mutableStateOf(false) }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF161B22),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        AvatarPlaceholder(name = participant.name, isActiveSpeaker = participant.isActiveSpeaker, sizeDp = 40)
                        Spacer(Modifier.width(12.dp))

                        // Name & Role
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    participant.name,
                                    style = Typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isMe) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "(You)",
                                        style = Typography.labelSmall,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (participant.isHost) {
                                Text(
                                    "Meeting Host",
                                    style = Typography.labelSmall,
                                    color = Color(0xFFF59E0B),
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (participant.isAiRep) {
                                Text(
                                    "AI Representative",
                                    style = Typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Media Status Icons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (participant.isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = if (participant.isMicOn) "Mic on" else "Mic muted",
                                tint = if (participant.isMicOn) EmeraldGreen else RoseRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                if (participant.isVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = if (participant.isVideoOn) "Camera on" else "Camera off",
                                tint = if (participant.isVideoOn) Color.White else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )

                            // Host Actions Menu
                            if (isHost && !isMe && !participant.isHost && !participant.isAiRep) {
                                Spacer(Modifier.width(6.dp))
                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, "Actions", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier.background(Color(0xFF1F242C))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Mute microphone", color = TextPrimary) },
                                            leadingIcon = { Icon(Icons.Default.MicOff, null, tint = TextPrimary) },
                                            onClick = {
                                                menuExpanded = false
                                                onMuteParticipant(targetUserId)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Turn off camera", color = TextPrimary) },
                                            leadingIcon = { Icon(Icons.Default.VideocamOff, null, tint = TextPrimary) },
                                            onClick = {
                                                menuExpanded = false
                                                onStopParticipantVideo(targetUserId)
                                            }
                                        )
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                        DropdownMenuItem(
                                            text = { Text("Remove from meeting", color = RoseRed, fontWeight = FontWeight.Bold) },
                                            leadingIcon = { Icon(Icons.Default.PersonRemove, null, tint = RoseRed) },
                                            onClick = {
                                                menuExpanded = false
                                                onRemoveParticipant(participant)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InMeetingTranscriptSheetContent(
    meetingId: String,
    onClose: () -> Unit,
    onFullScreen: () -> Unit,
    transcriptViewModel: TranscriptViewModel = viewModel(
        key = "InMeetingTranscript_$meetingId",
        factory = provideTranscriptViewModelFactory(meetingId.toLongOrNull() ?: -1L)
    )
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by transcriptViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.filteredSegments.size) {
        if (uiState.filteredSegments.isNotEmpty()) {
            listState.animateScrollToItem(uiState.filteredSegments.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Subtitles, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Live Transcript", style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(EmeraldGreen, CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text("STREAMING LIVE", style = Typography.labelSmall.copy(fontSize = 10.sp), color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val text = transcriptViewModel.getFormattedTranscriptText()
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "Transcript copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = {
                        val text = transcriptViewModel.getFormattedTranscriptText()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, text)
                            putExtra(Intent.EXTRA_SUBJECT, "Live Meeting Transcript")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Live Transcript"))
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onFullScreen,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Full Screen", tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Target language chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            transcriptViewModel.availableLanguages.forEach { lang ->
                val isSelected = uiState.selectedLanguage == lang.code
                FilterChip(
                    selected = isSelected,
                    onClick = { transcriptViewModel.selectLanguage(lang.code) },
                    label = { 
                        Text(
                            lang.displayName, 
                            style = Typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricIndigo,
                        selectedLabelColor = Color.White,
                        containerColor = BackgroundSurface,
                        labelColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { transcriptViewModel.onSearch(it) },
            placeholder = { Text("Filter live transcript...", style = Typography.bodySmall, color = TextDisabled) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { transcriptViewModel.onSearch("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricIndigo,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface
            ),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        // List
        if (uiState.isLoading && uiState.filteredSegments.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricIndigo, modifier = Modifier.size(32.dp))
            }
        } else if (uiState.filteredSegments.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Hearing, null, tint = TextDisabled, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Listening for live speech...", style = Typography.bodyMedium, color = TextSecondary)
                    Text("Spoken utterances will appear here in real time as participants talk.", style = Typography.bodySmall, color = TextDisabled, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(uiState.filteredSegments, key = { it.id }) { seg ->
                    InMeetingTranscriptItem(seg, uiState.searchQuery)
                }
            }
        }
    }
}

@Composable
private fun InMeetingTranscriptItem(
    segment: TranscriptSegment,
    searchQuery: String = ""
) {
    val speakerColor = when {
        segment.speaker.equals("you", ignoreCase = true) -> EmeraldGreen
        segment.speaker.contains("ai", ignoreCase = true) -> NeonCyan
        segment.speaker.contains("host", ignoreCase = true) -> PurpleViolet
        else -> ElectricIndigo
    }

    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = segment.speaker,
                    style = Typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = speakerColor
                )
                Text(
                    text = segment.time,
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextDisabled
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = segment.text,
                style = Typography.bodyMedium,
                color = TextPrimary,
                lineHeight = 20.sp
            )
        }
    }
}
