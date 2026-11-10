package com.developer_rahul.meetmind_ai.feature.ai.presentation

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
    viewModel: MeetingIntelligenceViewModel = viewModel(
        key = "AiDashboardScreen_$meetingId",
        factory = provideMeetingIntelligenceViewModelFactory(meetingId.toLongOrNull() ?: -1L)
    )
) {
    val context = LocalContext.current
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
                        IconButton(onClick = { viewModel.generateIntelligence() }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Synthesize with AI", tint = NeonCyan)
                        }
                        if (summary != null) {
                            IconButton(
                                onClick = {
                                    val shareText = buildString {
                                        appendLine("🧠 MeetMind AI — Meeting Intelligence Summary")
                                        appendLine("==========================================")
                                        appendLine()
                                        appendLine("📝 Executive Brief:")
                                        appendLine(summary.summary)
                                        appendLine()
                                        if (summary.keyPoints.isNotEmpty()) {
                                            appendLine("💡 Key Discussion Points:")
                                            summary.keyPoints.forEach { appendLine("• $it") }
                                            appendLine()
                                        }
                                        if (summary.decisions.isNotEmpty()) {
                                            appendLine("⚖️ Strategic Decisions:")
                                            summary.decisions.forEach { appendLine("• $it") }
                                            appendLine()
                                        }
                                        if (summary.actionItems.isNotEmpty()) {
                                            appendLine("✅ Action Items:")
                                            summary.actionItems.forEach { item ->
                                                val statusEmoji = if (item.status == "COMPLETED") "✔️" else "⏳"
                                                val assignee = if (!item.assignedUser.isNullOrBlank()) "@${item.assignedUser}" else "Unassigned"
                                                appendLine("$statusEmoji ${item.description} ($assignee${if (!item.dueDate.isNullOrBlank()) ", Due: ${item.dueDate}" else ""})")
                                            }
                                        }
                                    }
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        putExtra(Intent.EXTRA_SUBJECT, "Meeting Intelligence Summary")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Meeting Summary"))
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary)
                            }
                        }
                        IconButton(onClick = { viewModel.loadIntelligence() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
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
                EmptyState(onGenerate = { viewModel.generateIntelligence() })
            } else {
                when (selectedTab) {
                    0 -> SummaryTab(summary)
                    1 -> KeyPointsTab(
                        summary = summary,
                        onAddDecision = { viewModel.addDecision(it) },
                        onDeleteDecision = { viewModel.deleteDecision(it) }
                    )
                    2 -> ActionItemsTab(summary, onStatusChange = { id, status -> viewModel.updateActionItemStatus(id, status) })
                    3 -> TopicsTab(
                        summary = summary,
                        onAddParkingQuestion = { q, askedBy, status -> viewModel.addParkingQuestion(q, askedBy, status) },
                        onUpdateParkingStatus = { idx, status -> viewModel.updateParkingQuestionStatus(idx, status) },
                        onDeleteParkingQuestion = { idx -> viewModel.deleteParkingQuestion(idx) }
                    )
                    4 -> AnalyticsTab(summary)
                }
            }
        }
    }
}

@Composable
fun SummaryTab(summary: SummaryDetailResponseDto) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Executive Brief Hero Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "Executive Summary")
                Surface(
                    color = ElectricIndigo.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = ElectricIndigo,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "< 60s Brief",
                            style = Typography.labelSmall,
                            color = ElectricIndigo,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            MeetMindCard(isAiCard = true) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "3–5 Sentence Executive Brief",
                                style = Typography.labelMedium,
                                color = NeonCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(summary.summary))
                                copied = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy brief",
                                tint = if (copied) EmeraldGreen else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        summary.summary,
                        style = Typography.bodyLarge,
                        color = TextPrimary,
                        lineHeight = 26.sp
                    )

                    if (copied) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "✓ Brief copied to clipboard",
                            style = Typography.labelSmall,
                            color = EmeraldGreen
                        )
                    }
                }
            }
        }

        // 2. Bullet-Point Highlights
        if (summary.keyPoints.isNotEmpty()) {
            item {
                SectionHeader(title = "Bullet-Point Takeaways")
                MeetMindCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        summary.keyPoints.forEach { point ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 7.dp)
                                        .size(8.dp)
                                        .background(NeonCyan, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = point,
                                    style = Typography.bodyMedium,
                                    color = TextPrimary,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Key Decisions
        if (summary.decisions.isNotEmpty()) {
            item {
                SectionHeader(title = "Decisions Agreed Upon")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    summary.decisions.forEach { DecisionItem(it) }
                }
            }
        }

        // 4. Status Card
        item {
            StatusCard(status = summary.status)
        }
    }
}

