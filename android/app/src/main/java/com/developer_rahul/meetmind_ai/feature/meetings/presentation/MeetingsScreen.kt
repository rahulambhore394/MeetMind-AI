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
import androidx.compose.foundation.lazy.grid.GridItemSpan
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsScreen(
    onMeetingClick: (String) -> Unit,
    onCreateMeeting: () -> Unit,
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val layout = LocalResponsiveLayout.current

    val tabs = remember(uiState.upcomingMeetings.size, uiState.liveMeetings.size, uiState.pastMeetings.size, layout.isSmallPhone) {
        if (layout.isSmallPhone) {
            listOf(
                "Up (${uiState.upcomingMeetings.size})",
                "Live (${uiState.liveMeetings.size})",
                "Past (${uiState.pastMeetings.size})"
            )
        } else {
            listOf(
                "Upcoming (${uiState.upcomingMeetings.size})",
                "Live (${uiState.liveMeetings.size})",
                "History (${uiState.pastMeetings.size})"
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadMeetings()
    }

    LaunchedEffect(uiState.joinSuccess) {
        if (uiState.joinSuccess) {
            val selected = uiState.selectedMeeting
            viewModel.resetJoinState()
            if (selected != null) {
                onMeetingClick(selected.id.toString())
            }
        }
    }

    if (showFilterSheet) {
        FilterMeetingsSheet(
            onDismiss = { showFilterSheet = false },
            onApply = { showFilterSheet = false }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(BackgroundSurface)
                    .statusBarsPadding()
            ) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Meetings & Calls",
                                style = if (layout.isSmallPhone) Typography.titleMedium else Typography.headlineSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = ElectricIndigo.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Text(
                                    "${uiState.meetings.size}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = Typography.labelSmall,
                                    color = ElectricIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { isSearchActive = !isSearchActive },
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(
                                if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchActive) NeonCyan else TextPrimary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.loadMeetings() },
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                        }
                        IconButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
                )

                // Inline Search Bar (Animated)
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = layout.horizontalPadding, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 640.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = CardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = {
                                        Text(
                                            "Search by title, host, or code...",
                                            style = Typography.bodyMedium,
                                            color = TextDisabled
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Adaptive Pill Tabs Row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = layout.horizontalPadding, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (layout.isSmallPhone) 6.dp else 10.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Surface(
                                onClick = { selectedTab = index },
                                shape = CircleShape,
                                color = if (isSelected) ElectricIndigo else CardSurface,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .bounceClick()
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = if (layout.isSmallPhone) 8.dp else 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (index == 1 && uiState.liveMeetings.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(EmeraldGreen, CircleShape)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        Text(
                                            title,
                                            style = if (layout.isSmallPhone) Typography.labelSmall else Typography.labelMedium,
                                            color = if (isSelected) Color.White else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            GradientFloatingActionButton(
                text = if (layout.isSmallPhone) "New" else "New Meeting",
                onClick = onCreateMeeting,
                icon = Icons.Default.VideoCall
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        if (uiState.isLoading && uiState.meetings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricIndigo)
            }
        } else {
            val baseMeetings = when (selectedTab) {
                0 -> uiState.upcomingMeetings
                1 -> uiState.liveMeetings
                else -> uiState.pastMeetings
            }

            val filteredMeetings = remember(baseMeetings, searchQuery) {
                if (searchQuery.isBlank()) {
                    baseMeetings
                } else {
                    baseMeetings.filter { meeting ->
                        meeting.title.contains(searchQuery, ignoreCase = true) ||
                        meeting.hostName.contains(searchQuery, ignoreCase = true) ||
                        (meeting.meetingCode?.contains(searchQuery, ignoreCase = true) == true) ||
                        (meeting.description?.contains(searchQuery, ignoreCase = true) == true)
                    }
                }
            }

            ResponsiveContainer(maxWidth = 1000.dp) {
                val useGridLayout = layout.isTabletOrLarger || layout.isLandscape

                if (useGridLayout) {
                    // Wide / Tablet / Landscape 2-Column Grid Layout
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = layout.horizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 140.dp)
                    ) {
                        // LIVE MEETING HERO BANNER (Spans all columns)
                        if (uiState.liveMeetings.isNotEmpty() && searchQuery.isBlank() && selectedTab != 2) {
                            val activeMeeting = uiState.liveMeetings.first()
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LiveMeetingHeroBanner(
                                    meeting = activeMeeting,
                                    onRejoin = { onMeetingClick(activeMeeting.id.toString()) },
                                    isTablet = true
                                )
                            }
                        }

                        // QUICK JOIN BY CODE CARD (Spans all columns)
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            QuickJoinByCodeCard(
                                onJoinCode = { code -> viewModel.joinByCode(code) }
                            )
                        }

                        // MEETING LIST OR EMPTY STATE
                        if (filteredMeetings.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyMeetingsState(
                                    searchQuery = searchQuery,
                                    selectedTab = selectedTab,
                                    onAction = onCreateMeeting
                                )
                            }
                        } else {
                            items(filteredMeetings, key = { it.id }) { meeting ->
                                EnhancedMeetingItemCard(
                                    meeting = meeting,
                                    onClick = { onMeetingClick(meeting.id.toString()) },
                                    onJoinDirect = { onMeetingClick(meeting.id.toString()) }
                                )
                            }
                        }
                    }
                } else {
                    // Smartphone Single-Column List Layout
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = layout.horizontalPadding),
                        verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 140.dp)
                    ) {
                        // LIVE MEETING HERO BANNER
                        if (uiState.liveMeetings.isNotEmpty() && searchQuery.isBlank() && selectedTab != 2) {
                            val activeMeeting = uiState.liveMeetings.first()
                            item {
                                LiveMeetingHeroBanner(
                                    meeting = activeMeeting,
                                    onRejoin = { onMeetingClick(activeMeeting.id.toString()) },
                                    isTablet = false
                                )
                            }
                        }

                        // QUICK JOIN BY CODE CARD
                        item {
                            QuickJoinByCodeCard(
                                onJoinCode = { code -> viewModel.joinByCode(code) }
                            )
                        }

                        // MEETING ITEMS
                        if (filteredMeetings.isEmpty()) {
                            item {
                                EmptyMeetingsState(
                                    searchQuery = searchQuery,
                                    selectedTab = selectedTab,
                                    onAction = onCreateMeeting
                                )
                            }
                        } else {
                            items(filteredMeetings, key = { it.id }) { meeting ->
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
}

/**
 * Prominent Live Meeting Hero Banner with reactive tablet & phone layouts.
 */
@Composable
private fun LiveMeetingHeroBanner(
    meeting: Meeting,
    onRejoin: () -> Unit,
    isTablet: Boolean
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val layout = LocalResponsiveLayout.current

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(listOf(EmeraldGreen, NeonCyan))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = EmeraldGreen.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(EmeraldGreen.copy(alpha = 0.12f), CardSurface)
                    )
                )
                .padding(if (layout.isSmallPhone) 14.dp else 20.dp)
        ) {
            if (isTablet) {
                // Wide Display Row Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(EmeraldGreen, CircleShape)
                                    .pulse(color = EmeraldGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "LIVE MEETING IN PROGRESS",
                                style = Typography.labelMedium,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                            if (!meeting.meetingCode.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.width(10.dp))
                                MeetingCodePill(code = meeting.meetingCode)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            meeting.title,
                            style = Typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Hosted by ${meeting.hostName}",
                            style = Typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = onRejoin,
                        modifier = Modifier
                            .widthIn(min = 180.dp)
                            .height(48.dp)
                            .bounceClick(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Default.VideoCall, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("REJOIN NOW 🟢", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else {
                // Phone Column Layout
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(EmeraldGreen, CircleShape)
                                    .pulse(color = EmeraldGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "LIVE IN PROGRESS",
                                style = Typography.labelSmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!meeting.meetingCode.isNullOrEmpty()) {
                            MeetingCodePill(code = meeting.meetingCode)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        meeting.title,
                        style = if (layout.isSmallPhone) Typography.titleMedium else Typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Hosted by ${meeting.hostName}",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onRejoin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .bounceClick(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Default.VideoCall, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("REJOIN MEETING NOW 🟢", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Responsive Join with a Code Card with clipboard auto-paste detection.
 */
@Composable
private fun QuickJoinByCodeCard(onJoinCode: (String) -> Unit) {
    var inputCode by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val layout = LocalResponsiveLayout.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(if (layout.isSmallPhone) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputCode,
                onValueChange = { inputCode = it },
                placeholder = {
                    Text(
                        if (layout.isSmallPhone) "Code (e.g. mm-809)" else "Enter meeting code (e.g. mm-809)",
                        style = Typography.bodySmall,
                        color = TextDisabled
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.VpnKey, null, tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (inputCode.isEmpty()) {
                        IconButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    inputCode = clipText.trim()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, "Paste", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricIndigo,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = {
                    if (inputCode.isNotBlank()) {
                        onJoinCode(inputCode.trim())
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .bounceClick(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
            ) {
                Text("Join", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Compatible wrapper for callers expecting MeetingItemCard.
 */
@Composable
fun MeetingItemCard(meeting: Meeting, onClick: () -> Unit) {
    EnhancedMeetingItemCard(
        meeting = meeting,
        onClick = onClick,
        onJoinDirect = onClick
    )
}

/**
 * Enhanced Meeting Card with timing breakdown, status indicator, and direct action triggers.
 */
@Composable
fun EnhancedMeetingItemCard(
    meeting: Meeting,
    onClick: () -> Unit,
    onJoinDirect: () -> Unit
) {
    val layout = LocalResponsiveLayout.current
    val parsedTime = remember(meeting.scheduledAt) {
        MeetingDateTimeUtils.parseMeetingTime(meeting.scheduledAt)
    }

    val isLive = meeting.status == MeetingStatus.LIVE
    val statusColor = when (meeting.status) {
        MeetingStatus.LIVE -> EmeraldGreen
        MeetingStatus.SCHEDULED -> ElectricIndigo
        else -> TextDisabled
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(
            if (isLive) 1.5.dp else 1.dp,
            if (isLive) EmeraldGreen.copy(alpha = 0.6f) else BorderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
    ) {
        Row(
            modifier = Modifier.padding(if (layout.isSmallPhone) 12.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Vertical Indicator Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (layout.isSmallPhone) 64.dp else 76.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(if (layout.isSmallPhone) 10.dp else 14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Header Row: Title & Status Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        meeting.title,
                        style = if (layout.isSmallPhone) Typography.titleSmall else Typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(Modifier.width(6.dp))

                    val isRejoin = meeting.hasJoinedBefore || meeting.userParticipantStatus == "LEFT"
                    val statusLabel = when {
                        isLive && isRejoin -> "RE-JOIN"
                        isLive -> "LIVE 🟢"
                        else -> meeting.status.name
                    }

                    StatusChip(
                        text = statusLabel,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Date & Time Row with Relative Countdown
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (isLive) EmeraldGreen else NeonCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${parsedTime.dateText} • ${parsedTime.timeText}",
                        style = Typography.bodySmall,
                        color = if (isLive) EmeraldGreen else TextSecondary,
                        fontWeight = if (isLive) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Host & Meeting Code Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Host initials badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        val initials = meeting.hostName.split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercase() }
                            .take(2)
                            .joinToString("")
                            .ifEmpty { meeting.hostName.take(1).uppercase() }

                        Surface(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape,
                            color = ElectricIndigo
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    initials,
                                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            meeting.hostName,
                            style = Typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Meeting Code Copy Chip
                    if (!meeting.meetingCode.isNullOrEmpty()) {
                        MeetingCodePill(code = meeting.meetingCode)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Direct Join Button or Chevron Details Arrow
            if (isLive) {
                Surface(
                    onClick = onJoinDirect,
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldGreen,
                    modifier = Modifier.bounceClick()
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VideoCall, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            if (!layout.isSmallPhone) {
                                Spacer(Modifier.width(4.dp))
                                Text("Join", style = Typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Details",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Reusable Meeting Code Pill with 1-tap copy & toast feedback.
 */
@Composable
fun MeetingCodePill(code: String) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Surface(
        onClick = {
            clipboardManager.setText(AnnotatedString(code))
            Toast.makeText(context, "Code copied: $code", Toast.LENGTH_SHORT).show()
        },
        shape = CircleShape,
        color = NeonCyan.copy(alpha = 0.12f),
        modifier = Modifier.bounceClick()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                code,
                style = Typography.labelSmall.copy(fontSize = 11.sp),
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.ContentCopy, null, tint = NeonCyan, modifier = Modifier.size(11.dp))
        }
    }
}

/**
 * Responsive Empty State with contextual messages and creation trigger.
 */
@Composable
fun EmptyMeetingsState(
    searchQuery: String,
    selectedTab: Int,
    onAction: () -> Unit
) {
    val title = when {
        searchQuery.isNotBlank() -> "No matching meetings"
        selectedTab == 1 -> "No live meetings right now"
        selectedTab == 0 -> "No upcoming meetings scheduled"
        else -> "No past meetings found"
    }

    val description = when {
        searchQuery.isNotBlank() -> "No results found for \"$searchQuery\". Try checking the spelling or meeting code."
        selectedTab == 1 -> "When someone starts a meeting or an AI Proxy starts monitoring, it will appear here."
        selectedTab == 0 -> "Schedule a meeting with AI Proxy assistance or start an instant call anytime."
        else -> "Your past recorded and analyzed calls will be cataloged here."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = GlassBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Default.EventBusy,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = TextDisabled
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            title,
            style = Typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            description,
            style = Typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAction,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.bounceClick(),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Meeting", fontWeight = FontWeight.Bold)
        }
    }
}
