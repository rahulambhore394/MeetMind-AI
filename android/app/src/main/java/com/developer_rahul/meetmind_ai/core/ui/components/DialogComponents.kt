package com.developer_rahul.meetmind_ai.core.ui.components

import androidx.compose.foundation.layout.*
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
    var meetingId by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Meeting", style = Typography.titleLarge, color = TextPrimary) },
        text = {
            Column {
                Text("Enter the Meeting ID provided by the host.", style = Typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                MeetMindTextField(
                    value = meetingId,
                    onValueChange = { meetingId = it },
                    label = "Meeting ID"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onJoin(meetingId) },
                enabled = meetingId.isNotBlank()
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
