package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.BackgroundSurface
import com.developer_rahul.meetmind_ai.core.designsystem.BorderColor
import com.developer_rahul.meetmind_ai.core.designsystem.CardSurface
import com.developer_rahul.meetmind_ai.core.designsystem.ElectricIndigo
import com.developer_rahul.meetmind_ai.core.designsystem.EmeraldGreen
import com.developer_rahul.meetmind_ai.core.designsystem.NeonCyan
import com.developer_rahul.meetmind_ai.core.designsystem.RoseRed
import com.developer_rahul.meetmind_ai.core.designsystem.TextDisabled
import com.developer_rahul.meetmind_ai.core.designsystem.TextPrimary
import com.developer_rahul.meetmind_ai.core.designsystem.TextSecondary
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    var isInstantMeeting by remember { mutableStateOf(true) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
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
            containerColor = BackgroundSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = com.developer_rahul.meetmind_ai.core.designsystem.EmeraldGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Meeting Ready!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text(createdMeeting.title.ifEmpty { "New Meeting" }, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    Text("Meeting Code", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = CardSurface,
                        shape = MaterialTheme.shapes.small,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(meetingCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
                            IconButton(onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(meetingCode))
                                copyMessage = "Meeting code copied!"
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = TextPrimary)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text("Meeting Link", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = CardSurface,
                        shape = MaterialTheme.shapes.small,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(meetingLink, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                            IconButton(onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(meetingLink))
                                copyMessage = "Meeting link copied!"
                            }) {
                                Icon(Icons.Default.Link, contentDescription = "Copy Link", tint = TextPrimary)
                            }
                        }
                    }

                    if (copyMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(copyMessage!!, style = MaterialTheme.typography.bodySmall, color = EmeraldGreen)
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "Join my MeetMind AI meeting!\nTitle: ${createdMeeting.title}\nMeeting Code: $meetingCode\nLink: $meetingLink")
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Meeting Code"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share Link & Code")
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
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Start Meeting Now 🟢", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSuccessModal = false
                    viewModel.resetCreationState()
                    onCreated()
                }) {
                    Text("Done", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Meeting", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface,
        bottomBar = {
            Box(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                MeetMindButton(
                    text = if (isInstantMeeting) "⚡ Start Instant Meeting" else "📅 Schedule Meeting",
                    onClick = {
                        if (!uiState.isCreating) {
                            val finalTitle = title.ifBlank { if (isInstantMeeting) "Instant Meeting" else "Scheduled Meeting" }
                            val scheduledTime = if (isInstantMeeting) {
                                java.time.LocalDateTime.now().toString()
                            } else {
                                java.time.LocalDateTime.of(selectedYear, selectedMonth + 1, selectedDay, selectedHour, selectedMinute).toString()
                            }
                            viewModel.createMeeting(finalTitle, description, scheduledTime, null)
                        }
                    },
                    isLoading = uiState.isCreating
                )
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
                Text(
                    text = uiState.creationError!!,
                    color = RoseRed,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            SectionHeader(title = "Meeting Type")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = isInstantMeeting,
                    onClick = { isInstantMeeting = true },
                    label = { Text("⚡ Instant Meeting") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricIndigo,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isInstantMeeting,
                    onClick = { isInstantMeeting = false },
                    label = { Text("📅 Schedule for Later") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricIndigo,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(title = "Details")
            
            MeetMindTextField(
                value = title,
                onValueChange = { title = it },
                label = "Meeting Title",
                leadingIcon = Icons.Default.Title
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            MeetMindTextField(
                value = description,
                onValueChange = { description = it },
                label = "Agenda / Description (Optional)",
                leadingIcon = Icons.Default.Description
            )

            if (!isInstantMeeting) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "Schedule Date & Time")
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp), tint = ElectricIndigo)
                        Spacer(Modifier.width(8.dp))
                        Text(String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear))
                    }

                    OutlinedButton(
                        onClick = { timePickerDialog.show() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp), tint = ElectricIndigo)
                        Spacer(Modifier.width(8.dp))
                        val amPm = if (selectedHour >= 12) "PM" else "AM"
                        val hour12 = if (selectedHour % 12 == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
                        Text(String.format("%02d:%02d %s", hour12, selectedMinute, amPm))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = ElectricIndigo.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Scheduled for: $formattedPreview",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
