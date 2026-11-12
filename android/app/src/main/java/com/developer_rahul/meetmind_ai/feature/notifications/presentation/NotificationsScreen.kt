package com.developer_rahul.meetmind_ai.feature.notifications.presentation

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.notifications.domain.model.Notification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToMeeting: (String) -> Unit = {},
    onNavigateToLiveMeeting: (String) -> Unit = {},
    onNavigateToSummary: (String) -> Unit = {},
    onNavigateToTranscript: (String) -> Unit = {},
    onNavigateToRepresentativeReport: (Long, Long) -> Unit = { _, _ -> },
    viewModel: NotificationViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val layout = LocalResponsiveLayout.current

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showTestMenu by remember { mutableStateOf(false) }

    // Clear All Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(RoseRed.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = RoseRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Clear All Notifications", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Text(
                    "Are you sure you want to remove all notifications? This action cannot be undone.",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearAllNotifications()
                        Toast.makeText(context, "All notifications cleared", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Notifications",
                            style = if (layout.isSmallPhone) Typography.titleMedium else Typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = NeonCyan.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    "${uiState.unreadCount} new",
                                    style = Typography.labelSmall.copy(fontSize = 11.sp),
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.bounceClick()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Send Test Alert Trigger
                    Box {
                        IconButton(
                            onClick = { showTestMenu = true },
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(Icons.Default.AddAlert, contentDescription = "Simulate Notification", tint = NeonCyan)
                        }

                        DropdownMenu(
                            expanded = showTestMenu,
                            onDismissRequest = { showTestMenu = false },
                            modifier = Modifier.background(CardSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("⚡ Test Meeting Invitation", style = Typography.bodyMedium, color = TextPrimary) },
                                onClick = {
                                    showTestMenu = false
                                    viewModel.sendTestNotification("MEETING_INVITATION")
                                    Toast.makeText(context, "Test Meeting Invitation sent!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("✨ Test AI Summary Alert", style = Typography.bodyMedium, color = TextPrimary) },
                                onClick = {
                                    showTestMenu = false
                                    viewModel.sendTestNotification("AI_SUMMARY_READY")
                                    Toast.makeText(context, "Test AI Summary Alert sent!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🤖 Test AI Representative Alert", style = Typography.bodyMedium, color = TextPrimary) },
                                onClick = {
                                    showTestMenu = false
                                    viewModel.sendTestNotification("REPRESENTATIVE_REPORT_READY")
                                    Toast.makeText(context, "Test AI Representative Report sent!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    // Mark all read button
                    if (uiState.unreadCount > 0) {
                        IconButton(
                            onClick = {
                                viewModel.markAllAsRead()
                                Toast.makeText(context, "All marked as read", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Mark all read", tint = ElectricIndigo)
                        }
                    }

                    // Clear all button
                    if (uiState.notifications.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all", tint = TextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Tabs Row
            NotificationFilterTabsRow(
                selectedTab = uiState.selectedTab,
                onSelectTab = { viewModel.setFilterTab(it) },
                allCount = uiState.notifications.size,
                unreadCount = uiState.unreadCount.toInt(),
                meetingsCount = uiState.notifications.count { it.isMeetingRelated },
                aiCount = uiState.notifications.count { it.isAiRelated }
            )

            Spacer(Modifier.height(8.dp))

            // Main Content Area
            val filteredList = uiState.filteredNotifications

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else if (filteredList.isEmpty()) {
                EmptyNotificationState(
                    selectedTab = uiState.selectedTab,
                    onSendTest = { viewModel.sendTestNotification() }
                )
            } else {
                val isTwoColumn = layout.isTabletOrLarger || layout.isLandscape

                if (isTwoColumn) {
                    // Tablet & Landscape 2-Column Responsive Grid
                    ResponsiveContainer(maxWidth = 1100.dp) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = layout.horizontalPadding),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
                        ) {
                            items(filteredList, key = { it.id }) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    onCardClick = {
                                        viewModel.markAsRead(notification.id)
                                        handleNotificationNavigation(
                                            notification = notification,
                                            onNavigateToMeeting = onNavigateToMeeting,
                                            onNavigateToLiveMeeting = onNavigateToLiveMeeting,
                                            onNavigateToSummary = onNavigateToSummary,
                                            onNavigateToTranscript = onNavigateToTranscript,
                                            onNavigateToRepresentativeReport = onNavigateToRepresentativeReport
                                        )
                                    },
                                    onDelete = { viewModel.deleteNotification(notification.id) }
                                )
                            }
                        }
                    }
                } else {
                    // Phone 1-Column Responsive List
                    ResponsiveContainer(maxWidth = 640.dp) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = layout.horizontalPadding),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
                        ) {
                            items(filteredList, key = { it.id }) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    onCardClick = {
                                        viewModel.markAsRead(notification.id)
                                        handleNotificationNavigation(
                                            notification = notification,
                                            onNavigateToMeeting = onNavigateToMeeting,
                                            onNavigateToLiveMeeting = onNavigateToLiveMeeting,
                                            onNavigateToSummary = onNavigateToSummary,
                                            onNavigateToTranscript = onNavigateToTranscript,
                                            onNavigateToRepresentativeReport = onNavigateToRepresentativeReport
                                        )
                                    },
                                    onDelete = { viewModel.deleteNotification(notification.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter Tabs Horizontal Scroll Bar
 */
@Composable
private fun NotificationFilterTabsRow(
    selectedTab: NotificationFilterTab,
    onSelectTab: (NotificationFilterTab) -> Unit,
    allCount: Int,
    unreadCount: Int,
    meetingsCount: Int,
    aiCount: Int
) {
    val layout = LocalResponsiveLayout.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = layout.horizontalPadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NotificationFilterChip(
            title = "All",
            count = allCount,
            isSelected = selectedTab == NotificationFilterTab.ALL,
            onClick = { onSelectTab(NotificationFilterTab.ALL) }
        )
        NotificationFilterChip(
            title = "Unread",
            count = unreadCount,
            isSelected = selectedTab == NotificationFilterTab.UNREAD,
            accentColor = NeonCyan,
            onClick = { onSelectTab(NotificationFilterTab.UNREAD) }
        )
        NotificationFilterChip(
            title = "Meetings",
            count = meetingsCount,
            isSelected = selectedTab == NotificationFilterTab.MEETINGS,
            accentColor = EmeraldGreen,
            onClick = { onSelectTab(NotificationFilterTab.MEETINGS) }
        )
        NotificationFilterChip(
            title = "AI Insights",
            count = aiCount,
            isSelected = selectedTab == NotificationFilterTab.AI_INSIGHTS,
            accentColor = Color(0xFFA78BFA),
            onClick = { onSelectTab(NotificationFilterTab.AI_INSIGHTS) }
        )
    }
}

@Composable
private fun NotificationFilterChip(
    title: String,
    count: Int,
    isSelected: Boolean,
    accentColor: Color = ElectricIndigo,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.20f) else GlassWhite,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) accentColor else BorderColor
        ),
        modifier = Modifier.bounceClick()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                title,
                style = Typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) (if (accentColor == ElectricIndigo) NeonCyan else accentColor) else TextSecondary
            )
            if (count > 0) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(if (isSelected) accentColor else BackgroundSurface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$count",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else TextDisabled
                    )
                }
            }
        }
    }
}

