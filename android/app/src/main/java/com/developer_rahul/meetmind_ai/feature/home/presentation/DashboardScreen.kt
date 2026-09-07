package com.developer_rahul.meetmind_ai.feature.home.presentation

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting
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
                    Column {
                        Text(
                            "Welcome Back", 
                            style = Typography.titleMedium,
                            color = TextSecondary
                        )
                        Text(
                            "MEETMIND AI", 
                            style = Typography.titleLarge, 
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = ElectricIndigo
                        ) 
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNotifications,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(GlassWhite, CircleShape)
                    ) {
                        BadgedBox(badge = { 
                            if (notificationUiState.unreadCount > 0) {
                                Badge(containerColor = RoseRed) { 
                                    Text(notificationUiState.unreadCount.toString()) 
                                } 
                            }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = TextPrimary)
                        }
                    }
                    Surface(
                        onClick = onProfile,
                        modifier = Modifier.size(40.dp).padding(end = 8.dp),
                        shape = CircleShape,
                        color = GlassWhite
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("R", style = Typography.labelLarge, color = TextPrimary)
                        }
                    }
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val nextMeeting = uiState.upcomingMeetings.firstOrNull() ?: uiState.liveMeetings.firstOrNull()
            
            if (nextMeeting != null) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionHeader(title = "Upcoming Meeting")
                    UpcomingMeetingHero(
                        title = nextMeeting.title,
                        time = nextMeeting.scheduledAt,
                        status = nextMeeting.status.name,
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
                        title = "Join Meeting",
                        subtitle = "Enter Code",
                        icon = Icons.Default.AddBox,
                        color = NeonCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { showJoinDialog = true }
                    )
                    QuickActionCard(
                        title = "AI Rep",
                        subtitle = "Persona Setup",
                        icon = Icons.Default.SmartToy,
                        color = PurpleViolet,
                        modifier = Modifier.weight(1f),
                        onClick = onAiRepConfig
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Meeting Reports & Summaries",
                    action = "View All",
                    onActionClick = onSummaries
                )
                MeetMindCard(
                    modifier = Modifier.clickable { onSummaries() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Assessment, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Post-Meeting Reports & Work Assignments", 
                                style = Typography.titleMedium, 
                                color = TextPrimary, 
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Complete summary reports, key decisions, work assigned per person, and user-specific task breakdown.", 
                                style = Typography.bodySmall, 
                                color = TextSecondary
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = TextSecondary)
                    }
                }
            }

            if (uiState.meetings.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Today's Schedule",
                        action = "View All",
                        onActionClick = onViewAllSchedule
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                SectionHeader(
                    title = "AI Intelligence Insights",
                    action = "Configure",
                    onActionClick = onAiRepConfig
                )
            }

            item {
                MeetMindCard(isAiCard = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).background(NeonCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Realtime AI Engine Active", style = Typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Start or join a live meeting to generate automated summaries, key action items, and live transcripts.", style = Typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(140.dp))
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
        modifier = modifier
            .bounceClick()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title, 
                style = Typography.labelLarge, 
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                subtitle,
                style = Typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun UpcomingMeetingHero(
    title: String,
    time: String,
    status: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
            .clickable { onClick() }
            .aiGlow(color = ElectricIndigo.copy(alpha = 0.3f)),
        shape = MaterialTheme.shapes.extraLarge,
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Box {
            // Background Glow effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ElectricIndigo.copy(alpha = 0.2f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(1000f, 0f),
                            radius = 600f
                        )
                    )
            )

            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (status == "LIVE") EmeraldGreen else RoseRed, CircleShape)
                                .pulse()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (status == "LIVE") "LIVE MEETING NOW" else status,
                            style = Typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = if (status == "LIVE") EmeraldGreen else RoseRed
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(title, style = Typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(time, style = Typography.bodyMedium, color = TextSecondary)
                    }
                }
                
                Surface(
                    onClick = onClick,
                    shape = CircleShape,
                    color = ElectricIndigo,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Join", tint = Color.White, modifier = Modifier.size(28.dp))
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
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val accentColor = when (status) {
        "LIVE" -> EmeraldGreen
        "SCHEDULED" -> ElectricIndigo
        else -> TextSecondary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
            .clickable { onClick?.invoke() },
        shape = MaterialTheme.shapes.large,
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Icon Squircle Container with Status Color Accent
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = accentColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (status == "LIVE") Icons.Default.VideoCall else Icons.Default.Event,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Main Content Area
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
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (meetingCode.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(meetingCode))
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricIndigo.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = meetingCode,
                                    style = Typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy Code",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(time, style = Typography.bodySmall, color = TextSecondary)
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(host, style = Typography.bodySmall, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right Status Badge / Action Indicator
            if (status == "LIVE") {
                StatusChip(
                    text = "LIVE 🟢",
                    color = EmeraldGreen
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = GlassWhite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Details",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiInsightCard(insight: AiInsight) {
    MeetMindCard(isAiCard = true) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = NeonCyan.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(insight.title, style = Typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    insight.description, 
                    style = Typography.bodyMedium, 
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

data class AiInsight(
    val title: String,
    val description: String
)
