package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*

@Composable
fun MeetingEndedScreen(
    duration: String = "45:22",
    onViewSummary: () -> Unit,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = EmeraldGreen.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(2.dp, EmeraldGreen.copy(alpha = 0.4f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(64.dp), tint = EmeraldGreen)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "Meeting Ended",
            style = Typography.displaySmall,
            color = TextPrimary,
            fontWeight = FontWeight.Black
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            "The session has successfully concluded.",
            style = Typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        MeetMindCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Duration", style = Typography.labelSmall, color = TextSecondary)
                    Text(duration, style = Typography.titleLarge, color = TextPrimary)
                }
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(BorderColor))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AI Insights", style = Typography.labelSmall, color = TextSecondary)
                    Text("Ready", style = Typography.titleLarge, color = NeonCyan)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        MeetMindButton(
            text = "View AI Summary",
            onClick = onViewSummary,
            isAiAction = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassWhite)
        ) {
            Text("Back to Dashboard", color = TextPrimary, style = Typography.labelLarge)
        }
    }
}
