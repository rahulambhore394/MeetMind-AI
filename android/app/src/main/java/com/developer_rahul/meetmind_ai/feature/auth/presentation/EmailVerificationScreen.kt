package com.developer_rahul.meetmind_ai.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
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
fun EmailVerificationScreen(
    onContinue: () -> Unit,
    onResend: () -> Unit
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
            color = ElectricIndigo.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(2.dp, ElectricIndigo.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = ElectricIndigo
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            "Verify Your Email",
            style = Typography.displaySmall,
            color = TextPrimary,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "We've sent a secure link to your email. Click the link to complete your registration and unlock AI features.",
            style = Typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )

        Spacer(modifier = Modifier.height(64.dp))

        MeetMindButton(
            text = "Continue to App",
            onClick = onContinue
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onResend) {
            Text("Resend Verification Email", color = NeonCyan, style = Typography.labelLarge)
        }
    }
}
