package com.developer_rahul.meetmind_ai.feature.home.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
    onAddParticipant: (String) -> Unit
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
            NavigationBar(
                containerColor = BackgroundSurface,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricIndigo,
                            selectedTextColor = ElectricIndigo,
                            unselectedIconColor = TextDisabled,
                            unselectedTextColor = TextDisabled,
                            indicatorColor = GlassWhite
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController, 
            startDestination = "home_tab", 
            Modifier.padding(innerPadding)
        ) {
            composable("home_tab") {
                DashboardScreen(
                    onNewMeeting = onNewMeeting,
                    onJoinMeeting = onJoinMeeting,
                    onAiRepConfig = onAiRepConfig,
                    onMeetingClick = onMeetingClick,
                    onNotifications = onNotifications,
                    onProfile = onProfile
                )
            }
            composable("meetings_tab") {
                MeetingsScreen(
                    onMeetingClick = onMeetingClick,
                    onCreateMeeting = onNewMeeting
                )
            }
            composable("calendar_tab") {
                // Placeholder for Calendar
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable("profile_tab") {
                ProfileScreen(
                    onBack = { },
                    onLogout = onLogout,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}

data class BottomNavItem(val title: String, val icon: ImageVector, val route: String)
