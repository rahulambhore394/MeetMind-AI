package com.developer_rahul.meetmind_ai.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.MeetMindButton

@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface)
    ) {
        // Aesthetic background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ElectricIndigo.copy(alpha = 0.15f),
                            Color.Transparent,
                            NeonCyan.copy(alpha = 0.05f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = ElectricIndigo.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "M",
                        style = Typography.displayLarge.copy(fontSize = 48.sp),
                        color = ElectricIndigo,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Experience the future\nof meetings",
                style = Typography.displaySmall,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black,
                lineHeight = 40.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Intelligent collaboration powered by AI. Send representatives, get insights, and bridge languages instantly.",
                style = Typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            MeetMindButton(
                text = "Sign In",
                onClick = onLogin
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassWhite),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Create Account", style = Typography.titleMedium, color = TextPrimary)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
