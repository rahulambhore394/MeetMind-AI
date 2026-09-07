package com.developer_rahul.meetmind_ai.feature.permissions.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val infiniteTransition = rememberInfiniteTransition(label = "shieldGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    Scaffold(
        containerColor = BackgroundSurface,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MeetMindButton(
                    text = "Grant & Continue 🚀",
                    onClick = onContinue,
                    isAiAction = true,
                    modifier = Modifier.bounceClick()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(onClick = onContinue) {
                    Text("Skip for now", color = TextSecondary, style = Typography.labelMedium)
                }
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
            Spacer(modifier = Modifier.height(20.dp))

            // Security Privacy Header Badge
            Surface(
                color = ElectricIndigo.copy(alpha = 0.15f),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "PRIVACY & ACCESS CONTROL",
                        style = Typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hero Shield Badge Icon
            Box(contentAlignment = Alignment.Center) {
                // Outer Pulse Ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(glowScale)
                        .clip(CircleShape)
                        .background(ElectricIndigo.copy(alpha = 0.12f))
                        .pulse(color = ElectricIndigo)
                )

                Surface(
                    modifier = Modifier
                        .size(84.dp)
                        .aiGlow(color = NeonCyan, radius = 20.dp),
                    shape = CircleShape,
                    color = CardSurface,
                    border = androidx.compose.foundation.BorderStroke(2.dp, GlassWhite)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ElectricIndigo, PurpleViolet, NeonCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "App Permissions",
                style = Typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "MeetMind AI requires access to your hardware to enable HD WebRTC video, live transcription, and smart AI alerts.",
                style = Typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Permission Items List
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PermissionItemCard(
                    icon = Icons.Default.CameraAlt,
                    title = "Camera Access",
                    description = "Required for HD video calls, PIP preview, and AI representative avatar creation.",
                    badgeText = "REQUIRED FOR VIDEO",
                    color = ElectricIndigo
                )

                PermissionItemCard(
                    icon = Icons.Default.Mic,
                    title = "Microphone Access",
                    description = "Essential for clear audio transmission and real-time AI transcription.",
                    badgeText = "REQUIRED FOR AUDIO",
                    color = NeonCyan
                )

                PermissionItemCard(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    description = "Receive instant alerts when AI reports and meeting summaries are ready.",
                    badgeText = "MEETING ALERTS",
                    color = EmeraldGreen
                )

                PermissionItemCard(
                    icon = Icons.AutoMirrored.Filled.ScreenShare,
                    title = "Screen Sharing",
                    description = "Present your screen seamlessly to meeting participants.",
                    badgeText = "COLLABORATION",
                    color = PurpleViolet
                )
            }
        }
    }
}

@Composable
private fun PermissionItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    badgeText: String,
    color: Color
) {
    MeetMindCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = MaterialTheme.shapes.medium,
                color = color.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(title, style = Typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                    
                    Surface(
                        color = color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = badgeText,
                            style = Typography.labelSmall.copy(fontSize = 9.sp),
                            color = color,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(description, style = Typography.bodySmall, color = TextSecondary, lineHeight = 16.sp)
            }
        }
    }
}

