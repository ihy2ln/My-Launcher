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
    BLANK,
    CLOCK,
    WEATHER,
    APP_DRAWER,
    YOUTUBE,
    POWERAMP,
    TWITCH,
    SPOTIFY,
    MUSIC,
    VIDEO,
    GAME,
    SEARCH,
    CALENDAR,
    NOTES,
}

fun WidgetType.displayName(): String = when (this) {
    WidgetType.BLANK -> "App widget"
    WidgetType.CLOCK -> "Clock"
    WidgetType.WEATHER -> "Weather"
    WidgetType.APP_DRAWER -> "App drawer"
    WidgetType.YOUTUBE -> "YouTube"
    WidgetType.POWERAMP -> "Poweramp"
    WidgetType.TWITCH -> "Twitch"
    WidgetType.SPOTIFY -> "Spotify"
    WidgetType.MUSIC -> "Music player"
    WidgetType.VIDEO -> "Video player"
    WidgetType.GAME -> "Game"
    WidgetType.SEARCH -> "Search"
    WidgetType.CALENDAR -> "Calendar"
    WidgetType.NOTES -> "Notes"
}

fun WidgetType.brandColor(): Long = when (this) {
    WidgetType.BLANK -> 0xFF455A64
    WidgetType.CLOCK -> 0xFF2A2A2E
    WidgetType.WEATHER -> 0xFF4A90A4
    WidgetType.APP_DRAWER -> 0xFF82B1FF
    WidgetType.YOUTUBE -> 0xFFFF0000
    WidgetType.POWERAMP -> 0xFFF5A623
    WidgetType.TWITCH -> 0xFF9146FF
    WidgetType.SPOTIFY -> 0xFF1DB954
    WidgetType.MUSIC -> 0xFFE91E63
    WidgetType.VIDEO -> 0xFF1A237E
    WidgetType.GAME -> 0xFF00C853
    WidgetType.SEARCH -> 0xFF4285F4
    WidgetType.CALENDAR -> 0xFFEA4335
    WidgetType.NOTES -> 0xFFFFC107
}

/** Preferred package names to launch for media / utility widgets. */
fun WidgetType.launchPackages(): List<String> = when (this) {
    WidgetType.YOUTUBE -> listOf("com.google.android.youtube", "com.vanced.android.youtube")
    WidgetType.POWERAMP -> listOf("com.maxmpz.audioplayer", "com.maxmpz.audioplayer.unlock")
    WidgetType.TWITCH -> listOf("tv.twitch.android.app")
    WidgetType.SPOTIFY -> listOf("com.spotify.music")
    WidgetType.MUSIC -> listOf(
        "com.google.android.apps.youtube.music",
        "com.spotify.music",
        "com.apple.android.music",
        "com.amazon.mp3",
        "com.pandora.android",
        "com.soundcloud.android",
        "com.maxmpz.audioplayer",
        "com.aspiro.tidal",
        "deezer.android.app",
        "com.sec.android.app.music",
    )
    WidgetType.VIDEO -> listOf(
        "com.netflix.mediaclient",
        "com.disney.disneyplus",
        "com.hulu.plus",
        "com.amazon.avod.thirdpartyclient",
        "com.google.android.videos",
        "com.vudu.android",
        "com.plexapp.android",
        "org.videolan.vlc",
        "com.mxtech.videoplayer.ad",
    )
    WidgetType.GAME -> emptyList()
    WidgetType.SEARCH -> listOf("com.google.android.googlequicksearchbox", "com.android.chrome")
    WidgetType.CALENDAR -> listOf("com.google.android.calendar", "com.samsung.android.calendar")
    WidgetType.NOTES -> listOf("com.google.android.keep", "com.samsung.android.app.notes")
    WidgetType.BLANK -> emptyList()
    else -> emptyList()
}

