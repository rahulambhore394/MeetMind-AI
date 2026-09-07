package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.Meeting
import com.developer_rahul.meetmind_ai.feature.meetings.domain.model.MeetingStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val hasMeeting: Boolean
)

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
    var isMonthView by remember { mutableStateOf(true) }

    // Week Strip Days
    val daysInWeek = remember(selectedDate) {
        val startOfWeek = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    // Full Month Grid Days
    val monthGridDays = remember(selectedDate, uiState.meetings) {
        val firstOfMonth = selectedDate.withDayOfMonth(1)
        val firstDayOfWeek = firstOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
        val daysInMonth = selectedDate.lengthOfMonth()

        val days = mutableListOf<CalendarDay>()

        // Previous month padding
        val prevMonth = selectedDate.minusMonths(1)
        val daysInPrevMonth = prevMonth.lengthOfMonth()
        val paddingStart = firstDayOfWeek - 1
        for (i in (paddingStart - 1) downTo 0) {
            val date = prevMonth.withDayOfMonth(daysInPrevMonth - i)
            days.add(CalendarDay(date, isCurrentMonth = false, isToday = date == LocalDate.now(), isSelected = date == selectedDate, hasMeeting = false))
        }

        // Current month days
        for (day in 1..daysInMonth) {
            val date = selectedDate.withDayOfMonth(day)
            val hasMeeting = uiState.meetings.any { meeting ->
                val iso = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val dayMonth = date.format(DateTimeFormatter.ofPattern("dd MMM"))
                val monthDay = date.format(DateTimeFormatter.ofPattern("MMM d"))
                meeting.scheduledAt.contains(iso) || meeting.scheduledAt.contains(dayMonth, ignoreCase = true) || meeting.scheduledAt.contains(monthDay, ignoreCase = true)
            }
            days.add(CalendarDay(date, isCurrentMonth = true, isToday = date == LocalDate.now(), isSelected = date == selectedDate, hasMeeting = hasMeeting))
        }

        // Next month padding to complete 7-column grid
        val remaining = (7 - (days.size % 7)) % 7
        val nextMonth = selectedDate.plusMonths(1)
        for (day in 1..remaining) {
            val date = nextMonth.withDayOfMonth(day)
            days.add(CalendarDay(date, isCurrentMonth = false, isToday = date == LocalDate.now(), isSelected = date == selectedDate, hasMeeting = false))
        }

        days
    }

    // Filter meetings scheduled on selectedDate (precise matching)
    val meetingsForSelectedDate = remember(uiState.meetings, selectedDate) {
        val iso = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val dayMonth = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM"))
        val monthDay = selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))
        val altDay = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        uiState.meetings.filter { meeting ->
            meeting.scheduledAt.contains(iso) || 
            meeting.scheduledAt.contains(dayMonth, ignoreCase = true) || 
            meeting.scheduledAt.contains(monthDay, ignoreCase = true) ||
            meeting.scheduledAt.contains(altDay)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Calendar & Agenda", style = Typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NeonCyan, CircleShape)
                        )
                    }
                },
                actions = {
                    Surface(
                        onClick = { selectedDate = LocalDate.now() },
                        shape = CircleShape,
                        color = ElectricIndigo.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .bounceClick()
                    ) {
                        Text(
                            "Today",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = Typography.labelMedium,
                            color = ElectricIndigo,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        floatingActionButton = {
            GradientFloatingActionButton(
                text = "Schedule Meeting",
                onClick = onCreateMeeting,
                icon = Icons.Default.Event
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Month Header Card & View Toggle
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .glassmorphism(),
                shape = RoundedCornerShape(20.dp),
                color = CardSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = Typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Month / Week View Toggle Button
                        IconButton(
                            onClick = { isMonthView = !isMonthView },
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(
                                if (isMonthView) Icons.Default.ViewWeek else Icons.Default.CalendarViewMonth,
                                contentDescription = "Toggle View",
                                tint = NeonCyan
                            )
                        }
                        
                        IconButton(
                            onClick = { 
                                selectedDate = if (isMonthView) selectedDate.minusMonths(1) else selectedDate.minusWeeks(1) 
                            },
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(Icons.Default.ChevronLeft, null, tint = TextPrimary)
                        }
                        IconButton(
                            onClick = { 
                                selectedDate = if (isMonthView) selectedDate.plusMonths(1) else selectedDate.plusWeeks(1) 
                            },
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(Icons.Default.ChevronRight, null, tint = TextPrimary)
                        }
                    }
                }
            }

            // Calendar Body (Month Grid vs Week Strip)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Weekday Labels Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { day ->
                            Text(
                                text = day,
                                style = Typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (isMonthView) {
                        // Full 7-Column Month Grid View
                        val rows = monthGridDays.chunked(7)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            rows.forEach { weekRow ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    weekRow.forEach { dayItem ->
                                        Surface(
                                            onClick = { selectedDate = dayItem.date },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (dayItem.isSelected) ElectricIndigo else Color.Transparent,
                                            border = if (!dayItem.isSelected && dayItem.isToday) androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan) else null,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .padding(2.dp)
                                                .bounceClick()
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = dayItem.date.dayOfMonth.toString(),
                                                    style = Typography.bodyMedium,
                                                    fontWeight = if (dayItem.isSelected || dayItem.isToday) FontWeight.Bold else FontWeight.Medium,
                                                    color = when {
                                                        dayItem.isSelected -> Color.White
                                                        !dayItem.isCurrentMonth -> TextDisabled
                                                        dayItem.isToday -> NeonCyan
                                                        else -> TextPrimary
                                                    }
                                                )
                                                if (dayItem.hasMeeting) {
                                                    Spacer(Modifier.height(2.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .background(if (dayItem.isSelected) Color.White else NeonCyan, CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 7-Day Week Strip View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            daysInWeek.forEach { date ->
                                val isSelected = date == selectedDate
                                val isToday = date == LocalDate.now()

                                val hasMeetingOnDate = remember(uiState.meetings, date) {
                                    val iso = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                    uiState.meetings.any { it.scheduledAt.contains(iso) }
                                }

                                Surface(
                                    onClick = { selectedDate = date },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) ElectricIndigo else Color.Transparent,
                                    border = if (!isSelected && isToday) androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .bounceClick()
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            style = Typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        if (hasMeetingOnDate) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(if (isSelected) Color.White else NeonCyan, CircleShape)
                                            )
                                        } else {
                                            Spacer(Modifier.height(5.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Agenda Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Schedule for ${selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))}",
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Surface(
                    color = CardSurface,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Text(
                        "${meetingsForSelectedDate.size} Meeting${if (meetingsForSelectedDate.size == 1) "" else "s"}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = Typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (uiState.isLoading && uiState.meetings.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricIndigo)
                }
            } else if (meetingsForSelectedDate.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 30.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(76.dp),
                            shape = CircleShape,
                            color = GlassBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = TextDisabled
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("No Meetings Scheduled", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap + to schedule a meeting for ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}.",
                            style = Typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onCreateMeeting,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.bounceClick(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Schedule Meeting", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 160.dp)
                ) {
                    items(meetingsForSelectedDate) { meeting ->
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


