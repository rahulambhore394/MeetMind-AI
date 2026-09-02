package com.developer_rahul.meetmind_ai.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.components.*

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
    viewModel: AuthViewModel = viewModel(factory = ViewModelFactory)
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", style = Typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(32.dp))
                
                Surface(
                    modifier = Modifier.size(110.dp),
                    shape = CircleShape,
                    color = GlassWhite,
                    border = androidx.compose.foundation.BorderStroke(2.dp, ElectricIndigo)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("R", style = Typography.displayLarge, color = TextPrimary)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text("Rahul Developer", style = Typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("rahul@meetmind.ai", style = Typography.bodyLarge, color = TextSecondary)
                
                Spacer(modifier = Modifier.height(40.dp))
            }
            
            item {
                ProfileOption(icon = Icons.Default.Person, title = "Account Information")
                ProfileOption(icon = Icons.Default.Settings, title = "Preferences", onClick = onNavigateToSettings)
                ProfileOption(icon = Icons.Default.Security, title = "Security & Privacy")
                ProfileOption(icon = Icons.Default.Description, title = "AI Subscription")
                
                Spacer(modifier = Modifier.height(32.dp))
                
                TextButton(
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = RoseRed)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Logout, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sign Out", style = Typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ProfileOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit = {}
) {
    MeetMindCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = ElectricIndigo.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = Typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = TextDisabled)
        }
    }
}
