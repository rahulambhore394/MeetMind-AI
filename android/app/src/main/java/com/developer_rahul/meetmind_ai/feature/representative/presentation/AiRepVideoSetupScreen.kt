package com.developer_rahul.meetmind_ai.feature.representative.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRepVideoSetupScreen(
    onBack: () -> Unit,
    onRecord: () -> Unit,
    onUpload: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identity Verification", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Surface(
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                color = ElectricIndigo.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(2.dp, ElectricIndigo.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Face, null, modifier = Modifier.size(72.dp), tint = ElectricIndigo)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(EmeraldGreen, CircleShape)
                            .border(2.dp, BackgroundSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                "Approve Your Representative",
                style = Typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "To maintain ethical standards, your AI Representative requires a pre-recorded identity approval. This video confirms you have authorized this agent to represent you.",
                style = Typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MeetMindButton(
                    text = "Record Video Approval",
                    onClick = onRecord
                )
                
                OutlinedButton(
                    onClick = onUpload,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassWhite),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Upload Existing Approved Media", color = TextPrimary, style = Typography.labelLarge)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            MeetMindCard {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Shield, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "Your video will only be used for participant disclosure and identity verification. We never store biometric data.",
                        style = Typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
