package com.developer_rahul.meetmind_ai.feature.splash.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: SplashViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val layoutInfo = rememberResponsiveLayoutInfo()
    val isCompactScreen = layoutInfo.screenHeightDp < 640.dp || layoutInfo.isSmallPhone

    val scale = remember { Animatable(0.35f) }
    val alpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splashAura")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val auraRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auraRotation"
    )

    // Dynamic status text cycling
    var statusIndex by remember { mutableIntStateOf(0) }
    val statusMessages = listOf(
        "Initializing neural workspace...",
        "Securing WebRTC media channels...",
        "Preparing Live Translation...",
        "Ready to collaborate."
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
            animationSpec = tween(700)
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            if (statusIndex < statusMessages.size - 1) {
                statusIndex++
            }
        }
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
            .background(BackgroundGradient)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Multi-ring Ambient Glowing Aura
        Box(
            modifier = Modifier
                .size(if (isCompactScreen) 260.dp else 360.dp)
                .scale(glowScale)
                .rotate(auraRotation)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            ElectricIndigo.copy(alpha = 0.22f),
                            PurpleViolet.copy(alpha = 0.16f),
                            NeonCyan.copy(alpha = 0.25f),
                            ElectricIndigo.copy(alpha = 0.22f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = layoutInfo.horizontalPadding)
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // Brand Logo Orb with Multi-Layer Glow
            val logoSize = if (isCompactScreen) 84.dp else 108.dp
            val outerRingSize = if (isCompactScreen) 100.dp else 126.dp
            
            Box(contentAlignment = Alignment.Center) {
                // Pulse Ring
                Box(
                    modifier = Modifier
                        .size(outerRingSize)
                        .clip(CircleShape)
                        .background(ElectricIndigo.copy(alpha = 0.12f))
                        .pulse(color = ElectricIndigo, targetScale = 1.15f)
                )

                Surface(
                    modifier = Modifier
                        .size(logoSize)
                        .aiGlow(color = NeonCyan, radius = if (isCompactScreen) 20.dp else 30.dp),
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
                        Text(
                            text = "M",
                            style = Typography.displayLarge.copy(
                                fontSize = if (isCompactScreen) 38.sp else 52.sp
                            ),
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isCompactScreen) 20.dp else 32.dp))

            // Brand Title Row with Sparkle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = NeonCyan.copy(alpha = 0.18f),
                    modifier = Modifier.size(if (isCompactScreen) 24.dp else 28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(if (isCompactScreen) 14.dp else 18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "MEETMIND AI",
                    style = if (isCompactScreen) Typography.titleLarge else Typography.displaySmall,
                    color = TextPrimary,
                    letterSpacing = if (isCompactScreen) 3.sp else 5.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle Tagline Pill
            Surface(
                color = GlassBackground,
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "INTELLIGENT VIDEO & AUTONOMOUS AI REP",
                        style = Typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompactScreen) 16.dp else 24.dp))

            // Feature Highlights Responsive Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeatureBadge(icon = Icons.Default.Videocam, text = "WebRTC HD")
                FeatureBadge(icon = Icons.Default.SmartToy, text = "AI Rep")
                FeatureBadge(icon = Icons.Default.Translate, text = "Live Translate")
            }
        }

        // Bottom Loading & Animated Status
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isCompactScreen) 24.dp else 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = NeonCyan,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedContent(
                targetState = statusMessages[statusIndex],
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                label = "statusAnimation"
            ) { text ->
                Text(
                    text = text,
                    style = Typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FeatureBadge(icon: ImageVector, text: String) {
    Surface(
        color = CardSurface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = Typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
