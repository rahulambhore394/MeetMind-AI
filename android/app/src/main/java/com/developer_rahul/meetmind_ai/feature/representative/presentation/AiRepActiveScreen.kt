package com.developer_rahul.meetmind_ai.feature.representative.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*

@Composable
fun AiRepActiveScreen(
    meetingTitle: String = "Weekly Team Planning",
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "monitoring")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            color = NeonCyan.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(2.dp, NeonCyan.copy(alpha = 0.4f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.SmartToy,
                    null,
                    modifier = Modifier.size(72.dp).pulse(color = NeonCyan),
                    tint = NeonCyan
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            "AI Representative Active",
            style = Typography.displaySmall,
            color = TextPrimary,
            fontWeight = FontWeight.Black
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            "Monitoring meeting: $meetingTitle",
            style = Typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mandatory Ethical AI Disclosure Banner
        Surface(
            color = ElectricIndigo.copy(alpha = 0.15f),
            shape = MaterialTheme.shapes.medium,
            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SmartToy, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "AUTOMATED AI PARTICIPANT — Disclosed to all participants in meeting",
                    style = Typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        MeetMindCard(isAiCard = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.GraphicEq, 
                    null, 
                    tint = NeonCyan,
                    modifier = Modifier.alpha(alpha)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "Analyzing live audio and transcription...", 
                    style = Typography.bodyMedium, 
                    color = TextPrimary,
                    modifier = Modifier.alpha(alpha)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseRed),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, RoseRed.copy(alpha = 0.3f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Close, null)
            Spacer(Modifier.width(8.dp))
            Text("Stop Representative", style = Typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
