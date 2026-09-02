package com.developer_rahul.meetmind_ai.feature.recording.presentation

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingRecordingsScreen(
    onBack: () -> Unit,
    onPlayRecording: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recordings", style = Typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.FilterList, null, tint = TextPrimary)
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            items(sampleRecordings) { item ->
                RecordingItemCard(
                    title = item.title,
                    date = item.date,
                    duration = item.duration,
                    status = "COMPLETED",
                    hasAiSummary = item.hasAiSummary,
                    onClick = { onPlayRecording(item.id) }
                )
            }
        }
    }
}

@Composable
fun RecordingItemCard(
    title: String,
    date: String,
    duration: String,
    status: String,
    hasAiSummary: Boolean = false,
    onClick: () -> Unit
) {
    MeetMindCard(onClick = onClick, isAiCard = hasAiSummary) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = MaterialTheme.shapes.medium,
                color = when (status) {
                    "READY_FOR_TRANSCRIPTION", "COMPLETED" -> ElectricIndigo.copy(alpha = 0.1f)
                    "FAILED" -> RoseRed.copy(alpha = 0.1f)
                    else -> ElectricIndigo.copy(alpha = 0.05f)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (status == "COMPLETED" || status == "READY_FOR_TRANSCRIPTION") {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(28.dp))
                    } else if (status == "FAILED") {
                        Icon(Icons.Default.Error, contentDescription = null, tint = RoseRed, modifier = Modifier.size(24.dp))
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = ElectricIndigo,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = Typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("$date • $duration", style = Typography.bodySmall, color = TextSecondary)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (status) {
                        "READY_FOR_TRANSCRIPTION" -> EmeraldGreen
                        "COMPLETED" -> NeonCyan
                        "FAILED" -> RoseRed
                        "STARTED", "PROCESSING", "AUDIO_PREPARING", "AUDIO_CHUNKING" -> ElectricIndigo
                        else -> TextDisabled
                    }
                    StatusChip(text = status.replace("_", " "), color = statusColor)
                    
                    if (hasAiSummary) {
                        StatusChip(text = "AI INSIGHTS", color = NeonCyan)
                    }
                }
            }
            
            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextDisabled)
            }
        }
    }
}

data class MeetingRecording(
    val id: String,
    val title: String,
    val date: String,
    val duration: String,
    val size: String,
    val hasAiSummary: Boolean = false
)

val sampleRecordings = listOf(
    MeetingRecording("1", "Architecture Review", "Oct 12, 2023", "45:20", "124 MB", true),
    MeetingRecording("2", "Product Design Sync", "Oct 11, 2023", "15:05", "42 MB", true),
    MeetingRecording("3", "Budget Planning Q4", "Oct 10, 2023", "1:12:40", "310 MB", false),
    MeetingRecording("4", "Security Implementation", "Oct 09, 2023", "30:15", "85 MB", true)
)
