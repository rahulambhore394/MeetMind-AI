package com.developer_rahul.meetmind_ai.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MeetMindDarkColorScheme = darkColorScheme(
    primary = ElectricIndigo,
    onPrimary = Color.White,
    secondary = NeonCyan,
    onSecondary = DeepCharcoal,
    tertiary = EmeraldGreen,
    onTertiary = DeepCharcoal,
    background = BackgroundSurface,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    error = RoseRed,
    onError = Color.White,
    outline = BorderColor,
    surfaceVariant = DeepCharcoal,
    onSurfaceVariant = TextSecondary
)

private val MeetMindLightColorScheme = lightColorScheme(
    primary = ElectricIndigo,
    onPrimary = Color.White,
    secondary = NeonCyan,
    onSecondary = Color.White,
    tertiary = EmeraldGreen,
    onTertiary = Color.White,
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    error = RoseRed,
    onError = Color.White,
    outline = Color(0xFFD1D9E0),
    surfaceVariant = Color(0xFFF1F3F4),
    onSurfaceVariant = Color(0xFF444746)
)

@Composable
fun MeetMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Premium preference for Dark Theme in AI context, but allowing system toggle
    val colorScheme = if (darkTheme) MeetMindDarkColorScheme else MeetMindLightColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
