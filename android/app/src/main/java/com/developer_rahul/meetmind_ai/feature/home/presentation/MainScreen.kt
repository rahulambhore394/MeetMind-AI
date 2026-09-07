package com.developer_rahul.meetmind_ai.feature.home.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.navigation.Screen
import com.developer_rahul.meetmind_ai.feature.meetings.presentation.MeetingsScreen
import com.developer_rahul.meetmind_ai.feature.notifications.presentation.NotificationsScreen
import com.developer_rahul.meetmind_ai.feature.profile.presentation.ProfileScreen

@Composable
fun MainScreen(
    onNewMeeting: () -> Unit,
    onJoinMeeting: () -> Unit,
    onAiRepConfig: () -> Unit,
    onMeetingClick: (String) -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddParticipant: (String) -> Unit,
    onSummaries: () -> Unit = {}
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem("Home", Icons.Default.Home, "home_tab"),
        BottomNavItem("Meetings", Icons.Default.VideoCall, "meetings_tab"),
        BottomNavItem("Calendar", Icons.Default.DateRange, "calendar_tab"),
        BottomNavItem("Profile", Icons.Default.Person, "profile_tab")
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            Surface(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(36.dp), spotColor = ElectricIndigo.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(36.dp),
                color = CardSurface.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(listOf(GlassWhite, ElectricIndigo.copy(alpha = 0.4f), BorderColor))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        
                        CustomBottomNavItem(
                            item = item,
                            isSelected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        containerColor = BackgroundSurface
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = "home_tab", 
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home_tab") {
                DashboardScreen(
                    onNewMeeting = onNewMeeting,
                    onJoinMeeting = onJoinMeeting,
                    onAiRepConfig = onAiRepConfig,
                    onMeetingClick = onMeetingClick,
                    onNotifications = onNotifications,
                    onProfile = { navController.navigate("profile_tab") },
                    onSummaries = onSummaries,
                    onViewAllSchedule = { navController.navigate("meetings_tab") }
                )
            }
            composable("meetings_tab") {
                MeetingsScreen(
                    onMeetingClick = onMeetingClick,
                    onCreateMeeting = onNewMeeting
                )
            }
            composable("calendar_tab") {
                com.developer_rahul.meetmind_ai.feature.meetings.presentation.CalendarScreen(
                    onMeetingClick = onMeetingClick,
                    onCreateMeeting = onNewMeeting,
                    onNotifications = onNotifications
                )
            }
            composable("profile_tab") {
                ProfileScreen(
                    onBack = { navController.navigate("home_tab") },
                    onLogout = onLogout,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
private fun CustomBottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val animatedIconColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan else TextDisabled,
        label = "iconColor"
    )

    val itemModifier = if (isSelected) {
        Modifier
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        ElectricIndigo.copy(alpha = 0.35f),
                        NeonCyan.copy(alpha = 0.20f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.6f), ElectricIndigo.copy(alpha = 0.4f))),
                shape = RoundedCornerShape(24.dp)
            )
            .aiGlow(color = ElectricIndigo, radius = 12.dp)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .bounceClick()
            .clip(RoundedCornerShape(24.dp))
            .then(itemModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = animatedIconColor,
                modifier = Modifier.size(22.dp)
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.title,
                        style = Typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

data class BottomNavItem(val title: String, val icon: ImageVector, val route: String)


