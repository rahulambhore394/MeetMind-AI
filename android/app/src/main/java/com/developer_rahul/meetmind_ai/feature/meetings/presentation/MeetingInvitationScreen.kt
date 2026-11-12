package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Invitation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingInvitationScreen(
    inviteCode: String? = null,
    onBack: () -> Unit,
    onJoinMeeting: (String) -> Unit = {},
    onViewMeetingDetails: (String) -> Unit = {},
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val layout = LocalResponsiveLayout.current
    val isTablet = layout.isTabletOrLarger || (layout.isLandscape && !layout.isSmallPhone)

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Pending, 1: Accepted, 2: Declined
    var singleModeView by remember { mutableStateOf(!inviteCode.isNullOrBlank()) }

    LaunchedEffect(inviteCode) {
        if (!inviteCode.isNullOrBlank()) {
            singleModeView = true
            viewModel.loadInvitationByCode(inviteCode)
        } else {
            viewModel.loadInvitations()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadInvitations()
    }

    val pendingInvitations = remember(uiState.invitations) {
        uiState.invitations.filter { "INVITED".equals(it.status, ignoreCase = true) }
    }
    val acceptedInvitations = remember(uiState.invitations) {
        uiState.invitations.filter { "ACCEPTED".equals(it.status, ignoreCase = true) }
    }
    val declinedInvitations = remember(uiState.invitations) {
        uiState.invitations.filter { "DECLINED".equals(it.status, ignoreCase = true) }
    }

    val displayedInvitations = when (selectedTab) {
        0 -> pendingInvitations
        1 -> acceptedInvitations
        else -> declinedInvitations
    }

    Scaffold(
        containerColor = BackgroundSurface,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (singleModeView) "Meeting Invitation" else "Invitations",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Black
                        )
                        if (!singleModeView && pendingInvitations.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = ElectricIndigo,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = pendingInvitations.size.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (singleModeView && !inviteCode.isNullOrBlank()) {
                                onBack()
                            } else if (singleModeView) {
                                singleModeView = false
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(GlassWhite, CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!inviteCode.isNullOrBlank()) {
                                viewModel.loadInvitationByCode(inviteCode)
                            }
                            viewModel.loadInvitations()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        }
    ) { padding ->
        ResponsiveContainer(maxWidth = if (isTablet) 1000.dp else 600.dp) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = layout.horizontalPadding)
            ) {
                // Success / Error Feedback Banners
                if (uiState.invitationMessage != null) {
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                uiState.invitationMessage!!,
                                color = EmeraldGreen,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearInvitationMessage() }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                if (uiState.invitationError != null) {
                    Surface(
                        color = RoseRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, RoseRed.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Close, null, tint = RoseRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                uiState.invitationError!!,
                                color = RoseRed,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearInvitationMessage() }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, null, tint = RoseRed, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // If opened with a specific invite code, render the targeted Invitation Hero Screen
                if (singleModeView && uiState.selectedInvitation != null) {
                    SingleInvitationView(
                        invitation = uiState.selectedInvitation!!,
                        actionInProgress = uiState.invitationActionInProgress,
                        onAccept = {
                            val code = uiState.selectedInvitation!!.meetingCode
                            viewModel.acceptInvitationByCode(code) { accepted ->
                                onJoinMeeting(accepted.meetingId.toString())
                            }
                        },
                        onDecline = {
                            viewModel.declineInvitation(uiState.selectedInvitation!!.meetingId) {
                                singleModeView = false
                            }
                        },
                        onViewAll = { singleModeView = false }
                    )
                } else if (singleModeView && uiState.isLoadingInvitations) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricIndigo)
                    }
                } else {
                    // ALL INVITATIONS MANAGER VIEW
                    // Tab Navigation
                    Surface(
                        color = CardSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val tabs = listOf(
                                "Pending (${pendingInvitations.size})",
                                "Accepted (${acceptedInvitations.size})",
                                "Declined (${declinedInvitations.size})"
                            )
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) ElectricIndigo else Color.Transparent)
                                        .clickable { selectedTab = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (uiState.isLoadingInvitations && uiState.invitations.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ElectricIndigo)
                        }
                    } else if (displayedInvitations.isEmpty()) {
                        EmptyInvitationsView(
                            tabIndex = selectedTab,
                            onRefresh = { viewModel.loadInvitations() }
                        )
                    } else {
                        if (isTablet) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(displayedInvitations, key = { it.meetingId }) { invite ->
                                    InvitationCard(
                                        invitation = invite,
                                        actionInProgress = uiState.invitationActionInProgress,
                                        onAccept = {
                                            viewModel.acceptInvitation(invite.meetingId) {
                                                onJoinMeeting(invite.meetingId.toString())
                                            }
                                        },
                                        onDecline = {
                                            viewModel.declineInvitation(invite.meetingId)
                                        },
                                        onJoin = {
                                            onJoinMeeting(invite.meetingId.toString())
                                        },
                                        onViewDetails = {
                                            onViewMeetingDetails(invite.meetingId.toString())
                                        }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(displayedInvitations, key = { it.meetingId }) { invite ->
                                    InvitationCard(
                                        invitation = invite,
                                        actionInProgress = uiState.invitationActionInProgress,
                                        onAccept = {
                                            viewModel.acceptInvitation(invite.meetingId) {
                                                onJoinMeeting(invite.meetingId.toString())
                                            }
                                        },
                                        onDecline = {
                                            viewModel.declineInvitation(invite.meetingId)
                                        },
                                        onJoin = {
                                            onJoinMeeting(invite.meetingId.toString())
                                        },
                                        onViewDetails = {
                                            onViewMeetingDetails(invite.meetingId.toString())
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

// -------------------------------------------------------------
// SINGLE INVITATION VIEW (TARGETED INVITATION / DEEP LINK)
// -------------------------------------------------------------

@Composable
private fun SingleInvitationView(
    invitation: Invitation,
    actionInProgress: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onViewAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing Icon
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(ElectricIndigo.copy(alpha = 0.35f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ElectricIndigo),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Email, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "You're Invited!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "${invitation.hostName} has invited you to collaborate on MeetMind AI.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(24.dp))

        // Details Card
        Surface(
            color = CardSurface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Topic
                Text("MEETING TOPIC", style = MaterialTheme.typography.labelSmall, color = TextDisabled, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    invitation.meetingTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (invitation.meetingDescription.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        invitation.meetingDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 16.dp))

                // Host
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricIndigo.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            invitation.hostName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricIndigo
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Host", style = MaterialTheme.typography.labelSmall, color = TextDisabled)
                        Text(invitation.hostName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Schedule
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Schedule, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("When", style = MaterialTheme.typography.labelSmall, color = TextDisabled)
                        val formattedTime = invitation.scheduledAt?.let {
                            val p = MeetingDateTimeUtils.parseMeetingTime(it)
                            if (p.timeText.isNotBlank()) "${p.dateText} at ${p.timeText}" else p.dateText
                        } ?: "Instant Live Call"
                        Text(formattedTime, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Code
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Key, null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Room Code", style = MaterialTheme.typography.labelSmall, color = TextDisabled)
                        Text(invitation.meetingCode, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Action Buttons
        val isAlreadyAccepted = "ACCEPTED".equals(invitation.status, ignoreCase = true)

        if (isAlreadyAccepted) {
            Surface(
                color = EmeraldGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("You have already accepted this invitation.", style = MaterialTheme.typography.bodyMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Icon(Icons.Default.VideoCall, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Join Meeting Room Now 🚀", fontWeight = FontWeight.Bold)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !actionInProgress,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Text("Decline", color = TextSecondary, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAccept,
                    enabled = !actionInProgress,
                    modifier = Modifier
                        .weight(1.8f)
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    if (actionInProgress) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Accept & Join 🟢", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onViewAll) {
            Text("View All My Invitations →", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// -------------------------------------------------------------
// INVITATION LIST CARD
// -------------------------------------------------------------

@Composable
private fun InvitationCard(
    invitation: Invitation,
    actionInProgress: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onJoin: () -> Unit,
    onViewDetails: () -> Unit
) {
    val isPending = "INVITED".equals(invitation.status, ignoreCase = true)
    val isAccepted = "ACCEPTED".equals(invitation.status, ignoreCase = true)

    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isPending) ElectricIndigo.copy(alpha = 0.4f) else BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Host & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPending) ElectricIndigo.copy(alpha = 0.2f)
                                else if (isAccepted) EmeraldGreen.copy(alpha = 0.2f)
                                else Color.Gray.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            invitation.hostName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPending) ElectricIndigo else if (isAccepted) EmeraldGreen else TextSecondary
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            invitation.hostName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Host",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDisabled
                        )
                    }
                }

                // Status Chip
                Surface(
                    color = when {
                        isAccepted -> EmeraldGreen.copy(alpha = 0.15f)
                        isPending -> ElectricIndigo.copy(alpha = 0.15f)
                        else -> RoseRed.copy(alpha = 0.15f)
                    },
                    shape = CircleShape
                ) {
                    Text(
                        text = when {
                            isAccepted -> "Accepted"
                            isPending -> "Pending"
                            else -> "Declined"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isAccepted -> EmeraldGreen
                            isPending -> ElectricIndigo
                            else -> RoseRed
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Title
            Text(
                text = invitation.meetingTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (invitation.meetingDescription.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = invitation.meetingDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            // Metadata Row: Time & Room Code
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    val formatted = invitation.scheduledAt?.let {
                        val p = MeetingDateTimeUtils.parseMeetingTime(it)
                        if (p.timeText.isNotBlank()) "${p.dateText} • ${p.timeText}" else p.dateText
                    } ?: "Instant Call"
                    Text(formatted, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }

                Text(
                    invitation.meetingCode,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }

            Spacer(Modifier.height(16.dp))

            // Action Buttons
            if (isPending) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        enabled = !actionInProgress,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Text("Decline", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    }

                    Button(
                        onClick = onAccept,
                        enabled = !actionInProgress,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Accept & Join", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (isAccepted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewDetails,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Text("Details", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    }

                    Button(
                        onClick = onJoin,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                    ) {
                        Icon(Icons.Default.VideoCall, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Join Room", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Declined card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onAccept) {
                        Text("Change to Accept", style = MaterialTheme.typography.labelSmall, color = ElectricIndigo)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// EMPTY STATE
// -------------------------------------------------------------

@Composable
private fun EmptyInvitationsView(
    tabIndex: Int,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(CardSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when (tabIndex) {
                    0 -> Icons.Default.MailOutline
                    1 -> Icons.Default.CheckCircleOutline
                    else -> Icons.Default.HighlightOff
                },
                contentDescription = null,
                tint = TextDisabled,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = when (tabIndex) {
                0 -> "No Pending Invitations"
                1 -> "No Accepted Invitations"
                else -> "No Declined Invitations"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = when (tabIndex) {
                0 -> "When teammates invite you to video conferences, they will appear here."
                1 -> "Meetings you have accepted will appear here."
                else -> "Invitations you have declined are recorded here."
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onRefresh,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Refresh", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
    }
}
