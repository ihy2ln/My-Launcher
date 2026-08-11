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
    EDIT_HOME,
}

enum class WidgetType {
    CLOCK,
    WEATHER,
    APP_DRAWER,
}

enum class WallpaperMode {
    COLOR,
    GRADIENT,
    IMAGE,
    VIDEO,
}

enum class ScrollEffect {
    SIMPLE,
    CUBE,
    CARD_STACK,
    TABLET,
    REVOLVING_DOOR,
}

data class LauncherSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColor: Long = 0xFF82B1FF,
    val useMaterialYou: Boolean = false,
    val iconShape: IconShape = IconShape.CIRCLE,
    val iconSizeDp: Int = 56,
    val labelSizeSp: Int = 11,
    val showLabels: Boolean = true,
    val labelColor: Long = 0xFFFFFFFF,
    val homeColumns: Int = 5,
    val homeRows: Int = 6,
    val dockSlots: Int = 6,
    val dockBackgroundAlpha: Float = 0.45f,
    val drawerColumns: Int = 5,
    val drawerScroll: DrawerScroll = DrawerScroll.VERTICAL,
    val searchBarPosition: SearchBarPosition = SearchBarPosition.TOP,
    val showDrawerCards: Boolean = true,
    val wallpaperStyle: Int = 4,
    val wallpaperMode: WallpaperMode = WallpaperMode.COLOR,
    val wallpaperColor: Long = 0xFF3A4F50,
    val wallpaperImageUri: String? = null,
    val wallpaperVideoUri: String? = null,
    val moduleOpacity: Float = 0.45f,
    val scrollEffect: ScrollEffect = ScrollEffect.CUBE,
    val swipeUp: GestureAction = GestureAction.OPEN_DRAWER,
    val swipeDown: GestureAction = GestureAction.OPEN_SEARCH,
    val doubleTap: GestureAction = GestureAction.NONE,
    val pinchIn: GestureAction = GestureAction.EDIT_HOME,
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

data class ModuleStyle(
    val opacity: Float = 0.45f,
    val color: Long = 0xFF1A1A1A,
    val saturation: Float = 0.2f,
    val brightness: Float = 0.35f,
    val imageUri: String? = null,
    val videoUri: String? = null,
)

data class LauncherLayout(
    val homeSlots: List<HomeSlot?>,
    val dockSlots: List<HomeSlot?>,
    val folders: Map<String, FolderInfo> = emptyMap(),
    val hiddenApps: Set<String> = emptySet(),
    val drawerGroups: List<DrawerGroup> = emptyList(),
    val moduleStyles: Map<Int, ModuleStyle> = emptyMap(),
)

sealed class HomeSlot {
    data class App(val key: String) : HomeSlot()
    data class Folder(val folderId: String) : HomeSlot()
    data class Widget(val type: WidgetType, val id: String = "w_${type.name.lowercase()}") : HomeSlot()
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
