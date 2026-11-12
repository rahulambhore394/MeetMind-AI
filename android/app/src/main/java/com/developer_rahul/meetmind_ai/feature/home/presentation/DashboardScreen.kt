package com.developer_rahul.meetmind_ai.feature.home.presentation

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
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
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.MeetingStatus
import com.developer_rahul.meetmind_ai.feature.meetings.presentation.MeetingViewModel
import com.developer_rahul.meetmind_ai.feature.notifications.presentation.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class MeetingFilterTab(val label: String) {
    ALL("All"),
    LIVE("Live Now"),
    UPCOMING("Upcoming"),
    PAST("Past")
}

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
    onInvitations: () -> Unit = {},
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory),
    notificationViewModel: NotificationViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val notificationUiState by notificationViewModel.uiState.collectAsState()
    var showJoinDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(MeetingFilterTab.ALL) }
    val context = LocalContext.current
    val pendingInvites = remember(uiState.invitations) {
        uiState.invitations.filter { "INVITED".equals(it.status, ignoreCase = true) }
    }

    // Time-aware greeting
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember(currentHour) {
        when (currentHour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val todayFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    // Filter meetings based on active tab and search query
    val filteredMeetings = remember(uiState.meetings, selectedTab, searchQuery) {
        val baseList = when (selectedTab) {
            MeetingFilterTab.ALL -> uiState.meetings
            MeetingFilterTab.LIVE -> uiState.liveMeetings
            MeetingFilterTab.UPCOMING -> uiState.upcomingMeetings
            MeetingFilterTab.PAST -> uiState.pastMeetings
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            val query = searchQuery.trim().lowercase()
            baseList.filter { meeting ->
                meeting.title.lowercase().contains(query) ||
                meeting.hostName.lowercase().contains(query) ||
                (meeting.meetingCode ?: "").lowercase().contains(query)
            }
        }
    }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ElectricIndigo.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "MEETMIND AI", 
                                    style = Typography.titleMedium, 
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp,
                                    color = NeonCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                                "Live Intelligence & AI Proxy", 
                                style = Typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        }
                    }
                },
                actions = {
                    // Manual sync/refresh button
                    IconButton(
                        onClick = {
                            viewModel.loadMeetings()
                            Toast.makeText(context, "Refreshing schedule...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(GlassWhite, CircleShape)
                            .bounceClick()
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    // Notifications button with unread count
                    IconButton(
                        onClick = onNotifications,
                        modifier = Modifier
                            .size(36.dp)
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
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = TextPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    // User Profile button
                    Surface(
                        onClick = onProfile,
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick(),
                        shape = CircleShape,
                        color = ElectricIndigo.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, NeonCyan.copy(alpha = 0.6f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val userInitial = viewModel.currentUserName.firstOrNull()?.uppercase() ?: "U"
                            Text(
                                text = userInitial,
                                style = Typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        val layoutInfo = LocalResponsiveLayout.current
        ResponsiveContainer(maxWidth = 960.dp) { layout ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = layout.horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
            // 1. Personalized Greeting Header with scrollable superpower pills
            item {
                Spacer(modifier = Modifier.height(2.dp))
                GreetingHeader(
                    greeting = greeting,
                    userName = viewModel.currentUserName,
                    todayFormatted = todayFormatted
                )
            }

            // Pending Meeting Invitations Banner (if any)
            if (pendingInvites.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onInvitations() },
                        color = CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricIndigo)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(ElectricIndigo.copy(alpha = 0.2f), NeonCyan.copy(alpha = 0.08f), androidx.compose.ui.graphics.Color.Transparent)
                                    )
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(ElectricIndigo),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MailOutline, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${pendingInvites.size} Pending Meeting Invitation${if (pendingInvites.size > 1) "s" else ""}",
                                    style = Typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    "Tap to view, accept & join immediately",
                                    style = Typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // 2. Upcoming / Live Meeting Hero Banner
            val activeHeroMeeting = uiState.liveMeetings.firstOrNull() ?: uiState.upcomingMeetings.firstOrNull()
            item {
                if (activeHeroMeeting != null) {
                    UpcomingMeetingHero(
                        meeting = activeHeroMeeting,
                        onClick = { onMeetingClick(activeHeroMeeting.id.toString()) }
                    )
                } else {
                    EmptyHeroCard(
                        onNewMeeting = onNewMeeting,
                        onSchedule = onViewAllSchedule
                    )
                }
            }

            // 3. 2x2 Bento Quick Controls Grid (Device-compatible on any screen width)
            item {
                Text(
                    "Quick Controls",
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                ResponsiveQuickControlsGrid(
                    onNewMeeting = onNewMeeting,
                    onJoinCall = { showJoinDialog = true },
                    onSchedule = onViewAllSchedule,
                    onAiProxy = onAiRepConfig
                )
            }

            // 4. AI Post-Meeting Intelligence Hub Banner
            item {
                AiIntelligenceHubCard(
                    onSummariesClick = onSummaries,
                    onAiRepConfig = onAiRepConfig
                )
            }

            // 5. Search Bar & Meeting Filter Tabs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Meetings & Agendas",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        TextButton(
                            onClick = onViewAllSchedule,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Calendar View", style = Typography.labelMedium, color = NeonCyan)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Search input
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text("Search by title, host, or code...", style = Typography.bodyMedium, color = TextDisabled)
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Clear search", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Filter Tab Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(MeetingFilterTab.entries) { tab ->
                            val count = when (tab) {
                                MeetingFilterTab.ALL -> uiState.meetings.size
                                MeetingFilterTab.LIVE -> uiState.liveMeetings.size
                                MeetingFilterTab.UPCOMING -> uiState.upcomingMeetings.size
                                MeetingFilterTab.PAST -> uiState.pastMeetings.size
                            }
                            val isSelected = selectedTab == tab
                            FilterTabChip(
                                label = "${tab.label} ($count)",
                                isSelected = isSelected,
                                isLive = tab == MeetingFilterTab.LIVE && count > 0,
                                onClick = { selectedTab = tab }
                            )
                        }
                    }
                }
            }

            // 6. Meeting Schedule List
            if (filteredMeetings.isEmpty()) {
                item {
                    EmptyMeetingFilterCard(
                        searchQuery = searchQuery,
                        selectedTab = selectedTab.label,
                        onClearSearch = { searchQuery = "" },
                        onNewMeeting = onNewMeeting
                    )
                }
            } else {
                items(filteredMeetings.take(10), key = { it.id }) { meeting ->
                    MeetingScheduleItem(
                        meeting = meeting,
                        onClick = { onMeetingClick(meeting.id.toString()) }
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun GreetingHeader(
    greeting: String,
    userName: String,
    todayFormatted: String
) {
    val layout = LocalResponsiveLayout.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (layout.isSmallPhone) 12.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$greeting, $userName 👋",
                        style = if (layout.isSmallPhone) Typography.titleLarge else Typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = todayFormatted,
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldGreen.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(EmeraldGreen, CircleShape)
                                .pulse(color = EmeraldGreen)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "AI Engine Ready",
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable AI Capabilities pill bar (never truncates on 360dp or smaller screens)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    SuperpowerPill(
                        icon = Icons.Default.GraphicEq,
                        text = "Live Speech STT",
                        accent = NeonCyan
                    )
                }
                item {
                    SuperpowerPill(
                        icon = Icons.Default.Translate,
                        text = "12+ Languages",
                        accent = PurpleViolet
                    )
                }
                item {
                    SuperpowerPill(
                        icon = Icons.Default.SmartToy,
                        text = "Talking AI Proxy",
                        accent = AmberGold
                    )
                }
                item {
                    SuperpowerPill(
                        icon = Icons.Default.Assessment,
                        text = "Smart Summaries",
                        accent = EmeraldGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun SuperpowerPill(
    icon: ImageVector,
    text: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = Typography.labelSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun UpcomingMeetingHero(
    meeting: Meeting,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isLive = meeting.status == MeetingStatus.LIVE
    val code = meeting.meetingCode ?: "mm-${meeting.id}"

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isLive) EmeraldGreen.copy(alpha = 0.8f) else NeonCyan.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = if (isLive) EmeraldGreen.copy(alpha = 0.35f) else ElectricIndigo.copy(alpha = 0.25f)
            )
            .bounceClick()
    ) {
        Box {
            // Gradient Header Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.linearGradient(
                            colors = if (isLive) {
                                listOf(
                                    EmeraldGreen.copy(alpha = 0.25f),
                                    Color(0xFF064E3B).copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    ElectricIndigo.copy(alpha = 0.30f),
                                    PurpleViolet.copy(alpha = 0.20f),
                                    Color.Transparent
                                )
                            }
                        )
                    )
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isLive) EmeraldGreen else ElectricIndigo, CircleShape)
                                .pulse(color = if (isLive) EmeraldGreen else ElectricIndigo)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLive) "LIVE NOW 🟢" else "NEXT UP",
                            style = Typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp,
                            color = if (isLive) EmeraldGreen else NeonCyan,
                            maxLines = 1
                        )
                    }

                    // Copy Code Pill
                    Surface(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(code))
                            Toast.makeText(context, "Meeting code copied: $code", Toast.LENGTH_SHORT).show()
                        },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(code, style = Typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ContentCopy, "Copy Code", tint = NeonCyan, modifier = Modifier.size(11.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = meeting.title, 
                    style = Typography.titleLarge, 
                    fontWeight = FontWeight.Bold, 
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Schedule, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(meeting.scheduledAt, style = Typography.bodySmall, color = TextSecondary)

                    if (meeting.hostName.isNotBlank()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Host: ${meeting.hostName}", 
                            style = Typography.bodySmall, 
                            color = TextSecondary, 
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary CTA Join Button
                MeetMindButton(
                    text = if (isLive) "Join Live Meeting 🟢" else "Enter Meeting Room",
                    onClick = onClick,
                    isAiAction = isLive,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EmptyHeroCard(
    onNewMeeting: () -> Unit,
    onSchedule: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = ElectricIndigo.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.VideoCall, null, tint = ElectricIndigo, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "You're All Caught Up 🎉",
                style = Typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "No upcoming calls scheduled. Start an instant meeting or plan one ahead.",
                style = Typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNewMeeting,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Instant Call", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onSchedule,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Schedule", color = TextPrimary)
                }
            }
        }
    }
}

/**
 * Bento Grid: Dynamically adapts across compact phones, standard screens, and wide tablets/foldables.
 */
@Composable
private fun ResponsiveQuickControlsGrid(
    onNewMeeting: () -> Unit,
    onJoinCall: () -> Unit,
    onSchedule: () -> Unit,
    onAiProxy: () -> Unit
) {
    val layout = LocalResponsiveLayout.current
    val cols = layout.bentoGridColumns

    if (cols >= 4) {
        // Panoramic single row on large tablets / landscape
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickControlCard("Instant Call", "Start a new room", Icons.Default.VideoCall, ElectricIndigo, Modifier.weight(1f), onNewMeeting)
            QuickControlCard("Join Call", "Enter code or ID", Icons.Default.AddBox, NeonCyan, Modifier.weight(1f), onJoinCall)
            QuickControlCard("Schedule", "Plan meeting agenda", Icons.Default.CalendarMonth, AmberGold, Modifier.weight(1f), onSchedule)
            QuickControlCard("AI Proxy Agent", "Voice & talking photo", Icons.Default.SmartToy, PurpleViolet, Modifier.weight(1f), onAiProxy)
        }
    } else {
        // Adaptive 2x2 grid for smartphones & portrait screens
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickControlCard("Instant Call", "Start a new room", Icons.Default.VideoCall, ElectricIndigo, Modifier.weight(1f), onNewMeeting)
                QuickControlCard("Join Call", "Enter code or ID", Icons.Default.AddBox, NeonCyan, Modifier.weight(1f), onJoinCall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickControlCard("Schedule", "Plan meeting agenda", Icons.Default.CalendarMonth, AmberGold, Modifier.weight(1f), onSchedule)
                QuickControlCard("AI Proxy Agent", "Voice & talking photo", Icons.Default.SmartToy, PurpleViolet, Modifier.weight(1f), onAiProxy)
            }
        }
    }
}

