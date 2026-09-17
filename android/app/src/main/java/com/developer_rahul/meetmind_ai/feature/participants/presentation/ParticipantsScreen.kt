package com.developer_rahul.meetmind_ai.feature.participants.presentation

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.ParticipantRole
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.ParticipantStatus
import com.developer_rahul.meetmind_ai.feature.meetings.presentation.MeetingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantsScreen(
    meetingId: String,
    onBack: () -> Unit,
    onAddParticipant: () -> Unit,
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val participants = uiState.participants
    
    androidx.compose.runtime.LaunchedEffect(meetingId) {
        viewModel.loadMeetingDetails(meetingId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Participants", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onAddParticipant) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add", tint = ElectricIndigo)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        if (uiState.isLoadingDetails && participants.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricIndigo)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                val host = participants.find { it.role == ParticipantRole.HOST }
                val others = participants.filter { it.role != ParticipantRole.HOST }

                if (host != null) {
                    item {
                        SectionHeader(title = "Host")
                        ParticipantItem(
                            name = host.name,
                            role = host.role.name,
                            isOnline = host.isOnline,
                            status = host.status
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (others.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Participants (${others.size})")
                    }
                    items(others) { participant ->
                        ParticipantItem(
                            name = participant.name,
                            role = participant.role.name,
                            isOnline = participant.isOnline,
                            isAi = participant.role == ParticipantRole.AI_REPRESENTATIVE,
                            status = participant.status
                        )
                    }
                }
                
                if (participants.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text("No participants yet", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParticipantItem(
    name: String,
    role: String,
    isOnline: Boolean,
    isAi: Boolean = false,
    status: ParticipantStatus? = null
) {
    MeetMindCard(isAiCard = isAi) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isAi) NeonCyan.copy(alpha = 0.1f) else Color.DarkGray
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isAi) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                    } else {
                        Text(name.take(1), style = Typography.titleMedium, color = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = Typography.titleSmall, color = TextPrimary)
                    if (isOnline) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(8.dp).background(EmeraldGreen, CircleShape))
                    }
                    if (status == ParticipantStatus.INVITED) {
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusChip(text = "INVITED", color = Color(0xFFD97706))
                    }
                }
                Text(role, style = Typography.bodySmall, color = TextSecondary)
            }
            
            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextDisabled)
            }
        }
    }
}

data class ParticipantInfo(
    val name: String,
    val role: String,
    val isOnline: Boolean,
    val isAi: Boolean = false
)
