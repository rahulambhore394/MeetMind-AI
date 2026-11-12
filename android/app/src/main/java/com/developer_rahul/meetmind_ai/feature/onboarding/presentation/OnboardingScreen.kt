package com.developer_rahul.meetmind_ai.feature.onboarding.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = ViewModelFactory)
) {
    val layoutInfo = rememberResponsiveLayoutInfo()
    val isCompactScreen = layoutInfo.screenHeightDp < 640.dp || layoutInfo.isSmallPhone
    val scope = rememberCoroutineScope()

    val pages = remember {
        listOf(
            OnboardingPageData(
                title = "Ultra-HD Video Meetings",
                subtitle = "Sub-second WebRTC low latency with intelligent active-speaker highlighting and encrypted peer connections.",
                badgeText = "WEBRTC ENGINE",
                accentColor = ElectricIndigo,
                type = OnboardingType.WEBRTC
            ),
            OnboardingPageData(
                title = "Live Speech Translation",
                subtitle = "Break language barriers in real time. Speak English, hear Marathi — with instant voice synthesis & bilingual subtitles.",
                badgeText = "SIMULTANEOUS AI TRANSLATION",
                accentColor = NeonCyan,
                type = OnboardingType.TRANSLATION
            ),
            OnboardingPageData(
                title = "Gemini Meeting Intelligence",
                subtitle = "Autonomous executive briefing, key decisions, and prioritized action items synthesized in seconds after every call.",
                badgeText = "POWERED BY GEMINI 3.5 FLASH",
                accentColor = EmeraldGreen,
                type = OnboardingType.INTELLIGENCE
            ),
            OnboardingPageData(
                title = "Autonomous AI Representative",
                subtitle = "Busy or double-booked? Send your custom AI representative to attend, interact, take notes, and brief you afterward.",
                badgeText = "AUTONOMOUS PROXY",
                accentColor = PurpleViolet,
                type = OnboardingType.AI_REP
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    // Auto-advance timer (pauses if user is dragging)
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage < pages.size - 1) {
            delay(5500)
            if (!pagerState.isScrollInProgress && pagerState.currentPage < pages.size - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    val handleFinish = {
        viewModel.completeOnboarding()
        onFinish()
    }

    Scaffold(
        containerColor = BackgroundSurface,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = layoutInfo.horizontalPadding, vertical = if (isCompactScreen) 12.dp else 20.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Smooth Animated Pill Indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        val pillWidth by animateDpAsState(
                            targetValue = if (isSelected) 32.dp else 8.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "pillWidth"
                        )
                        val currentColor = pages[pagerState.currentPage].accentColor

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(pillWidth)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(currentColor, NeonCyan)
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(GlassWhite, GlassWhite)
                                        )
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompactScreen) 14.dp else 22.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage < pages.size - 1) {
                        TextButton(
                            onClick = handleFinish,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .bounceClick()
                        ) {
                            Text(
                                "Skip",
                                color = TextSecondary,
                                style = Typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (pagerState.currentPage < pages.size - 1) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                handleFinish()
                            }
                        },
                        modifier = Modifier
                            .weight(if (pagerState.currentPage < pages.size - 1) 2.2f else 1f)
                            .height(52.dp)
                            .bounceClick(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pages[pagerState.currentPage].accentColor
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                if (pagerState.currentPage == pages.size - 1) "Get Started 🚀" else "Continue",
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (pagerState.currentPage < pages.size - 1) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { pageIndex ->
            OnboardingPageContent(
                data = pages[pageIndex],
                isCompact = isCompactScreen,
                horizontalPadding = layoutInfo.horizontalPadding
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(
    data: OnboardingPageData,
    isCompact: Boolean,
    horizontalPadding: androidx.compose.ui.unit.Dp
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = horizontalPadding, vertical = if (isCompact) 12.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Tag badge pill
        Surface(
            color = data.accentColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(100.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, data.accentColor.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = data.accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = data.badgeText,
                    style = Typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = data.accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 28.dp))

        // Interactive Visual Mockup Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (isCompact) 210.dp else 260.dp),
            contentAlignment = Alignment.Center
        ) {
            when (data.type) {
                OnboardingType.WEBRTC -> WebRtcMockupCard(isCompact)
                OnboardingType.TRANSLATION -> TranslationMockupCard(isCompact)
                OnboardingType.INTELLIGENCE -> IntelligenceMockupCard(isCompact)
                OnboardingType.AI_REP -> AiRepMockupCard(isCompact)
            }
        }

        Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 36.dp))

        // Title
        Text(
            text = data.title,
            style = if (isCompact) Typography.titleLarge.copy(fontSize = 22.sp) else Typography.displaySmall.copy(fontSize = 28.sp),
            color = TextPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitle
        Text(
            text = data.subtitle,
            style = if (isCompact) Typography.bodyMedium else Typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = if (isCompact) 20.sp else 26.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

// -------------------------------------------------------------
// Interactive Feature Visual Mockup Cards
// -------------------------------------------------------------

@Composable
private fun WebRtcMockupCard(isCompact: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .aiGlow(color = ElectricIndigo, radius = 24.dp),
        shape = RoundedCornerShape(24.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Meeting Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RoseRed)
                            .pulse(color = RoseRed, targetScale = 1.3f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "LIVE ROOM #301",
                        style = Typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "1080p • 60fps",
                        style = Typography.labelSmall,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Video Grid Mockup (2 tiles)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tile 1: Active Speaker
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isCompact) 100.dp else 125.dp)
                        .border(2.dp, ElectricIndigo, RoundedCornerShape(16.dp)),
                    color = BackgroundSurface,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = ElectricIndigo.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("R", color = ElectricIndigo, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Rahul (Host)",
                                style = Typography.labelSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Speaking indicator pill
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp),
                            color = ElectricIndigo,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Speaking", style = Typography.labelSmall.copy(fontSize = 9.sp), color = Color.White)
                            }
                        }
                    }
                }

                // Tile 2: Remote Participant
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isCompact) 100.dp else 125.dp),
                    color = BackgroundSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = PurpleViolet.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("A", color = PurpleViolet, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Ananya (Pune)",
                                style = Typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Toolbar Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlIcon(Icons.Default.Mic, ElectricIndigo)
                ControlIcon(Icons.Default.Videocam, ElectricIndigo)
                ControlIcon(Icons.Default.Translate, NeonCyan)
                ControlIcon(Icons.Default.CallEnd, RoseRed)
            }
        }
    }
}

