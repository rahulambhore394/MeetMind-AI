package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
    var scheduledDateText by remember { mutableStateOf(java.time.LocalDateTime.now().plusHours(1).toString()) }
    
    var showSuccessModal by remember { mutableStateOf(false) }
    var copyMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.creationSuccess) {
        if (uiState.creationSuccess) {
            if (isInstantMeeting && uiState.selectedMeeting != null) {
                // Instantly launch meeting or show popup
                showSuccessModal = true
            } else {
                showSuccessModal = true
            }
        }
    }

    if (showSuccessModal && uiState.selectedMeeting != null) {
        val createdMeeting = uiState.selectedMeeting!!
        val meetingCode = createdMeeting.meetingCode ?: "mm-${createdMeeting.id}"
        val meetingLink = "http://10.70.44.195:8080/join/$meetingCode"

        AlertDialog(
            onDismissRequest = {
                showSuccessModal = false
                viewModel.resetCreationState()
                onCreated()
            },
            containerColor = BackgroundSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Meeting Ready!", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text(createdMeeting.title.ifEmpty { "New Meeting" }, style = Typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    Text("Meeting Code", style = Typography.labelMedium, color = TextSecondary)
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
                            Text(meetingCode, style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
                            IconButton(onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(meetingCode))
                                copyMessage = "Meeting code copied!"
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = TextPrimary)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text("Meeting Link", style = Typography.labelMedium, color = TextSecondary)
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
                            Text(meetingLink, style = Typography.bodySmall, color = TextSecondary, maxLines = 1)
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
                        Text(copyMessage!!, style = Typography.bodySmall, color = EmeraldGreen)
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
                title = { Text("New Meeting", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
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
                        val finalTitle = title.ifBlank { if (isInstantMeeting) "Instant Meeting" else "Scheduled Meeting" }
                        val scheduledTime = if (isInstantMeeting) java.time.LocalDateTime.now().toString() else scheduledDateText
                        viewModel.createMeeting(finalTitle, description, scheduledTime, null)
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
                Text(
                    "Scheduled for 1 hour from now",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
