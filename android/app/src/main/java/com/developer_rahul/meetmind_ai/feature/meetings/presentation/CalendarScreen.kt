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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import java.util.Locale

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val hasMeeting: Boolean,
    val hasLiveMeeting: Boolean
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
    var agendaFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Live, 2: Scheduled

    val layout = LocalResponsiveLayout.current
    val today = remember { LocalDate.now() }

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
            days.add(
                CalendarDay(
                    date = date,
                    isCurrentMonth = false,
                    isToday = date == today,
                    isSelected = date == selectedDate,
                    hasMeeting = false,
                    hasLiveMeeting = false
                )
            )
        }

        // Current month days
        for (day in 1..daysInMonth) {
            val date = selectedDate.withDayOfMonth(day)
            val meetingsOnDay = uiState.meetings.filter { MeetingDateTimeUtils.matchesDate(it.scheduledAt, date) }
            val hasMeeting = meetingsOnDay.isNotEmpty()
            val hasLiveMeeting = meetingsOnDay.any { it.status == MeetingStatus.LIVE }

            days.add(
                CalendarDay(
                    date = date,
                    isCurrentMonth = true,
                    isToday = date == today,
                    isSelected = date == selectedDate,
                    hasMeeting = hasMeeting,
                    hasLiveMeeting = hasLiveMeeting
                )
            )
        }

        // Next month padding to complete 7-column grid
        val remaining = (7 - (days.size % 7)) % 7
        val nextMonth = selectedDate.plusMonths(1)
        for (day in 1..remaining) {
            val date = nextMonth.withDayOfMonth(day)
            days.add(
                CalendarDay(
                    date = date,
                    isCurrentMonth = false,
                    isToday = date == today,
                    isSelected = date == selectedDate,
                    hasMeeting = false,
                    hasLiveMeeting = false
                )
            )
        }

        days
    }

    // Filter meetings scheduled on selectedDate
    val meetingsForSelectedDate = remember(uiState.meetings, selectedDate) {
        uiState.meetings.filter { MeetingDateTimeUtils.matchesDate(it.scheduledAt, selectedDate) }
    }

    val filteredAgendaMeetings = remember(meetingsForSelectedDate, agendaFilter) {
        when (agendaFilter) {
            1 -> meetingsForSelectedDate.filter { it.status == MeetingStatus.LIVE }
            2 -> meetingsForSelectedDate.filter { it.status == MeetingStatus.SCHEDULED }
            else -> meetingsForSelectedDate
        }
    }

    // Monthly Meeting Stats
    val monthlyMeetingsCount = remember(uiState.meetings, selectedDate) {
        uiState.meetings.count { meeting ->
            val parsed = MeetingDateTimeUtils.parseMeetingTime(meeting.scheduledAt)
            parsed.localDate?.let { it.month == selectedDate.month && it.year == selectedDate.year } ?: false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Calendar & Agenda",
                            style = if (layout.isSmallPhone) Typography.titleMedium else Typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
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
                        onClick = { selectedDate = today },
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
                    IconButton(onClick = onNotifications, modifier = Modifier.bounceClick()) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        floatingActionButton = {
            GradientFloatingActionButton(
                text = if (layout.isSmallPhone) "Schedule" else "Schedule Meeting",
                onClick = onCreateMeeting,
                icon = Icons.Default.Event
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        val isTwoPane = layout.isTabletOrLarger || layout.isLandscape

        if (isTwoPane) {
            // Tablet / Landscape Dual-Pane Master-Detail Layout
            ResponsiveContainer(maxWidth = 1100.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = layout.horizontalPadding, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // LEFT PANE: Calendar & Month Navigator
                    Column(
                        modifier = Modifier
                            .width(if (layout.isLandscape && !layout.isTablet) 360.dp else 400.dp)
                            .fillMaxHeight()
                    ) {
                        CalendarHeaderCard(
                            selectedDate = selectedDate,
                            isMonthView = isMonthView,
                            onToggleView = { isMonthView = !isMonthView },
                            onPrev = {
                                selectedDate = if (isMonthView) selectedDate.minusMonths(1) else selectedDate.minusWeeks(1)
                            },
                            onNext = {
                                selectedDate = if (isMonthView) selectedDate.plusMonths(1) else selectedDate.plusWeeks(1)
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        CalendarGridCard(
                            selectedDate = selectedDate,
                            isMonthView = isMonthView,
                            monthGridDays = monthGridDays,
                            daysInWeek = daysInWeek,
                            allMeetings = uiState.meetings,
                            onSelectDate = { selectedDate = it }
                        )

                        Spacer(Modifier.height(12.dp))

                        // Monthly Quick Stats Bento Card
                        MonthStatsCard(
                            selectedDate = selectedDate,
                            monthlyMeetingsCount = monthlyMeetingsCount,
                            todayMeetingsCount = uiState.meetings.count { MeetingDateTimeUtils.matchesDate(it.scheduledAt, today) }
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(BorderColor)
                    )

                    // RIGHT PANE: Selected Day's Agenda
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        AgendaHeaderRow(
                            selectedDate = selectedDate,
                            totalCount = meetingsForSelectedDate.size,
                            filterIndex = agendaFilter,
                            onSelectFilter = { agendaFilter = it },
                            onCreateMeeting = onCreateMeeting
                        )

                        Spacer(Modifier.height(10.dp))

                        if (uiState.isLoading && uiState.meetings.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = ElectricIndigo)
                            }
                        } else if (filteredAgendaMeetings.isEmpty()) {
                            EmptyAgendaState(
                                selectedDate = selectedDate,
                                onCreateMeeting = onCreateMeeting
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 140.dp)
                            ) {
                                items(filteredAgendaMeetings, key = { it.id }) { meeting ->
                                    EnhancedMeetingItemCard(
                                        meeting = meeting,
                                        onClick = { onMeetingClick(meeting.id.toString()) },
                                        onJoinDirect = { onMeetingClick(meeting.id.toString()) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Smartphone Portrait Single-Pane Vertical Layout
            ResponsiveContainer(maxWidth = 840.dp) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = layout.horizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp)
                ) {
                    // Month Header Card
                    item {
                        CalendarHeaderCard(
                            selectedDate = selectedDate,
                            isMonthView = isMonthView,
                            onToggleView = { isMonthView = !isMonthView },
                            onPrev = {
                                selectedDate = if (isMonthView) selectedDate.minusMonths(1) else selectedDate.minusWeeks(1)
                            },
                            onNext = {
                                selectedDate = if (isMonthView) selectedDate.plusMonths(1) else selectedDate.plusWeeks(1)
                            }
                        )
                    }

                    // Calendar Grid or Week Strip Card
                    item {
                        CalendarGridCard(
                            selectedDate = selectedDate,
                            isMonthView = isMonthView,
                            monthGridDays = monthGridDays,
                            daysInWeek = daysInWeek,
                            allMeetings = uiState.meetings,
                            onSelectDate = { selectedDate = it }
                        )
                    }

                    // Agenda Section Header
                    item {
                        AgendaHeaderRow(
                            selectedDate = selectedDate,
                            totalCount = meetingsForSelectedDate.size,
                            filterIndex = agendaFilter,
                            onSelectFilter = { agendaFilter = it },
                            onCreateMeeting = onCreateMeeting
                        )
                    }

                    // Agenda Items or Empty State
                    if (uiState.isLoading && uiState.meetings.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = ElectricIndigo)
                            }
                        }
                    } else if (filteredAgendaMeetings.isEmpty()) {
                        item {
                            EmptyAgendaState(
                                selectedDate = selectedDate,
                                onCreateMeeting = onCreateMeeting
                            )
                        }
                    } else {
                        items(filteredAgendaMeetings, key = { it.id }) { meeting ->
                            EnhancedMeetingItemCard(
                                meeting = meeting,
                                onClick = { onMeetingClick(meeting.id.toString()) },
                                onJoinDirect = { onMeetingClick(meeting.id.toString()) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Month Header Card with smooth navigation arrows and Month/Week toggle.
 */
@Composable
private fun CalendarHeaderCard(
    selectedDate: LocalDate,
    isMonthView: Boolean,
    onToggleView: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val layout = LocalResponsiveLayout.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism(),
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (layout.isSmallPhone) 10.dp else 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    style = if (layout.isSmallPhone) Typography.titleMedium else Typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleView,
                    modifier = Modifier.bounceClick()
                ) {
                    Icon(
                        if (isMonthView) Icons.Default.ViewWeek else Icons.Default.CalendarViewMonth,
                        contentDescription = "Toggle Month/Week",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onPrev,
                    modifier = Modifier.bounceClick()
                ) {
                    Icon(Icons.Default.ChevronLeft, "Previous", tint = TextPrimary)
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.bounceClick()
                ) {
                    Icon(Icons.Default.ChevronRight, "Next", tint = TextPrimary)
                }
            }
        }
    }
}

/**
 * Interactive Calendar Card supporting both 7-Column Full Month Grid and 7-Day Week Strip.
 */
@Composable
private fun CalendarGridCard(
    selectedDate: LocalDate,
    isMonthView: Boolean,
    monthGridDays: List<CalendarDay>,
    daysInWeek: List<LocalDate>,
    allMeetings: List<Meeting>,
    onSelectDate: (LocalDate) -> Unit
) {
    val layout = LocalResponsiveLayout.current
    val weekdayLabels = remember(layout.isSmallPhone) {
        if (layout.isSmallPhone) listOf("M", "T", "W", "T", "F", "S", "S")
        else listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(if (layout.isSmallPhone) 8.dp else 12.dp)) {
            // Weekday Header Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekdayLabels.forEach { label ->
                    Text(
                        text = label,
                        style = Typography.labelSmall.copy(fontSize = if (layout.isSmallPhone) 10.sp else 11.sp),
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
                Column(verticalArrangement = Arrangement.spacedBy(if (layout.isSmallPhone) 4.dp else 6.dp)) {
                    rows.forEach { weekRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            weekRow.forEach { dayItem ->
                                Surface(
                                    onClick = { onSelectDate(dayItem.date) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (dayItem.isSelected) ElectricIndigo else Color.Transparent,
                                    border = if (!dayItem.isSelected && dayItem.isToday) androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(if (layout.isSmallPhone) 38.dp else 44.dp)
                                        .padding(1.dp)
                                        .bounceClick()
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = dayItem.date.dayOfMonth.toString(),
                                            style = if (layout.isSmallPhone) Typography.bodySmall else Typography.bodyMedium,
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
                                                    .size(if (layout.isSmallPhone) 3.5.dp else 4.5.dp)
                                                    .background(
                                                        when {
                                                            dayItem.isSelected -> Color.White
                                                            dayItem.hasLiveMeeting -> EmeraldGreen
                                                            else -> NeonCyan
                                                        },
                                                        CircleShape
                                                    )
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

                        val hasMeetingOnDate = remember(allMeetings, date) {
                            allMeetings.any { MeetingDateTimeUtils.matchesDate(it.scheduledAt, date) }
                        }
                        val hasLiveOnDate = remember(allMeetings, date) {
                            allMeetings.any { it.status == MeetingStatus.LIVE && MeetingDateTimeUtils.matchesDate(it.scheduledAt, date) }
                        }

                        Surface(
                            onClick = { onSelectDate(date) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) ElectricIndigo else Color.Transparent,
                            border = if (!isSelected && isToday) androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan) else null,
                            modifier = Modifier
                                .weight(1f)
                                .bounceClick()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = if (layout.isSmallPhone) 6.dp else 10.dp)
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = if (layout.isSmallPhone) Typography.titleSmall else Typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                if (hasMeetingOnDate) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(
                                                when {
                                                    isSelected -> Color.White
                                                    hasLiveOnDate -> EmeraldGreen
                                                    else -> NeonCyan
                                                },
                                                CircleShape
                                            )
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
}

/**
 * Monthly Summary Bento Card showing meeting statistics.
 */
@Composable
private fun MonthStatsCard(
    selectedDate: LocalDate,
    monthlyMeetingsCount: Int,
    todayMeetingsCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$monthlyMeetingsCount",
                    style = Typography.titleLarge,
                    color = ElectricIndigo,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "This Month",
                    style = Typography.labelSmall,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(BorderColor)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$todayMeetingsCount",
                    style = Typography.titleLarge,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Today",
                    style = Typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Agenda Section Header Row with date title and status filter chips.
 */
@Composable
private fun AgendaHeaderRow(
    selectedDate: LocalDate,
    totalCount: Int,
    filterIndex: Int,
    onSelectFilter: (Int) -> Unit,
    onCreateMeeting: () -> Unit
) {
    val layout = LocalResponsiveLayout.current
    val formattedDate = remember(selectedDate) {
        val today = LocalDate.now()
        when (selectedDate) {
            today -> "Today, ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))}"
            today.plusDays(1) -> "Tomorrow, ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))}"
            else -> selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedDate,
                    style = if (layout.isSmallPhone) Typography.titleSmall else Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = CardSurface,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Text(
                        "$totalCount",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = Typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Filter Chips (All, Live, Scheduled)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("All", "Live", "Upcoming").forEachIndexed { index, label ->
                    val isSelected = filterIndex == index
                    Surface(
                        onClick = { onSelectFilter(index) },
                        shape = CircleShape,
                        color = if (isSelected) ElectricIndigo.copy(alpha = 0.2f) else CardSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) ElectricIndigo else BorderColor
                        ),
                        modifier = Modifier.bounceClick()
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = Typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (isSelected) ElectricIndigo else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty Agenda State with date-specific prompt and schedule shortcut.
 */
@Composable
fun EmptyAgendaState(
    selectedDate: LocalDate,
    onCreateMeeting: () -> Unit
) {
    val layout = LocalResponsiveLayout.current
    val dateText = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (layout.isSmallPhone) 24.dp else 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 20.dp)
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
            Text(
                "No Meetings on $dateText",
                style = Typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your schedule is completely clear. Tap below to book or invite team members.",
                style = Typography.bodySmall,
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
                Text("Schedule for $dateText", fontWeight = FontWeight.Bold)
            }
        }
    }
}
