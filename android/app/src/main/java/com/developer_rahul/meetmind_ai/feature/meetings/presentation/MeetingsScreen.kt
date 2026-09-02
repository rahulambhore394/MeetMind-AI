package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.MeetingStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsScreen(
    onMeetingClick: (String) -> Unit,
    onCreateMeeting: () -> Unit,
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Upcoming", "Live", "History")
    
    var showFilterSheet by remember { mutableStateOf(false) }

    if (showFilterSheet) {
        FilterMeetingsSheet(
            onDismiss = { showFilterSheet = false },
            onApply = { showFilterSheet = false }
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(BackgroundSurface)) {
                TopAppBar(
                    title = { 
                        Text(
                            "Meetings", 
                            style = Typography.headlineMedium, 
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    actions = {
                        IconButton(onClick = { /* Navigate to Search handled by AppNav */ }) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextPrimary)
                        }
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
                )
                
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BackgroundSurface,
                    contentColor = ElectricIndigo,
                    edgePadding = 20.dp,
                    divider = {},
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = ElectricIndigo
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    title, 
                                    style = if (selectedTab == index) Typography.labelLarge else Typography.bodyMedium,
                                    color = if (selectedTab == index) ElectricIndigo else TextSecondary
                                ) 
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onCreateMeeting,
                containerColor = ElectricIndigo,
                contentColor = Color.White,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Meeting", modifier = Modifier.size(32.dp))
            }
        },
        containerColor = BackgroundSurface
    ) { padding ->
        if (uiState.isLoading && uiState.meetings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricIndigo)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                // PROMINENT GOOGLE MEET-STYLE REJOIN LIVE MEETING BANNER
                if (uiState.liveMeetings.isNotEmpty()) {
                    val activeMeeting = uiState.liveMeetings.first()
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(EmeraldGreen, androidx.compose.foundation.shape.CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "LIVE MEETING IN PROGRESS",
                                        style = Typography.labelMedium,
                                        color = EmeraldGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    activeMeeting.title,
                                    style = Typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!activeMeeting.meetingCode.isNullOrEmpty()) {
                                    Text(
                                        "Code: ${activeMeeting.meetingCode}",
                                        style = Typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                MeetMindButton(
                                    text = "REJOIN MEETING NOW 🟢",
                                    onClick = { onMeetingClick(activeMeeting.id.toString()) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // JOIN WITH A CODE INPUT CARD
                item {
                    var inputCode by remember { mutableStateOf("") }
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = BackgroundSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputCode,
                                onValueChange = { inputCode = it },
                                placeholder = { Text("Enter meeting code (e.g. mm-809)", style = Typography.bodySmall) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricIndigo,
                                    unfocusedBorderColor = BorderColor
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (inputCode.isNotBlank()) {
                                        viewModel.joinByCode(inputCode.trim())
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                            ) {
                                Text("Join")
                            }
                        }
                    }
                }

                val meetings = when (selectedTab) {
                    0 -> uiState.upcomingMeetings
                    1 -> uiState.liveMeetings
                    else -> uiState.pastMeetings
                }
                
                if (meetings.isEmpty()) {
                    item {
                        EmptyMeetingsState(
                            onAction = onCreateMeeting
                        )
                    }
                } else {
                    items(meetings) { meeting ->
                        MeetingItemCard(meeting = meeting, onClick = { onMeetingClick(meeting.id.toString()) })
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingItemCard(meeting: Meeting, onClick: () -> Unit) {
    MeetMindCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    meeting.title, 
                    style = Typography.titleMedium, 
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(meeting.scheduledAt, style = Typography.bodySmall, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color.DarkGray
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(meeting.hostName.take(1), style = Typography.labelSmall, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Hosted by ${meeting.hostName}", 
                        style = Typography.labelSmall, 
                        color = TextSecondary
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(
                    text = meeting.status.name,
                    color = when (meeting.status) {
                        MeetingStatus.LIVE -> RoseRed
                        MeetingStatus.SCHEDULED -> ElectricIndigo
                        else -> TextDisabled
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyMeetingsState(onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = GlassWhite
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.EventBusy,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = TextDisabled
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("No meetings found", style = Typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Your scheduled and past meetings will appear here.", 
            style = Typography.bodyLarge, 
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        MeetMindButton(
            text = "Create Meeting",
            onClick = onAction,
            modifier = Modifier.width(200.dp)
        )
    }
}
