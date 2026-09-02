package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMeetingScreen(
    meetingId: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    viewModel: MeetingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val meeting = uiState.selectedMeeting

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(meetingId) {
        viewModel.loadMeetingDetails(meetingId)
    }

    LaunchedEffect(meeting) {
        if (meeting != null) {
            title = meeting.title
            description = meeting.description ?: ""
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = BackgroundSurface,
            title = { Text("Delete Meeting?", style = Typography.titleMedium, color = TextPrimary) },
            text = { Text("Are you sure you want to delete this meeting? This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        val id = meetingId.toLongOrNull()
                        if (id != null) {
                            viewModel.deleteMeeting(id) {
                                showDeleteConfirm = false
                                onSave()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Meeting", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val id = meetingId.toLongOrNull()
                        if (id != null && meeting != null) {
                            // Update meeting title/description
                            onSave()
                        }
                    }) {
                        Text("Save", color = NeonCyan, style = Typography.labelLarge)
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            SectionHeader(title = "Information")
            
            MeetMindTextField(
                value = title,
                onValueChange = { title = it },
                label = "Meeting Title",
                leadingIcon = Icons.Default.Title
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            MeetMindTextField(
                value = description,
                onValueChange = { description = it },
                label = "Agenda / Description",
                leadingIcon = Icons.Default.Description
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            SectionHeader(title = "Participants")
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.participants.forEach { p ->
                    EditParticipantRow(name = p.name)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            MeetMindButton(
                text = "Delete Meeting 🗑️",
                onClick = { showDeleteConfirm = true },
                containerColor = RoseRed.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun EditParticipantRow(name: String) {
    MeetMindCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = Color.DarkGray
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(name.take(1), color = Color.White, style = Typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, style = Typography.bodyLarge, color = TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { }) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = RoseRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}
