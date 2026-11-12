package com.developer_rahul.meetmind_ai.feature.profile.presentation

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*
import com.developer_rahul.meetmind_ai.feature.auth.presentation.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    authViewModel: AuthViewModel = viewModel(factory = ViewModelFactory),
    profileViewModel: ProfileViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by profileViewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val layout = LocalResponsiveLayout.current

    val user = uiState.userProfile
    val userName = user?.name ?: "MeetMind User"
    val userEmail = user?.email ?: "user@meetmind.ai"
    val userId = user?.id ?: -1L
    val initialLetter = userName.take(1).uppercase().ifEmpty { "U" }

    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    // Edit Profile Modal Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ElectricIndigo.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Edit Profile", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    MeetMindTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = "Full Name",
                        leadingIcon = Icons.Default.Person
                    )
                    Spacer(Modifier.height(16.dp))
                    MeetMindTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = "Email Address",
                        leadingIcon = Icons.Default.Email
                    )
                }
            },
            confirmButton = {
                MeetMindButton(
                    text = "Save Changes",
                    onClick = {
                        if (editName.isNotBlank() && editEmail.isNotBlank()) {
                            profileViewModel.updateProfile(editName.trim(), editEmail.trim())
                            showEditDialog = false
                        } else {
                            Toast.makeText(context, "Name and Email cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.height(44.dp).padding(horizontal = 4.dp),
                    isAiAction = true
                )
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    // Sign Out Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(RoseRed.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = RoseRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Sign Out", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Text(
                    "Are you sure you want to sign out from MeetMind AI? You will need to log back in to access your meetings.",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        authViewModel.logout()
                        onLogout()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Profile & Account",
                            style = if (layout.isSmallPhone) Typography.titleMedium else Typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NeonCyan, CircleShape)
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != {}) {
                        IconButton(onClick = onBack, modifier = Modifier.bounceClick()) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { profileViewModel.loadUserProfile() },
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Profile", tint = TextPrimary)
                    }
                    IconButton(
                        onClick = {
                            editName = userName
                            editEmail = userEmail
                            showEditDialog = true
                        },
                        modifier = Modifier.bounceClick()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = GlassWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        if (uiState.isLoading && uiState.userProfile == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else {
            val isTwoColumn = layout.isTabletOrLarger || layout.isLandscape

            if (isTwoColumn) {
                // Tablet / Landscape Two-Column Master-Detail Layout
                ResponsiveContainer(maxWidth = 1080.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = layout.horizontalPadding, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // LEFT COLUMN: Profile Hero Card + Stats + Sign Out
                        Column(
                            modifier = Modifier
                                .width(if (layout.isLandscape && !layout.isTablet) 360.dp else 400.dp)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProfileHeroCard(
                                userName = userName,
                                userEmail = userEmail,
                                userId = userId,
                                initialLetter = initialLetter,
                                onEdit = {
                                    editName = userName
                                    editEmail = userEmail
                                    showEditDialog = true
                                }
                            )

                            ProfileStatsBentoCard(
                                totalMeetings = uiState.totalMeetingsCount,
                                totalRecordings = uiState.totalRecordingsCount
                            )

                            SignOutButton(onClick = { showLogoutConfirmDialog = true })
                        }

                        // Vertical Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(BorderColor)
                        )

                        // RIGHT COLUMN: Preferences, AI Proxy, Security & Diagnostics
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            item {
                                SectionHeader(title = "Preferences & AI Configuration")
                            }

                            item {
                                ProfileOptionCard(
                                    icon = Icons.Default.Settings,
                                    iconTint = Color(0xFFA78BFA),
                                    title = "Meeting Preferences",
                                    subtitle = "Audio devices, HD camera, dark theme & notifications",
                                    onClick = onNavigateToSettings
                                )
                            }

                            item {
                                ProfileOptionCard(
                                    icon = Icons.Default.Psychology,
                                    iconTint = NeonCyan,
                                    title = "AI Proxy & Digital Twin",
                                    subtitle = "Voice synthesis, photo avatar & automated Q&A",
                                    onClick = onNavigateToSettings
                                )
                            }

                            item {
                                ProfileOptionCard(
                                    icon = Icons.Default.Security,
                                    iconTint = EmeraldGreen,
                                    title = "Security & Encryption",
                                    subtitle = "End-to-End WebRTC encryption & account access"
                                )
                            }

                            item {
                                ProfileOptionCard(
                                    icon = Icons.Default.AutoAwesome,
                                    iconTint = Color(0xFFFBBF24),
                                    title = "AI Subscription & Credits",
                                    subtitle = "Unlimited smart transcripts, action item summaries"
                                )
                            }

                            item {
                                Spacer(Modifier.height(8.dp))
                                SystemDiagnosticsCard()
                            }
                        }
                    }
                }
            } else {
                // Smartphone Single-Column Scroll Feed
                ResponsiveContainer(maxWidth = 640.dp) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = layout.horizontalPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp)
                    ) {
                        // Profile Hero Glass Card
                        item {
                            ProfileHeroCard(
                                userName = userName,
                                userEmail = userEmail,
                                userId = userId,
                                initialLetter = initialLetter,
                                onEdit = {
                                    editName = userName
                                    editEmail = userEmail
                                    showEditDialog = true
                                }
                            )
                        }

                        // Quick Stats Bento Card
                        item {
                            ProfileStatsBentoCard(
                                totalMeetings = uiState.totalMeetingsCount,
                                totalRecordings = uiState.totalRecordingsCount
                            )
                        }

                        // Preferences & Account Sections
                        item {
                            SectionHeader(title = "Account & Preferences")
                        }

                        item {
                            ProfileOptionCard(
                                icon = Icons.Default.Settings,
                                iconTint = Color(0xFFA78BFA),
                                title = "Meeting Preferences",
                                subtitle = "Audio devices, HD camera, theme & alerts",
                                onClick = onNavigateToSettings
                            )
                        }

                        item {
                            ProfileOptionCard(
                                icon = Icons.Default.Psychology,
                                iconTint = NeonCyan,
                                title = "AI Proxy & Representative",
                                subtitle = "Voice clone, photo avatar & auto-reply persona",
                                onClick = onNavigateToSettings
                            )
                        }

                        item {
                            ProfileOptionCard(
                                icon = Icons.Default.Security,
                                iconTint = EmeraldGreen,
                                title = "Security & Encryption",
                                subtitle = "E2E WebRTC signaling, passcode & data privacy"
                            )
                        }

                        item {
                            ProfileOptionCard(
                                icon = Icons.Default.AutoAwesome,
                                iconTint = Color(0xFFFBBF24),
                                title = "AI Subscription & Plan",
                                subtitle = "Pro Tier • Unlimited real-time speech translation"
                            )
                        }

                        item {
                            SystemDiagnosticsCard()
                        }

                        // Sign Out Button
                        item {
                            Spacer(Modifier.height(6.dp))
                            SignOutButton(onClick = { showLogoutConfirmDialog = true })
                        }
                    }
                }
            }
        }
    }
}

