package com.example.tagnod.ui.theme

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

@Composable
fun TagNodTheme(
    themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    accentColorName: String = "WHITE", // "WHITE", "PURPLE", "TEAL", "TURQUOISE"
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val primaryAccent = getAccentColor(accentColorName)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryAccent,
            onPrimary = if (primaryAccent == Color.White) Color.Black else Color.White,
            background = AmoledBackground,
            onBackground = AmoledTextPrimary,
            surface = AmoledSurface,
            onSurface = AmoledTextPrimary,
            surfaceVariant = AmoledSurfaceVariant,
            onSurfaceVariant = AmoledTextSecondary,
            secondary = primaryAccent,
            onSecondary = AmoledBackground
        )
    } else {
        lightColorScheme(
            primary = primaryAccent,
            onPrimary = if (primaryAccent == Color.White) Color.Black else Color.White,
            background = LightBackground,
            onBackground = LightTextPrimary,
            surface = LightSurface,
            onSurface = LightTextPrimary,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightTextSecondary,
            secondary = primaryAccent,
            onSecondary = LightBackground
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
