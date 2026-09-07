package com.developer_rahul.meetmind_ai.feature.profile.presentation

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    var editName by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }

    val user = uiState.userProfile
    val userName = user?.name ?: "User Profile"
    val userEmail = user?.email ?: "loading..."
    val initialLetter = userName.take(1).uppercase().ifEmpty { "U" }

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
                        profileViewModel.updateProfile(editName, editEmail)
                        showEditDialog = false
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Settings", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != {}) {
                        IconButton(onClick = onBack, modifier = Modifier.bounceClick()) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                        }
                    }
                },
                actions = {
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
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Profile Header Glass Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Brush.linearGradient(listOf(ElectricIndigo.copy(alpha = 0.5f), NeonCyan.copy(alpha = 0.3f))))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar Badge with Ambient Aura Glow
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .aiGlow(color = ElectricIndigo, radius = 24.dp)
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
                                            style = Typography.displaySmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(userName, style = Typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(userEmail, style = Typography.bodyMedium, color = TextSecondary)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            StatusChip(
                                text = "⚡ PRO AI MEMBER",
                                color = NeonCyan
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))

                            // Quick Stats Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                ProfileStatItem(count = "${uiState.totalMeetingsCount}", label = "Meetings")
                                Box(modifier = Modifier.width(1.dp).height(32.dp).background(BorderColor))
                                ProfileStatItem(count = "${uiState.totalRecordingsCount}", label = "AI Recordings")
                                Box(modifier = Modifier.width(1.dp).height(32.dp).background(BorderColor))
                                ProfileStatItem(count = "⚡ Pro", label = "AI Plan")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))

                    SectionHeader(title = "Account & Preferences")
                }
                
                item {
                    ProfileOptionCard(
                        icon = Icons.Default.Person,
                        iconTint = NeonCyan,
                        title = "Edit Account Details",
                        subtitle = "Update your full name and email address",
                        onClick = {
                            editName = userName
                            editEmail = userEmail
                            showEditDialog = true
                        }
                    )

                    ProfileOptionCard(
                        icon = Icons.Default.Settings,
                        iconTint = Color(0xFFA78BFA),
                        title = "Preferences",
                        subtitle = "Audio, video, dark mode & notifications",
                        onClick = onNavigateToSettings
                    )

                    ProfileOptionCard(
                        icon = Icons.Default.Security,
                        iconTint = Color(0xFF34D399),
                        title = "Security & Privacy",
                        subtitle = "Manage security settings & passcode lock"
                    )

                    ProfileOptionCard(
                        icon = Icons.Default.AutoAwesome,
                        iconTint = Color(0xFFFBBF24),
                        title = "AI Subscription & Credits",
                        subtitle = "Unlimited smart transcripts & real-time notes"
                    )
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // Glassmorphic Sign Out Button
                    Surface(
                        onClick = {
                            authViewModel.logout()
                            onLogout()
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = RoseRed.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoseRed.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", tint = RoseRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sign Out", style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = RoseRed)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileStatItem(
    count: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = Typography.labelSmall, color = TextSecondary)
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
    MeetMindCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .bounceClick(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.medium,
                color = iconTint.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, iconTint.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = Typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = Typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(20.dp))
        }
    }
}

