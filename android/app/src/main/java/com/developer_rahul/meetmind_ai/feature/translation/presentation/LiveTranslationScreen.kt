package com.developer_rahul.meetmind_ai.feature.translation.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.MeetMindCard
import com.developer_rahul.meetmind_ai.core.ui.provideLiveTranslationViewModelFactory
import com.developer_rahul.meetmind_ai.feature.translation.domain.model.Subtitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTranslationScreen(
    meetingId: String,
    onBack: () -> Unit,
    viewModel: LiveTranslationViewModel = viewModel(factory = provideLiveTranslationViewModelFactory(meetingId.toLongOrNull() ?: -1L))
) {
    val uiState by viewModel.uiState.collectAsState()
    val subtitles by viewModel.subtitles.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Translation", style = Typography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TextPrimary)
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
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LanguageSelector(lang = uiState.sourceLanguage)
                    Spacer(Modifier.width(16.dp))
                    Icon(Icons.Default.SwapHoriz, null, tint = ElectricIndigo)
                    Spacer(Modifier.width(16.dp))
                    LanguageSelector(lang = uiState.selectedTargetLanguage)
                }
            }

            if (subtitles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No translations yet", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(subtitles) { subtitle ->
                        TranslationItem(subtitle)
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelector(lang: String) {
    Surface(
        color = GlassWhite,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(lang, style = Typography.labelLarge, color = TextPrimary)
            Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun TranslationItem(item: Subtitle) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.speaker, style = Typography.titleSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            // Formatting timestamp for display
            Text("Now", style = Typography.labelSmall, color = TextDisabled)
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        MeetMindCard(isAiCard = true) {
            Column {
                Text(
                    text = item.originalText,
                    style = Typography.bodyMedium,
                    color = TextSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.translatedText,
                    style = Typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp
                )
            }
        }
    }
}
