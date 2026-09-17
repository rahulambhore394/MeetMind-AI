package com.developer_rahul.meetmind_ai.feature.participants.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.meetings.presentation.MeetingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddParticipantScreen(
    meetingId: String,
    onBack: () -> Unit,
    onInviteSent: () -> Unit,
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    
    // For MVP, we'll just invite one by one using email
    
    LaunchedEffect(meetingId) {
        if (meetingId.isNotBlank()) {
            viewModel.loadMeetingDetails(meetingId)
        }
    }

    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invite Participants", style = Typography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface,
        bottomBar = {
            Box(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                MeetMindButton(
                    text = "Send Broadcast Invitation",
                    onClick = {
                        val targetId = meetingId.toLongOrNull() ?: uiState.selectedMeeting?.id
                        if (targetId != null && email.isNotBlank()) {
                            viewModel.batchInviteParticipants(targetId, email) { success, msg ->
                                statusMessage = msg
                                if (success) {
                                    email = "" // Reset after sending
                                }
                            }
                        } else {
                            statusMessage = if (email.isBlank()) "Please enter at least one email address." else "Unable to resolve meeting ID."
                        }
                    },
                    isLoading = uiState.actionInProgress,
                    enabled = email.isNotBlank()
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Text(
                "Invite via Email Broadcast",
                style = Typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Invite one or multiple people simultaneously. Enter email addresses separated by commas, spaces, or lines.",
                style = Typography.bodyMedium,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            MeetMindTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address(es)",
                leadingIcon = Icons.Default.Email,
                singleLine = false,
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    statusMessage!!, 
                    color = if (statusMessage!!.startsWith("Failed") || statusMessage!!.startsWith("Please")) RoseRed else EmeraldGreen, 
                    style = Typography.bodySmall
                )
            } else if (uiState.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(uiState.error!!, color = RoseRed, style = Typography.bodySmall)
            }
        }
    }
}

@Composable
fun UserSearchItem(name: String, isSelected: Boolean, onToggle: () -> Unit) {
    MeetMindCard(onClick = onToggle) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color.DarkGray) {
                Box(contentAlignment = Alignment.Center) {
                    Text(name.take(1), color = Color.White)
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(name, style = Typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Default.Close, null, tint = RoseRed)
            } else {
                Icon(Icons.Default.Add, null, tint = ElectricIndigo)
            }
        }
    }
}
