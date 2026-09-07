package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.VideoCall
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMeetingScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    onStartMeeting: (String) -> Unit = {},
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var isInstantMeeting by remember { mutableStateOf(true) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var enableAiRep by remember { mutableStateOf(true) }
    var aiFocusMode by remember { mutableStateOf("Action Items & Summary") }
    var showAiConfig by remember { mutableStateOf(false) }

    val calendar = remember { java.util.Calendar.getInstance().apply { add(java.util.Calendar.HOUR_OF_DAY, 1) } }
    var selectedYear by remember { mutableStateOf(calendar.get(java.util.Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(java.util.Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(calendar.get(java.util.Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableStateOf(calendar.get(java.util.Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(java.util.Calendar.MINUTE)) }

    val formattedPreview = remember(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute) {
        val ldt = java.time.LocalDateTime.of(selectedYear, selectedMonth + 1, selectedDay, selectedHour, selectedMinute)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy 'at' hh:mm a")
        ldt.format(formatter)
    }

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDay = dayOfMonth
        },
        selectedYear,
        selectedMonth,
        selectedDay
    )

    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            selectedHour = hourOfDay
            selectedMinute = minute
        },
        selectedHour,
        selectedMinute,
        false
    )

    var showSuccessModal by remember { mutableStateOf(false) }
    var copyMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.creationSuccess) {
        if (uiState.creationSuccess) {
            showSuccessModal = true
        }
    }

    // Modal when meeting creation succeeds
    if (showSuccessModal && uiState.selectedMeeting != null) {
        val createdMeeting = uiState.selectedMeeting!!
        val meetingCode = createdMeeting.meetingCode ?: "mm-${createdMeeting.id}"
        val meetingLink = "https://meetmind-backend-s3yy.onrender.com/join/$meetingCode"

        AlertDialog(
            onDismissRequest = {
                showSuccessModal = false
                viewModel.resetCreationState()
                onCreated()
            },
            containerColor = CardSurface,
            shape = RoundedCornerShape(28.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Meeting Created! 🚀",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Share code or join instantly",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Meeting Topic Card
                    Surface(
                        color = BackgroundSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("MEETING TOPIC", style = MaterialTheme.typography.labelSmall, color = TextDisabled, letterSpacing = 1.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                createdMeeting.title.ifEmpty { "New Meeting" },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Meeting Code Box
                    Column {
                        Text("Meeting Code", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = BackgroundSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    meetingCode,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = NeonCyan
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(meetingCode))
                                        copyMessage = "Meeting code copied!"
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ElectricIndigo.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Direct Join Link Box
                    Column {
                        Text("Direct Join Link", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = BackgroundSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    meetingLink,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(meetingLink))
                                        copyMessage = "Meeting link copied!"
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(GlassWhite, CircleShape)
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = "Copy Link", tint = TextPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    if (copyMessage != null) {
                        Surface(
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    copyMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Share Invitation Button
                    Button(
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "Join my MeetMind AI meeting!\nTitle: ${createdMeeting.title}\nMeeting Code: $meetingCode\nLink: $meetingLink"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Meeting Code"))
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share Invitation", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessModal = false
                        val id = createdMeeting.id
                        viewModel.resetCreationState()
                        viewModel.startMeeting(id)
                        onStartMeeting(id.toString())
                    },
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Start Now 🟢", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSuccessModal = false
                        viewModel.resetCreationState()
                        onCreated()
                    }
                ) {
                    Text("Dashboard", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "New Meeting",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            color = ElectricIndigo.copy(alpha = 0.15f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = ElectricIndigo, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "AI Ready",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 4.dp).background(GlassWhite, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(BackgroundSurface.copy(alpha = 0.0f), BackgroundSurface, BackgroundSurface)
                        )
                    )
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = {
                        if (!uiState.isCreating) {
                            val finalTitle = title.ifBlank { if (isInstantMeeting) "Instant Meeting" else "Scheduled Meeting" }
                            val scheduledLdt = if (isInstantMeeting) {
                                java.time.LocalDateTime.now().plusSeconds(30)
                            } else {
                                java.time.LocalDateTime.of(selectedYear, selectedMonth + 1, selectedDay, selectedHour, selectedMinute, 0)
                            }
                            val scheduledTime = scheduledLdt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            viewModel.createMeeting(finalTitle, description, scheduledTime, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInstantMeeting) ElectricIndigo else EmeraldGreen
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    if (uiState.isCreating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isInstantMeeting) Icons.Default.FlashOn else Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (isInstantMeeting) "Start Instant Meeting Now" else "Confirm & Schedule Meeting",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (uiState.creationError != null) {
                Surface(
                    color = RoseRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, RoseRed.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Close, null, tint = RoseRed, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = uiState.creationError!!,
                            color = RoseRed,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Premium Animated Hero Visual Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = CardSurface,
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (isInstantMeeting) ElectricIndigo.copy(alpha = 0.22f) else EmeraldGreen.copy(alpha = 0.20f),
                                    NeonCyan.copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isInstantMeeting) ElectricIndigo else EmeraldGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isInstantMeeting) Icons.Default.FlashOn else Icons.Default.VideoCall,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isInstantMeeting) "Instant Live Room" else "Scheduled AI Meeting",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = (if (isInstantMeeting) ElectricIndigo else EmeraldGreen).copy(alpha = 0.2f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        if (isInstantMeeting) "⚡ Live Entry" else "📅 Auto-Sync",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isInstantMeeting) ElectricIndigo else EmeraldGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (isInstantMeeting)
                                    "Generates meeting code immediately with 1-click room entry."
                                else
                                    "Set date, time, and agenda for automated calendar invite.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Segmented Control (Mode Switcher)
            SectionHeader(title = "Meeting Mode")
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = CardSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(6.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Instant Toggle Tab
                    val instantBg by animateColorAsState(if (isInstantMeeting) ElectricIndigo else Color.Transparent)
                    val instantTextColor by animateColorAsState(if (isInstantMeeting) Color.White else TextSecondary)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(instantBg)
                            .clickable { isInstantMeeting = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FlashOn, null, tint = instantTextColor, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("⚡ Instant", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = instantTextColor)
                        }
                    }

                    // Scheduled Toggle Tab
                    val scheduledBg by animateColorAsState(if (!isInstantMeeting) EmeraldGreen else Color.Transparent)
                    val scheduledTextColor by animateColorAsState(if (!isInstantMeeting) Color.White else TextSecondary)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(scheduledBg)
                            .clickable { isInstantMeeting = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, tint = scheduledTextColor, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("📅 Schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = scheduledTextColor)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            SectionHeader(title = "Meeting Details")
            Spacer(modifier = Modifier.height(10.dp))

            MeetMindTextField(
                value = title,
                onValueChange = { title = it },
                label = "Meeting Title",
                leadingIcon = Icons.Default.Title
            )

            Spacer(modifier = Modifier.height(16.dp))

            MeetMindTextField(
                value = description,
                onValueChange = { description = it },
                label = "Agenda / Topic Description (Optional)",
                leadingIcon = Icons.Default.Description
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Expandable AI Assistant Card
            Surface(
                color = CardSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (enableAiRep) ElectricIndigo.copy(alpha = 0.4f) else BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showAiConfig = !showAiConfig },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ElectricIndigo.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Psychology, null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("AI Co-Host & Summarizer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Auto-transcribe & extract action items", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Switch(
                            checked = enableAiRep,
                            onCheckedChange = { enableAiRep = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ElectricIndigo
                            )
                        )
                    }

                    AnimatedVisibility(
                        visible = enableAiRep && showAiConfig,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 12.dp))
                            Text("Summary Focus Mode", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Action Items", "Full Summary", "Executive").forEach { mode ->
                                    val isSelected = aiFocusMode.contains(mode)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { aiFocusMode = mode },
                                        label = { Text(mode, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricIndigo,
                                            selectedLabelColor = Color.White,
                                            containerColor = BackgroundSurface,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Animated Date Picker section for Scheduled Meetings
            AnimatedVisibility(
                visible = !isInstantMeeting,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(26.dp))
                    SectionHeader(title = "Date & Time Selection")
                    Spacer(modifier = Modifier.height(10.dp))

                    // Date & Time Pickers Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date Card Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { datePickerDialog.show() },
                            color = CardSurface,
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ElectricIndigo.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp), tint = ElectricIndigo)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Date", style = MaterialTheme.typography.labelSmall, color = TextDisabled)
                                    Text(
                                        String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        // Time Card Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { timePickerDialog.show() },
                            color = CardSurface,
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp), tint = NeonCyan)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Time", style = MaterialTheme.typography.labelSmall, color = TextDisabled)
                                    val amPm = if (selectedHour >= 12) "PM" else "AM"
                                    val hour12 = if (selectedHour % 12 == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
                                    Text(
                                        String.format("%02d:%02d %s", hour12, selectedMinute, amPm),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Quick Time Preset Chips
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Quick Presets", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // In 30 mins
                        AssistChip(
                            onClick = {
                                val c = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MINUTE, 30) }
                                selectedYear = c.get(java.util.Calendar.YEAR)
                                selectedMonth = c.get(java.util.Calendar.MONTH)
                                selectedDay = c.get(java.util.Calendar.DAY_OF_MONTH)
                                selectedHour = c.get(java.util.Calendar.HOUR_OF_DAY)
                                selectedMinute = c.get(java.util.Calendar.MINUTE)
                            },
                            label = { Text("+30 Mins", style = MaterialTheme.typography.labelSmall, color = TextPrimary) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = CardSurface),
                            border = AssistChipDefaults.assistChipBorder(borderColor = BorderColor, enabled = true)
                        )

                        // Tomorrow 10 AM
                        AssistChip(
                            onClick = {
                                val c = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_MONTH, 1) }
                                selectedYear = c.get(java.util.Calendar.YEAR)
                                selectedMonth = c.get(java.util.Calendar.MONTH)
                                selectedDay = c.get(java.util.Calendar.DAY_OF_MONTH)
                                selectedHour = 10
                                selectedMinute = 0
                            },
                            label = { Text("Tomorrow 10 AM", style = MaterialTheme.typography.labelSmall, color = TextPrimary) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = CardSurface),
                            border = AssistChipDefaults.assistChipBorder(borderColor = BorderColor, enabled = true)
                        )

                        // Tomorrow 3 PM
                        AssistChip(
                            onClick = {
                                val c = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_MONTH, 1) }
                                selectedYear = c.get(java.util.Calendar.YEAR)
                                selectedMonth = c.get(java.util.Calendar.MONTH)
                                selectedDay = c.get(java.util.Calendar.DAY_OF_MONTH)
                                selectedHour = 15
                                selectedMinute = 0
                            },
                            label = { Text("Tomorrow 3 PM", style = MaterialTheme.typography.labelSmall, color = TextPrimary) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = CardSurface),
                            border = AssistChipDefaults.assistChipBorder(borderColor = BorderColor, enabled = true)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Formatted Schedule Banner
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("SCHEDULED FOR", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(
                                    formattedPreview,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

