package com.developer_rahul.meetmind_ai.feature.onboarding.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "AI-Powered Meetings",
            description = "Experience a new era of collaboration with high-fidelity video and intelligent tools.",
            icon = Icons.Default.AutoAwesome,
            color = ElectricIndigo
        ),
        OnboardingPage(
            title = "Live Transcription",
            description = "Real-time transcription and translation to break language barriers instantly.",
            icon = Icons.Default.Translate,
            color = NeonCyan
        ),
        OnboardingPage(
            title = "Meeting Intelligence",
            description = "Get AI-generated summaries, action items, and key decisions automatically.",
            icon = Icons.Default.Insights,
            color = EmeraldGreen
        ),
        OnboardingPage(
            title = "AI Representative",
            description = "Send your automated representative to meetings when you can't attend.",
            icon = Icons.Default.SmartToy,
            color = AmberGold
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = BackgroundSurface,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) ElectricIndigo else GlassWhite
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(if (pagerState.currentPage == iteration) 24.dp else 8.dp, 8.dp)
                                .background(color, CircleShape)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (pagerState.currentPage < pages.size - 1) {
                        TextButton(
                            onClick = onFinish,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Skip", color = TextSecondary, style = Typography.labelLarge)
                        }
                    }
                    
                    MeetMindButton(
                        text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Continue",
                        onClick = {
                            if (pagerState.currentPage < pages.size - 1) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                onFinish()
                            }
                        },
                        modifier = Modifier.weight(2f),
                        isAiAction = pagerState.currentPage == pages.size - 1
                    )
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
            OnboardingPageContent(pages[pageIndex])
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(220.dp),
            shape = CircleShape,
            color = page.color.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(1.dp, page.color.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Background glow
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(Brush.radialGradient(listOf(page.color.copy(alpha = 0.2f), Color.Transparent)))
                )
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(90.dp),
                    tint = page.color
                )
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = page.title,
            style = Typography.displaySmall,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = Typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)
