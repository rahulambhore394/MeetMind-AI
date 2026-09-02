package com.developer_rahul.meetmind_ai.feature.permissions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*

@Composable
fun PermissionScreen(
    onContinue: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundSurface,
        bottomBar = {
            Box(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                MeetMindButton(
                    text = "Grant Permissions",
                    onClick = onContinue
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                "Permissions",
                style = Typography.displaySmall,
                color = TextPrimary,
                fontWeight = FontWeight.Black
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "MeetMind AI needs these to provide a seamless meeting experience.",
                style = Typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PermissionItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Camera",
                    description = "Required for video calls and identity verification.",
                    color = ElectricIndigo
                )
                
                PermissionItem(
                    icon = Icons.Default.Mic,
                    title = "Microphone",
                    description = "Essential for clear audio and live transcription.",
                    color = NeonCyan
                )
                
                PermissionItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    description = "Get alerted when AI reports and insights are ready.",
                    color = EmeraldGreen
                )
            }
        }
    }
}

@Composable
fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    color: androidx.compose.ui.graphics.Color
) {
    MeetMindCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(title, style = Typography.titleMedium, color = TextPrimary)
                Text(description, style = Typography.bodySmall, color = TextSecondary)
            }
        }
    }
}
