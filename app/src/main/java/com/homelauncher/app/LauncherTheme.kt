package com.homelauncher.app

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object LauncherColors {
    val WallpaperTop = Color(0xFF1A237E)
    val WallpaperMid = Color(0xFF4527A0)
    val WallpaperBottom = Color(0xFF880E4F)

    val DockBackground = Color(0x66000000)
    val DrawerBackground = Color(0xFF161A22)
    val SearchBackground = Color(0xFF252A35)
    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.65f)
    val Accent = Color(0xFF82B1FF)

    val WallpaperBrush = Brush.verticalGradient(
        colors = listOf(WallpaperTop, WallpaperMid, WallpaperBottom),
    )
}
