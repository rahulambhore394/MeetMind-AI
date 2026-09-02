package com.developer_rahul.meetmind_ai.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// MeetMind AI Primary Palette - Premium Dark Theme
val DeepCharcoal = Color(0xFF0D1117)
val CardSurface = Color(0xFF161B22)
val BorderColor = Color(0xFF30363D)

val ElectricIndigo = Color(0xFF6366F1) // Primary Action
val NeonCyan = Color(0xFF06B6D4)      // AI Highlights
val RoseRed = Color(0xFFEF4444)       // Live/Recording Indicator
val EmeraldGreen = Color(0xFF10B981)   // Active/Verified
val AmberGold = Color(0xFFF59E0B)      // Warning/AI Confidence

// Glassmorphism Overlays
val GlassBackground = Color(0xCC161B22)
val GlassWhite = Color(0x1AFFFFFF)

// Gradients for "AI Intelligence" feel
val AiGradient = Brush.linearGradient(
    colors = listOf(ElectricIndigo, NeonCyan)
)

val SurfaceGradient = Brush.verticalGradient(
    colors = listOf(CardSurface, DeepCharcoal)
)

// Secondary & Neutral
val TextPrimary = Color(0xFFE6EDF3)
val TextSecondary = Color(0xFF8B949E)
val TextDisabled = Color(0xFF484F58)
val BackgroundSurface = Color(0xFF010409)

// Semantic colors
val Success = EmeraldGreen
val Warning = AmberGold
val Error = RoseRed
val Info = NeonCyan
