package com.developer_rahul.meetmind_ai.feature.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.notifications.domain.model.Notification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.markAllAsRead() }) {
                        Text("Mark all read", color = ElectricIndigo)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = NeonCyan)
            } else if (uiState.error != null) {
                Text(uiState.error!!, color = Color.Red, modifier = Modifier.align(Alignment.Center))
            } else if (uiState.notifications.isEmpty()) {
                EmptyNotifications(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(uiState.notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = { viewModel.markAsRead(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification, onClick: () -> Unit) {
    val color = when (notification.type) {
        "MEETING_STARTED" -> EmeraldGreen
        "AI_SUMMARY_READY", "REPRESENTATIVE_REPORT_READY" -> NeonCyan
        "ACTION_ITEM_ASSIGNED" -> AmberGold
        else -> ElectricIndigo
    }
    
    val icon = when (notification.type) {
        "MEETING_STARTED" -> Icons.Default.VideoCall
        "AI_SUMMARY_READY" -> Icons.Default.AutoAwesome
        "REPRESENTATIVE_REPORT_READY" -> Icons.Default.SmartToy
        "ACTION_ITEM_ASSIGNED" -> Icons.Default.Assignment
        else -> Icons.Default.Notifications
    }

    MeetMindCard(
        isAiCard = notification.type.contains("AI") || notification.type.contains("REPRESENTATIVE"),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.medium,
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(notification.title, style = Typography.titleSmall, color = TextPrimary)
                    if (!notification.isRead) {
                        Box(modifier = Modifier.size(8.dp).background(ElectricIndigo, CircleShape))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(notification.body, style = Typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(notification.createdAt, style = Typography.labelSmall, color = TextDisabled)
            }
        }
    }
}

@Composable
fun EmptyNotifications(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.NotificationsNone, null, modifier = Modifier.size(64.dp), tint = TextDisabled)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No notifications yet", style = Typography.titleMedium, color = TextPrimary)
        Text("Stay tuned for meeting updates and AI insights.", style = Typography.bodyMedium, color = TextSecondary)
    }
}
