package com.developer_rahul.meetmind_ai.feature.transcript.presentation

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.provideTranscriptViewModelFactory
import com.developer_rahul.meetmind_ai.feature.transcript.domain.model.TranscriptSegment
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptScreen(
    meetingId: String,
    onBack: () -> Unit,
    viewModel: TranscriptViewModel = viewModel(
        key = "TranscriptScreen_$meetingId",
        factory = provideTranscriptViewModelFactory(meetingId.toLongOrNull() ?: -1L)
    )
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    var autoScrollEnabled by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.filteredSegments.size) {
        if (autoScrollEnabled && uiState.filteredSegments.isNotEmpty()) {
            listState.animateScrollToItem(uiState.filteredSegments.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSearchActive) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearch(it) },
                            placeholder = { Text("Search transcript...", color = TextDisabled) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = ElectricIndigo
                            ),
                            singleLine = true
                        )
                    } else {
                        Column {
                            Text("Meeting Transcript", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) 
                            Text("Live Multilingual Speech-to-Text", style = Typography.labelSmall, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (isSearchActive) { isSearchActive = false; viewModel.onSearch("") } else onBack() }) {
                        Icon(if (isSearchActive) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    if (isSearchActive) {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearch("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextPrimary)
                            }
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                        }
                        IconButton(
                            onClick = {
                                val text = viewModel.getFormattedTranscriptText()
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "Transcript copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy all", tint = TextPrimary)
                        }
                        IconButton(
                            onClick = {
                                val text = viewModel.getFormattedTranscriptText()
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    putExtra(Intent.EXTRA_SUBJECT, "Meeting Transcript")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Meeting Transcript"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary)
                        }
                        IconButton(
                            onClick = {
                                viewModel.exportFromServer(
                                    onSuccess = { serverText ->
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, serverText)
                                            putExtra(Intent.EXTRA_SUBJECT, "Official Meeting Transcript")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Export Official Transcript"))
                                    },
                                    onError = { _ ->
                                        val text = viewModel.getFormattedTranscriptText()
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, text)
                                            putExtra(Intent.EXTRA_SUBJECT, "Meeting Transcript")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Export Transcript"))
                                    }
                                )
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download Export", tint = NeonCyan)
                        }
                        IconButton(onClick = { viewModel.loadTranscript() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Preferred Language selector bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CardSurface,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "TARGET LANGUAGE:", 
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), 
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.availableLanguages.forEach { lang ->
                                val isSelected = uiState.selectedLanguage == lang.code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectLanguage(lang.code) },
                                    label = { 
                                        Text(
                                            lang.displayName, 
                                            style = Typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ) 
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricIndigo,
                                        selectedLabelColor = Color.White,
                                        containerColor = BackgroundSurface,
                                        labelColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }

                // Live status banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BackgroundSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isActive = uiState.transcript?.status != "COMPLETED"
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isActive) EmeraldGreen else TextDisabled, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            if (isActive) "LIVE TRANSCRIPTION STREAMING" else "TRANSCRIPTION COMPLETED", 
                            style = Typography.labelLarge, 
                            color = if (isActive) EmeraldGreen else TextSecondary, 
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        val count = uiState.filteredSegments.size
                        Text(
                            "$count utterance${if (count == 1) "" else "s"}",
                            style = Typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricIndigo)
                    }
                } else if (uiState.error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.error!!, color = RoseRed)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadTranscript() }) {
                                Text("Retry")
                            }
                        }
                    }
                } else if (uiState.isEmpty) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                color = ElectricIndigo.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.3f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Subtitles, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(36.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No transcript available yet", color = TextPrimary, style = Typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Speak during the live meeting to see live spoken utterances appear here in real time.", style = Typography.bodySmall, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.filteredSegments, key = { it.id }) { segment ->
                            TranscriptSegmentCard(
                                segment = segment,
                                searchQuery = uiState.searchQuery,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString("[${segment.time}] ${segment.speaker}: ${segment.text}"))
                                    Toast.makeText(context, "Segment copied", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // Floating Auto-Scroll Toggle Button
            if (uiState.filteredSegments.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = if (autoScrollEnabled) ElectricIndigo else CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (autoScrollEnabled) ElectricIndigo else BorderColor),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                autoScrollEnabled = !autoScrollEnabled
                                if (autoScrollEnabled && uiState.filteredSegments.isNotEmpty()) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(uiState.filteredSegments.size - 1)
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (autoScrollEnabled) Icons.Default.VerticalAlignBottom else Icons.Default.Pause,
                            contentDescription = null,
                            tint = if (autoScrollEnabled) Color.White else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (autoScrollEnabled) "Auto-scroll ON" else "Auto-scroll PAUSED",
                            style = Typography.labelSmall,
                            color = if (autoScrollEnabled) Color.White else TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TranscriptSegmentCard(
    segment: TranscriptSegment,
    searchQuery: String = "",
    onCopy: () -> Unit = {}
) {
    val speakerColor = getSpeakerColor(segment.speaker)
    val initials = getSpeakerInitials(segment.speaker)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speaker Avatar Badge
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = speakerColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, speakerColor.copy(alpha = 0.6f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            initials,
                            style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                            color = speakerColor
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    segment.speaker, 
                    style = Typography.titleSmall, 
                    color = speakerColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = BackgroundSurface,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Text(
                        segment.time, 
                        style = Typography.labelSmall, 
                        color = TextDisabled,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextDisabled, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            val annotatedText = remember(segment.text, searchQuery) {
                if (searchQuery.isBlank() || !segment.text.contains(searchQuery, ignoreCase = true)) {
                    AnnotatedString(segment.text)
                } else {
                    buildAnnotatedString {
                        val full = segment.text
                        var startIndex = 0
                        while (startIndex < full.length) {
                            val matchIndex = full.indexOf(searchQuery, startIndex, ignoreCase = true)
                            if (matchIndex == -1) {
                                append(full.substring(startIndex))
                                break
                            }
                            append(full.substring(startIndex, matchIndex))
                            pushStyle(SpanStyle(color = NeonCyan, fontWeight = FontWeight.Black, background = NeonCyan.copy(alpha = 0.15f)))
                            append(full.substring(matchIndex, matchIndex + searchQuery.length))
                            pop()
                            startIndex = matchIndex + searchQuery.length
                        }
                    }
                }
            }

            Text(
                text = annotatedText,
                style = Typography.bodyLarge,
                color = TextPrimary,
                lineHeight = 22.sp
            )
        }
    }
}

private fun getSpeakerColor(speaker: String): Color {
    val clean = speaker.trim().lowercase()
    return when {
        clean == "you" || clean.contains("me") -> EmeraldGreen
        clean.contains("ai") || clean.contains("assistant") || clean.contains("rep") -> NeonCyan
        clean.contains("host") -> PurpleViolet
        else -> {
            val colors = listOf(ElectricIndigo, AmberGold, RoseRed, PurpleViolet, EmeraldGreen)
            val hash = Math.abs(speaker.hashCode())
            colors[hash % colors.size]
        }
    }
}

private fun getSpeakerInitials(speaker: String): String {
    val words = speaker.trim().split(" ")
    return when {
        words.isEmpty() || words[0].isBlank() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "${words[0].first()}${words[1].first()}".uppercase()
    }
}
