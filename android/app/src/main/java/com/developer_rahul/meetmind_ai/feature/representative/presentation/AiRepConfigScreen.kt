package com.developer_rahul.meetmind_ai.feature.representative.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.core.ui.provideConfigureAiRepViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.util.ImageUtils
import com.developer_rahul.meetmind_ai.feature.representative.domain.model.PredefinedQa

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRepConfigScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ConfigureAiRepViewModel = viewModel(factory = provideConfigureAiRepViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    var step by remember { mutableIntStateOf(1) }
    val totalSteps = 3

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Representative / Proxy", style = Typography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface,
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                color = BackgroundSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (step > 1) step-- else onBack() },
                        enabled = !uiState.isSaving
                    ) {
                        Text(if (step == 1) "Cancel" else "Previous", color = TextSecondary)
                    }
                    
                    MeetMindButton(
                        text = if (step == totalSteps) "Enable AI Proxy" else "Next Step",
                        onClick = { 
                            if (step < totalSteps) {
                                step++
                            } else {
                                viewModel.saveConfiguration()
                            }
                        },
                        modifier = Modifier.width(200.dp),
                        isAiAction = step == totalSteps,
                        isLoading = uiState.isSaving
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            if (uiState.error != null) {
                Text(uiState.error!!, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(totalSteps) { i ->
                    val isCompleted = i + 1 < step
                    val isCurrent = i + 1 == step
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                if (isCompleted || isCurrent) NeonCyan else TextDisabled,
                                CircleShape
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            when (step) {
                1 -> SetupStepOne(uiState, viewModel)
                2 -> SetupStepTwo(uiState, viewModel)
                3 -> SetupStepThree(uiState)
            }
        }
    }
}

@Composable
private fun SetupStepOne(uiState: ConfigureAiRepUiState, viewModel: ConfigureAiRepViewModel) {
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = ImageUtils.uriToBase64(context, uri)
            viewModel.setAvatarPhoto(base64)
        }
    }

    val avatarBitmap = remember(uiState.avatarPhotoBase64) {
        ImageUtils.decodeBase64ToBitmap(uiState.avatarPhotoBase64)
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("AI Proxy Identity & Context", style = Typography.headlineMedium, color = TextPrimary)
        Text("Assign a meeting and optionally provide your photo for your AI Proxy.", style = Typography.bodyLarge, color = TextSecondary)
        
        Spacer(modifier = Modifier.height(24.dp))

        // Avatar Photo Upload Section
        MeetMindCard(isAiCard = uiState.avatarPhotoBase64 != null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(ElevatedSurface)
                            .border(
                                width = 2.dp,
                                color = if (avatarBitmap != null) NeonCyan else BorderColor,
                                shape = CircleShape
                            )
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap,
                                contentDescription = "AI Proxy Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = "Upload Photo",
                                tint = NeonCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Proxy Photo",
                                style = Typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusChip(
                                text = if (avatarBitmap != null) "CONFIGURED" else "OPTIONAL",
                                color = if (avatarBitmap != null) EmeraldGreen else TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Displayed in the meeting grid and animated when your AI Proxy speaks.",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Upload, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (avatarBitmap != null) "Change" else "Upload Photo", color = NeonCyan, style = Typography.labelMedium)
                            }

                            if (avatarBitmap != null) {
                                TextButton(
                                    onClick = { viewModel.setAvatarPhoto(null) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, tint = RoseRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Remove", color = RoseRed, style = Typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Select Scheduled Meeting", style = Typography.labelMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        
        uiState.upcomingMeetings.forEach { meeting ->
            val isSelected = uiState.selectedMeeting?.id == meeting.id
            MeetMindCard(
                onClick = { viewModel.selectMeeting(meeting) },
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, NeonCyan) else null
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(meeting.title, style = Typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(meeting.scheduledAt, style = Typography.bodySmall, color = TextSecondary)
                    }
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, "Selected", tint = NeonCyan)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.upcomingMeetings.isEmpty() && !uiState.isLoadingMeetings) {
            Text("No upcoming scheduled meetings found.", color = TextSecondary)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        MeetMindTextField(
            value = uiState.monitoredTopics,
            onValueChange = { viewModel.updateTopics(it) },
            label = "Topics to Monitor (comma separated)",
            leadingIcon = Icons.Default.Tag
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SetupStepTwo(uiState: ConfigureAiRepUiState, viewModel: ConfigureAiRepViewModel) {
    var newQuestion by remember { mutableStateOf("") }
    var newAnswer by remember { mutableStateOf("") }

    val quickQuestions = listOf(
        Pair("What is the current project status?", "We are currently tracking on schedule with all high-priority deliverables."),
        Pair("When is the deployment planned?", "The deployment is scheduled for end of day following final staging tests."),
        Pair("Can you take this offline?", "Yes, I will follow up directly with notes right after this call."),
        Pair("Who is handling this task?", "I am leading this initiative and can answer any technical questions.")
    )

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Predefined Voice Q&A", style = Typography.headlineMedium, color = TextPrimary)
        Text(
            "When someone asks any of these questions in the meeting, your AI Proxy will automatically speak your exact answer out loud!",
            style = Typography.bodyLarge,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Predefined Q&A Form
        MeetMindCard(isAiCard = true) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Predefined Talking Response", style = Typography.titleMedium, color = TextPrimary)
                }

                MeetMindTextField(
                    value = newQuestion,
                    onValueChange = { newQuestion = it },
                    label = "Question asked by participant",
                    leadingIcon = Icons.Default.QuestionMark
                )

                MeetMindTextField(
                    value = newAnswer,
                    onValueChange = { newAnswer = it },
                    label = "Answer for AI Proxy to speak out loud",
                    leadingIcon = Icons.Default.VolumeUp
                )

                // Quick suggestions
                Text("Quick templates:", style = Typography.labelSmall, color = TextSecondary)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickQuestions) { (q, a) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ElevatedSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.clickable {
                                newQuestion = q
                                newAnswer = a
                            }
                        ) {
                            Text(
                                text = q,
                                style = Typography.bodySmall,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (newQuestion.isNotBlank() && newAnswer.isNotBlank()) {
                            viewModel.addPredefinedQa(newQuestion, newAnswer)
                            newQuestion = ""
                            newAnswer = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    enabled = newQuestion.isNotBlank() && newAnswer.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Q&A Talking Pair", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Configured Q&A List
        if (uiState.predefinedQaList.isNotEmpty()) {
            Text("Configured Talking Responses (${uiState.predefinedQaList.size})", style = Typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            uiState.predefinedQaList.forEachIndexed { index, qa ->
                MeetMindCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HelpOutline, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(qa.question, style = Typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeUp, null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("\"${qa.answer}\"", style = Typography.bodySmall, color = TextSecondary)
                            }
                        }

                        IconButton(onClick = { viewModel.removePredefinedQa(index) }) {
                            Icon(Icons.Default.DeleteOutline, "Remove", tint = RoseRed)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        MeetMindTextField(
            value = uiState.monitoredQuestions,
            onValueChange = { viewModel.updateQuestions(it) },
            label = "Additional Monitored Questions (comma separated)",
            leadingIcon = Icons.Default.QuestionAnswer
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        MeetMindTextField(
            value = uiState.importantPeople,
            onValueChange = { viewModel.updateImportantPeople(it) },
            label = "Important Participants to Track (comma separated)",
            leadingIcon = Icons.Default.Person
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SectionHeader(title = "Ethical Disclosure")
        MeetMindCard {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Gavel, null, tint = AmberGold)
                Spacer(Modifier.width(16.dp))
                Text(
                    "The representative will be clearly identified as your AI Proxy. Impersonation of your voice or video is strictly disabled.",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SetupStepThree(uiState: ConfigureAiRepUiState) {
    val avatarBitmap = remember(uiState.avatarPhotoBase64) {
        ImageUtils.decodeBase64ToBitmap(uiState.avatarPhotoBase64)
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Review & Activate", style = Typography.headlineMedium, color = TextPrimary)
        Text("Review your AI Proxy configuration before activating the agent.", style = Typography.bodyLarge, color = TextSecondary)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        MeetMindCard(isAiCard = true) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Avatar review
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ElevatedSurface)
                            .border(2.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap,
                                contentDescription = "AI Proxy Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.SmartToy, "Default Avatar", tint = NeonCyan, modifier = Modifier.size(28.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            if (avatarBitmap != null) "Custom Photo Configured" else "Default AI Representative",
                            style = Typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (avatarBitmap != null) "Will appear as participant avatar in call" else "Will appear as MeetMind AI agent",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = BorderColor)

                ReviewItem(label = "Target Meeting", value = uiState.selectedMeeting?.title ?: "Not selected")
                ReviewItem(label = "Monitored Topics", value = if (uiState.monitoredTopics.isBlank()) "None specified" else uiState.monitoredTopics)
                ReviewItem(
                    label = "Predefined Talking Q&A Pairs",
                    value = if (uiState.predefinedQaList.isEmpty()) "None (Proxy will use autonomous LLM reasoning)" else "${uiState.predefinedQaList.size} custom Q&A pairs configured"
                )
                if (uiState.predefinedQaList.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ElevatedSurface, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.predefinedQaList.forEach { qa ->
                            Text(
                                "• Q: ${qa.question} -> Speaks: \"${qa.answer}\"",
                                style = Typography.bodySmall,
                                color = NeonCyan
                            )
                        }
                    }
                }

                ReviewItem(label = "Important Participants", value = if (uiState.importantPeople.isBlank()) "All attendees" else uiState.importantPeople)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ReviewItem(label: String, value: String) {
    Column {
        Text(label, style = Typography.labelSmall, color = TextSecondary)
        Text(value, style = Typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
