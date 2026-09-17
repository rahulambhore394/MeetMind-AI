package com.developer_rahul.meetmind_ai.feature.recording.presentation

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer_rahul.meetmind_ai.MeetMindApplication
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.core.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingPlayerScreen(
    recordingId: String,
    meetingId: String = "",
    onBack: () -> Unit,
    onViewSummary: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appContainer = remember { (context.applicationContext as MeetMindApplication).container }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var meetingTitle by remember { mutableStateOf("Meeting Recording #$recordingId") }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    // Pulsing animation for audio waves
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Load meeting details for title if available
    LaunchedEffect(meetingId) {
        val mId = meetingId.toLongOrNull()
        if (mId != null && mId > 0L) {
            when (val result = appContainer.meetingRepository.getMeeting(mId)) {
                is NetworkResult.Success -> {
                    meetingTitle = result.data.title.ifBlank { "Meeting Recording #$recordingId" }
                }
                else -> {}
            }
        }
    }

    // Load Audio File and Setup MediaPlayer
    LaunchedEffect(recordingId, meetingId) {
        isLoading = true
        errorMessage = null

        withContext(Dispatchers.IO) {
            try {
                val recIdLong = recordingId.toLongOrNull() ?: 0L
                val meetIdLong = meetingId.toLongOrNull() ?: 0L

                // 1. Check cache for locally saved recording from this session
                val cacheDir = context.cacheDir
                val candidateFiles = cacheDir.listFiles()?.filter { file ->
                    file.name.contains("recording_audio_${meetIdLong}") ||
                    file.name.contains("recording_${recIdLong}") ||
                    file.name.contains("download_rec_${recIdLong}")
                }?.sortedByDescending { it.lastModified() }

                var audioFile = candidateFiles?.firstOrNull { it.exists() && it.length() > 0 }

                // 2. If not found locally in cache, download from backend
                if (audioFile == null || audioFile.length() == 0L) {
                    val dest = File(cacheDir, "download_rec_${recIdLong}.m4a")
                    val downloadResult = appContainer.recordingRepository.downloadRecording(meetIdLong, recIdLong, dest)
                    if (downloadResult is NetworkResult.Success && dest.exists() && dest.length() > 0) {
                        audioFile = dest
                    }
                }

                if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                    val player = MediaPlayer().apply {
                        setDataSource(audioFile.absolutePath)
                        prepare()
                        setOnCompletionListener {
                            isPlaying = false
                            currentPositionMs = 0
                        }
                    }

                    withContext(Dispatchers.Main) {
                        mediaPlayer = player
                        durationMs = player.duration.coerceAtLeast(1)
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Audio recording file is currently processing or unavailable."
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RecordingPlayer", "Error preparing player", e)
                withContext(Dispatchers.Main) {
                    errorMessage = e.message ?: "Failed to load audio recording"
                    isLoading = false
                }
            }
        }
    }

    // Position Tracker Loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { player ->
                if (player.isPlaying && !isDraggingSlider) {
                    currentPositionMs = player.currentPosition
                }
            }
            delay(250)
        }
    }

    // Cleanup MediaPlayer on exit
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recording Playback", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                MeetMindButton(
                    text = "View AI Summary & Notes",
                    onClick = onViewSummary,
                    isAiAction = true
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Audio Visualizer Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = ElevatedSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ElectricIndigo.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ElectricIndigo, strokeWidth = 3.dp)
                            Spacer(Modifier.height(16.dp))
                            Text("Loading recording audio...", style = Typography.bodyMedium, color = TextSecondary)
                        }
                    } else if (errorMessage != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RoseRed, modifier = Modifier.size(44.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(errorMessage ?: "", style = Typography.bodyMedium, color = TextPrimary)
                        }
                    } else {
                        // Dynamic waveform visualization
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val barHeights = listOf(24, 48, 72, 100, 60, 85, 40, 95, 110, 65, 45, 80, 50, 30)
                            barHeights.forEachIndexed { index, height ->
                                val active = isPlaying
                                val heightFraction = if (active) {
                                    val scale = if (index % 2 == 0) pulseScale else (2f - pulseScale)
                                    (height * scale).coerceIn(12f, 130f)
                                } else {
                                    height.toFloat() * 0.4f
                                }
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(heightFraction.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (active) Brush.verticalGradient(listOf(NeonCyan, ElectricIndigo))
                                            else Brush.verticalGradient(listOf(TextDisabled, TextDisabled.copy(alpha = 0.5f)))
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Metadata
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = meetingTitle,
                    style = Typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(text = "RECORDING #$recordingId", color = ElectricIndigo)
                    StatusChip(text = "AUDIO HD", color = NeonCyan)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Seek Slider
            val effectiveDuration = if (durationMs > 0) durationMs.toFloat() else 1f
            val currentSliderValue = if (isDraggingSlider) sliderPosition else (currentPositionMs.toFloat() / effectiveDuration).coerceIn(0f, 1f)

            Slider(
                value = currentSliderValue,
                onValueChange = {
                    isDraggingSlider = true
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    val targetMs = (sliderPosition * effectiveDuration).toInt()
                    mediaPlayer?.seekTo(targetMs)
                    currentPositionMs = targetMs
                    isDraggingSlider = false
                },
                enabled = mediaPlayer != null && !isLoading,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = ElectricIndigo,
                    inactiveTrackColor = GlassWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(if (isDraggingSlider) (sliderPosition * effectiveDuration).toInt() else currentPositionMs),
                    style = Typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = formatMs(durationMs),
                    style = Typography.labelMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Playback Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Rewind 10s
                IconButton(
                    onClick = {
                        mediaPlayer?.let { player ->
                            val newPos = (player.currentPosition - 10000).coerceAtLeast(0)
                            player.seekTo(newPos)
                            currentPositionMs = newPos
                        }
                    },
                    enabled = mediaPlayer != null
                ) {
                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = TextPrimary, modifier = Modifier.size(36.dp))
                }

                // Main Play/Pause Button
                Surface(
                    onClick = {
                        mediaPlayer?.let { player ->
                            if (player.isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else {
                                player.start()
                                isPlaying = true
                            }
                        }
                    },
                    enabled = mediaPlayer != null && !isLoading,
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    color = ElectricIndigo,
                    shadowElevation = 10.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(44.dp),
                            tint = Color.White
                        )
                    }
                }

                // Forward 30s
                IconButton(
                    onClick = {
                        mediaPlayer?.let { player ->
                            val newPos = (player.currentPosition + 30000).coerceAtMost(player.duration)
                            player.seekTo(newPos)
                            currentPositionMs = newPos
                        }
                    },
                    enabled = mediaPlayer != null
                ) {
                    Icon(Icons.Default.Forward30, contentDescription = "Forward 30s", tint = TextPrimary, modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
