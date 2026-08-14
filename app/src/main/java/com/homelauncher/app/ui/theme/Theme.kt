package com.homelauncher.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.homelauncher.app.model.IconShape
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.ThemeMode
import com.homelauncher.app.model.toComposeColor

data class LauncherPalette(
    val isDark: Boolean,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val drawerBackground: Color,
    val searchBackground: Color,
    val dockBackground: Color,
    val surface: Color,
    val wallpaper: Brush,
)

@Composable
fun rememberPalette(settings: LauncherSettings): LauncherPalette {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val dynamicAccent = if (settings.useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        scheme.primary
    } else null
    val accent = dynamicAccent ?: settings.accentColor.toComposeColor()
    val wallpapers = listOf(
        listOf(Color(0xFF1A237E), Color(0xFF4527A0), Color(0xFF880E4F)),
        listOf(Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF26C6DA)),
        listOf(Color(0xFF1B5E20), Color(0xFF388E3C), Color(0xFF81C784)),
        listOf(Color(0xFF3E2723), Color(0xFF5D4037), Color(0xFF8D6E63)),
        listOf(Color(0xFF263238), Color(0xFF37474F), Color(0xFF546E7A)),
        listOf(Color(0xFFFFF8E1), Color(0xFFFFE082), Color(0xFFFFB300)),
    )
    val colors = wallpapers[settings.wallpaperStyle.coerceIn(wallpapers.indices)]
    return remember(settings, isDark, accent) {
        LauncherPalette(
            isDark = isDark,
            accent = accent,
            textPrimary = if (isDark) Color.White else Color(0xFF121212),
            textSecondary = if (isDark) Color.White.copy(0.65f) else Color(0xFF121212).copy(0.6f),
            drawerBackground = if (isDark) Color(0xFF161A22) else Color(0xFFF5F6FA),
            searchBackground = if (isDark) Color(0xFF252A35) else Color(0xFFE8EAF0),
            dockBackground = Color.Black.copy(alpha = settings.dockBackgroundAlpha),
            surface = if (isDark) Color(0xFF1E232C) else Color.White,
            wallpaper = Brush.verticalGradient(colors),
        )
    }
}

fun iconShape(shape: IconShape): Shape = when (shape) {
    IconShape.SYSTEM, IconShape.SQUIRCLE -> RoundedCornerShape(22.dp)
    IconShape.CIRCLE -> CircleShape
    IconShape.SQUARE -> RoundedCornerShape(8.dp)
    IconShape.TEARDROP -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 8.dp)
}

@Composable
fun HomeLauncherTheme(settings: LauncherSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val palette = rememberPalette(settings)
    val scheme = if (settings.useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (palette.isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (palette.isDark) {
        darkColorScheme(primary = palette.accent, background = palette.drawerBackground, surface = palette.surface)
    } else {
        lightColorScheme(primary = palette.accent, background = palette.drawerBackground, surface = palette.surface)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
