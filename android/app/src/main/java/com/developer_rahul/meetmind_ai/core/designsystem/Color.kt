package com.developer_rahul.meetmind_ai.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// MeetMind AI Primary Palette - Constant Action Accents
val ElectricIndigo = Color(0xFF6366F1) // Primary Action
val NeonCyan = Color(0xFF06B6D4)      // AI Highlights
val RoseRed = Color(0xFFEF4444)       // Live/Recording Indicator
val EmeraldGreen = Color(0xFF10B981)   // Active/Verified
val AmberGold = Color(0xFFF59E0B)      // Warning/AI Confidence
val PurpleViolet = Color(0xFFA855F7)   // AI Rep Accent

// Dynamic Palette Properties - Automatically adapt to Light / Dark device theme
val BackgroundSurface: Color
    @Composable get() = MaterialTheme.colorScheme.background

val CardSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val ElevatedSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val CardSurfaceHover: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val DeepCharcoal: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF0D1117) else Color(0xFFF8FAFC)

val TextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

val TextSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val TextDisabled: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

val BorderColor: Color
    @Composable get() = MaterialTheme.colorScheme.outline

val GlassBackground: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xCC131822) else Color(0xF5FFFFFF)

val GlassWhite: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0x1AFFFFFF) else Color(0x0F000000)

val GlassBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

// Dynamic Gradients
val AiGradient = Brush.linearGradient(
    colors = listOf(ElectricIndigo, NeonCyan)
)

val HeroAiGradient = Brush.linearGradient(
    colors = listOf(ElectricIndigo, PurpleViolet, NeonCyan)
)

val LiveGradient = Brush.linearGradient(
    colors = listOf(RoseRed, Color(0xFFF43F5E))
)

val SurfaceGradient: Brush
    @Composable get() = if (isSystemInDarkTheme()) {
        Brush.verticalGradient(listOf(Color(0xFF131822), Color(0xFF090D14)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC)))
    }

val BackgroundGradient: Brush
    @Composable get() = if (isSystemInDarkTheme()) {
        Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF090D14)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC)))
    }

// Semantic colors
val Success = EmeraldGreen
val Warning = AmberGold
val Error = RoseRed
val Info = NeonCyan
