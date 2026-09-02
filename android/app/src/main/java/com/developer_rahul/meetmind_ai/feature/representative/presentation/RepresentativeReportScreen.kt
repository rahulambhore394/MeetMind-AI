package com.developer_rahul.meetmind_ai.feature.representative.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.core.ui.provideRepresentativeReportViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepresentativeReportScreen(
    meetingId: Long,
    representativeId: Long,
    onBack: () -> Unit,
    viewModel: RepresentativeReportViewModel = viewModel(factory = provideRepresentativeReportViewModelFactory(meetingId, representativeId))
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meeting Report", style = Typography.titleLarge, color = TextPrimary) },
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
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = Color.Red)
            }
        } else if (uiState.report != null) {
            val report = uiState.report!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Executive Summary", style = Typography.headlineSmall, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(report.summary, style = Typography.bodyLarge, color = TextSecondary)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                ReportSection(title = "Important Discussions", items = report.importantDiscussions)
                ReportSection(title = "Decisions", items = report.decisions)
                ReportSection(title = "Action Items", items = report.actionItems)
                ReportSection(title = "Monitored Questions & Answers", items = report.ownerRelevantQuestions)
                ReportSection(title = "Monitored Topics Found", items = report.monitoredTopicsFound)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text("Attendance Details", style = Typography.headlineSmall, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                MeetMindCard {
                    report.attendanceTimeline.forEach { (key, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(key, style = Typography.labelMedium, color = TextSecondary)
                            Text(value, style = Typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun ReportSection(title: String, items: List<String>) {
    if (items.isEmpty()) return
    
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(title, style = Typography.headlineSmall, color = TextPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        items.forEach { item ->
            MeetMindCard(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(item, style = Typography.bodyMedium, color = TextPrimary)
            }
        }
    }
}
