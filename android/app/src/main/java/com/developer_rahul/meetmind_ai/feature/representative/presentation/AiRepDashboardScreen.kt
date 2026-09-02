package com.developer_rahul.meetmind_ai.feature.representative.presentation

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.core.ui.provideAiRepDashboardViewModelFactory
import com.developer_rahul.meetmind_ai.feature.representative.domain.model.AiRepresentative
import java.util.ArrayList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRepDashboardScreen(
    onBack: () -> Unit,
    onConfigureRep: (Long) -> Unit,
    onViewReport: (Long) -> Unit,
    viewModel: AiRepDashboardViewModel = viewModel(factory = provideAiRepDashboardViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Upcoming", "History")

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(BackgroundSurface)) {
                TopAppBar(
                    title = { Text("AI Representative", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
                )
                
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BackgroundSurface,
                    contentColor = NeonCyan,
                    divider = { HorizontalDivider(color = BorderColor) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    title, 
                                    style = if (selectedTab == index) Typography.labelLarge else Typography.bodyMedium,
                                    color = if (selectedTab == index) NeonCyan else TextSecondary
                                ) 
                            }
                        )
                    }
                }
            }
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = NeonCyan)
            } else if (uiState.error != null) {
                Text(uiState.error!!, color = Color.Red, modifier = Modifier.align(Alignment.Center))
            } else {
                val reps: List<AiRepresentative> = uiState.representatives
                val filteredReps = ArrayList<AiRepresentative>()
                for (rep in reps) {
                    if (selectedTab == 0) {
                        if (rep.status == "SCHEDULED" || rep.status == "CREATED" || rep.status == "ACTIVE") {
                            filteredReps.add(rep)
                        }
                    } else {
                        if (rep.status == "COMPLETED" || rep.status == "FAILED" || rep.status == "CANCELLED") {
                            filteredReps.add(rep)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 24.dp)
                ) {
                    if (filteredReps.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No ${tabs[selectedTab].lowercase()} agents",
                                description = "Enable your AI representative in meeting details to automate your attendance.",
                                icon = Icons.Default.SmartToy
                            )
                        }
                    } else {
                        items(filteredReps) { repItem ->
                            val meeting = uiState.meetings[repItem.meetingId]
                            AiRepItemCard(
                                rep = repItem,
                                meetingTitle = meeting?.title ?: "Meeting #${repItem.meetingId}",
                                dateTime = meeting?.scheduledAt ?: repItem.createdAt,
                                onClick = {
                                    onViewReport(repItem.meetingId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiRepItemCard(
    rep: AiRepresentative,
    meetingTitle: String,
    dateTime: String,
    onClick: () -> Unit
) {
    MeetMindCard(onClick = onClick, isAiCard = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(meetingTitle, style = Typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(dateTime, style = Typography.bodySmall, color = TextSecondary)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(
                        text = rep.status,
                        color = when (rep.status) {
                            "ACTIVE" -> NeonCyan
                            "COMPLETED" -> Color.Green
                            "FAILED" -> Color.Red
                            "CANCELLED" -> Color.Gray
                            else -> AmberGold
                        }
                    )
                }
            }
            
            IconButton(onClick = onClick) {
                Icon(Icons.Default.ChevronRight, null, tint = TextDisabled)
            }
        }
    }
}

@Composable
fun EmptyState(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = GlassWhite
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = TextDisabled, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = Typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(description, style = Typography.bodyMedium, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
