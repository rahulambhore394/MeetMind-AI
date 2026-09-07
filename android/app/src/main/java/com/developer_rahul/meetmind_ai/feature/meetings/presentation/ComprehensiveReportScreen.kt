package com.developer_rahul.meetmind_ai.feature.meetings.presentation

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.StatusChip
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.ActionItemDto
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.ComprehensiveReportDto
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.RealMeetingRepository
import kotlinx.coroutines.launch

@Composable
fun ComprehensiveReportScreen(
    meetingId: String,
    onBack: () -> Unit
) {
    val repository = remember { com.developer_rahul.meetmind_ai.MeetMindApplication.instance.container.meetingRepository }
    var report by remember { mutableStateOf<ComprehensiveReportDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(meetingId) {
        val mid = meetingId.toLongOrNull()
        if (mid != null) {
            try {
                when (val result = repository.getComprehensiveReport(mid)) {
                    is com.developer_rahul.meetmind_ai.core.network.model.NetworkResult.Success -> {
                        report = result.data
                    }
                    else -> {}
                }
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    ComprehensiveReportScreen(
        onBack = onBack,
        report = report,
        isLoading = isLoading
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprehensiveReportScreen(
    onBack: () -> Unit,
    report: ComprehensiveReportDto?,
    isLoading: Boolean = false,
    onUpdateTaskStatus: (Long, String) -> Unit = { _, _ -> }
) {
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        if (isLoading || report == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ElectricIndigo)
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
