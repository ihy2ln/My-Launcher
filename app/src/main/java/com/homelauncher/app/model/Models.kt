package com.homelauncher.app.model

import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class IconShape {
    SYSTEM,
    CIRCLE,
    SQUIRCLE,
    SQUARE,
    TEARDROP,
}

enum class DrawerScroll { VERTICAL, HORIZONTAL }

enum class SearchBarPosition { TOP, BOTTOM }

enum class GestureAction {
    NONE,
    OPEN_DRAWER,
    OPEN_SEARCH,
    OPEN_SETTINGS,
    EXPAND_NOTIFICATIONS,
}

data class LauncherSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColor: Long = 0xFF82B1FF,
    val useMaterialYou: Boolean = false,
    val iconShape: IconShape = IconShape.SQUIRCLE,
    val iconSizeDp: Int = 58,
    val labelSizeSp: Int = 12,
    val showLabels: Boolean = true,
    val labelColor: Long = 0xFFFFFFFF,
    val homeColumns: Int = 4,
    val homeRows: Int = 5,
    val dockSlots: Int = 5,
    val dockBackgroundAlpha: Float = 0.4f,
    val drawerColumns: Int = 4,
    val drawerScroll: DrawerScroll = DrawerScroll.VERTICAL,
    val searchBarPosition: SearchBarPosition = SearchBarPosition.TOP,
    val wallpaperStyle: Int = 0,
    val swipeUp: GestureAction = GestureAction.OPEN_DRAWER,
    val swipeDown: GestureAction = GestureAction.OPEN_SEARCH,
    val doubleTap: GestureAction = GestureAction.NONE,
    val pinchIn: GestureAction = GestureAction.OPEN_SETTINGS,
)

data class FolderInfo(
    val id: String,
    val title: String,
    val appKeys: List<String>,
)

data class DrawerGroup(
    val id: String,
    val title: String,
    val appKeys: List<String>,
)

data class LauncherLayout(
    val homeSlots: List<HomeSlot?>,
    val dockSlots: List<HomeSlot?>,
    val folders: Map<String, FolderInfo> = emptyMap(),
    val hiddenApps: Set<String> = emptySet(),
    val drawerGroups: List<DrawerGroup> = emptyList(),
)

sealed class HomeSlot {
    data class App(val key: String) : HomeSlot()
    data class Folder(val folderId: String) : HomeSlot()
}

fun defaultLayout(homeSize: Int, dockSize: Int) = LauncherLayout(
    homeSlots = List(homeSize) { null },
    dockSlots = List(dockSize) { null },
)

fun Color.toArgbLong(): Long {
    val a = (alpha * 255).toInt()
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return ((a.toLong() and 0xFF) shl 24) or
        ((r.toLong() and 0xFF) shl 16) or
        ((g.toLong() and 0xFF) shl 8) or
        (b.toLong() and 0xFF)
}

fun Long.toComposeColor(): Color = Color(this)