@Composable
private fun QuickControlCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val layout = LocalResponsiveLayout.current
    val isSmall = layout.isSmallPhone
    val iconBoxSize = if (isSmall) 38.dp else 44.dp
    val iconInnerSize = if (isSmall) 18.dp else 22.dp
    val padH = if (isSmall) 10.dp else 14.dp
    val padV = if (isSmall) 10.dp else 14.dp
    val spacerW = if (isSmall) 8.dp else 12.dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier.bounceClick()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = padH, vertical = padV),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(iconBoxSize),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconInnerSize))
                }
            }
            Spacer(modifier = Modifier.width(spacerW))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = if (isSmall) Typography.labelLarge else Typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = if (isSmall) Typography.bodySmall.copy(fontSize = 11.sp) else Typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AiIntelligenceHubCard(
    onSummariesClick: () -> Unit,
    onAiRepConfig: () -> Unit
) {
    Surface(
        onClick = onSummariesClick,
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, NeonCyan.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = NeonCyan.copy(alpha = 0.2f))
            .bounceClick()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = NeonCyan.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, NeonCyan.copy(alpha = 0.4f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Assessment, 
                            contentDescription = null, 
                            tint = NeonCyan, 
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "AI Meeting Intelligence", 
                            style = Typography.titleMedium, 
                            color = TextPrimary, 
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(6.dp))
                        StatusChip(text = "AI", color = NeonCyan)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Automatic summaries, key decisions, and action items.", 
                        style = Typography.bodySmall, 
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward, 
                    null, 
                    tint = NeonCyan, 
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onSummariesClick,
                    shape = RoundedCornerShape(10.dp),
                    color = ElevatedSurface,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Assignment, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("View Summaries", style = Typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                Surface(
                    onClick = onAiRepConfig,
                    shape = RoundedCornerShape(10.dp),
                    color = ElevatedSurface,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, null, tint = PurpleViolet, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Proxy Setup", style = Typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTabChip(
    label: String,
    isSelected: Boolean,
    isLive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            if (isLive) EmeraldGreen.copy(alpha = 0.2f) else ElectricIndigo.copy(alpha = 0.25f)
        } else CardSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) {
                if (isLive) EmeraldGreen else NeonCyan
            } else BorderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(EmeraldGreen, CircleShape)
                        .pulse(color = EmeraldGreen)
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = Typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) {
                    if (isLive) EmeraldGreen else NeonCyan
                } else TextSecondary
            )
        }
    }
}

