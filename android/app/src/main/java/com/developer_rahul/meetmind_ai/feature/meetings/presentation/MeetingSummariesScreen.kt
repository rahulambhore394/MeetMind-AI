package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.developer_rahul.meetmind_ai.feature.meetings.data.remote.dto.MeetingReportSummaryDto
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.RealMeetingRepository
import kotlinx.coroutines.launch

@Composable
fun MeetingSummariesScreen(
    onBack: () -> Unit,
    onReportClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = remember { com.developer_rahul.meetmind_ai.MeetMindApplication.instance.container.meetingRepository }
    var reports by remember { mutableStateOf<List<MeetingReportSummaryDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchReports() {
        scope.launch {
            isLoading = true
            try {
                when (val result = repository.getAllMeetingReports()) {
                    is com.developer_rahul.meetmind_ai.core.network.model.NetworkResult.Success -> {
                        reports = result.data
                    }
                    else -> {}
                }
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchReports()
    }

    MeetingSummariesScreen(
        onBack = onBack,
        onReportClick = onReportClick,
        reports = reports,
        isLoading = isLoading,
        onRefresh = { fetchReports() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingSummariesScreen(
    onBack: () -> Unit,
    onReportClick: (String) -> Unit,
    reports: List<MeetingReportSummaryDto> = emptyList(),
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf(0) } // 0: All, 1: Normal, 2: AI Rep

    val filteredReports = remember(reports, selectedFilter) {
        when (selectedFilter) {
            1 -> reports.filter { !it.hasAiRepresentative && it.meetingType == "NORMAL" }
            2 -> reports.filter { it.hasAiRepresentative || it.meetingType == "AI_REPRESENTATIVE" }
            else -> reports
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Meeting Summaries", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Reports & Work Assignments", style = Typography.labelMedium, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ElectricIndigo)
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Filter Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilterTabChip(
                    title = "All (${reports.size})",
                    isSelected = selectedFilter == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedFilter = 0 }
                )
                FilterTabChip(
                    title = "Normal",
                    isSelected = selectedFilter == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedFilter = 1 }
                )
                FilterTabChip(
                    title = "AI Rep",
                    isSelected = selectedFilter == 2,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedFilter = 2 }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricIndigo)
                }
            } else if (filteredReports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = TextDisabled,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Meeting Summaries Found",
                            style = Typography.titleMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Conclude a meeting to automatically generate intelligence reports & task assignments.",
                            style = Typography.bodySmall,
                            color = TextDisabled,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(filteredReports) { report ->
                        MeetingSummaryCard(
                            report = report,
                            onClick = { onReportClick(report.meetingId.toString()) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterTabChip(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) ElectricIndigo else Color.Transparent,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = Typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextSecondary
            )
        }
    }
}

@Composable
fun MeetingSummaryCard(
    report: MeetingReportSummaryDto,
    onClick: () -> Unit
) {
    val accentColor = if (report.hasAiRepresentative) PurpleViolet else ElectricIndigo

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (report.hasAiRepresentative) Icons.Default.SmartToy else Icons.Default.Groups,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (report.hasAiRepresentative) "AI Rep Meeting" else "Normal Meeting",
                                style = Typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (report.meetingCode != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = report.meetingCode,
                            style = Typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                StatusChip(
                    text = report.status,
                    color = if (report.status == "ENDED") RoseRed else EmeraldGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = report.title,
                style = Typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = report.summarySnippet ?: "Meeting completed. Tap to view full intelligence report.",
                style = Typography.bodySmall,
                color = TextSecondary,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TaskAlt, null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${report.actionItemCount} Tasks",
                        style = Typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (report.myTaskCount > 0) {
                        Spacer(Modifier.width(12.dp))
                        Surface(
                            shape = CircleShape,
                            color = ElectricIndigo.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${report.myTaskCount} Assigned To You",
                                style = Typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
