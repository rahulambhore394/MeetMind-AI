package com.developer_rahul.meetmind_ai.feature.transcript.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.provideTranscriptViewModelFactory
import com.developer_rahul.meetmind_ai.feature.transcript.domain.model.TranscriptSegment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptScreen(
    meetingId: String,
    onBack: () -> Unit,
    viewModel: TranscriptViewModel = viewModel(factory = provideTranscriptViewModelFactory(meetingId.toLongOrNull() ?: -1L))
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSearchActive) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearch(it) },
                            placeholder = { Text("Search transcript...", color = TextDisabled) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = ElectricIndigo
                            ),
                            singleLine = true
                        )
                    } else {
                        Text("Meeting Transcript", style = Typography.titleLarge, color = TextPrimary) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (isSearchActive) { isSearchActive = false; viewModel.onSearch("") } else onBack() }) {
                        Icon(if (isSearchActive) Icons.Default.ArrowBack else Icons.Default.Close, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, null, tint = TextPrimary)
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
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricIndigo)
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error!!, color = RoseRed)
                        Button(onClick = { viewModel.loadTranscript() }) {
                            Text("Retry")
                        }
                    }
                }
            } else if (uiState.isEmpty) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transcript available for this meeting", color = TextSecondary)
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BackgroundSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isActive = uiState.transcript?.status != "COMPLETED"
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isActive) EmeraldGreen else TextDisabled, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (isActive) "TRANSCRIPTION IN PROGRESS" else "TRANSCRIPTION COMPLETED", 
                            style = Typography.labelLarge, 
                            color = if (isActive) EmeraldGreen else TextSecondary, 
                            letterSpacing = 1.sp
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    items(uiState.filteredSegments) { segment ->
                        TranscriptSegmentItem(segment)
                    }
                }
            }
        }
    }
}

@Composable
fun TranscriptSegmentItem(segment: TranscriptSegment) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.width(60.dp)) {
            Text(segment.time, style = Typography.labelSmall, color = TextDisabled)
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                segment.speaker, 
                style = Typography.titleSmall, 
                color = if (segment.speaker.contains("AI")) NeonCyan else ElectricIndigo,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                segment.text,
                style = Typography.bodyLarge,
                color = TextPrimary,
                lineHeight = 26.sp
            )
        }
    }
}
