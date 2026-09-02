package com.developer_rahul.meetmind_ai.feature.representative.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
fun AiRepDetailsScreen(
    onBack: () -> Unit,
    onViewReport: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent Details", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                MeetMindCard(isAiCard = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = EmeraldGreen.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, null, tint = EmeraldGreen)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Product Strategy Sync", style = Typography.titleMedium, color = TextPrimary)
                            Text("Status: Mission Completed", style = Typography.bodySmall, color = EmeraldGreen)
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Configuration")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailRow(icon = Icons.Default.ChatBubble, label = "Intro Message", text = "Hello everyone, I am Rahul's AI Representative...")
                    DetailRow(icon = Icons.Default.Tag, label = "Monitored Topics", text = "WebRTC, Security, Q4 Budget")
                    DetailRow(icon = Icons.Default.Videocam, label = "Identity Video", text = "Verified Approval Video (0:15)")
                }
            }

            item {
                SectionHeader(title = "Actions")
                MeetMindButton(
                    text = "View Intelligence Report",
                    onClick = onViewReport,
                    isAiAction = true
                )
            }
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = Typography.labelSmall, color = TextSecondary)
            Text(text, style = Typography.bodyMedium, color = TextPrimary)
        }
    }
}
