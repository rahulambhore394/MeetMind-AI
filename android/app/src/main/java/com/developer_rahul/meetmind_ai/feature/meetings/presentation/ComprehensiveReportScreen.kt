package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.MeetMindButton
import com.developer_rahul.meetmind_ai.core.ui.components.MeetMindCard
import com.developer_rahul.meetmind_ai.core.ui.components.StatusChip
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.ActionItemDto
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.ComprehensiveReportDto
import kotlinx.coroutines.launch

@Composable
fun ComprehensiveReportScreen(
    meetingId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val meetingRepository = remember { com.developer_rahul.meetmind_ai.MeetMindApplication.instance.container.meetingRepository }
    val intelligenceRepository = remember { com.developer_rahul.meetmind_ai.MeetMindApplication.instance.container.intelligenceRepository }
    val coroutineScope = rememberCoroutineScope()
    var report by remember { mutableStateOf<ComprehensiveReportDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadReport() {
        val mid = meetingId.toLongOrNull()
        if (mid != null) {
            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    when (val result = meetingRepository.getComprehensiveReport(mid)) {
                        is com.developer_rahul.meetmind_ai.core.network.model.NetworkResult.Success -> {
                            report = result.data
                        }
                        is com.developer_rahul.meetmind_ai.core.network.model.NetworkResult.Error -> {
                            errorMessage = result.error.message
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Failed to load comprehensive report"
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
            errorMessage = "Invalid meeting ID"
        }
    }

    LaunchedEffect(meetingId) {
        loadReport()
    }

    ComprehensiveReportScreen(
        onBack = onBack,
        report = report,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onRetry = { loadReport() },
        onUpdateTaskStatus = { taskId, newStatus ->
            val mid = meetingId.toLongOrNull()
            if (mid != null) {
                report = report?.let { r ->
                    val updatedAll = r.allWorkAssignments.map { if (it.id == taskId) it.copy(status = newStatus) else it }
                    val updatedMine = r.myAssignedTasks.map { if (it.id == taskId) it.copy(status = newStatus) else it }
                    val updatedBreakdown = r.userTaskBreakdown.mapValues { (_, list) ->
                        list.map { if (it.id == taskId) it.copy(status = newStatus) else it }
                    }
                    r.copy(allWorkAssignments = updatedAll, myAssignedTasks = updatedMine, userTaskBreakdown = updatedBreakdown)
                }
                coroutineScope.launch {
                    intelligenceRepository.updateActionItemStatus(mid, taskId, newStatus)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprehensiveReportScreen(
    onBack: () -> Unit,
    report: ComprehensiveReportDto?,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    onUpdateTaskStatus: (Long, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(report?.title ?: "Meeting Report", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Comprehensive Intelligence Summary", style = Typography.labelMedium, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (report != null) {
                        IconButton(
                            onClick = {
                                val shareText = buildString {
                                    appendLine("📊 Comprehensive Meeting Report: ${report.title}")
                                    appendLine("Meeting Code: ${report.meetingCode ?: ""}")
                                    appendLine("Host: ${report.hostName ?: "Host"}")
                                    appendLine()
                                    appendLine("📝 Executive Summary:")
                                    appendLine(report.executiveSummary ?: "Meeting concluded.")
                                    appendLine()
                                    if (report.keyPoints.isNotEmpty()) {
                                        appendLine("💡 Key Discussion Points:")
                                        report.keyPoints.forEach { appendLine("• $it") }
                                        appendLine()
                                    }
                                    if (report.decisions.isNotEmpty()) {
                                        appendLine("⚖️ Decisions Made:")
                                        report.decisions.forEach { appendLine("• $it") }
                                        appendLine()
                                    }
                                    if (report.allWorkAssignments.isNotEmpty()) {
                                        appendLine("✅ Work Assignments & Tasks:")
                                        report.allWorkAssignments.forEach { task ->
                                            val statusEmoji = if (task.status == "COMPLETED") "✔️" else "⏳"
                                            val assignee = if (!task.assignedUser.isNull_or_blank()) "@${task.assignedUser}" else "Unassigned"
                                            appendLine("$statusEmoji ${task.description} ($assignee${if (!task.dueDate.isNull_or_blank()) ", Due: ${task.dueDate}" else ""})")
                                        }
                                    }
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    putExtra(Intent.EXTRA_SUBJECT, "Meeting Report: ${report.title}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Meeting Report"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Report", tint = TextPrimary)
                        }
                    }
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ElectricIndigo)
            }
        } else if (report == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                MeetMindCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = ElectricIndigo.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Report Not Available Yet",
                            style = Typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            errorMessage ?: "The meeting intelligence report is being generated or no spoken utterances were recorded.",
                            style = Typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        MeetMindButton(
                            text = "Retry Report",
                            onClick = onRetry
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // Header Info Card
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (report.meetingType == "AI_REPRESENTATIVE") PurpleViolet.copy(alpha = 0.15f) else ElectricIndigo.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (report.meetingType == "AI_REPRESENTATIVE") Icons.Default.SmartToy else Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = if (report.meetingType == "AI_REPRESENTATIVE") PurpleViolet else ElectricIndigo,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (report.meetingType == "AI_REPRESENTATIVE") "AI Representative Attended" else "Standard Live Meeting",
                                            style = Typography.labelMedium,
                                            color = if (report.meetingType == "AI_REPRESENTATIVE") PurpleViolet else ElectricIndigo,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = report.meetingCode ?: "mm-${report.meetingId}",
                                    style = Typography.titleMedium,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(report.title, style = Typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            if (!report.description.isNull_or_blank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(report.description ?: "", style = Typography.bodyMedium, color = TextSecondary)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Host: ${report.hostName ?: "Host"}", style = Typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }

                // Executive Summary Section
                item {
                    SectionCardHeader(title = "Executive Summary", icon = Icons.Default.AutoAwesome, color = NeonCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Text(
                            text = report.executiveSummary ?: "Meeting concluded successfully.",
                            style = Typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Dedicated Section: My Assigned Tasks
                if (report.myAssignedTasks.isNotEmpty()) {
                    item {
                        SectionCardHeader(title = "My Assigned Tasks", icon = Icons.Default.AssignmentInd, color = ElectricIndigo)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            report.myAssignedTasks.forEach { task ->
                                TaskItemCard(
                                    task = task,
                                    isMyTask = true,
                                    onStatusToggle = { onUpdateTaskStatus(task.id, if (task.status == "COMPLETED") "OPEN" else "COMPLETED") }
                                )
                            }
                        }
                    }
                }

                // Per-User Assigned Work Breakdown (Whoom What Section)
                if (report.userTaskBreakdown.isNotEmpty()) {
                    item {
                        SectionCardHeader(title = "Work Assignments by Participant", icon = Icons.Default.PeopleOutline, color = EmeraldGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            report.userTaskBreakdown.forEach { (userName, tasks) ->
                                UserTaskGroupCard(userName = userName, tasks = tasks)
                            }
                        }
                    }
                }

                // Key Discussion Points
                if (report.keyPoints.isNotEmpty()) {
                    item {
                        SectionCardHeader(title = "Key Discussion Points", icon = Icons.Default.CheckCircleOutline, color = ElectricIndigo)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                report.keyPoints.forEach { point ->
                                    BulletPointRow(text = point, color = ElectricIndigo)
                                }
                            }
                        }
                    }
                }

                // Key Decisions
                if (report.decisions.isNotEmpty()) {
                    item {
                        SectionCardHeader(title = "Decisions Made", icon = Icons.Default.Gavel, color = AmberGold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                report.decisions.forEach { decision ->
                                    BulletPointRow(text = decision, color = AmberGold)
                                }
                            }
                        }
                    }
                }

                // AI Representative Section (if applicable)
                if (report.meetingType == "AI_REPRESENTATIVE") {
                    item {
                        SectionCardHeader(title = "AI Representative Insights", icon = Icons.Default.SmartToy, color = PurpleViolet)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PurpleViolet.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (report.ownerRelevantQuestions.isNotEmpty()) {
                                    Text("Questions Raised for Owner:", style = Typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    report.ownerRelevantQuestions.forEach { q ->
                                        BulletPointRow(text = q, color = PurpleViolet)
                                    }
                                }
                                if (report.monitoredTopicsFound.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Monitored Topics Analyzed:", style = Typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    report.monitoredTopicsFound.forEach { topic ->
                                        BulletPointRow(text = topic, color = NeonCyan)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionCardHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
fun BulletPointRow(text: String, color: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, style = Typography.bodyMedium, color = TextPrimary, lineHeight = 20.sp)
    }
}

@Composable
fun UserTaskGroupCard(userName: String, tasks: List<ActionItemDto>) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(userName.take(1).uppercase(), style = Typography.labelMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "$userName's Assigned Work",
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.weight(1f))
                Text("${tasks.size} Task(s)", style = Typography.labelSmall, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tasks.forEach { task ->
                    TaskItemCard(task = task, isMyTask = false)
                }
            }
        }
    }
}

@Composable
fun TaskItemCard(
    task: ActionItemDto,
    isMyTask: Boolean,
    onStatusToggle: () -> Unit = {}
) {
    val isCompleted = task.status == "COMPLETED"

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = BackgroundSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isMyTask) ElectricIndigo.copy(alpha = 0.4f) else BorderColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMyTask) {
                IconButton(onClick = onStatusToggle, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isCompleted) EmeraldGreen else TextDisabled
                    )
                }
                Spacer(Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.description,
                    style = Typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isCompleted) TextDisabled else TextPrimary
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!task.assignedUser.isNull_or_blank()) {
                        Text(
                            text = "Assignee: ${task.assignedUser}",
                            style = Typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!task.dueDate.isNull_or_blank()) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Due: ${task.dueDate}",
                            style = Typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            StatusChip(
                text = task.status,
                color = if (isCompleted) EmeraldGreen else AmberGold
            )
        }
    }
}

fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
