package com.developer_rahul.meetmind_ai.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer_rahul.meetmind_ai.core.designsystem.*

@Composable
fun MeetMindAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    text: String,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    confirmButtonColor: Color = ElectricIndigo
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title, style = Typography.titleLarge, color = TextPrimary) },
        text = { Text(text, style = Typography.bodyMedium, color = TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = confirmButtonColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(dismissLabel, color = TextSecondary)
            }
        },
        containerColor = CardSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}

@Composable
fun LeaveMeetingDialog(
    onDismiss: () -> Unit,
    onLeave: () -> Unit
) {
    MeetMindAlertDialog(
        onDismissRequest = onDismiss,
        onConfirm = onLeave,
        title = "Leave Meeting",
        text = "Are you sure you want to leave the meeting? You can join again later from the dashboard.",
        confirmLabel = "Leave",
        confirmButtonColor = RoseRed
    )
}

@Composable
fun HostLeaveOrEndDialog(
    onDismiss: () -> Unit,
    onLeave: () -> Unit,
    onEndForEveryone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = ElectricIndigo)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Leave or End Meeting?", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                "You are the host of this meeting. You can leave temporarily and rejoin later, or end the meeting for all participants.",
                style = Typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onLeave,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("1. Leave Meeting (Rejoin Later)", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onEndForEveryone,
                    border = androidx.compose.foundation.BorderStroke(1.dp, RoseRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CallEnd, null, modifier = Modifier.size(18.dp), tint = RoseRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("2. End Meeting for All", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        },
        containerColor = CardSurface
    )
}

@Composable
fun RecordingConsentDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    MeetMindAlertDialog(
        onDismissRequest = onDismiss,
        onConfirm = onConfirm,
        title = "Record Meeting",
        text = "By starting a recording, you consent to capturing the audio and video of this meeting. Participants will be notified that the meeting is being recorded. This recording will be processed by MeetMind AI to generate summaries and insights.",
        confirmLabel = "Start Recording",
        confirmButtonColor = ElectricIndigo
    )
}

@Composable
fun JoinMeetingDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var meetingIdOrCode by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Meeting", style = Typography.titleLarge, color = TextPrimary) },
        text = {
            Column {
                Text("Enter the Meeting Code or ID provided by the host (e.g. mm-100-a1b2).", style = Typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                MeetMindTextField(
                    value = meetingIdOrCode,
                    onValueChange = { meetingIdOrCode = it },
                    label = "Meeting Code / ID"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onJoin(meetingIdOrCode.trim()) },
                enabled = meetingIdOrCode.isNotBlank()
            ) {
                Text("Join", color = ElectricIndigo, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardSurface
    )
}
