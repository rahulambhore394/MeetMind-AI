package com.developer_rahul.meetmind_ai.feature.splash.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(800)
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
            .background(BackgroundSurface), // Dynamic device theme background
        contentAlignment = Alignment.Center
    ) {
        // Multi-ring Glowing Ambient Background Aura
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(glowScale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ElectricIndigo.copy(alpha = 0.25f),
                            NeonCyan.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // Glowing Hero Brand Badge Icon
            Box(contentAlignment = Alignment.Center) {
                // Outer Pulse Ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(ElectricIndigo.copy(alpha = 0.15f))
                        .pulse(color = ElectricIndigo)
                )
                
                Surface(
                    modifier = Modifier
                        .size(96.dp)
                        .aiGlow(color = NeonCyan, radius = 24.dp),
                    shape = CircleShape,
                    color = CardSurface,
                    border = androidx.compose.foundation.BorderStroke(2.dp, GlassWhite)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ElectricIndigo, Color(0xFF8B5CF6), NeonCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "M",
                                style = Typography.displayLarge.copy(fontSize = 46.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Brand Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome, 
                    contentDescription = null, 
                    tint = NeonCyan, 
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "MEETMIND AI",
                    style = Typography.headlineLarge,
                    color = TextPrimary,
                    letterSpacing = 5.sp,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Surface(
                color = ElectricIndigo.copy(alpha = 0.15f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.3f))
            ) {
                Text(
                    "INTELLIGENT MEETINGS & LIVE AI REP",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = Typography.labelMedium,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }

        // Bottom Animated Loading Indicator
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = NeonCyan,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Securing session...",
                style = Typography.labelSmall,
                color = TextDisabled
            )
        }
    }
}

