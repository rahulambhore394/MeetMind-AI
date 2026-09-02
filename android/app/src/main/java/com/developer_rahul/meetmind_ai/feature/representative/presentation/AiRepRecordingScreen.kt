package com.developer_rahul.meetmind_ai.feature.representative.presentation

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
fun AiRepRecordingScreen(
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                seconds++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recording Approval", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Immersive Recording View
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f),
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.Black,
                border = androidx.compose.foundation.BorderStroke(2.dp, if (isRecording) RoseRed else GlassWhite)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Mock camera feed
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(140.dp), tint = Color.White.copy(alpha = 0.05f))
                    
                    if (isRecording) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 24.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(RoseRed, CircleShape).pulse())
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                String.format("%02d:%02d", seconds / 60, seconds % 60),
                                color = Color.White,
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    
                    if (!isRecording && seconds > 0) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = EmeraldGreen
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (!isRecording && seconds == 0) {
                Text(
                    "Please state: \"I [Your Name] approve this AI representative to attend meetings on my behalf.\"",
                    style = Typography.bodyLarge,
                    color = TextPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                IconButton(
                    onClick = { isRecording = true },
                    modifier = Modifier.size(88.dp).background(RoseRed, CircleShape)
                ) {
                    Icon(Icons.Default.FiberManualRecord, null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
            } else if (isRecording) {
                IconButton(
                    onClick = { isRecording = false },
                    modifier = Modifier.size(88.dp).background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.Stop, null, tint = RoseRed, modifier = Modifier.size(40.dp))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { seconds = 0 },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassWhite)
                    ) {
                        Text("Retake", color = TextPrimary, style = Typography.labelLarge)
                    }
                    MeetMindButton(
                        text = "Finish",
                        onClick = onFinished,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
