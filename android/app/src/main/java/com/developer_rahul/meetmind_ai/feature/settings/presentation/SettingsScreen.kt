package com.developer_rahul.meetmind_ai.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.sp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            item {
                SettingsSectionHeader(title = "Account")
                SettingsItem(title = "Personal Information", icon = Icons.Default.Person)
                SettingsItem(title = "Security & Password", icon = Icons.Default.Security)
                SettingsItem(title = "Notifications", icon = Icons.Default.Notifications)
                
                SettingsSectionHeader(title = "Appearance")
                SettingsItem(title = "Theme", value = "Dark Mode", icon = Icons.Default.Palette)
                SettingsItem(title = "Language", value = "English (US)", icon = Icons.Default.Language)
                
                SettingsSectionHeader(title = "Meetings & AI")
                SettingsItem(title = "Camera & Audio Defaults", icon = Icons.Default.Settings)
                SettingsItem(title = "Transcription Settings", icon = Icons.Default.Description)
                SettingsItem(title = "AI Representative Defaults", icon = Icons.Default.SmartToy)
                
                SettingsSectionHeader(title = "About")
                SettingsItem(title = "Help Center", icon = Icons.Default.Help)
                SettingsItem(title = "Terms of Service", icon = Icons.Default.Gavel)
                SettingsItem(title = "App Version", value = "1.0.42-beta", icon = Icons.Default.Info)
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = Typography.labelMedium,
        color = ElectricIndigo,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 32.dp, bottom = 12.dp, start = 4.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String? = null
) {
    MeetMindCard(
        modifier = Modifier.padding(vertical = 4.dp),
        onClick = { }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = MaterialTheme.shapes.medium,
                color = GlassWhite
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = Typography.bodyLarge, color = TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            if (value != null) {
                Text(value, style = Typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextDisabled, modifier = Modifier.size(20.dp))
        }
    }
}
