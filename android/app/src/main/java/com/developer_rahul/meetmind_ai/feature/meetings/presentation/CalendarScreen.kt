package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onMeetingClick: (String) -> Unit,
    onCreateMeeting: () -> Unit,
    onNotifications: () -> Unit,
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val daysInWeek = remember(selectedDate) {
        val startOfWeek = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateMeeting,
                containerColor = ElectricIndigo,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Meeting")
            }
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Month Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row {
                    IconButton(onClick = { selectedDate = selectedDate.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, null, tint = TextPrimary)
                    }
                    IconButton(onClick = { selectedDate = selectedDate.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, null, tint = TextPrimary)
                    }
                }
            }

            // Days of Week Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysInWeek.forEach { date ->
                    val isSelected = date == selectedDate
                    val isToday = date == LocalDate.now()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .background(
                                color = if (isSelected) ElectricIndigo else if (isToday) GlassWhite else Color.Transparent,
                                shape = MaterialTheme.shapes.medium
                            )
                            .clickable { selectedDate = date }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("EEE")),
                            style = Typography.labelSmall,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionHeader(
                title = "Schedule for ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(8.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else if (uiState.meetings.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventAvailable, null, modifier = Modifier.size(64.dp), tint = TextDisabled)
                        Spacer(Modifier.height(16.dp))
                        Text("No Meetings Scheduled", style = Typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap + to schedule a meeting for this day.", style = Typography.bodyMedium, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.meetings) { meeting ->
                        MeetingItemCard(
                            meeting = meeting,
                            onClick = { onMeetingClick(meeting.id.toString()) }
                        )
                    }
                }
            }
        }
    }
}
