package com.homelauncher.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LauncherBackground = Color(0xFF0D1117)
val LauncherSurface = Color(0xFF161B22)
val LauncherSurfaceElevated = Color(0xFF21262D)
val LauncherAccent = Color(0xFF58A6FF)
val LauncherTextPrimary = Color(0xFFF0F6FC)
val LauncherTextSecondary = Color(0xFF8B949E)
val LauncherDockBackground = Color(0xCC1C2128)
val WallpaperGradientTop = Color(0xFF1A1F35)
val WallpaperGradientMid = Color(0xFF0F1525)
val WallpaperGradientBottom = Color(0xFF080B12)

private val LauncherColorScheme = darkColorScheme(
    primary = LauncherAccent,
    onPrimary = LauncherBackground,
    surface = LauncherSurface,
    onSurface = LauncherTextPrimary,
    background = LauncherBackground,
    onBackground = LauncherTextPrimary,
)

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LauncherColorScheme,
        content = content,
    )
}