/**
 * Hero Profile Glass Card displaying avatar aura, name, email, user ID, and edit button.
 */
@Composable
private fun ProfileHeroCard(
    userName: String,
    userEmail: String,
    userId: Long,
    initialLetter: String,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val layout = LocalResponsiveLayout.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(ElectricIndigo.copy(alpha = 0.5f), NeonCyan.copy(alpha = 0.3f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (layout.isSmallPhone) 16.dp else 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with Dynamic Aura Glow
            Box(
                modifier = Modifier
                    .size(if (layout.isSmallPhone) 76.dp else 92.dp)
                    .aiGlow(color = ElectricIndigo, radius = 20.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(ElectricIndigo, Color(0xFF1E1B4B))
                                ),
                                CircleShape
                            )
                            .border(2.5.dp, Brush.linearGradient(listOf(NeonCyan, ElectricIndigo)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initialLetter,
                            style = if (layout.isSmallPhone) Typography.headlineMedium else Typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                userName,
                style = if (layout.isSmallPhone) Typography.titleMedium else Typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))
            Text(
                userEmail,
                style = Typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Badges Row: Pro AI Member + User ID Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(
                    text = "⚡ PRO AI",
                    color = NeonCyan
                )

                if (userId > 0) {
                    Surface(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("$userId"))
                            Toast.makeText(context, "User ID copied: $userId", Toast.LENGTH_SHORT).show()
                        },
                        shape = CircleShape,
                        color = CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ID: $userId",
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ContentCopy, null, tint = TextSecondary, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Edit Profile Outline Button
            OutlinedButton(
                onClick = onEdit,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.6f)),
                modifier = Modifier
                    .height(36.dp)
                    .bounceClick()
            ) {
                Icon(Icons.Default.Edit, null, tint = ElectricIndigo, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit Profile", style = Typography.labelMedium, color = ElectricIndigo, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Profile Stats Bento Card showing metrics (Meetings, Recordings, AI Proxy, Tier).
 */
@Composable
private fun ProfileStatsBentoCard(
    totalMeetings: Int,
    totalRecordings: Int
) {
    val layout = LocalResponsiveLayout.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (layout.isSmallPhone) 10.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileStatItem(
                count = "$totalMeetings",
                label = "Meetings",
                color = ElectricIndigo
            )

            Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderColor))

            ProfileStatItem(
                count = "$totalRecordings",
                label = "AI Records",
                color = NeonCyan
            )

            Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderColor))

            ProfileStatItem(
                count = "Active 🟢",
                label = "AI Proxy",
                color = EmeraldGreen
            )

            if (!layout.isSmallPhone) {
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderColor))

                ProfileStatItem(
                    count = "100%",
                    label = "Audio E2E",
                    color = Color(0xFFA78BFA)
                )
            }
        }
    }
}

@Composable
private fun ProfileStatItem(
    count: String,
    label: String,
    color: Color = TextPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = Typography.labelSmall.copy(fontSize = 11.sp), color = TextSecondary)
    }
}

/**
 * Glassmorphic Sign Out Button with destructive styling.
 */
@Composable
private fun SignOutButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = RoseRed.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, RoseRed.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .bounceClick()
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", tint = RoseRed, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Sign Out", style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = RoseRed)
        }
    }
}

/**
 * System Diagnostics & App Info Card.
 */
@Composable
private fun SystemDiagnosticsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardSurface.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(EmeraldGreen, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Local SFU / Backend Online",
                        style = Typography.labelSmall,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    "v1.2.0",
                    style = Typography.labelSmall,
                    color = TextDisabled
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "MeetMind AI Multi-Agent Engine • Google DeepMind & WebRTC Powered",
                style = Typography.bodySmall.copy(fontSize = 11.sp),
                color = TextSecondary
            )
        }
    }
}

@Composable
fun ProfileOptionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    val layout = LocalResponsiveLayout.current

    MeetMindCard(
        modifier = Modifier
            .fillMaxWidth()
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
                modifier = Modifier.size(if (layout.isSmallPhone) 38.dp else 44.dp),
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, iconTint.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(if (layout.isSmallPhone) 18.dp else 22.dp))
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = Typography.bodySmall.copy(fontSize = if (layout.isSmallPhone) 11.sp else 12.sp),
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(18.dp))
        }
    }
}