/** Maps an installed app package to a built-in launcher widget when available. */
fun widgetTypeForPackage(packageName: String): WidgetType? {
    val specific = listOf(
        WidgetType.YOUTUBE, WidgetType.POWERAMP, WidgetType.TWITCH, WidgetType.SPOTIFY,
        WidgetType.SEARCH, WidgetType.CALENDAR, WidgetType.NOTES,
    ).firstOrNull { type ->
        type.launchPackages().any { packageName.equals(it, true) || packageName.startsWith("$it.") }
    }
    if (specific != null) return specific
    return listOf(WidgetType.MUSIC, WidgetType.VIDEO).firstOrNull { type ->
        type.launchPackages().any { packageName.equals(it, true) || packageName.startsWith("$it.") }
    }
}

/**
 * Resolve the best widget theme for an app using package allowlists and [AppCategory] metadata.
 */
fun resolveWidgetTheme(packageName: String, category: AppCategory): WidgetType? =
    widgetTypeForPackage(packageName) ?: category.toWidgetType()

fun WidgetType.webFallback(): String? = when (this) {
    WidgetType.YOUTUBE -> "https://www.youtube.com"
    WidgetType.TWITCH -> "https://www.twitch.tv"
    WidgetType.SPOTIFY -> "https://open.spotify.com"
    WidgetType.SEARCH -> "https://www.google.com"
    else -> null
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
    val homePages: Int = 1,
    val dockSlots: Int = 6,
    val dockBackgroundAlpha: Float = 0.45f,
    val drawerColumns: Int = 4,
    val drawerScroll: DrawerScroll = DrawerScroll.VERTICAL,
    val searchBarPosition: SearchBarPosition = SearchBarPosition.TOP,
    val showDrawerCards: Boolean = true,
    val showSuggestedApps: Boolean = true,
    val showAzScrubber: Boolean = true,
    val showNotificationBadges: Boolean = true,
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
    val title: String? = null,
)

