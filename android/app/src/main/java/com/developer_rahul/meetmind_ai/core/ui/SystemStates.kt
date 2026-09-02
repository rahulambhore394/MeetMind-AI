package com.developer_rahul.meetmind_ai.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.MeetMindButton

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = ElectricIndigo)
    }
}

@Composable
fun ErrorScreen(
    message: String = "Something went wrong",
    onRetry: () -> Unit
) {
    FullScreenState(
        icon = Icons.Default.ErrorOutline,
        title = "Oops!",
        description = message,
        actionLabel = "Try Again",
        onAction = onRetry,
        iconColor = RoseRed
    )
}

@Composable
fun NoInternetScreen(onRetry: () -> Unit) {
    FullScreenState(
        icon = Icons.Default.CloudOff,
        title = "No Connection",
        description = "Please check your internet connection and try again.",
        actionLabel = "Retry",
        onAction = onRetry
    )
}

@Composable
fun FullScreenState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    iconColor: androidx.compose.ui.graphics.Color = ElectricIndigo
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = iconColor
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            title,
            style = Typography.headlineMedium,
            color = TextPrimary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            description,
            style = Typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(48.dp))
            MeetMindButton(
                text = actionLabel,
                onClick = onAction
            )
        }
    }
}
