package com.developer_rahul.meetmind_ai.feature.transcript.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
                        Column {
                            Text("Meeting Transcript", style = Typography.titleLarge, color = TextPrimary) 
                            Text("Live Multilingual Translation", style = Typography.labelSmall, color = TextSecondary)
                        }
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
            // Preferred Language selector bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardSurface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "PREFERRED LANGUAGE:", 
                            style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), 
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.availableLanguages.forEach { lang ->
                            val isSelected = uiState.selectedLanguage == lang.code
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectLanguage(lang.code) },
                                label = { 
                                    Text(
                                        lang.displayName, 
                                        style = Typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricIndigo,
                                    selectedLabelColor = Color.White,
                                    containerColor = BackgroundSurface,
                                    labelColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }

            // Live status banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BackgroundSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isActive = uiState.transcript?.status != "COMPLETED"
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isActive) EmeraldGreen else TextDisabled, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (isActive) "LIVE TRANSCRIPTION IN PROGRESS" else "TRANSCRIPTION COMPLETED", 
                        style = Typography.labelLarge, 
                        color = if (isActive) EmeraldGreen else TextSecondary, 
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val currentLangName = viewModel.availableLanguages.find { it.code == uiState.selectedLanguage }?.displayName ?: uiState.selectedLanguage
                    Text(
                        currentLangName,
                        style = Typography.labelSmall,
                        color = ElectricIndigo,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricIndigo)
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error!!, color = RoseRed)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadTranscript() }) {
                            Text("Retry")
                        }
                    }
                }
            } else if (uiState.isEmpty) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Subtitles, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No transcript available in this language yet", color = TextSecondary)
                        Text("Speak in the live meeting to see live translation!", style = Typography.bodySmall, color = TextDisabled)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.filteredSegments) { segment ->
                        TranscriptSegmentCard(segment)
                    }
                }
            }
        }
    }
}

@Composable
fun TranscriptSegmentCard(segment: TranscriptSegment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    segment.speaker, 
                    style = Typography.titleSmall, 
                    color = if (segment.speaker.contains("AI", ignoreCase = true)) NeonCyan else ElectricIndigo,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = BackgroundSurface,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        segment.time, 
                        style = Typography.labelSmall, 
                        color = TextDisabled,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                segment.text,
                style = Typography.bodyLarge,
                color = TextPrimary,
                lineHeight = 24.sp
            )
        }
    }
}
