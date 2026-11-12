package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.MeetMindApplication
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
    onPlayRecording: (String, String) -> Unit = { _, _ -> },
    onViewIntelligence: (String) -> Unit = { _ -> },
    onViewReport: (String) -> Unit = { _ -> },
    onViewTranscript: (String) -> Unit = { _ -> },
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val meeting = uiState.selectedMeeting
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val layout = LocalResponsiveLayout.current

    val currentUserId = remember {
        MeetMindApplication.instance.container.tokenProvider.getUserId()
    }
    val isCurrentUserHost = meeting?.hostId == currentUserId

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
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteEmailInput by remember { mutableStateOf("") }
    var inviteMessage by remember { mutableStateOf<String?>(null) }
    var isSendingInvites by remember { mutableStateOf(false) }

    // Parse friendly date & time
    val formattedTime = remember(meeting?.scheduledAt) {
        MeetingDateTimeUtils.parseMeetingTime(meeting?.scheduledAt)
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    // 1. Delete Confirmation Dialog
    if (showDeleteConfirm && meeting != null) {
        AlertDialog(
            onDismissRequest = { if (!uiState.actionInProgress) showDeleteConfirm = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(RoseRed.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = RoseRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Delete Meeting?", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Text(
                    "Are you sure you want to delete '${meeting.title}'? All scheduled records, transcripts, and intelligence reports will be removed.",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMeeting(
                            meetingId = meeting.id,
                            onSuccess = {
                                showDeleteConfirm = false
                                Toast.makeText(context, "Meeting deleted", Toast.LENGTH_SHORT).show()
                                onBack()
                            },
                            onError = { err ->
                                showDeleteConfirm = false
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.actionInProgress
                ) {
                    if (uiState.actionInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    enabled = !uiState.actionInProgress
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 2. Upgraded Invite Participants Dialog
    if (showInviteDialog && meeting != null) {
        val meetingCode = meeting.meetingCode ?: "mm-${meeting.id}"

        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(ElectricIndigo.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Invite Participants", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Meeting Code Copy Chip
                    Surface(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(meetingCode))
                            Toast.makeText(context, "Code copied: $meetingCode", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = GlassWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth().bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Meeting Code", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextDisabled)
                                Text(meetingCode, style = Typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = NeonCyan)
                            }
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Native Share Sheet Trigger
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "MeetMind AI Invitation: ${meeting.title}")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Join my MeetMind AI meeting: ${meeting.title}\n📅 Date: ${formattedTime.dateText} at ${formattedTime.timeText}\n🔑 Meeting Code: $meetingCode"
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Meeting Invitation"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth().height(42.dp).bounceClick()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share via Other Apps", style = Typography.labelMedium, fontWeight = FontWeight.Bold, color = ElectricIndigo)
                    }

                    HorizontalDivider(color = BorderColor, thickness = 1.dp)

                    Text("Or Invite Directly by Email", style = Typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)

                    MeetMindTextField(
                        value = inviteEmailInput,
                        onValueChange = { inviteEmailInput = it },
                        label = "Emails (comma separated)",
                        leadingIcon = Icons.Default.Email
                    )

                    if (inviteMessage != null) {
                        Text(
                            text = inviteMessage!!,
                            color = if (inviteMessage!!.startsWith("Failed")) RoseRed else EmeraldGreen,
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                MeetMindButton(
                    text = if (isSendingInvites) "Sending..." else "Send Invites",
                    onClick = {
                        if (inviteEmailInput.isNotBlank()) {
                            isSendingInvites = true
                            viewModel.batchInviteParticipants(meeting.id, inviteEmailInput) { success, msg ->
                                isSendingInvites = false
                                inviteMessage = msg
                                if (success) {
                                    Toast.makeText(context, "Invitations sent successfully!", Toast.LENGTH_SHORT).show()
                                    viewModel.refreshMeetingParticipants(meeting.id)
                                    showInviteDialog = false
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please enter at least one email", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.height(42.dp),
                    isAiAction = true,
                    isLoading = isSendingInvites
                )
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // ==========================================
    // SCAFFOLD
    // ==========================================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Meeting Details",
                        style = if (layout.isSmallPhone) Typography.titleMedium else Typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.bounceClick()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (meeting != null) {
                        IconButton(onClick = { showInviteDialog = true }, modifier = Modifier.bounceClick()) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Invite", tint = NeonCyan)
                        }
                        if (isCurrentUserHost) {
                            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.bounceClick()) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = RoseRed)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface,
        bottomBar = {
            // Bottom Action Bar on Smartphones
            if (meeting != null && !layout.isTabletOrLarger && !layout.isLandscape) {
                Surface(
                    modifier = Modifier.navigationBarsPadding(),
                    color = CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = layout.horizontalPadding, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onConfigureRep,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.SmartToy, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AI Proxy", color = NeonCyan, fontWeight = FontWeight.Bold, style = Typography.labelMedium)
                        }

                        val isLive = meeting.status == MeetingStatus.LIVE
                        val isScheduled = meeting.status == MeetingStatus.SCHEDULED
                        val buttonText = when {
                            isLive -> "Join Live Room 🟢"
                            isScheduled && isCurrentUserHost -> "Start Meeting 🚀"
                            isScheduled -> "Pre-Join Room"
                            else -> "View Summary ✨"
                        }

                        MeetMindButton(
                            text = buttonText,
                            onClick = {
                                if (isScheduled && isCurrentUserHost) {
                                    viewModel.startMeeting(meeting.id)
                                } else if (isLive || isScheduled) {
                                    onJoin()
                                } else {
                                    onViewIntelligence(meeting.id.toString())
                                }
                            },
                            modifier = Modifier
                                .weight(1.6f)
                                .height(52.dp),
                            isLoading = uiState.actionInProgress,
                            isAiAction = isLive
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoadingDetails) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else if (uiState.detailsError != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.ErrorOutline, null, tint = RoseRed, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(uiState.detailsError!!, color = RoseRed, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    MeetMindButton(text = "Retry", onClick = { viewModel.loadMeetingDetails(meetingId) })
                }
            }
        } else if (meeting == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Meeting not found", color = TextSecondary, style = Typography.bodyLarge)
            }
        } else {
            val isTwoColumn = layout.isTabletOrLarger || layout.isLandscape

            if (isTwoColumn) {
                // ==========================================
                // TABLET & LANDSCAPE TWO-COLUMN SPLIT
                // ==========================================
                ResponsiveContainer(maxWidth = 1140.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = layout.horizontalPadding, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // LEFT COLUMN: Overview, Hero Card, Code & Actions
                        Column(
                            modifier = Modifier
                                .width(if (layout.isLandscape && !layout.isTablet) 380.dp else 420.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            MeetingHeroCard(
                                meeting = meeting,
                                formattedTime = formattedTime,
                                isCurrentUserHost = isCurrentUserHost
                            )

                            MeetingCodeShareCard(
                                meeting = meeting,
                                formattedTime = formattedTime,
                                onInviteClick = { showInviteDialog = true }
                            )

                            // Tablet CTA Buttons directly in Left Column
                            TabletActionButtons(
                                meeting = meeting,
                                isCurrentUserHost = isCurrentUserHost,
                                actionInProgress = uiState.actionInProgress,
                                onConfigureRep = onConfigureRep,
                                onStartMeeting = { viewModel.startMeeting(meeting.id) },
                                onJoin = onJoin,
                                onViewIntelligence = { onViewIntelligence(meeting.id.toString()) }
                            )

                            MeetingDescriptionCard(description = meeting.description)
                        }

                        // Vertical Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(BorderColor)
                        )

                        // RIGHT COLUMN: AI Intelligence, Participants, Transcripts & Recordings
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 60.dp)
                        ) {
                            // AI Meeting Intelligence Bento Card
                            item {
                                SectionHeader(title = "AI Meeting Intelligence")
                                AiIntelligenceBentoCard(
                                    meetingId = meeting.id.toString(),
                                    uiState = uiState,
                                    onViewIntelligence = onViewIntelligence,
                                    onViewReport = onViewReport,
                                    onViewTranscript = onViewTranscript
                                )
                            }

                            // Participants List Card
                            item {
                                SectionHeader(title = "Participants (${uiState.participants.size})")
                                ParticipantsBentoCard(
                                    participants = uiState.participants,
                                    hostId = meeting.hostId,
                                    onInviteClick = { showInviteDialog = true }
                                )
                            }

                            // Transcripts Card
                            item {
                                SectionHeader(title = "Meeting Transcript")
                                TranscriptHighlightCard(
                                    meetingId = meeting.id.toString(),
                                    onViewTranscript = onViewTranscript
                                )
                            }

                            // Recordings Section (if any)
                            if (uiState.recordings.isNotEmpty()) {
                                item {
                                    SectionHeader(title = "Recordings (${uiState.recordings.size})")
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        uiState.recordings.forEach { recording ->
                                            RecordingItemCard(
                                                title = "Recording #${recording.id}",
                                                date = recording.createdAt.take(10),
                                                duration = recording.duration?.let { "${it / 60}:${String.format("%02d", it % 60)}" } ?: "Processing",
                                                status = recording.status,
                                                onClick = { onPlayRecording(recording.id.toString(), meeting.id.toString()) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // SMARTPHONE SINGLE-COLUMN SCROLL FEED
                // ==========================================
                ResponsiveContainer(maxWidth = 640.dp) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = layout.horizontalPadding),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
                    ) {
                        item {
                            MeetingHeroCard(
                                meeting = meeting,
                                formattedTime = formattedTime,
                                isCurrentUserHost = isCurrentUserHost
                            )
                        }

                        item {
                            MeetingCodeShareCard(
                                meeting = meeting,
                                formattedTime = formattedTime,
                                onInviteClick = { showInviteDialog = true }
                            )
                        }

                        item {
                            MeetingDescriptionCard(description = meeting.description)
                        }

                        // AI Meeting Intelligence Card
                        item {
                            SectionHeader(title = "AI Meeting Intelligence")
                            AiIntelligenceBentoCard(
                                meetingId = meeting.id.toString(),
                                uiState = uiState,
                                onViewIntelligence = onViewIntelligence,
                                onViewReport = onViewReport,
                                onViewTranscript = onViewTranscript
                            )
                        }

                        // Participants Card
                        item {
                            SectionHeader(title = "Participants (${uiState.participants.size})")
                            ParticipantsBentoCard(
                                participants = uiState.participants,
                                hostId = meeting.hostId,
                                onInviteClick = { showInviteDialog = true }
                            )
                        }

                        // Transcript Card
                        item {
                            SectionHeader(title = "Meeting Transcript")
                            TranscriptHighlightCard(
                                meetingId = meeting.id.toString(),
                                onViewTranscript = onViewTranscript
                            )
                        }

                        // Recordings List
                        if (uiState.recordings.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Recordings (${uiState.recordings.size})")
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    uiState.recordings.forEach { recording ->
                                        RecordingItemCard(
                                            title = "Recording #${recording.id}",
                                            date = recording.createdAt.take(10),
                                            duration = recording.duration?.let { "${it / 60}:${String.format("%02d", it % 60)}" } ?: "Processing",
                                            status = recording.status,
                                            onClick = { onPlayRecording(recording.id.toString(), meeting.id.toString()) }
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

// ==========================================
// SUBCOMPONENTS
// ==========================================

/**
 * Hero Card displaying status, title, host, and formatted schedule time
 */
@Composable
private fun MeetingHeroCard(
    meeting: com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting,
    formattedTime: FormattedMeetingTime,
    isCurrentUserHost: Boolean
) {
    val layout = LocalResponsiveLayout.current
    val isLive = meeting.status == MeetingStatus.LIVE

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    if (isLive) EmeraldGreen.copy(alpha = 0.5f) else ElectricIndigo.copy(alpha = 0.5f),
                    BorderColor
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (layout.isSmallPhone) 14.dp else 18.dp)
        ) {
            // Status row + Relative timing badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(
                    text = if (isLive) "LIVE NOW 🟢" else meeting.status.name,
                    color = if (isLive) EmeraldGreen else ElectricIndigo
                )

                if (formattedTime.relativeBadge.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = GlassWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Text(
                            text = formattedTime.relativeBadge,
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Meeting Title
            Text(
                meeting.title,
                style = if (layout.isSmallPhone) Typography.headlineSmall else Typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = if (layout.isSmallPhone) 28.sp else 34.sp
            )

            Spacer(Modifier.height(14.dp))

            // Timing & Host Detail Rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Scheduled Date & Time Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GlassWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Event, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(formattedTime.dateText, style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextDisabled)
                            Text(formattedTime.timeText.ifEmpty { "All day" }, style = Typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }

                // Host Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GlassWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(if (isCurrentUserHost) "You (Host 👑)" else "Host", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextDisabled)
                            Text(meeting.hostName.ifBlank { "Host" }, style = Typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Meeting Code Copy & Native Share Card
 */
@Composable
private fun MeetingCodeShareCard(
    meeting: com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting,
    formattedTime: FormattedMeetingTime,
    onInviteClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val meetingCode = meeting.meetingCode ?: "mm-${meeting.id}"
    var copiedNotice by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Meeting Code & Invite Link", style = Typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    meetingCode,
                    style = Typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = NeonCyan
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(meetingCode))
                            copiedNotice = true
                            Toast.makeText(context, "Meeting code copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            if (copiedNotice) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy Code",
                            tint = if (copiedNotice) EmeraldGreen else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Join MeetMind AI Meeting: ${meeting.title}")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Join my MeetMind AI meeting: ${meeting.title}\n📅 Date: ${formattedTime.dateText} at ${formattedTime.timeText}\n🔑 Meeting Code: $meetingCode"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Meeting Code"))
                        },
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Link", tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onInviteClick,
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Invite", tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                }
            }

            AnimatedVisibility(visible = copiedNotice) {
                Text("✓ Code copied to clipboard", style = Typography.labelSmall, color = EmeraldGreen)
            }
        }
    }
}

/**
 * Tablet Action Buttons Column
 */
@Composable
private fun TabletActionButtons(
    meeting: com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting,
    isCurrentUserHost: Boolean,
    actionInProgress: Boolean,
    onConfigureRep: () -> Unit,
    onStartMeeting: () -> Unit,
    onJoin: () -> Unit,
    onViewIntelligence: () -> Unit
) {
    val isLive = meeting.status == MeetingStatus.LIVE
    val isScheduled = meeting.status == MeetingStatus.SCHEDULED
    val buttonText = when {
        isLive -> "Join Live Room 🟢"
        isScheduled && isCurrentUserHost -> "Start Meeting 🚀"
        isScheduled -> "Pre-Join Room"
        else -> "View Summary ✨"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MeetMindButton(
            text = buttonText,
            onClick = {
                if (isScheduled && isCurrentUserHost) {
                    onStartMeeting()
                } else if (isLive || isScheduled) {
                    onJoin()
                } else {
                    onViewIntelligence()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            isLoading = actionInProgress,
            isAiAction = isLive
        )

        OutlinedButton(
            onClick = onConfigureRep,
            modifier = Modifier.fillMaxWidth().height(48.dp).bounceClick(),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.6f))
        ) {
            Icon(Icons.Default.SmartToy, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Configure AI Representative", color = NeonCyan, fontWeight = FontWeight.Bold, style = Typography.labelMedium)
        }
    }
}

/**
 * Meeting Description Card
 */
@Composable
private fun MeetingDescriptionCard(description: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Description", style = Typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(
                description ?: "No description provided for this meeting.",
                style = Typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * AI Meeting Intelligence Bento Card with metric pills and 3 action buttons
 */
@Composable
private fun AiIntelligenceBentoCard(
    meetingId: String,
    uiState: MeetingUiState,
    onViewIntelligence: (String) -> Unit,
    onViewReport: (String) -> Unit,
    onViewTranscript: (String) -> Unit
) {
    val intel = uiState.intelligenceSummary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = ElectricIndigo.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("AI Insights & Executive Summary", style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        if (intel != null) "Analysis Ready ⚡" else "Automated Intelligence Active",
                        style = Typography.labelSmall,
                        color = if (intel != null) EmeraldGreen else NeonCyan
                    )
                }
            }

            // Metrics Pill Badges
            if (intel != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (intel.actionItems.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldGreen.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.25f))
                        ) {
                            Text(
                                "✓ ${intel.actionItems.size} Tasks",
                                style = Typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (intel.decisions.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElectricIndigo.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.25f))
                        ) {
                            Text(
                                "⚖ ${intel.decisions.size} Decisions",
                                style = Typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ElectricIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (intel.topics.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonCyan.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f))
                        ) {
                            Text(
                                "🏷 ${intel.topics.size} Topics",
                                style = Typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val summaryText = intel?.summary
            Text(
                text = summaryText ?: "Meeting intelligence automatically synthesizes discussions, isolates key decisions, extracts action items with deadlines, and computes speaker analytics.",
                style = Typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(16.dp))

            // Action Buttons: AI Studio, Report, Transcript
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onViewIntelligence(meetingId) },
                    modifier = Modifier.weight(1f).height(44.dp).bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("AI Studio", style = Typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onViewReport(meetingId) },
                    modifier = Modifier.weight(1f).height(44.dp).bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Report", style = Typography.labelSmall, color = TextPrimary)
                }

                OutlinedButton(
                    onClick = { onViewTranscript(meetingId) },
                    modifier = Modifier.weight(1f).height(44.dp).bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Subtitles, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Transcript", style = Typography.labelSmall, fontWeight = FontWeight.Bold, color = NeonCyan)
                }
            }
        }
    }
}

/**
 * Participants Bento Card showing members list and host badge
 */
@Composable
private fun ParticipantsBentoCard(
    participants: List<com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Participant>,
    hostId: Long,
    onInviteClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Meeting Roster", style = Typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                TextButton(onClick = onInviteClick, modifier = Modifier.bounceClick()) {
                    Icon(Icons.Default.PersonAdd, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Invite", style = Typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }

            if (participants.isEmpty()) {
                Text("No participants have joined or been invited yet.", style = Typography.bodySmall, color = TextDisabled)
            } else {
                participants.forEach { participant ->
                    ParticipantRow(
                        name = participant.name,
                        status = participant.status.name,
                        isOnline = participant.isOnline,
                        isHost = participant.userId == hostId
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Transcript Highlight Card
 */
@Composable
private fun TranscriptHighlightCard(
    meetingId: String,
    onViewTranscript: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = NeonCyan.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Subtitles, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Spoken Discussion Transcript", style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Real-time text timeline with speaker identification", style = Typography.labelSmall, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                "Search keywords across every spoken segment, switch display languages, and export the official plain text meeting minutes.",
                style = Typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = { onViewTranscript(meetingId) },
                modifier = Modifier.fillMaxWidth().height(46.dp).bounceClick(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.Article, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Complete Transcript 📝", style = Typography.labelMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
            }
        }
    }
}

@Composable
fun ParticipantRow(
    name: String,
    status: String,
    isOnline: Boolean = false,
    isHost: Boolean = false
) {
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifEmpty { name.take(1).uppercase() }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = GlassWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = ElectricIndigo.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(initials, style = Typography.titleSmall, fontWeight = FontWeight.Bold, color = NeonCyan)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = Typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    if (isOnline) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(7.dp).background(EmeraldGreen, CircleShape))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    status.lowercase().replaceFirstChar { it.uppercase() },
                    style = Typography.labelSmall.copy(fontSize = 11.sp),
                    color = TextSecondary
                )
            }

            if (isHost) {
                StatusChip(text = "Host 👑", color = AmberGold)
            } else if (status.equals("INVITED", ignoreCase = true)) {
                StatusChip(text = "INVITED", color = Color(0xFFD97706))
            } else {
                StatusChip(text = "Participant", color = TextSecondary)
            }
        }
    }
}
