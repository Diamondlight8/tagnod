package com.example.tagnod.ui.theme

import androidx.compose.ui.graphics.Color

// Accent Colours
val AccentWhite = Color(0xFFFFFFFF)
val AccentPurple = Color(0xFF9B59B6)
val AccentTeal = Color(0xFF008080)
val AccentTurquoise = Color(0xFF40E0D0)

fun getAccentColor(name: String): Color {
    return when (name.uppercase()) {
        "PURPLE" -> AccentPurple
        "TEAL" -> AccentTeal
        "TURQUOISE" -> AccentTurquoise
        else -> AccentWhite
    }
}

// Dark AMOLED Theme Colors
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF121214)
val AmoledSurfaceVariant = Color(0xFF1C1C1E)
val AmoledTextPrimary = Color(0xFFFFFFFF)
val AmoledTextSecondary = Color(0xFFA0A0A0)

// Light Theme Colors
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF2F2F7)
val LightSurfaceVariant = Color(0xFFE5E5EA)
val LightTextPrimary = Color(0xFF000000)
val LightTextSecondary = Color(0xFF6B6B6B)
