package com.developer_rahul.meetmind_ai.feature.splash.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer_rahul.meetmind_ai.core.designsystem.*

import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: SplashViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val scale = remember { Animatable(0.6f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            SplashUiState.FirstLaunch -> onNavigateToOnboarding()
            SplashUiState.Unauthenticated -> onNavigateToAuth()
            SplashUiState.Authenticated -> onNavigateToDashboard()
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface),
        contentAlignment = Alignment.Center
    ) {
        // Decorative glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        listOf(ElectricIndigo.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = ElectricIndigo.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(2.dp, ElectricIndigo)
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "MEETMIND AI",
                style = Typography.displaySmall,
                color = TextPrimary,
                letterSpacing = 6.sp,
                fontWeight = FontWeight.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Intelligent Meetings Redefined",
                style = Typography.bodyMedium,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
        }
    }
}
