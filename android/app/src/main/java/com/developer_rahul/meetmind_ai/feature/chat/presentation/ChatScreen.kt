package com.developer_rahul.meetmind_ai.feature.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.MeetMindTextField

import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.MeetMindTextField
import com.developer_rahul.meetmind_ai.feature.chat.domain.model.ChatMessage

import com.developer_rahul.meetmind_ai.core.ui.provideChatViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    meetingId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(factory = provideChatViewModelFactory(meetingId.toLongOrNull() ?: -1L))
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Meeting Chat", style = Typography.titleLarge, color = TextPrimary)
                        Text("Real-time communication", style = Typography.labelSmall, color = EmeraldGreen)
                    }
                },
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
            Surface(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
                color = BackgroundSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MeetMindTextField(
                        value = uiState.pendingMessage,
                        onValueChange = viewModel::onMessageChange,
                        label = "Message...",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        onClick = viewModel::sendMessage,
                        enabled = uiState.pendingMessage.isNotBlank(),
                        shape = CircleShape,
                        color = if (uiState.pendingMessage.isNotBlank()) ElectricIndigo else GlassWhite,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading && uiState.messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricIndigo)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                reverseLayout = true
            ) {
                if (uiState.error != null) {
                    item {
                        Text(uiState.error!!, color = RoseRed, style = Typography.labelSmall)
                    }
                }

                items(uiState.messages, key = { it.id }) { chatMessage ->
                    ChatBubble(chatMessage)
                }
                
                if (uiState.messages.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No messages yet. Say hello!", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isMe = message.isMe
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = Color.DarkGray
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(message.senderName.take(1), style = Typography.labelSmall, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (!isMe) {
                Text(message.senderName, style = Typography.labelSmall, color = TextSecondary, modifier = Modifier.padding(start = 4.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = if (isMe) ElectricIndigo else CardSurface,
                shape = MaterialTheme.shapes.medium,
                border = if (isMe) null else androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = if (isMe) Color.White else TextPrimary,
                    style = Typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(message.timestamp.takeLast(5), style = Typography.labelSmall, color = TextDisabled, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}