data class FloatingWidget(
    val id: String,
    val type: WidgetType,
    val title: String = "",
    val xFrac: Float = 0.08f,
    val yFrac: Float = 0.22f,
    val widthFrac: Float = 0.42f,
    val heightFrac: Float = 0.16f,
    val appKey: String? = null,
    val linkedType: WidgetType? = null,
    val opacity: Float = 1f,
    /** Bound Android AppWidget id, or [android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID]. */
    val appWidgetId: Int = -1,
    /** Flattened [android.content.ComponentName] of the hosted AppWidgetProvider. */
    val providerFlat: String? = null,
) {
    val hostsNativeWidget: Boolean
        get() = appWidgetId != -1 && !providerFlat.isNullOrBlank()

    fun effectiveType(): WidgetType = when {
        hostsNativeWidget -> WidgetType.BLANK
        type == WidgetType.BLANK && linkedType != null -> linkedType
        else -> type
    }

    companion object {
        fun defaultsFor(type: WidgetType, index: Int = 0): FloatingWidget {
            val id = "fw_${type.name.lowercase()}_${System.currentTimeMillis()}_$index"
            val stagger = (index % 5) * 0.03f
            return when (type) {
                WidgetType.BLANK -> FloatingWidget(
                    id = id, type = type, title = type.displayName(),
                    xFrac = 0.1f, yFrac = 0.26f + stagger, widthFrac = 0.45f, heightFrac = 0.16f,
                )
                WidgetType.CLOCK -> FloatingWidget(
                    id = id, type = type, title = type.displayName(),
                    xFrac = 0.08f, yFrac = 0.18f + stagger, widthFrac = 0.55f, heightFrac = 0.14f,
                )
                WidgetType.WEATHER -> FloatingWidget(
                    id = id, type = type, title = type.displayName(),
                    xFrac = 0.1f, yFrac = 0.34f + stagger, widthFrac = 0.4f, heightFrac = 0.16f,
                )
                WidgetType.APP_DRAWER -> FloatingWidget(
                    id = id, type = type, title = type.displayName(),
                    xFrac = 0.35f, yFrac = 0.52f + stagger, widthFrac = 0.28f, heightFrac = 0.12f,
                )
                WidgetType.YOUTUBE -> FloatingWidget(
                    id = id, type = type, title = type.displayName(),
                    xFrac = 0.08f, yFrac = 0.28f + stagger, widthFrac = 0.5f, heightFrac = 0.14f,
                )
                WidgetType.POWERAMP -> FloatingWidget(
                    id = id, type = type, title = type.displayName(),
                    xFrac = 0.08f, yFrac = 0.44f + stagger, widthFrac = 0.55f, heightFrac = 0.15f,
                )
                WidgetType.TWITCH -> FloatingWidget(
                    id = id, type = type, title = type.displayName(),
                    xFrac = 0.12f, yFrac = 0.36f + stagger, widthFrac = 0.48f, heightFrac = 0.14f,
                )
                WidgetType.SPOTIFY -> FloatingWidget(
                    id = id, type = type, title = type.displayName(),
                    xFrac = 0.1f, yFrac = 0.4f + stagger, widthFrac = 0.52f, heightFrac = 0.15f,
                )
                WidgetType.MUSIC -> FloatingWidget(
                    id = id, type = type, title = type.displayName(),
                    xFrac = 0.08f, yFrac = 0.42f + stagger, widthFrac = 0.55f, heightFrac = 0.16f,
                )
                WidgetType.VIDEO -> FloatingWidget(
                    id = id, type = type, title = type.displayName(),
                    xFrac = 0.1f, yFrac = 0.3f + stagger, widthFrac = 0.58f, heightFrac = 0.18f,
                )
                WidgetType.GAME -> FloatingWidget(
                    id = id, type = type, title = type.displayName(),
                    xFrac = 0.2f, yFrac = 0.36f + stagger, widthFrac = 0.4f, heightFrac = 0.16f,
                )
                WidgetType.SEARCH -> FloatingWidget(
                    id = id, type = type, title = type.displayName(),
                    xFrac = 0.1f, yFrac = 0.2f + stagger, widthFrac = 0.8f, heightFrac = 0.08f,
                )
                WidgetType.CALENDAR -> FloatingWidget(
                    id = id, type = type, title = type.displayName(),
                    xFrac = 0.55f, yFrac = 0.3f + stagger, widthFrac = 0.35f, heightFrac = 0.16f,
                )
                WidgetType.NOTES -> FloatingWidget(
                    id = id, type = type, title = type.displayName(),
                    xFrac = 0.55f, yFrac = 0.48f + stagger, widthFrac = 0.35f, heightFrac = 0.14f,
                )
            }
        }
    }
}

data class LauncherLayout(
    val homeSlots: List<HomeSlot?>,
    val dockSlots: List<HomeSlot?>,
    val folders: Map<String, FolderInfo> = emptyMap(),
    val hiddenApps: Set<String> = emptySet(),
    val drawerGroups: List<DrawerGroup> = emptyList(),
    val moduleStyles: Map<Int, ModuleStyle> = emptyMap(),
    val floatingWidgets: List<FloatingWidget> = emptyList(),
    val appAliases: Map<String, String> = emptyMap(),
)

sealed class HomeSlot {
    data class App(val key: String) : HomeSlot()
    data class Folder(val folderId: String) : HomeSlot()
    /** Legacy grid widget — migrated to [FloatingWidget] on load. */
    data class Widget(val type: WidgetType, val id: String = "w_${type.name.lowercase()}") : HomeSlot()
}

fun LauncherSettings.homeCapacity(): Int =
    homeColumns * homeRows * homePages.coerceAtLeast(1)

fun defaultLayout(homeSize: Int, dockSize: Int) = LauncherLayout(
    homeSlots = List(homeSize) { null },
    dockSlots = List(dockSize) { null },
)

fun displayAppLabel(key: String, systemLabel: String, aliases: Map<String, String>): String =
    aliases[key]?.takeIf { it.isNotBlank() } ?: systemLabel

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
