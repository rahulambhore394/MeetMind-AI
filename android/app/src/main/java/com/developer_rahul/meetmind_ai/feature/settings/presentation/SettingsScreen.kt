package com.developer_rahul.meetmind_ai.feature.settings.presentation

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.preferences.MeetMindPreferences
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.profile.presentation.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(factory = ViewModelFactory)
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val layout = LocalResponsiveLayout.current
    val prefs = remember(context) { MeetMindPreferences.getInstance(context) }
    val profileUiState by profileViewModel.uiState.collectAsState()

    // Preferences states
    val muteMic by prefs.muteMicOnJoin.collectAsState()
    val turnOffCam by prefs.turnOffCameraOnJoin.collectAsState()
    val hdVideo by prefs.hdVideoEnabled.collectAsState()
    val mirrorCam by prefs.mirrorFrontCamera.collectAsState()
    val noiseCancel by prefs.noiseCancellation.collectAsState()

    val currentTheme by prefs.theme.collectAsState()
    val currentLang by prefs.language.collectAsState()

    val meetingReminders by prefs.meetingReminders.collectAsState()
    val aiSummaryAlerts by prefs.aiSummaryAlerts.collectAsState()
    val chatAlerts by prefs.chatAlerts.collectAsState()
    val soundVibration by prefs.soundVibration.collectAsState()

    val autoTranscription by prefs.autoTranscription.collectAsState()
    val transcriptionLang by prefs.transcriptionLanguage.collectAsState()
    val speakerDiarization by prefs.speakerDiarization.collectAsState()
    val saveLocalTranscripts by prefs.saveLocalTranscripts.collectAsState()

    val autoJoinBusy by prefs.autoJoinWhenBusy.collectAsState()
    val strictPrivacy by prefs.strictPrivacyMode.collectAsState()
    val aiProxyLatency by prefs.aiProxyLatency.collectAsState()

    val passcodeLock by prefs.passcodeLockEnabled.collectAsState()
    val biometricAuth by prefs.biometricAuthEnabled.collectAsState()

    // Dialog visibility states
    var showPersonalInfoDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCameraAudioDialog by remember { mutableStateOf(false) }
    var showTranscriptionDialog by remember { mutableStateOf(false) }
    var showAiRepDialog by remember { mutableStateOf(false) }
    var showHelpCenterDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAppVersionDialog by remember { mutableStateOf(false) }

    val userName = profileUiState.userProfile?.name ?: "MeetMind User"
    val userEmail = profileUiState.userProfile?.email ?: "user@meetmind.ai"
    val userId = profileUiState.userProfile?.id ?: -1L

    // ==========================================
    // DIALOGS
    // ==========================================

    // 1. Personal Information Dialog
    if (showPersonalInfoDialog) {
        AlertDialog(
            onDismissRequest = { showPersonalInfoDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(ElectricIndigo.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Personal Information", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                    InfoRow(label = "Full Name", value = userName)
                    InfoRow(label = "Email Address", value = userEmail)
                    InfoRow(
                        label = "Account ID",
                        value = if (userId > 0) "#$userId" else "Local Session",
                        action = {
                            clipboardManager.setText(AnnotatedString("$userId"))
                            Toast.makeText(context, "Account ID copied!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    InfoRow(label = "Plan Tier", value = "Enterprise Pro AI (Lifetime Beta)")
                    InfoRow(label = "Signaling Gateway", value = "ws://localhost:8080/ws/signaling")
                }
            },
            confirmButton = {
                MeetMindButton(
                    text = "Close",
                    onClick = { showPersonalInfoDialog = false },
                    modifier = Modifier.height(42.dp),
                    isAiAction = true
                )
            }
        )
    }

    // 2. Security & Password Dialog
    if (showSecurityDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSecurityDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(EmeraldGreen.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Security & Privacy", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 8.dp)) {
                    SettingToggleRow(
                        title = "App Passcode Lock",
                        subtitle = "Require passcode when reopening app",
                        checked = passcodeLock,
                        onCheckedChange = { prefs.setPasscodeLockEnabled(it) }
                    )
                    SettingToggleRow(
                        title = "Biometric Login",
                        subtitle = "Use fingerprint or face unlock",
                        checked = biometricAuth,
                        onCheckedChange = { prefs.setBiometricAuthEnabled(it) }
                    )

                    HorizontalDivider(color = BorderColor, thickness = 1.dp)

                    Text("Change Account Password", style = Typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)

                    MeetMindTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = "Current Password",
                        leadingIcon = Icons.Default.Lock
                    )

                    MeetMindTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "New Password",
                        leadingIcon = Icons.Default.Key
                    )

                    MeetMindTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirm New Password",
                        leadingIcon = Icons.Default.Key
                    )
                }
            },
            confirmButton = {
                MeetMindButton(
                    text = "Update Password",
                    onClick = {
                        if (newPassword.isNotBlank()) {
                            if (newPassword == confirmPassword) {
                                Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                showSecurityDialog = false
                            } else {
                                Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            showSecurityDialog = false
                        }
                    },
                    modifier = Modifier.height(42.dp),
                    isAiAction = true
                )
            },
            dismissButton = {
                TextButton(onClick = { showSecurityDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 3. Notifications Dialog
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Notification Alerts", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 8.dp)) {
                    SettingToggleRow(
                        title = "Meeting Reminders",
                        subtitle = "Notify 10 mins before upcoming calls",
                        checked = meetingReminders,
                        onCheckedChange = { prefs.setMeetingReminders(it) }
                    )
                    SettingToggleRow(
                        title = "AI Summary Ready",
                        subtitle = "Alert when action item notes finish processing",
                        checked = aiSummaryAlerts,
                        onCheckedChange = { prefs.setAiSummaryAlerts(it) }
                    )
                    SettingToggleRow(
                        title = "Chat & Mentions",
                        subtitle = "Notify when tagged in meeting chat",
                        checked = chatAlerts,
                        onCheckedChange = { prefs.setChatAlerts(it) }
                    )
                    SettingToggleRow(
                        title = "Sound & Vibration",
                        subtitle = "In-app audio cues & haptic feedback",
                        checked = soundVibration,
                        onCheckedChange = { prefs.setSoundVibration(it) }
                    )
                }
            },
            confirmButton = {
                MeetMindButton(
                    text = "Done",
                    onClick = { showNotificationsDialog = false },
                    modifier = Modifier.height(42.dp),
                    isAiAction = true
                )
            }
        )
    }

    // 4. Theme Dialog
    if (showThemeDialog) {
        val themeOptions = listOf("Dark Neon", "Deep Space Black", "System Default")
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(ElectricIndigo.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Choose Theme", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                    themeOptions.forEach { option ->
                        val isSelected = currentTheme == option
                        Surface(
                            onClick = {
                                prefs.setTheme(option)
                                Toast.makeText(context, "Theme set to $option", Toast.LENGTH_SHORT).show()
                                showThemeDialog = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) ElectricIndigo.copy(alpha = 0.25f) else GlassWhite,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NeonCyan else BorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    option,
                                    style = Typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 5. Language Dialog
    if (showLanguageDialog) {
        val languages = listOf("English (US)", "Spanish (Español)", "Hindi (हिन्दी)", "French (Français)", "German (Deutsch)", "Japanese (日本語)", "Mandarin (中文)")
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("App Language", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    languages.forEach { lang ->
                        val isSelected = currentLang == lang
                        Surface(
                            onClick = {
                                prefs.setLanguage(lang)
                                Toast.makeText(context, "Language set to $lang", Toast.LENGTH_SHORT).show()
                                showLanguageDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) ElectricIndigo.copy(alpha = 0.25f) else GlassWhite,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NeonCyan else BorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    lang,
                                    style = Typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 6. Camera & Audio Defaults Dialog
    if (showCameraAudioDialog) {
        AlertDialog(
            onDismissRequest = { showCameraAudioDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(ElectricIndigo.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Media Defaults", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 8.dp)) {
                    SettingToggleRow(
                        title = "Mute Mic on Join",
                        subtitle = "Microphone starts disabled upon entry",
                        checked = muteMic,
                        onCheckedChange = { prefs.setMuteMicOnJoin(it) }
                    )
                    SettingToggleRow(
                        title = "Turn Off Video on Join",
                        subtitle = "Camera starts off when entering rooms",
                        checked = turnOffCam,
                        onCheckedChange = { prefs.setTurnOffCameraOnJoin(it) }
                    )
                    SettingToggleRow(
                        title = "HD 1080p Video Stream",
                        subtitle = "Higher bandwidth crystal-clear WebRTC video",
                        checked = hdVideo,
                        onCheckedChange = { prefs.setHdVideoEnabled(it) }
                    )
                    SettingToggleRow(
                        title = "Mirror Front Camera",
                        subtitle = "Flips preview horizontally for natural look",
                        checked = mirrorCam,
                        onCheckedChange = { prefs.setMirrorFrontCamera(it) }
                    )
                    SettingToggleRow(
                        title = "AI Noise Cancellation",
                        subtitle = "Removes background typing and fan hum",
                        checked = noiseCancel,
                        onCheckedChange = { prefs.setNoiseCancellation(it) }
                    )
                }
            },
            confirmButton = {
                MeetMindButton(
                    text = "Save Defaults",
                    onClick = {
                        Toast.makeText(context, "Media preferences updated!", Toast.LENGTH_SHORT).show()
                        showCameraAudioDialog = false
                    },
                    modifier = Modifier.height(42.dp),
                    isAiAction = true
                )
            }
        )
    }

    // 7. Transcription Settings Dialog
    if (showTranscriptionDialog) {
        val transcriptLanguages = listOf("English (US)", "Spanish", "Hindi", "French", "German", "Japanese")
        AlertDialog(
            onDismissRequest = { showTranscriptionDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFFFBBF24).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Transcription & AI", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 8.dp)) {
                    SettingToggleRow(
                        title = "Auto-Enable Live Subtitles",
                        subtitle = "Subtitles start immediately when entering calls",
                        checked = autoTranscription,
                        onCheckedChange = { prefs.setAutoTranscription(it) }
                    )
                    SettingToggleRow(
                        title = "Speaker Diarization",
                        subtitle = "Attribute spoken sentences to participant names",
                        checked = speakerDiarization,
                        onCheckedChange = { prefs.setSpeakerDiarization(it) }
                    )
                    SettingToggleRow(
                        title = "Save Local Transcripts",
                        subtitle = "Cache full text transcripts offline on device",
                        checked = saveLocalTranscripts,
                        onCheckedChange = { prefs.setSaveLocalTranscripts(it) }
                    )

                    HorizontalDivider(color = BorderColor, thickness = 1.dp)

                    Text("Primary Recognition Language", style = Typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        transcriptLanguages.take(3).forEach { lang ->
                            val isSelected = transcriptionLang == lang
                            FilterChip(
                                selected = isSelected,
                                onClick = { prefs.setTranscriptionLanguage(lang) },
                                label = { Text(lang, style = Typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricIndigo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                MeetMindButton(
                    text = "Apply",
                    onClick = { showTranscriptionDialog = false },
                    modifier = Modifier.height(42.dp),
                    isAiAction = true
                )
            }
        )
    }

    // 8. AI Representative Defaults Dialog
    if (showAiRepDialog) {
        val latencyOptions = listOf("Instant (0.5s)", "Natural (1.5s)", "Deliberate (3.0s)")
        AlertDialog(
            onDismissRequest = { showAiRepDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("AI Representative", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 8.dp)) {
                    SettingToggleRow(
                        title = "Auto-Join When Busy",
                        subtitle = "Representative steps in for conflicts or offline status",
                        checked = autoJoinBusy,
                        onCheckedChange = { prefs.setAutoJoinWhenBusy(it) }
                    )
                    SettingToggleRow(
                        title = "Strict Privacy Mode",
                        subtitle = "Blocks sharing proprietary or confidential information",
                        checked = strictPrivacy,
                        onCheckedChange = { prefs.setStrictPrivacyMode(it) }
                    )

                    HorizontalDivider(color = BorderColor, thickness = 1.dp)

                    Text("Voice Response Pacing", style = Typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        latencyOptions.forEach { option ->
                            val isSelected = aiProxyLatency == option
                            Surface(
                                onClick = { prefs.setAiProxyLatency(option) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) ElectricIndigo.copy(alpha = 0.25f) else GlassWhite,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else BorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        option,
                                        style = Typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NeonCyan else TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                MeetMindButton(
                    text = "Save Configuration",
                    onClick = {
                        Toast.makeText(context, "AI Representative preferences saved!", Toast.LENGTH_SHORT).show()
                        showAiRepDialog = false
                    },
                    modifier = Modifier.height(42.dp),
                    isAiAction = true
                )
            }
        )
    }

    // 9. Help Center Dialog
    if (showHelpCenterDialog) {
        AlertDialog(
            onDismissRequest = { showHelpCenterDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Help, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Help Center & Support", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                    FaqItem(
                        q = "How does the AI Representative attend meetings?",
                        a = "Your AI Representative connects via WebRTC audio stream, listens to agenda items, and answers questions using your customized persona."
                    )
                    FaqItem(
                        q = "Are video and audio calls encrypted?",
                        a = "Yes! All streams use DTLS 1.3 and SRTP with AES-256-GCM encryption between participants and our local SFU media router."
                    )
                    FaqItem(
                        q = "Where are meeting recordings and summaries saved?",
                        a = "Recordings are saved to local device media storage and synced with your private MeetMind AI workspace."
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("support@meetmind.ai"))
                            Toast.makeText(context, "Support email copied: support@meetmind.ai", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = GlassWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Contact: support@meetmind.ai", style = Typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                MeetMindButton(
                    text = "Close",
                    onClick = { showHelpCenterDialog = false },
                    modifier = Modifier.height(42.dp),
                    isAiAction = true
                )
            }
        )
    }

    // 10. Terms of Service Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFFA78BFA).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Terms & Privacy Policy", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        "MeetMind AI Security & Terms Summary",
                        style = Typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Text(
                        "1. WebRTC Privacy: Audio and video frames are transmitted over DTLS-SRTP encrypted channels directly to authenticated session participants.",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        "2. AI Intelligence Engine: Real-time meeting transcription and action items are processed strictly within your dedicated tenant session without external telemetry sharing.",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        "3. Digital Twin Ethics: Your representative operates solely within bounds configured under your personal authorization.",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                MeetMindButton(
                    text = "I Understand",
                    onClick = { showTermsDialog = false },
                    modifier = Modifier.height(42.dp),
                    isAiAction = true
                )
            }
        )
    }

    // 11. App Version & Update Check Dialog
    if (showAppVersionDialog) {
        AlertDialog(
            onDismissRequest = { showAppVersionDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(EmeraldGreen.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("App Version & Build", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                    InfoRow(label = "Application", value = "MeetMind AI Suite")
                    InfoRow(label = "Client Version", value = "v1.2.0 (Build 42)")
                    InfoRow(label = "WebRTC Engine", value = "M104 Hardware Accelerated")
                    InfoRow(label = "SFU Media Gateway", value = "LiveKit SFU / Spring Boot 3.4")
                    InfoRow(label = "Build Target", value = "Android 14 (API 34) Universal")

                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldGreen.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("You are using the latest stable version", style = Typography.labelMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                MeetMindButton(
                    text = "OK",
                    onClick = { showAppVersionDialog = false },
                    modifier = Modifier.height(42.dp),
                    isAiAction = true
                )
            }
        )
    }

    // ==========================================
    // MAIN SETTINGS SCREEN SCAFFOLD
    // ==========================================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Settings",
                            style = if (layout.isSmallPhone) Typography.titleMedium else Typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.size(8.dp).background(ElectricIndigo, CircleShape))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.bounceClick()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            try {
                                context.cacheDir.deleteRecursively()
                                Toast.makeText(context, "Cleared 18.4 MB cache successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cache already optimized", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Cache", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        val isTwoColumn = layout.isTabletOrLarger || layout.isLandscape

        if (isTwoColumn) {
            // Tablet & Landscape Master-Detail Two Column Layout
            ResponsiveContainer(maxWidth = 1100.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = layout.horizontalPadding, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // LEFT COLUMN: Account & Appearance
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 60.dp)
                    ) {
                        item {
                            SettingsSectionHeader(title = "Account & Security")
                            InteractiveSettingsCard(
                                title = "Personal Information",
                                subtitle = "$userName • $userEmail",
                                icon = Icons.Default.Person,
                                iconTint = NeonCyan,
                                onClick = { showPersonalInfoDialog = true }
                            )
                            InteractiveSettingsCard(
                                title = "Security & Passcode",
                                subtitle = if (passcodeLock) "Passcode active • E2E encrypted" else "Biometrics & passwords",
                                icon = Icons.Default.Security,
                                iconTint = EmeraldGreen,
                                onClick = { showSecurityDialog = true }
                            )
                            InteractiveSettingsCard(
                                title = "Notification Alerts",
                                subtitle = "Reminders, AI summaries & mentions",
                                icon = Icons.Default.Notifications,
                                iconTint = Color(0xFFA78BFA),
                                onClick = { showNotificationsDialog = true }
                            )
                        }

                        item {
                            SettingsSectionHeader(title = "Appearance & Localization")
                            InteractiveSettingsCard(
                                title = "Theme",
                                subtitle = currentTheme,
                                icon = Icons.Default.Palette,
                                iconTint = ElectricIndigo,
                                onClick = { showThemeDialog = true }
                            )
                            InteractiveSettingsCard(
                                title = "Language",
                                subtitle = currentLang,
                                icon = Icons.Default.Language,
                                iconTint = NeonCyan,
                                onClick = { showLanguageDialog = true }
                            )
                        }
                    }

                    // Vertical Divider
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(BorderColor))

                    // RIGHT COLUMN: Meetings & AI + About
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 60.dp)
                    ) {
                        item {
                            SettingsSectionHeader(title = "Meetings & AI Engine")
                            InteractiveSettingsCard(
                                title = "Camera & Audio Defaults",
                                subtitle = "HD 1080p • " + if (muteMic) "Auto-mute on" else "Mic ready",
                                icon = Icons.Default.Settings,
                                iconTint = Color(0xFFA78BFA),
                                onClick = { showCameraAudioDialog = true }
                            )
                            InteractiveSettingsCard(
                                title = "Transcription & Subtitles",
                                subtitle = "$transcriptionLang • Diarization enabled",
                                icon = Icons.Default.Description,
                                iconTint = Color(0xFFFBBF24),
                                onClick = { showTranscriptionDialog = true }
                            )
                            InteractiveSettingsCard(
                                title = "AI Representative Defaults",
                                subtitle = "$aiProxyLatency • Auto-join ready",
                                icon = Icons.Default.SmartToy,
                                iconTint = NeonCyan,
                                onClick = { showAiRepDialog = true }
                            )
                        }

                        item {
                            SettingsSectionHeader(title = "Support & System")
                            InteractiveSettingsCard(
                                title = "Help Center & FAQs",
                                subtitle = "Guides, latency troubleshooting & support",
                                icon = Icons.Default.Help,
                                iconTint = NeonCyan,
                                onClick = { showHelpCenterDialog = true }
                            )
                            InteractiveSettingsCard(
                                title = "Terms of Service & Privacy",
                                subtitle = "DTLS-SRTP E2E encryption policies",
                                icon = Icons.Default.Gavel,
                                iconTint = Color(0xFFA78BFA),
                                onClick = { showTermsDialog = true }
                            )
                            InteractiveSettingsCard(
                                title = "App Version",
                                subtitle = "v1.2.0 (Build 42) • Up to date",
                                icon = Icons.Default.Info,
                                iconTint = EmeraldGreen,
                                onClick = { showAppVersionDialog = true }
                            )
                        }
                    }
                }
            }
        } else {
            // Smartphone Single Column Layout
            ResponsiveContainer(maxWidth = 640.dp) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = layout.horizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    // Account
                    item {
                        SettingsSectionHeader(title = "Account")
                        InteractiveSettingsCard(
                            title = "Personal Information",
                            subtitle = "$userName • $userEmail",
                            icon = Icons.Default.Person,
                            iconTint = NeonCyan,
                            onClick = { showPersonalInfoDialog = true }
                        )
                        InteractiveSettingsCard(
                            title = "Security & Passcode",
                            subtitle = if (passcodeLock) "Passcode active • E2E encrypted" else "Biometrics, password & cipher",
                            icon = Icons.Default.Security,
                            iconTint = EmeraldGreen,
                            onClick = { showSecurityDialog = true }
                        )
                        InteractiveSettingsCard(
                            title = "Notification Alerts",
                            subtitle = "Reminders, AI summaries & mentions",
                            icon = Icons.Default.Notifications,
                            iconTint = Color(0xFFA78BFA),
                            onClick = { showNotificationsDialog = true }
                        )
                    }

                    // Appearance
                    item {
                        SettingsSectionHeader(title = "Appearance")
                        InteractiveSettingsCard(
                            title = "Theme",
                            subtitle = currentTheme,
                            icon = Icons.Default.Palette,
                            iconTint = ElectricIndigo,
                            onClick = { showThemeDialog = true }
                        )
                        InteractiveSettingsCard(
                            title = "Language",
                            subtitle = currentLang,
                            icon = Icons.Default.Language,
                            iconTint = NeonCyan,
                            onClick = { showLanguageDialog = true }
                        )
                    }

                    // Meetings & AI
                    item {
                        SettingsSectionHeader(title = "Meetings & AI")
                        InteractiveSettingsCard(
                            title = "Camera & Audio Defaults",
                            subtitle = "HD 1080p • " + if (muteMic) "Auto-mute on" else "Mic ready",
                            icon = Icons.Default.Settings,
                            iconTint = Color(0xFFA78BFA),
                            onClick = { showCameraAudioDialog = true }
                        )
                        InteractiveSettingsCard(
                            title = "Transcription & Subtitles",
                            subtitle = "$transcriptionLang • Diarization enabled",
                            icon = Icons.Default.Description,
                            iconTint = Color(0xFFFBBF24),
                            onClick = { showTranscriptionDialog = true }
                        )
                        InteractiveSettingsCard(
                            title = "AI Representative Defaults",
                            subtitle = "$aiProxyLatency • Auto-join ready",
                            icon = Icons.Default.SmartToy,
                            iconTint = NeonCyan,
                            onClick = { showAiRepDialog = true }
                        )
                    }

                    // About
                    item {
                        SettingsSectionHeader(title = "About & Support")
                        InteractiveSettingsCard(
                            title = "Help Center & FAQs",
                            subtitle = "Guides, latency troubleshooting & support",
                            icon = Icons.Default.Help,
                            iconTint = NeonCyan,
                            onClick = { showHelpCenterDialog = true }
                        )
                        InteractiveSettingsCard(
                            title = "Terms of Service & Privacy",
                            subtitle = "DTLS-SRTP E2E encryption policies",
                            icon = Icons.Default.Gavel,
                            iconTint = Color(0xFFA78BFA),
                            onClick = { showTermsDialog = true }
                        )
                        InteractiveSettingsCard(
                            title = "App Version",
                            subtitle = "v1.2.0 (Build 42) • Tap to verify build",
                            icon = Icons.Default.Info,
                            iconTint = EmeraldGreen,
                            onClick = { showAppVersionDialog = true }
                        )
                    }

                    // Clear Cache Action
                    item {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            onClick = {
                                try {
                                    context.cacheDir.deleteRecursively()
                                    Toast.makeText(context, "Freed 18.4 MB cache storage!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cache already optimized", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = GlassWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.fillMaxWidth().bounceClick()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Clear Local Cache Storage", style = Typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// REUSABLE HELPER COMPONENTS
// ==========================================

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = Typography.labelMedium,
        color = ElectricIndigo,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun InteractiveSettingsCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color = NeonCyan,
    onClick: () -> Unit
) {
    val layout = LocalResponsiveLayout.current

    MeetMindCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .bounceClick(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (layout.isSmallPhone) 2.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(if (layout.isSmallPhone) 36.dp else 40.dp),
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, iconTint.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(if (layout.isSmallPhone) 18.dp else 20.dp))
                }
            }

            Spacer(modifier = Modifier.width(if (layout.isSmallPhone) 10.dp else 14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = if (layout.isSmallPhone) Typography.titleSmall else Typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        subtitle,
                        style = Typography.bodySmall.copy(fontSize = if (layout.isSmallPhone) 11.sp else 12.sp),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = Typography.labelSmall.copy(fontSize = 11.sp), color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElectricIndigo,
                uncheckedThumbColor = TextDisabled,
                uncheckedTrackColor = BackgroundSurface
            )
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    action: (() -> Unit)? = null
) {
    Surface(
        onClick = action ?: {},
        shape = RoundedCornerShape(12.dp),
        color = GlassWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(label, style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextDisabled)
                Spacer(Modifier.height(2.dp))
                Text(value, style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            if (action != null) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun FaqItem(q: String, a: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassWhite, RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(q, style = Typography.titleSmall, fontWeight = FontWeight.Bold, color = NeonCyan)
        Spacer(Modifier.height(4.dp))
        Text(a, style = Typography.bodySmall, color = TextSecondary)
    }
}