@Composable
fun MeetingScheduleItem(
    meeting: Meeting,
    onClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val isLive = meeting.status == MeetingStatus.LIVE
    val isScheduled = meeting.status == MeetingStatus.SCHEDULED
    val code = meeting.meetingCode ?: "mm-${meeting.id}"
    val accentColor = when (meeting.status) {
        MeetingStatus.LIVE -> EmeraldGreen
        MeetingStatus.SCHEDULED -> ElectricIndigo
        else -> TextSecondary
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isLive) EmeraldGreen.copy(alpha = 0.6f) else accentColor.copy(alpha = 0.22f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (meeting.status) {
                            MeetingStatus.LIVE -> Icons.Default.VideoCall
                            MeetingStatus.SCHEDULED -> Icons.Default.Event
                            else -> Icons.Default.History
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = meeting.title,
                        style = Typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (code.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(code))
                                Toast.makeText(context, "Code copied: $code", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricIndigo.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = code,
                                    style = Typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(3.dp))
                                Icon(Icons.Default.ContentCopy, null, tint = NeonCyan, modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccessTime, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(meeting.scheduledAt, style = Typography.bodySmall, color = TextSecondary, maxLines = 1)
                    
                    if (meeting.hostName.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            meeting.hostName, 
                            style = Typography.bodySmall, 
                            color = TextSecondary, 
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isLive) {
                StatusChip(text = "LIVE 🟢", color = EmeraldGreen)
            } else if (isScheduled) {
                StatusChip(text = "UPCOMING", color = ElectricIndigo)
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

@Composable
private fun EmptyMeetingFilterCard(
    searchQuery: String,
    selectedTab: String,
    onClearSearch: () -> Unit,
    onNewMeeting: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                tint = TextDisabled,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (searchQuery.isNotEmpty()) "No results matching \"$searchQuery\"" else "No $selectedTab meetings found",
                style = Typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (searchQuery.isNotEmpty()) "Check your spelling or search for another meeting title or code." else "Ready to connect? Start an instant call now.",
                style = Typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (searchQuery.isNotEmpty()) {
                TextButton(onClick = onClearSearch) {
                    Text("Clear Search", color = NeonCyan)
                }
            } else {
                TextButton(onClick = onNewMeeting) {
                    Icon(Icons.Default.VideoCall, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start Meeting Now", color = NeonCyan)
                }
            }
        }
    }
}
