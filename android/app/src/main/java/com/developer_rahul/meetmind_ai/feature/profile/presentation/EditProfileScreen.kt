package com.developer_rahul.meetmind_ai.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var name by remember { mutableStateOf("Rahul Developer") }
    var email by remember { mutableStateOf("rahul@meetmind.ai") }
    var bio by remember { mutableStateOf("Senior Android Engineer focused on AI productivity tools.") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface,
        bottomBar = {
            Box(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                MeetMindButton(
                    text = "Save Changes",
                    onClick = onSave
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
            Spacer(modifier = Modifier.height(32.dp))
            
            Box {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = GlassWhite,
                    border = androidx.compose.foundation.BorderStroke(2.dp, ElectricIndigo)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("R", style = Typography.displayLarge, color = TextPrimary)
                    }
                }
                
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(ElectricIndigo, CircleShape)
                        .size(36.dp)
                        .padding(4.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                MeetMindTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    leadingIcon = Icons.Default.Person
                )
                
                MeetMindTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    leadingIcon = Icons.Default.Email,
                    enabled = false
                )

                MeetMindTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = "Bio",
                    leadingIcon = Icons.Default.Info
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Email address cannot be changed. Contact support if you need to update it.",
                style = Typography.labelSmall,
                color = TextDisabled,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
