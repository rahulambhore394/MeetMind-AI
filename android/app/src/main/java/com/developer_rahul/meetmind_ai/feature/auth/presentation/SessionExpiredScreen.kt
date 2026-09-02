package com.developer_rahul.meetmind_ai.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*

@Composable
fun SessionExpiredScreen(
    onSignInAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = RoseRed.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = RoseRed
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Session Expired",
            style = Typography.displaySmall,
            color = TextPrimary,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "For your security, you've been signed out due to inactivity. Please sign in again to access your meetings.",
            style = Typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        MeetMindButton(
            text = "Sign In Again",
            onClick = onSignInAgain
        )
    }
}