@Composable
private fun TranslationMockupCard(isCompact: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .aiGlow(color = NeonCyan, radius = 24.dp),
        shape = RoundedCornerShape(24.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "LIVE DUAL-AUDIO TRANSLATION",
                        style = Typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = NeonCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "EN ⇄ MR",
                        style = Typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bubble 1: English Speaker
            Surface(
                color = BackgroundSurface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🇺🇸 Rahul (English)", style = Typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.GraphicEq, null, tint = ElectricIndigo, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "\"Let's review the sprint deployment plan.\"",
                        style = Typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bubble 2: Marathi Translation & TTS
            Surface(
                color = NeonCyan.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, NeonCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🇮🇳 Ananya hears (मराठी AI TTS)", style = Typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(shape = CircleShape, color = NeonCyan) {
                            Icon(Icons.Default.VolumeUp, null, tint = Color.Black, modifier = Modifier.size(14.dp).padding(2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "\"चला स्प्रिंट उपयोजन योजनेचा आढावा घेऊया.\"",
                        style = Typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun IntelligenceMockupCard(isCompact: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .aiGlow(color = EmeraldGreen, radius = 24.dp),
        shape = RoundedCornerShape(24.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "GEMINI EXECUTIVE BRIEF",
                        style = Typography.labelSmall,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Instant AI Summary",
                        style = Typography.labelSmall,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Decision Item
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Key Decision", style = Typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text("Upgraded to Gemini 3.5 Flash for <1.5s live inference.", style = Typography.bodySmall, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Item
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Assignment, null, tint = AmberGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Action Item (Assigned to @Rahul)", style = Typography.labelSmall, color = AmberGold, fontWeight = FontWeight.Bold)
                    Text("Configure STOMP translation channels on mobile client.", style = Typography.bodySmall, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sentiment Pill
            Surface(
                color = BackgroundSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Meeting Sentiment", style = Typography.labelSmall, color = TextSecondary)
                    Text("98% Positive & Aligned ✨", style = Typography.labelSmall, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AiRepMockupCard(isCompact: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .aiGlow(color = PurpleViolet, radius = 24.dp),
        shape = RoundedCornerShape(24.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, null, tint = PurpleViolet, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "AI REPRESENTATIVE PROXY",
                        style = Typography.labelSmall,
                        color = PurpleViolet,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = PurpleViolet.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Autonomous Mode",
                        style = Typography.labelSmall,
                        color = PurpleViolet,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Persona & Status Box
            Surface(
                color = BackgroundSurface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PurpleViolet.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Psychology, null, tint = PurpleViolet, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Rahul's Technical Proxy", style = Typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Attending Client Architecture Review", style = Typography.bodySmall, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Autonomous Log
            Surface(
                color = PurpleViolet.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PurpleViolet.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.RecordVoiceOver, null, tint = PurpleViolet, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "\"Rahul approved the database migration offline. I will record Q&A for him.\"",
                        style = Typography.bodySmall,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlIcon(icon: ImageVector, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.16f),
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
    }
}

enum class OnboardingType {
    WEBRTC,
    TRANSLATION,
    INTELLIGENCE,
    AI_REP
}

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val accentColor: Color,
    val type: OnboardingType
)
