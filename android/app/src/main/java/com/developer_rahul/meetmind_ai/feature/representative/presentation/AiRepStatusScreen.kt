package com.developer_rahul.meetmind_ai.feature.representative.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.core.ui.provideAiRepStatusViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRepStatusScreen(
    meetingId: Long,
    onBack: () -> Unit,
    onViewReport: (Long, Long) -> Unit,
    viewModel: AiRepStatusViewModel = viewModel(factory = provideAiRepStatusViewModelFactory(meetingId))
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Representative Status", style = Typography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = NeonCyan)
            } else if (uiState.error != null) {
                Text(uiState.error!!, color = Color.Red)
            } else if (uiState.representative != null) {
                val rep = uiState.representative!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("AI Representative", style = Typography.headlineMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusChip(
                        text = rep.status,
                        color = when (rep.status) {
                            "ACTIVE" -> NeonCyan
                            "COMPLETED" -> Color.Green
                            "FAILED" -> Color.Red
                            "CANCELLED" -> Color.Gray
                            else -> AmberGold
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    MeetMindCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            InfoRow(label = "Meeting ID", value = rep.meetingId.toString())
                            InfoRow(label = "Started At", value = rep.startedAt ?: "Not started")
                            InfoRow(label = "Ended At", value = rep.endedAt ?: "N/A")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    if (rep.status == "COMPLETED") {
                        MeetMindButton(
                            text = "View Meeting Report",
                            onClick = { onViewReport(rep.meetingId, rep.id) },
                            isAiAction = true
                        )
                    } else if (rep.status == "SCHEDULED" || rep.status == "CREATED") {
                        MeetMindButton(
                            text = "Cancel Representative",
                            onClick = { viewModel.cancelRepresentative() },
                            containerColor = Color.Gray,
                            isLoading = uiState.isCancelling
                        )
                    }
                }
            } else {
                Text("No representative found for this meeting.", color = TextSecondary)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = Typography.labelMedium, color = TextSecondary)
        Text(value, style = Typography.bodyMedium, color = TextPrimary)
    }
}
