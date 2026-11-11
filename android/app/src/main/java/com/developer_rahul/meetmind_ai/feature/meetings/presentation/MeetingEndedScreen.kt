package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    duration: String = "00:00",
    onViewTranscript: () -> Unit = {},
    onViewSummary: () -> Unit,
    onViewReport: () -> Unit = {},
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier.size(110.dp),
            shape = CircleShape,
            color = EmeraldGreen.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(2.dp, EmeraldGreen.copy(alpha = 0.4f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(60.dp), tint = EmeraldGreen)
            }
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        Text(
            "Meeting Concluded",
            style = Typography.displaySmall,
            color = TextPrimary,
            fontWeight = FontWeight.Black
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            "The meeting session has ended. AI processing is synthesizing comprehensive reports and saving recordings.",
            style = Typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(36.dp))
        
        MeetMindCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Duration", style = Typography.labelSmall, color = TextSecondary)
                    Text(duration, style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(BorderColor))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AI Intelligence", style = Typography.labelSmall, color = TextSecondary)
                    Text("Generated ⚡", style = Typography.titleLarge, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        MeetMindButton(
            text = "View Comprehensive Report 📊",
            onClick = onViewReport,
            isAiAction = true
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onViewTranscript,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan)
        ) {
            Icon(Icons.Default.Subtitles, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("View Meeting Transcript 📝", color = NeonCyan, style = Typography.labelLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onViewSummary,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricIndigo)
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = ElectricIndigo, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("View AI Studio Insights", color = ElectricIndigo, style = Typography.labelLarge)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite)
        ) {
            Text("Back to Dashboard", color = TextPrimary, style = Typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
