package com.developer_rahul.meetmind_ai.feature.ai.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.provideMeetingIntelligenceViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.intelligence.data.remote.dto.SummaryDetailResponseDto
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDashboardScreen(
    meetingId: String,
    onBack: () -> Unit,
    viewModel: MeetingIntelligenceViewModel = viewModel(factory = provideMeetingIntelligenceViewModelFactory(meetingId.toLongOrNull() ?: -1L))
) {
    val uiState by viewModel.uiState.collectAsState()
    val summary = uiState.summary
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Summary", "Points", "Tasks", "Topics", "Stats")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Text(
                            "Meeting Intelligence", 
                            style = Typography.titleLarge, 
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.loadIntelligence() }) {
                            Icon(Icons.Default.Refresh, null, tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
                )
                
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BackgroundSurface,
                    contentColor = ElectricIndigo,
                    edgePadding = 16.dp,
                    divider = { HorizontalDivider(color = BorderColor) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, style = Typography.labelLarge) }
                        )
                    }
                }
            }
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricIndigo)
                }
            } else if (uiState.error != null) {
                ErrorState(message = uiState.error!!, onRetry = { viewModel.loadIntelligence() })
            } else if (summary == null) {
                EmptyState()
            } else {
                when (selectedTab) {
                    0 -> SummaryTab(summary)
                    1 -> KeyPointsTab(summary)
                    2 -> ActionItemsTab(summary, onStatusChange = { id, done -> viewModel.updateActionItemStatus(id, done) })
                    3 -> TopicsTab(summary)
                    4 -> AnalyticsTab(summary)
                }
            }
        }
    }
}

@Composable
fun SummaryTab(summary: SummaryDetailResponseDto) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            SectionHeader(title = "Executive Summary")
            MeetMindCard(isAiCard = true) {
                Text(
                    summary.summary,
                    style = Typography.bodyLarge,
                    color = TextPrimary,
                    lineHeight = 26.sp
                )
            }
        }
        
        item {
            StatusCard(status = summary.status)
        }
    }
}

@Composable
fun KeyPointsTab(summary: SummaryDetailResponseDto) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (summary.decisions.isNotEmpty()) {
            item {
                SectionHeader(title = "Decisions")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    summary.decisions.forEach { DecisionItem(it) }
                }
            }
        }

        if (summary.keyPoints.isNotEmpty()) {
            item {
                SectionHeader(title = "Key Takeaways")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    summary.keyPoints.forEach { point ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(modifier = Modifier.padding(top = 8.dp).size(6.dp).background(NeonCyan, CircleShape))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(point, style = Typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionItemsTab(summary: SummaryDetailResponseDto, onStatusChange: (Long, Boolean) -> Unit) {
    if (summary.actionItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No action items identified", color = TextSecondary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(summary.actionItems) { action ->
                ActionItemCard(
                    description = action.description,
                    assignee = action.assignedUser ?: "Unassigned",
                    dueDate = action.dueDate ?: "No deadline",
                    confidence = (action.confidence * 100).toInt(),
                    status = action.status,
                    onStatusToggle = { onStatusChange(action.id, it) }
                )
            }
        }
    }
}

@Composable
fun TopicsTab(summary: SummaryDetailResponseDto) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            SectionHeader(title = "Discussion Topics")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                summary.topics.forEach { StatusChip(text = it, color = ElectricIndigo) }
            }
        }

        if (summary.questions.isNotEmpty()) {
            item {
                SectionHeader(title = "Questions Raised")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    summary.questions.forEach { question ->
                        MeetMindCard {
                            Text("\"$question\"", style = Typography.bodyLarge, color = TextPrimary, fontStyle = FontStyle.Italic)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsTab(summary: SummaryDetailResponseDto) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            SectionHeader(title = "Transcript Statistics")
            MeetMindCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val wordCount = summary.analysisMetrics["wordCount"]?.toString() ?: "N/A"
                    val speakerCount = summary.analysisMetrics["speakerCount"]?.toString() ?: "N/A"
                    val sentiment = summary.analysisMetrics["sentiment"]?.let { 
                        if (it is JsonPrimitive) it.content else it.toString()
                    } ?: "N/A"
                    
                    MetricRow(label = "Total Words", value = wordCount)
                    MetricRow(label = "Speakers", value = speakerCount)
                    MetricRow(label = "Sentiment Tone", value = sentiment)
                }
            }
        }
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = Typography.bodyMedium, color = TextSecondary)
        Text(value, style = Typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusCard(status: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GlassWhite,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("AI analysis state: $status", style = Typography.labelLarge, color = TextPrimary)
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = RoseRed, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No intelligence report available", color = TextSecondary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
    }
}

@Composable
fun DecisionItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EmeraldGreen.copy(alpha = 0.05f), MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = Typography.bodyLarge, color = TextPrimary)
    }
}

@Composable
fun ActionItemCard(
    description: String,
    assignee: String,
    dueDate: String,
    confidence: Int,
    status: String,
    onStatusToggle: (Boolean) -> Unit = {}
) {
    val isCompleted = status == "COMPLETED"
    
    MeetMindCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(
                    text = "$confidence% Confidence",
                    color = if (confidence > 80) EmeraldGreen else AmberGold
                )
                Text(dueDate, style = Typography.labelSmall, color = TextSecondary)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = onStatusToggle,
                    colors = CheckboxDefaults.colors(checkedColor = ElectricIndigo)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    description, 
                    style = Typography.titleMedium, 
                    color = if (isCompleted) TextDisabled else TextPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = GlassWhite,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        assignee,
                        style = Typography.labelSmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                StatusChip(text = status, color = if (isCompleted) EmeraldGreen else NeonCyan)
            }
        }
    }
}
