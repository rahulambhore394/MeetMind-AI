package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.MeetMindApplication
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.media.audio.AudioDeviceManager
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.meetingroom.presentation.AvatarPlaceholder
import org.webrtc.VideoTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreJoinMeetingScreen(
    meetingId: String,
    onBack: () -> Unit,
    onJoin: () -> Unit,
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory)
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val meeting = uiState.selectedMeeting

    val webRtcManager = remember { MeetMindApplication.instance.container.webRtcManager }
    var localVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true && permissions[Manifest.permission.RECORD_AUDIO] == true) {
            try {
                webRtcManager.createLocalStream()
                localVideoTrack = webRtcManager.getLocalVideoTrack()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasCamera || !hasAudio) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        } else {
            try {
                webRtcManager.createLocalStream()
                localVideoTrack = webRtcManager.getLocalVideoTrack()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(meetingId) {
        viewModel.loadMeetingDetails(meetingId)
    }

    var isMicOn by remember { mutableStateOf(true) }
    var isCameraOn by remember { mutableStateOf(true) }
    var isFrontCamera by remember { mutableStateOf(true) }

    val availableAudioDevices = remember(context) { AudioDeviceManager.getAvailableAudioDevices(context) }
    var selectedAudioDevice by remember { mutableStateOf(availableAudioDevices.firstOrNull() ?: "Speaker") }

    LaunchedEffect(selectedAudioDevice) {
        AudioDeviceManager.selectAudioDevice(context, selectedAudioDevice)
    }

    val meetingTitle = meeting?.title ?: "Live AI Meeting"
    val meetingCode = meeting?.meetingCode ?: "mm-${meetingId}"
    val participantCount = uiState.participants.size.coerceAtLeast(1)

    // Pulse animation for mic audio meter check
    val infiniteTransition = rememberInfiniteTransition(label = "micMeter")
    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pre-Join Setup", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.bounceClick()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = ElectricIndigo.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = meetingCode,
                            style = Typography.labelMedium,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
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
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                val isRejoin = meeting?.hasJoinedBefore == true || meeting?.userParticipantStatus == "LEFT"
                val joinButtonText = if (isRejoin) "Re-join Meeting Now 🚀" else "Join Meeting Now 🚀"
                MeetMindButton(
                    text = joinButtonText,
                    onClick = onJoin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aiGlow(color = ElectricIndigo, radius = 16.dp)
                        .bounceClick(),
                    isAiAction = true
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Video Preview Glass Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f),
                shape = RoundedCornerShape(28.dp),
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp, 
                    Brush.linearGradient(listOf(GlassWhite, ElectricIndigo.copy(alpha = 0.4f), BorderColor))
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCameraOn) {
                        val currentTrack = localVideoTrack ?: webRtcManager.getLocalVideoTrack()
                        if (currentTrack != null) {
                            VideoRenderer(
                                modifier = Modifier.fillMaxSize(),
                                videoTrack = currentTrack
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                AvatarPlaceholder(name = "You", isActiveSpeaker = isMicOn, sizeDp = 88)
                            }
                        }

                        // Camera Flip Button
                        IconButton(
                            onClick = {
                                isFrontCamera = !isFrontCamera
                                webRtcManager.switchCamera()
                                Toast.makeText(context, if (isFrontCamera) "Front Camera Selected" else "Rear Camera Selected", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .bounceClick()
                        ) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(RoseRed.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VideocamOff, contentDescription = null, tint = RoseRed, modifier = Modifier.size(32.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Camera is Turned Off", style = Typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Other participants will see your avatar", style = Typography.bodySmall, color = TextSecondary)
                        }
                    }
                    
                    // Floating Controls Overlay (Mic & Camera Toggles)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PreJoinControlButton(
                                icon = if (isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                                isActive = isMicOn,
                                activeColor = EmeraldGreen,
                                inactiveColor = RoseRed,
                                label = if (isMicOn) "Mic On" else "Muted",
                                onClick = { 
                                    isMicOn = !isMicOn
                                    webRtcManager.setAudioEnabled(isMicOn)
                                    Toast.makeText(context, if (isMicOn) "Microphone Unmuted" else "Microphone Muted", Toast.LENGTH_SHORT).show()
                                }
                            )

                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderColor))

                            PreJoinControlButton(
                                icon = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                isActive = isCameraOn,
                                activeColor = ElectricIndigo,
                                inactiveColor = RoseRed,
                                label = if (isCameraOn) "Video On" else "Video Off",
                                onClick = { 
                                    isCameraOn = !isCameraOn
                                    webRtcManager.setVideoEnabled(isCameraOn)
                                    Toast.makeText(context, if (isCameraOn) "Camera Enabled" else "Camera Disabled", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Audio Output Device Selector Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = CardSurface.copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when(selectedAudioDevice) {
                                    "Headset" -> Icons.Default.Headset
                                    "Bluetooth" -> Icons.Default.Bluetooth
                                    "Earpiece" -> Icons.Default.PhoneInTalk
                                    else -> Icons.Default.VolumeUp
                                },
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Audio Output Source", style = Typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }

                        // Live Mic Audio Meter Status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(if (isMicOn) (8 * micPulseScale).dp else 8.dp)
                                    .background(if (isMicOn) EmeraldGreen else RoseRed, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isMicOn) "Mic Active" else "Mic Muted",
                                style = Typography.labelSmall,
                                color = if (isMicOn) EmeraldGreen else RoseRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableAudioDevices.forEach { device ->
                            AudioChip(
                                label = device,
                                isSelected = selectedAudioDevice == device,
                                onClick = {
                                    selectedAudioDevice = device
                                    Toast.makeText(context, "Audio Output: $device", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Meeting Details Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Ready to join?", style = Typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        meetingTitle,
                        style = Typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Participants Counter Banner
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(EmeraldGreen, CircleShape)
                                .pulse(color = EmeraldGreen)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$participantCount people in this meeting",
                            style = Typography.bodyMedium,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreJoinControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = if (isActive) activeColor.copy(alpha = 0.25f) else inactiveColor.copy(alpha = 0.25f),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) activeColor else inactiveColor)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, 
                    contentDescription = label, 
                    tint = if (isActive) activeColor else inactiveColor, 
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            style = Typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AudioChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) ElectricIndigo else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ElectricIndigo else BorderColor),
        modifier = Modifier.bounceClick()
    ) {
        Text(
            text = label,
            style = Typography.labelSmall,
            color = if (isSelected) Color.White else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}