/**
 * Individual Notification Card with Category Glow, Timestamp, and Action Button.
 */
@Composable
private fun NotificationCard(
    notification: Notification,
    onCardClick: () -> Unit,
    onDelete: () -> Unit
) {
    val layout = LocalResponsiveLayout.current

    val (badgeText, badgeColor, icon) = when (notification.type) {
        "MEETING_STARTED" -> Triple("LIVE CALL", EmeraldGreen, Icons.Default.VideoCall)
        "MEETING_INVITATION" -> Triple("INVITATION", EmeraldGreen, Icons.Default.Event)
        "MEETING_REMINDER" -> Triple("REMINDER", Color(0xFFFBBF24), Icons.Default.Schedule)
        "AI_SUMMARY_READY" -> Triple("AI SUMMARY", NeonCyan, Icons.Default.AutoAwesome)
        "TRANSCRIPT_READY" -> Triple("TRANSCRIPT", Color(0xFFA78BFA), Icons.Default.Description)
        "ACTION_ITEM_ASSIGNED" -> Triple("ACTION ITEM", AmberGold, Icons.AutoMirrored.Filled.Assignment)
        "REPRESENTATIVE_ACTIVE" -> Triple("AI PROXY", PurpleViolet, Icons.Default.SmartToy)
        "REPRESENTATIVE_REPORT_READY" -> Triple("DIGITAL TWIN", NeonCyan, Icons.Default.Psychology)
        "NEW_MESSAGE" -> Triple("CHAT", ElectricIndigo, Icons.Default.Chat)
        else -> Triple("SYSTEM", TextSecondary, Icons.Default.Notifications)
    }

    MeetMindCard(
        isAiCard = notification.isAiRelated,
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(),
        onClick = onCardClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (layout.isSmallPhone) 2.dp else 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Category Icon with glow
                Surface(
                    modifier = Modifier
                        .size(if (layout.isSmallPhone) 40.dp else 46.dp)
                        .aiGlow(color = badgeColor, radius = if (notification.isRead) 0.dp else 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = badgeColor.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(if (layout.isSmallPhone) 20.dp else 24.dp))
                    }
                }

                Spacer(modifier = Modifier.width(if (layout.isSmallPhone) 10.dp else 14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Header Row: Category Badge + Timestamp + Unread Dot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(text = badgeText, color = badgeColor)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                notification.formattedTime,
                                style = Typography.labelSmall.copy(fontSize = 11.sp),
                                color = TextDisabled
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!notification.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(NeonCyan, CircleShape)
                                        .aiGlow(color = NeonCyan, radius = 6.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                            }

                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = TextDisabled, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        notification.title,
                        style = if (layout.isSmallPhone) Typography.titleSmall else Typography.titleMedium,
                        fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        notification.body,
                        style = Typography.bodySmall.copy(fontSize = if (layout.isSmallPhone) 12.sp else 13.sp),
                        color = if (notification.isRead) TextSecondary else TextPrimary.copy(alpha = 0.9f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )

                    // Action Button on Card
                    if (notification.relatedMeetingId != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        val buttonActionText = when (notification.type) {
                            "MEETING_STARTED" -> "Join Live Room 🚀"
                            "MEETING_INVITATION" -> "View Details 📅"
                            "AI_SUMMARY_READY" -> "Open Intelligence ✨"
                            "TRANSCRIPT_READY" -> "Read Transcript 📄"
                            "REPRESENTATIVE_REPORT_READY" -> "View Twin Report 🤖"
                            else -> "Open Meeting"
                        }

                        Surface(
                            onClick = onCardClick,
                            shape = RoundedCornerShape(10.dp),
                            color = ElectricIndigo.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                            modifier = Modifier.bounceClick()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    buttonActionText,
                                    style = Typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (badgeColor == EmeraldGreen) EmeraldGreen else NeonCyan
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Handle notification clicks by deep-linking to the right feature
 */
private fun handleNotificationNavigation(
    notification: Notification,
    onNavigateToMeeting: (String) -> Unit,
    onNavigateToLiveMeeting: (String) -> Unit,
    onNavigateToSummary: (String) -> Unit,
    onNavigateToTranscript: (String) -> Unit,
    onNavigateToRepresentativeReport: (Long, Long) -> Unit
) {
    val meetingId = notification.relatedMeetingId?.toString()
    if (meetingId != null) {
        when (notification.type) {
            "MEETING_STARTED" -> onNavigateToLiveMeeting(meetingId)
            "MEETING_INVITATION", "MEETING_REMINDER" -> onNavigateToMeeting(meetingId)
            "AI_SUMMARY_READY" -> onNavigateToSummary(meetingId)
            "TRANSCRIPT_READY" -> onNavigateToTranscript(meetingId)
            "REPRESENTATIVE_REPORT_READY" -> {
                val repId = notification.relatedRepresentativeId ?: 1L
                val mId = notification.relatedMeetingId ?: -1L
                onNavigateToRepresentativeReport(mId, repId)
            }
            else -> onNavigateToMeeting(meetingId)
        }
    }
}

/**
 * Contextual Empty State for notifications
 */
@Composable
private fun EmptyNotificationState(
    selectedTab: NotificationFilterTab,
    onSendTest: () -> Unit
) {
    val (title, subtitle) = when (selectedTab) {
        NotificationFilterTab.ALL -> Pair("All caught up!", "You have no notifications right now. Alerts for meetings, action items, and AI summaries will appear here.")
        NotificationFilterTab.UNREAD -> Pair("No unread alerts", "Great job! All your notifications have been read.")
        NotificationFilterTab.MEETINGS -> Pair("No meeting alerts", "Upcoming meeting schedules and live call invitations will show up here.")
        NotificationFilterTab.AI_INSIGHTS -> Pair("No AI insights yet", "Summaries, action item logs, and digital twin reports will be delivered here.")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(ElectricIndigo.copy(alpha = 0.12f), CircleShape)
                .aiGlow(color = ElectricIndigo, radius = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(36.dp))
        }

        Spacer(Modifier.height(18.dp))

        Text(
            title,
            style = Typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            subtitle,
            style = Typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onSendTest,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier.bounceClick()
        ) {
            Icon(Icons.Default.AddAlert, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Simulate Test Alert", style = Typography.labelMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
        }
    }
}
