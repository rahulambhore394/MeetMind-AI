package com.developer_rahul.meetmind_ai.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.MeetingStatus
import com.developer_rahul.meetmind_ai.feature.meetings.presentation.MeetingViewModel
import com.developer_rahul.meetmind_ai.feature.notifications.presentation.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNewMeeting: () -> Unit,
    onJoinMeeting: () -> Unit,
    onAiRepConfig: () -> Unit,
    onMeetingClick: (String) -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onSummaries: () -> Unit = {},
    onViewAllSchedule: () -> Unit = {},
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory),
    notificationViewModel: NotificationViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val notificationUiState by notificationViewModel.uiState.collectAsState()
    var showJoinDialog by remember { mutableStateOf(false) }

    if (showJoinDialog) {
        JoinMeetingDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { id ->
                showJoinDialog = false
                onMeetingClick(id)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricIndigo.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "MEETMIND AI", 
                                    style = Typography.titleMedium, 
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp,
                                    color = NeonCyan
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(EmeraldGreen, CircleShape)
                                        .pulse(color = EmeraldGreen)
                                )
                            }
                            Text(
                                "AI Meeting Intelligence", 
                                style = Typography.labelSmall,
                                color = TextSecondary
                            ) 
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNotifications,
                        modifier = Modifier
                            .size(40.dp)
                            .background(GlassWhite, CircleShape)
                            .bounceClick()
                    ) {
                        BadgedBox(badge = { 
                            if (notificationUiState.unreadCount > 0) {
                                Badge(containerColor = RoseRed) { 
                                    Text(
                                        notificationUiState.unreadCount.toString(),
                                        style = Typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    ) 
                                } 
                            }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Surface(
                        onClick = onProfile,
                        modifier = Modifier
                            .size(40.dp)
                            .bounceClick(),
                        shape = CircleShape,
                        color = ElectricIndigo.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            val nextMeeting = uiState.liveMeetings.firstOrNull() ?: uiState.upcomingMeetings.firstOrNull()
            
            if (nextMeeting != null) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = if (nextMeeting.status == MeetingStatus.LIVE) "Live Meeting Now 🟢" else "Next Upcoming Meeting")
                    UpcomingMeetingHero(
                        title = nextMeeting.title,
                        time = nextMeeting.scheduledAt,
                        status = nextMeeting.status.name,
                        meetingCode = nextMeeting.meetingCode ?: "mm-${nextMeeting.id}",
                        onClick = { onMeetingClick(nextMeeting.id.toString()) }
                    )
                }
            }

            item {
                SectionHeader(title = "Quick Actions")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "New Meeting",
                        subtitle = "Instant Call",
                        icon = Icons.Default.VideoCall,
                        color = ElectricIndigo,
                        modifier = Modifier.weight(1f),
                        onClick = onNewMeeting
                    )
                    QuickActionCard(
                        title = "Join Call",
                        subtitle = "Enter Code",
                        icon = Icons.Default.AddBox,
                        color = NeonCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { showJoinDialog = true }
                    )
                    QuickActionCard(
                        title = "AI Rep",
                        subtitle = "Proxy Agent",
                        icon = Icons.Default.SmartToy,
                        color = PurpleViolet,
                        modifier = Modifier.weight(1f),
                        onClick = onAiRepConfig
                    )
                }
            }

            item {
                SectionHeader(
                    title = "AI Summaries & Reports",
                    action = "View All",
                    onActionClick = onSummaries
                )
                Surface(
                    onClick = onSummaries,
                    shape = RoundedCornerShape(20.dp),
                    color = CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Assessment, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Post-Meeting Reports & Tasks", 
                                    style = Typography.titleMedium, 
                                    color = TextPrimary, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                "Synthesized summaries, key decisions, action items, and participant task breakdown.", 
                                style = Typography.bodySmall, 
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (uiState.meetings.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Recent & Scheduled Meetings",
                        action = "View All",
                        onActionClick = onViewAllSchedule
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.meetings.take(4).forEach { meeting ->
                            MeetingScheduleItem(
                                title = meeting.title,
                                time = meeting.scheduledAt,
                                host = meeting.hostName,
                                status = meeting.status.name,
                                meetingCode = meeting.meetingCode ?: "mm-${meeting.id}",
                                onClick = { onMeetingClick(meeting.id.toString()) }
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "AI Intelligence Engine")
                MeetMindCard(isAiCard = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(NeonCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automated Realtime AI Active", style = Typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Start or join any live call to enable live transcriptions, multi-language translation, and automated AI summary reports.", style = Typography.bodySmall, color = TextSecondary, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier.bounceClick()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                title, 
                style = Typography.labelMedium, 
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                subtitle,
                style = Typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UpcomingMeetingHero(
    title: String,
    time: String,
    status: String,
    meetingCode: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier
            .fillMaxWidth()
            .aiGlow(color = ElectricIndigo.copy(alpha = 0.35f), radius = 16.dp)
            .bounceClick()
    ) {
        Box {
            // Background Hero Gradient Fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                ElectricIndigo.copy(alpha = 0.22f),
                                PurpleViolet.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (status == "LIVE") EmeraldGreen else ElectricIndigo, CircleShape)
                                .pulse(color = if (status == "LIVE") EmeraldGreen else ElectricIndigo)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (status == "LIVE") "LIVE MEETING NOW 🟢" else "UPCOMING CALL",
                            style = Typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = if (status == "LIVE") EmeraldGreen else NeonCyan
                        )
                    }

                    Surface(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(meetingCode))
                            android.widget.Toast.makeText(context, "Code copied: $meetingCode", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(meetingCode, style = Typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ContentCopy, null, tint = NeonCyan, modifier = Modifier.size(11.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = title, 
                    style = Typography.headlineMedium, 
                    fontWeight = FontWeight.Bold, 
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(time, style = Typography.bodyMedium, color = TextSecondary)
                    }

                    Surface(
                        onClick = onClick,
                        shape = CircleShape,
                        color = if (status == "LIVE") EmeraldGreen else ElectricIndigo,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Join Call", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingScheduleItem(
    title: String,
    time: String,
    host: String,
    status: String = "",
    meetingCode: String = "",
    onClick: (() -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val accentColor = when (status) {
        "LIVE" -> EmeraldGreen
        "SCHEDULED" -> ElectricIndigo
        else -> TextSecondary
    }

    Surface(
        onClick = { onClick?.invoke() },
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (status == "LIVE") Icons.Default.VideoCall else Icons.Default.Event,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (meetingCode.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(meetingCode))
                                android.widget.Toast.makeText(context, "Code copied: $meetingCode", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricIndigo.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = meetingCode,
                                    style = Typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(time, style = Typography.bodySmall, color = TextSecondary)
                    
                    if (host.isNotBlank()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(host, style = Typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (status == "LIVE") {
                StatusChip(text = "LIVE 🟢", color = EmeraldGreen)
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Details",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
