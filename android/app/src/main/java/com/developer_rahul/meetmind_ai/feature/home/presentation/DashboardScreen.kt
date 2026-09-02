package com.developer_rahul.meetmind_ai.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuickActionCard(
                        title = "New",
                        icon = Icons.Default.VideoCall,
                        color = ElectricIndigo,
                        modifier = Modifier.weight(1f),
                        onClick = onNewMeeting
                    )
                    QuickActionCard(
                        title = "Join",
                        icon = Icons.Default.AddBox,
                        color = NeonCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { showJoinDialog = true }
                    )
                    QuickActionCard(
                        title = "AI Rep",
                        icon = Icons.Default.SmartToy,
                        color = EmeraldGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onAiRepConfig
                    )
                }
            }

            if (uiState.meetings.isNotEmpty()) {
                item {
                    SectionHeader(title = "Today's Schedule", action = "View All")
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.meetings.take(3).forEach { meeting ->
                            MeetingScheduleItem(
                                title = meeting.title,
                                time = meeting.scheduledAt,
                                host = meeting.hostName
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "AI Intelligence", action = "Settings")
            }

            items(sampleInsights) { insight ->
                AiInsightCard(insight = insight)
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    MeetMindCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.medium,
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title, 
                style = Typography.labelLarge, 
                color = TextPrimary
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
            .clickable { onClick() }
            .aiGlow(color = ElectricIndigo.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.large,
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite)
    ) {
        Box {
            // Background Glow effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ElectricIndigo.copy(alpha = 0.15f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(1000f, 0f),
                            radius = 600f
                        )
                    )
            )

            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(RoseRed, CircleShape)
                                .pulse()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(status, style = Typography.labelLarge, color = RoseRed)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(title, style = Typography.headlineMedium, color = TextPrimary)
                    Text(time, style = Typography.bodyLarge, color = TextSecondary)
                }
                
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .size(56.dp)
                        .background(ElectricIndigo, CircleShape)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Join", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun MeetingScheduleItem(
    title: String,
    time: String,
    host: String
) {
    MeetMindCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = Typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(time, style = Typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(host, style = Typography.bodySmall, color = TextSecondary)
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

val sampleInsights = listOf(
    AiInsight("Budget Strategy", "The team reached a consensus on the $50k cloud budget for Q4."),
    AiInsight("Priority Task", "You were assigned to review the security architecture by this Friday."),
    AiInsight("Key Takeaway", "WebRTC implementation will use adaptive bitrate for mobile clients.")
)
