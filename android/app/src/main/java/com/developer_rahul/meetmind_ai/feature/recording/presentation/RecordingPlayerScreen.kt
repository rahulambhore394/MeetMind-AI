package com.developer_rahul.meetmind_ai.feature.recording.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun RecordingPlayerScreen(
    recordingId: String,
    onBack: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.35f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playback", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Download, null, tint = TextPrimary)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Share, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface,
        bottomBar = {
            Box(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                MeetMindButton(
                    text = "View AI Summary",
                    onClick = { },
                    isAiAction = true
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Modern Video Player Surface
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.77f),
                shape = MaterialTheme.shapes.large,
                color = Color.Black,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite),
                shadowElevation = 12.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Play/Pause Overlay Animation feel
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier.size(80.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Architecture Review Sync",
                    style = Typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black
                )
                Text("Oct 12, 2023 • 45 minutes", style = Typography.bodyLarge, color = TextSecondary)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Refined Slider
            Slider(
                value = progress,
                onValueChange = { progress = it },
                colors = SliderDefaults.colors(
                    thumbColor = ElectricIndigo,
                    activeTrackColor = ElectricIndigo,
                    inactiveTrackColor = GlassWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("15:22", style = Typography.labelMedium, color = TextSecondary)
                Text("45:20", style = Typography.labelMedium, color = TextSecondary)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Replay10, null, tint = TextPrimary, modifier = Modifier.size(32.dp))
                }
                
                Surface(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = ElectricIndigo,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }
                }
                
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Forward30, null, tint = TextPrimary, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}
