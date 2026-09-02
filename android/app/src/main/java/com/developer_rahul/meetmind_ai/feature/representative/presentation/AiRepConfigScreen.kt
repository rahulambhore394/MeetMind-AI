package com.developer_rahul.meetmind_ai.feature.representative.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.developer_rahul.meetmind_ai.core.ui.provideConfigureAiRepViewModelFactory

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
                title = { Text("AI Representative", style = Typography.titleLarge, color = TextPrimary) },
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
                        text = if (step == totalSteps) "Enable Representative" else "Next Step",
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
            
            Spacer(modifier = Modifier.height(32.dp))

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
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Mission Context", style = Typography.headlineMedium, color = TextPrimary)
        Text("Define what your representative should monitor and achieve.", style = Typography.bodyLarge, color = TextSecondary)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Select Meeting", style = Typography.labelMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        
        uiState.upcomingMeetings.forEach { meeting ->
            val isSelected = uiState.selectedMeeting?.id == meeting.id
            MeetMindCard(
                onClick = { viewModel.selectMeeting(meeting) },
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, NeonCyan) else null
            ) {
                Column {
                    Text(meeting.title, style = Typography.bodyLarge, color = TextPrimary)
                    Text(meeting.scheduledAt, style = Typography.bodySmall, color = TextSecondary)
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
    }
}

@Composable
private fun SetupStepTwo(uiState: ConfigureAiRepUiState, viewModel: ConfigureAiRepViewModel) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Questions & People", style = Typography.headlineMedium, color = TextPrimary)
        Text("What questions should be answered? Who is important?", style = Typography.bodyLarge, color = TextSecondary)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        MeetMindTextField(
            value = uiState.monitoredQuestions,
            onValueChange = { viewModel.updateQuestions(it) },
            label = "Monitored Questions (comma separated)",
            leadingIcon = Icons.Default.QuestionAnswer
        )
        
        Spacer(modifier = Modifier.height(20.dp))

        MeetMindTextField(
            value = uiState.importantPeople,
            onValueChange = { viewModel.updateImportantPeople(it) },
            label = "Important Participants (comma separated)",
            leadingIcon = Icons.Default.Person
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        SectionHeader(title = "Ethical Disclosure")
        MeetMindCard {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Gavel, null, tint = AmberGold)
                Spacer(Modifier.width(16.dp))
                Text(
                    "The representative will be clearly identified as an automated agent. Impersonation of your voice or video is strictly disabled.",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SetupStepThree(uiState: ConfigureAiRepUiState) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Review & Activate", style = Typography.headlineMedium, color = TextPrimary)
        Text("Review your configuration before activating the AI agent.", style = Typography.bodyLarge, color = TextSecondary)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        MeetMindCard(isAiCard = true) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ReviewItem(label = "Meeting", value = uiState.selectedMeeting?.title ?: "Not selected")
                ReviewItem(label = "Topics", value = if (uiState.monitoredTopics.length == 0) "None" else uiState.monitoredTopics)
                ReviewItem(label = "Questions", value = if (uiState.monitoredQuestions.length == 0) "None" else uiState.monitoredQuestions)
                ReviewItem(label = "Important People", value = if (uiState.importantPeople.length == 0) "None" else uiState.importantPeople)
            }
        }
    }
}

@Composable
private fun ReviewItem(label: String, value: String) {
    Column {
        Text(label, style = Typography.labelSmall, color = TextSecondary)
        Text(value, style = Typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
