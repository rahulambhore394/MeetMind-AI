package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingInvitationScreen(
    hostName: String = "Alice Chen",
    meetingTitle: String = "Quarterly Business Review",
    dateTime: String = "Oct 20, 2023 | 02:00 PM",
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundSurface,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onDecline) {
                        Icon(Icons.Default.Close, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = ElectricIndigo.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Email, null, modifier = Modifier.size(48.dp), tint = ElectricIndigo)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Meeting Invitation",
                style = Typography.displaySmall,
                color = TextPrimary,
                fontWeight = FontWeight.Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "$hostName has invited you to join a meeting on MeetMind AI.",
                style = Typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            MeetMindCard {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    InviteDetailRow(icon = Icons.Default.Person, label = "Invited by", value = hostName)
                    InviteDetailRow(icon = Icons.Default.Event, label = "Meeting", value = meetingTitle)
                    InviteDetailRow(icon = Icons.Default.Schedule, label = "Time", value = dateTime)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassWhite)
                ) {
                    Text("Decline", color = TextPrimary, style = Typography.labelLarge)
                }
                
                MeetMindButton(
                    text = "Accept",
                    onClick = onAccept,
                    modifier = Modifier.weight(1.5f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InviteDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = Typography.labelSmall, color = TextSecondary)
            Text(value, style = Typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}