@Composable
fun KeyPointsTab(
    summary: SummaryDetailResponseDto,
    onAddDecision: (String) -> Unit = {},
    onDeleteDecision: (Int) -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedPoints by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newDecisionText by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text("Record Key Agreement", style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        "Officially record a consensus moment or agreement agreed during this meeting:",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newDecisionText,
                        onValueChange = { newDecisionText = it },
                        label = { Text("Decision statement") },
                        placeholder = { Text("e.g. Selected PostgreSQL 15 over MongoDB...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDecisionText.isNotBlank()) {
                            onAddDecision(newDecisionText)
                            newDecisionText = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Confirm Agreement")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Key Decisions & Agreement Tracking Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader(title = "Key Decisions & Agreements")
                    if (summary.decisions.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text(
                                "${summary.decisions.size} Confirmed",
                                style = Typography.labelSmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Record", color = EmeraldGreen, style = Typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(8.dp))

            if (summary.decisions.isEmpty()) {
                MeetMindCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.FactCheck, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No official agreements recorded yet", style = Typography.bodyMedium, color = TextSecondary)
                        Text("Agreements will appear automatically when consensus moments are detected.", style = Typography.bodySmall, color = TextDisabled)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    summary.decisions.forEachIndexed { idx, decision ->
                        DecisionItem(
                            decision = decision,
                            index = idx,
                            onDelete = { onDeleteDecision(idx) }
                        )
                    }
                }
            }
        }

        // 2. Key Takeaways Section
        if (summary.keyPoints.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "Key Takeaways")
                    TextButton(onClick = {
                        val allPoints = summary.keyPoints.joinToString("\n• ", prefix = "• ")
                        clipboardManager.setText(AnnotatedString(allPoints))
                        copiedPoints = true
                    }) {
                        Icon(
                            if (copiedPoints) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (copiedPoints) EmeraldGreen else ElectricIndigo
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (copiedPoints) "Copied" else "Copy All",
                            style = Typography.labelMedium,
                            color = if (copiedPoints) EmeraldGreen else ElectricIndigo
                        )
                    }
                }

                MeetMindCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        summary.keyPoints.forEach { point ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 7.dp)
                                        .size(8.dp)
                                        .background(NeonCyan, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = point,
                                    style = Typography.bodyMedium,
                                    color = TextPrimary,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionItemsTab(
    summary: SummaryDetailResponseDto,
    onStatusChange: (Long, String) -> Unit
) {
    if (summary.actionItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Task,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("No action items identified yet", style = Typography.bodyLarge, color = TextSecondary)
                Text("Tasks will appear automatically as participants speak.", style = Typography.bodySmall, color = TextDisabled)
            }
        }
    } else {
        var selectedFilter by remember { mutableStateOf("ALL") }

        val totalTasks = summary.actionItems.size
        val pendingCount = summary.actionItems.count { it.status == "PENDING" || it.status == "OPEN" }
        val inProgressCount = summary.actionItems.count { it.status == "IN_PROGRESS" }
        val completedCount = summary.actionItems.count { it.status == "COMPLETED" }
        val progress = if (totalTasks > 0) completedCount.toFloat() / totalTasks.toFloat() else 0f

        val filteredItems = when (selectedFilter) {
            "PENDING" -> summary.actionItems.filter { it.status == "PENDING" || it.status == "OPEN" }
            "IN_PROGRESS" -> summary.actionItems.filter { it.status == "IN_PROGRESS" }
            "COMPLETED" -> summary.actionItems.filter { it.status == "COMPLETED" }
            else -> summary.actionItems
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Task Progress Card
            item {
                MeetMindCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.TaskAlt,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Action Tracking Progress",
                                    style = Typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "$completedCount of $totalTasks Completed",
                                style = Typography.labelMedium,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EmeraldGreen,
                            trackColor = GlassWhite
                        )
                    }
                }
            }

            // 2. Filter Chips Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterBadge(
                        label = "All ($totalTasks)",
                        selected = selectedFilter == "ALL",
                        color = ElectricIndigo,
                        onClick = { selectedFilter = "ALL" }
                    )
                    FilterBadge(
                        label = "Pending ($pendingCount)",
                        selected = selectedFilter == "PENDING",
                        color = AmberGold,
                        onClick = { selectedFilter = "PENDING" }
                    )
                    FilterBadge(
                        label = "In Progress ($inProgressCount)",
                        selected = selectedFilter == "IN_PROGRESS",
                        color = NeonCyan,
                        onClick = { selectedFilter = "IN_PROGRESS" }
                    )
                    FilterBadge(
                        label = "Done ($completedCount)",
                        selected = selectedFilter == "COMPLETED",
                        color = EmeraldGreen,
                        onClick = { selectedFilter = "COMPLETED" }
                    )
                }
            }

            // 3. Action Items List
            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No tasks in this category",
                            style = Typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { action ->
                    ActionItemCard(
                        description = action.description,
                        assignee = action.assignedUser ?: "Unassigned",
                        dueDate = action.dueDate ?: "No deadline",
                        confidence = (action.confidence * 100).toInt(),
                        status = action.status,
                        onStatusSelect = { newStatus -> onStatusChange(action.id, newStatus) },
                        onToggleDone = { done ->
                            val newStatus = if (done) "COMPLETED" else "PENDING"
                            onStatusChange(action.id, newStatus)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TopicsTab(
    summary: SummaryDetailResponseDto,
    onAddParkingQuestion: (question: String, askedBy: String, status: String) -> Unit = { _, _, _ -> },
    onUpdateParkingStatus: (index: Int, newStatus: String) -> Unit = { _, _ -> },
    onDeleteParkingQuestion: (index: Int) -> Unit = {}
) {
    var jumpFeedback by remember { mutableStateOf<String?>(null) }
    var parkingFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newQuestionText by remember { mutableStateOf("") }
    var newAskedBy by remember { mutableStateOf("Team") }
    var newStatus by remember { mutableStateOf("UNANSWERED") }

    val chapters = summary.chapters
    val parkingItems = summary.parkingLotItems

    val unansweredCount = parkingItems.count { it.status.equals("UNANSWERED", ignoreCase = true) }
    val parkingLotCount = parkingItems.count { it.status.equals("PARKING_LOT", ignoreCase = true) }
    val resolvedCount = parkingItems.count { it.status.equals("RESOLVED", ignoreCase = true) }

    val filteredParkingItems = when (parkingFilter) {
        "UNANSWERED" -> parkingItems.filter { it.status.equals("UNANSWERED", ignoreCase = true) }
        "PARKING_LOT" -> parkingItems.filter { it.status.equals("PARKING_LOT", ignoreCase = true) }
        "RESOLVED" -> parkingItems.filter { it.status.equals("RESOLVED", ignoreCase = true) }
        else -> parkingItems
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    "Record Question / Parking Lot Item",
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Track an unresolved question or deferred discussion item to ensure follow-up after the meeting:",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = newQuestionText,
                        onValueChange = { newQuestionText = it },
                        label = { Text("Question or Topic") },
                        placeholder = { Text("e.g. Who has access to the production AWS billing console?") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAskedBy,
                        onValueChange = { newAskedBy = it },
                        label = { Text("Asked By / Owner") },
                        placeholder = { Text("e.g. Alex (DevOps)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Category / Classification:", style = Typography.labelSmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterBadge(
                            label = "Unanswered Question",
                            selected = newStatus == "UNANSWERED",
                            color = RoseRed,
                            onClick = { newStatus = "UNANSWERED" }
                        )
                        FilterBadge(
                            label = "Parking Lot",
                            selected = newStatus == "PARKING_LOT",
                            color = AmberGold,
                            onClick = { newStatus = "PARKING_LOT" }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newQuestionText.isNotBlank()) {
                            onAddParkingQuestion(newQuestionText, newAskedBy, newStatus)
                            newQuestionText = ""
                            newAskedBy = "Team"
                            newStatus = "UNANSWERED"
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (newStatus == "PARKING_LOT") AmberGold else RoseRed
                    )
                ) {
                    Text("Add Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Feedback banner when timestamp is tapped
        if (jumpFeedback != null) {
            item {
                Surface(
                    color = NeonCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = jumpFeedback ?: "",
                                style = Typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonCyan
                            )
                        }
                        IconButton(
                            onClick = { jumpFeedback = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // ==========================================
        // 1. Topic Chapterization & Smart Timestamps
        // ==========================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionHeader(title = "Agenda Chapters & Smart Timestamps")
                    Text(
                        "Click any chapter or timestamp to jump directly to that moment",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Surface(
                    color = ElectricIndigo.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(100.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${chapters.ifEmpty { summary.topics }.size} Chapters",
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ElectricIndigo,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (chapters.isNotEmpty()) {
            items(chapters.size) { index ->
                val chapter = chapters[index]
                ChapterCard(
                    chapterIndex = index,
                    title = chapter.title,
                    timeRange = chapter.timeRange,
                    startMs = chapter.startMs,
                    endMs = chapter.endMs,
                    summary = chapter.summary,
                    keywords = chapter.keywords,
                    onSeek = { ms, range ->
                        val seconds = ms / 1000
                        jumpFeedback = "Jumping to $range ($seconds seconds) in audio recording & transcript..."
                    }
                )
            }
        } else if (summary.topics.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    summary.topics.forEachIndexed { idx, topic ->
                        MeetMindCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        jumpFeedback = "Jumping to Chapter #${idx + 1}: $topic"
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = ElectricIndigo,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = topic,
                                        style = Typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                                Icon(
                                    Icons.Default.ArrowOutward,
                                    contentDescription = "Seek",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                MeetMindCard {
                    Text(
                        "No agenda chapters detected for this meeting.",
                        style = Typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // ==========================================
        // 2. Unanswered Questions & Parking Lot Items
        // ==========================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionHeader(title = "Unanswered Questions & Parking Lot")
                    Text(
                        "Critical blockers and deferred discussion topics",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Item", style = Typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Category Filter Badges
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterBadge(
                    label = "All (${parkingItems.size})",
                    selected = parkingFilter == "ALL",
                    color = ElectricIndigo,
                    onClick = { parkingFilter = "ALL" }
                )
                FilterBadge(
                    label = "Unanswered ($unansweredCount)",
                    selected = parkingFilter == "UNANSWERED",
                    color = RoseRed,
                    onClick = { parkingFilter = "UNANSWERED" }
                )
                FilterBadge(
                    label = "Parking Lot ($parkingLotCount)",
                    selected = parkingFilter == "PARKING_LOT",
                    color = AmberGold,
                    onClick = { parkingFilter = "PARKING_LOT" }
                )
                FilterBadge(
                    label = "Resolved ($resolvedCount)",
                    selected = parkingFilter == "RESOLVED",
                    color = EmeraldGreen,
                    onClick = { parkingFilter = "RESOLVED" }
                )
            }
        }

        if (filteredParkingItems.isNotEmpty()) {
            items(filteredParkingItems.size) { index ->
                val item = filteredParkingItems[index]
                val originalIndex = parkingItems.indexOf(item).let { if (it == -1) index else it }
                ParkingQuestionCard(
                    question = item.question,
                    askedBy = item.askedBy,
                    timeFormatted = item.timeFormatted,
                    status = item.status,
                    notes = item.notes,
                    onStatusSelect = { newStatus -> onUpdateParkingStatus(originalIndex, newStatus) },
                    onDelete = { onDeleteParkingQuestion(originalIndex) }
                )
            }
        } else if (parkingItems.isEmpty() && summary.questions.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Extracted Inquiries from Discussion:",
                        style = Typography.labelMedium,
                        color = TextSecondary
                    )
                    summary.questions.forEachIndexed { idx, q ->
                        MeetMindCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        tint = RoseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "\"$q\"",
                                        style = Typography.bodyMedium,
                                        fontStyle = FontStyle.Italic,
                                        color = TextPrimary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onAddParkingQuestion(q, "Meeting", "UNANSWERED")
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AddCircleOutline,
                                        contentDescription = "Track Question",
                                        tint = ElectricIndigo,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                MeetMindCard {
                    Text(
                        "No items found in this parking lot category.",
                        style = Typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterCard(
    chapterIndex: Int,
    title: String,
    timeRange: String,
    startMs: Long,
    endMs: Long,
    summary: String,
    keywords: List<String>,
    onSeek: (startMs: Long, range: String) -> Unit
) {
    MeetMindCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSeek(startMs, timeRange) }
        ) {
            // Header Row: Timestamp Chip, Duration, and Seek Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ElectricIndigo.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Jump to timestamp",
                            tint = ElectricIndigo,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (timeRange.isNotBlank()) timeRange else "00:00 - 05:00",
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricIndigo
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (endMs > startMs) {
                        val durationSec = (endMs - startMs) / 1000
                        Text(
                            text = if (durationSec >= 60) "${durationSec / 60}m ${durationSec % 60}s" else "${durationSec}s",
                            style = Typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(
                        Icons.Default.ArrowOutward,
                        contentDescription = "Jump",
                        tint = ElectricIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chapter Title
            Text(
                text = "${chapterIndex + 1}. $title",
                style = Typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = summary,
                    style = Typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }

            // Keyword Tags
            if (keywords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    keywords.forEach { keyword ->
                        Surface(
                            color = NeonCyan.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "#$keyword",
                                style = Typography.labelSmall,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParkingQuestionCard(
    question: String,
    askedBy: String,
    timeFormatted: String,
    status: String,
    notes: String,
    onStatusSelect: (String) -> Unit,
    onDelete: () -> Unit
) {
    val isResolved = status.equals("RESOLVED", ignoreCase = true)
    val isParking = status.equals("PARKING_LOT", ignoreCase = true)
    val statusColor = when {
        isResolved -> EmeraldGreen
        isParking -> AmberGold
        else -> RoseRed
    }

    Surface(
        color = statusColor.copy(alpha = 0.06f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Status Badge, Timestamp, Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            isResolved -> Icons.Default.CheckCircle
                            isParking -> Icons.Default.Schedule
                            else -> Icons.Default.HelpOutline
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            isResolved -> "RESOLVED"
                            isParking -> "PARKING LOT"
                            else -> "UNANSWERED"
                        },
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        letterSpacing = 0.8.sp
                    )
                    if (timeFormatted.isNotBlank() && timeFormatted != "00:00") {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "@ $timeFormatted",
                            style = Typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete item",
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Question Text
            Text(
                text = "\"$question\"",
                style = Typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isResolved) TextDisabled else TextPrimary,
                textDecoration = if (isResolved) TextDecoration.LineThrough else null,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(8.dp))

            // Sub-row: Asked By & Notes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Asked by: $askedBy",
                        style = Typography.labelSmall,
                        color = TextSecondary
                    )
                }

                // Status Selector Quick Chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusPill(
                        text = "Unanswered",
                        isSelected = !isParking && !isResolved,
                        activeColor = RoseRed,
                        onClick = { onStatusSelect("UNANSWERED") }
                    )
                    StatusPill(
                        text = "Parking",
                        isSelected = isParking,
                        activeColor = AmberGold,
                        onClick = { onStatusSelect("PARKING_LOT") }
                    )
                    StatusPill(
                        text = "Resolved",
                        isSelected = isResolved,
                        activeColor = EmeraldGreen,
                        onClick = { onStatusSelect("RESOLVED") }
                    )
                }
            }

            if (notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = GlassWhite,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Note: $notes",
                        style = Typography.bodySmall,
                        color = TextSecondary,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
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
fun EmptyState(onGenerate: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        MeetMindCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = ElectricIndigo.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricIndigo,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "No Intelligence Report Yet",
                    style = Typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Synthesize executive brief, action items, decisions, and analytics from your meeting speech transcript.",
                    style = Typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                MeetMindButton(
                    text = "Synthesize with AI ✨",
                    onClick = onGenerate,
                    isAiAction = true
                )
            }
        }
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
fun DecisionItem(
    decision: String,
    index: Int = 0,
    onDelete: (() -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Surface(
        color = EmeraldGreen.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Agreed",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "AGREEMENT #${index + 1}",
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(decision))
                            copied = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy agreement",
                            tint = if (copied) EmeraldGreen else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete agreement",
                                tint = TextSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = decision,
                style = Typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun FilterBadge(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) color.copy(alpha = 0.2f) else GlassWhite,
        shape = RoundedCornerShape(100.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) color else BorderColor
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = Typography.labelSmall,
            color = if (selected) color else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ActionItemCard(
    description: String,
    assignee: String,
    dueDate: String,
    confidence: Int,
    status: String,
    onStatusSelect: (String) -> Unit = {},
    onToggleDone: (Boolean) -> Unit = {}
) {
    val isCompleted = status == "COMPLETED"
    val isInProgress = status == "IN_PROGRESS"

    MeetMindCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Confidence & Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(
                    text = "$confidence% Confidence",
                    color = if (confidence >= 80) EmeraldGreen else AmberGold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = null,
                        tint = if (dueDate != "No deadline") NeonCyan else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        dueDate,
                        style = Typography.labelSmall,
                        color = if (dueDate != "No deadline") NeonCyan else TextSecondary,
                        fontWeight = if (dueDate != "No deadline") FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Row: Checkbox & Task Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = onToggleDone,
                    colors = CheckboxDefaults.colors(
                        checkedColor = EmeraldGreen,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = description,
                    style = Typography.titleMedium,
                    color = if (isCompleted) TextDisabled else TextPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Row: Assignee Chip & Status Badge with Status Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Assignee
                Surface(
                    color = GlassWhite,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = ElectricIndigo,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            assignee,
                            style = Typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Interactive Status Selector Chips (PENDING, IN_PROGRESS, COMPLETED)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        text = "Pending",
                        isSelected = status == "PENDING" || status == "OPEN",
                        activeColor = AmberGold,
                        onClick = { onStatusSelect("PENDING") }
                    )
                    StatusPill(
                        text = "In Progress",
                        isSelected = isInProgress,
                        activeColor = NeonCyan,
                        onClick = { onStatusSelect("IN_PROGRESS") }
                    )
                    StatusPill(
                        text = "Done",
                        isSelected = isCompleted,
                        activeColor = EmeraldGreen,
                        onClick = { onStatusSelect("COMPLETED") }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(50.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) activeColor else BorderColor.copy(alpha = 0.5f)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            style = Typography.labelSmall,
            color = if (isSelected) activeColor else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            fontSize = 10.sp
        )
    }
}
