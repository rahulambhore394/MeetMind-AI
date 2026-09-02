package com.developer_rahul.meetmind_ai.feature.representative.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRepReportScreen(
    reportId: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Rep Report", style = Typography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Download, null, tint = TextPrimary)
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
                Column {
                    Text(
                        "Summary for Rahul", 
                        style = Typography.displayMedium.copy(fontSize = 26.sp), 
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Meeting: Product Sync (Oct 12, 2023)", 
                        style = Typography.bodyLarge, 
                        color = TextSecondary
                    )
                }
            }

            item {
                SectionHeader(title = "Attendance Timeline")
                MeetMindCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimelineItem("09:00 AM", "Joined as AUTOMATED_AGENT", NeonCyan)
                        TimelineItem("09:01 AM", "Broadcasted identity disclosure", TextSecondary)
                        TimelineItem("09:45 AM", "Left meeting after completion", EmeraldGreen)
                    }
                }
            }

            item {
                SectionHeader(title = "Monitored Topics Found")
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TopicReferenceItem(
                        topic = "ARCHITECTURE",
                        quote = "The team discussed moving to a micro-services architecture for the AI backend.",
                        time = "10:15 AM"
                    )
                    TopicReferenceItem(
                        topic = "BUDGET",
                        quote = "Rahul's proposed budget was reviewed and matches our current projections.",
                        time = "10:30 AM"
                    )
                }
            }

            item {
                SectionHeader(title = "Questions Directed to You")
                MeetMindCard(isAiCard = true) {
                    Column {
                        Text(
                            "\"Rahul, do you want us to prioritize iOS or Android for the first release?\"", 
                            color = TextPrimary, 
                            style = Typography.bodyLarge,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            lineHeight = 26.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(24.dp), shape = androidx.compose.foundation.shape.CircleShape, color = Color.DarkGray) {
                                Box(contentAlignment = Alignment.Center) { Text("A", style = Typography.labelSmall, color = Color.White) }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Alice Chen at 09:22 AM", color = TextSecondary, style = Typography.labelMedium)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun TimelineItem(time: String, event: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(time, style = Typography.labelMedium, color = TextSecondary, modifier = Modifier.width(70.dp))
        Box(modifier = Modifier.size(8.dp).background(color, androidx.compose.foundation.shape.CircleShape))
        Spacer(modifier = Modifier.width(16.dp))
        Text(event, style = Typography.bodyMedium, color = TextPrimary)
    }
}

@Composable
fun TopicReferenceItem(topic: String, quote: String, time: String) {
    MeetMindCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(text = topic, color = NeonCyan)
                Spacer(modifier = Modifier.weight(1f))
                Text(time, style = Typography.labelSmall, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "\"$quote\"", 
                style = Typography.bodyLarge, 
                color = TextPrimary,
                lineHeight = 24.sp
            )
        }
    }
}
