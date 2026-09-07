package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.MeetingStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsScreen(
    onMeetingClick: (String) -> Unit,
    onCreateMeeting: () -> Unit,
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val tabs = listOf(
        "Upcoming (${uiState.upcomingMeetings.size})",
        "Live (${uiState.liveMeetings.size})",
        "History (${uiState.pastMeetings.size})"
    )

    if (showFilterSheet) {
        FilterMeetingsSheet(
            onDismiss = { showFilterSheet = false },
            onApply = { showFilterSheet = false }
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(BackgroundSurface)) {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Meetings & Calls", 
                                style = Typography.headlineSmall, 
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = ElectricIndigo.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Text(
                                    "${uiState.meetings.size}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = Typography.labelSmall,
                                    color = ElectricIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.loadMeetings() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                        }
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
                )
                
                // Custom Modern Pill Tab Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Surface(
                            onClick = { selectedTab = index },
                            shape = CircleShape,
                            color = if (isSelected) ElectricIndigo else CardSurface,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            modifier = Modifier
                                .weight(1f)
                                .bounceClick()
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    title,
                                    style = Typography.labelMedium,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            GradientFloatingActionButton(
                text = "New Meeting",
                onClick = onCreateMeeting,
                icon = Icons.Default.VideoCall
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        if (uiState.isLoading && uiState.meetings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricIndigo)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 160.dp)
            ) {
                // PROMINENT REJOIN LIVE MEETING HERO BANNER
                if (uiState.liveMeetings.isNotEmpty()) {
                    val activeMeeting = uiState.liveMeetings.first()
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = EmeraldGreen.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(EmeraldGreen, CircleShape)
                                                .pulse(color = EmeraldGreen)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "LIVE MEETING IN PROGRESS",
                                            style = Typography.labelMedium,
                                            color = EmeraldGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (!activeMeeting.meetingCode.isNullOrEmpty()) {
                                        Surface(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(activeMeeting.meetingCode))
                                                Toast.makeText(context, "Code copied: ${activeMeeting.meetingCode}", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = CircleShape,
                                            color = EmeraldGreen.copy(alpha = 0.2f),
                                            modifier = Modifier.bounceClick()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    activeMeeting.meetingCode,
                                                    style = Typography.labelSmall,
                                                    color = EmeraldGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Default.ContentCopy, null, tint = EmeraldGreen, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    activeMeeting.title,
                                    style = Typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Hosted by ${activeMeeting.hostName}",
                                    style = Typography.bodyMedium,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { onMeetingClick(activeMeeting.id.toString()) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bounceClick(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                                ) {
                                    Icon(Icons.Default.VideoCall, null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("REJOIN MEETING NOW 🟢", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // JOIN WITH A CODE CARD
                item {
                    var inputCode by remember { mutableStateOf("") }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputCode,
                                onValueChange = { inputCode = it },
                                placeholder = { Text("Enter meeting code (e.g. mm-809)", style = Typography.bodySmall, color = TextDisabled) },
                                leadingIcon = {
                                    Icon(Icons.Default.VpnKey, null, tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricIndigo,
                                    unfocusedBorderColor = BorderColor
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    if (inputCode.isNotBlank()) {
                                        viewModel.joinByCode(inputCode.trim())
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.bounceClick(),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                            ) {
                                Text("Join", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                val meetings = when (selectedTab) {
                    0 -> uiState.upcomingMeetings
                    1 -> uiState.liveMeetings
                    else -> uiState.pastMeetings
                }
                
                if (meetings.isEmpty()) {
                    item {
                        EmptyMeetingsState(
                            onAction = onCreateMeeting
                        )
                    }
                } else {
                    items(meetings) { meeting ->
                        MeetingItemCard(meeting = meeting, onClick = { onMeetingClick(meeting.id.toString()) })
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingItemCard(meeting: Meeting, onClick: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Vertical Indicator Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(64.dp)
                    .clip(CircleShape)
                    .background(
                        when (meeting.status) {
                            MeetingStatus.LIVE -> EmeraldGreen
                            MeetingStatus.SCHEDULED -> ElectricIndigo
                            else -> TextDisabled
                        }
                    )
            )
            
            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        meeting.title, 
                        style = Typography.titleMedium, 
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    val isRejoin = meeting.hasJoinedBefore || meeting.userParticipantStatus == "LEFT"
                    val statusText = when {
                        meeting.status == MeetingStatus.LIVE && isRejoin -> "RE-JOIN"
                        else -> meeting.status.name
                    }
                    StatusChip(
                        text = statusText,
                        color = when (meeting.status) {
                            MeetingStatus.LIVE -> EmeraldGreen
                            MeetingStatus.SCHEDULED -> ElectricIndigo
                            else -> TextDisabled
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Meeting Code Copy Chip
                if (!meeting.meetingCode.isNullOrEmpty()) {
                    Surface(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(meeting.meetingCode!!))
                            Toast.makeText(context, "Code copied: ${meeting.meetingCode}", Toast.LENGTH_SHORT).show()
                        },
                        shape = CircleShape,
                        color = NeonCyan.copy(alpha = 0.12f),
                        modifier = Modifier.bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                meeting.meetingCode!!,
                                style = Typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ContentCopy, null, tint = NeonCyan, modifier = Modifier.size(11.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(meeting.scheduledAt, style = Typography.bodySmall, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val initials = meeting.hostName.split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .take(2)
                        .joinToString("")
                        .ifEmpty { meeting.hostName.take(1).uppercase() }

                    Surface(
                        modifier = Modifier.size(22.dp),
                        shape = CircleShape,
                        color = ElectricIndigo
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(initials, style = Typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Hosted by ${meeting.hostName}", 
                        style = Typography.labelSmall, 
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Arrow Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "View Details", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun EmptyMeetingsState(onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(90.dp),
            shape = CircleShape,
            color = GlassBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.EventBusy,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = TextDisabled
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("No Meetings Found", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Your scheduled, live, and past meetings\nwill appear here.", 
            style = Typography.bodyMedium, 
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAction,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.bounceClick(),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Meeting", fontWeight = FontWeight.Bold)
        }
    }
}

