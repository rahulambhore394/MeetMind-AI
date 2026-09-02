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
    
    LaunchedEffect(uiState.actionInProgress) {
        // If we wanted to navigate back only on success:
        // if (!uiState.actionInProgress && uiState.error == null && emailWasSubmitted) onInviteSent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Participant", style = Typography.titleLarge, color = TextPrimary) },
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
                    text = "Send Invite",
                    onClick = {
                        val id = meetingId.toLongOrNull()
                        if (id != null && email.isNotBlank()) {
                            viewModel.inviteParticipant(id, email)
                            email = "" // Reset after sending
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
                "Invite by Email",
                style = Typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "The person will receive an invitation to join this intelligent meeting.",
                style = Typography.bodyMedium,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            MeetMindTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                leadingIcon = Icons.Default.Email
            )
            
            if (uiState.error != null) {
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

val sampleUsers = listOf("Alice Chen", "Bob Wilson", "Charlie Day", "David Miller", "Eve Adams", "Frank Wright")
