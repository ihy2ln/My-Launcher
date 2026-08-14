package com.homelauncher.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.homelauncher.app.AppInfo
import com.homelauncher.app.model.WidgetType
import com.homelauncher.app.model.launchPackages
import kotlin.math.max

data class AppNativeWidgetInfo(
    val provider: ComponentName,
    val label: String,
    val minWidth: Int,
    val minHeight: Int,
    val targetCellWidth: Int,
    val targetCellHeight: Int,
    val resizeMode: Int,
    val previewImage: Int,
    val score: Int = 0,
)

/**
 * Discover AppWidget providers for [packageName], scored so music/video players
 * (Poweramp, Spotify, VLC, …) prefer their real themed home widgets — the same
 * ones Nova/Lawnchair surface from app metadata.
 */
fun findNativeWidgetsForPackage(context: Context, packageName: String): List<AppNativeWidgetInfo> {
    val manager = AppWidgetManager.getInstance(context) ?: return emptyList()
    val pm = context.packageManager
    return manager.installedProviders
        .asSequence()
        .filter { it.provider.packageName.equals(packageName, ignoreCase = true) }
        .map { info ->
            val label = info.loadLabel(pm)?.toString()
                ?: info.provider.shortClassName.substringAfterLast('.')
            val targetW = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                info.targetCellWidth.coerceAtLeast(0)
            } else {
                0
            }
            val targetH = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                info.targetCellHeight.coerceAtLeast(0)
            } else {
                0
            }
            AppNativeWidgetInfo(
                provider = info.provider,
                label = label,
                minWidth = info.minWidth,
                minHeight = info.minHeight,
                targetCellWidth = targetW,
                targetCellHeight = targetH,
                resizeMode = info.resizeMode,
                previewImage = info.previewImage,
                score = scoreProvider(packageName, info, label, targetW, targetH),
            )
        }
        .sortedByDescending { it.score }
        .toList()
}

fun preferredNativeWidget(context: Context, packageName: String): AppNativeWidgetInfo? =
    findNativeWidgetsForPackage(context, packageName).firstOrNull()

/**
 * Pick the best installed app for a catalog widget type that also ships a
 * native AppWidget provider (Poweramp → Poweramp's own widget, etc.).
 */
fun resolveAppAndNativeWidget(
    context: Context,
    apps: List<AppInfo>,
    type: WidgetType,
): Pair<AppInfo, AppNativeWidgetInfo>? {
    val candidates = candidateAppsForWidgetType(apps, type)
    for (app in candidates) {
        val native = preferredNativeWidget(context, app.packageName)
        if (native != null) return app to native
    }
    return null
}

fun candidateAppsForWidgetType(apps: List<AppInfo>, type: WidgetType): List<AppInfo> {
    val packages = type.launchPackages()
    if (packages.isNotEmpty()) {
        val matched = packages.mapNotNull { pkg ->
            apps.firstOrNull {
                it.packageName.equals(pkg, true) || it.packageName.startsWith("$pkg.")
            }
        }
        if (matched.isNotEmpty()) return matched
    }
    return when (type) {
        WidgetType.MUSIC -> apps.filter { it.category == com.homelauncher.app.model.AppCategory.MUSIC }
        WidgetType.VIDEO -> apps.filter { it.category == com.homelauncher.app.model.AppCategory.VIDEO }
        WidgetType.GAME -> apps.filter { it.category == com.homelauncher.app.model.AppCategory.GAME }
        else -> emptyList()
    }
}

fun resolveInstalledAppForWidgetType(apps: List<AppInfo>, type: WidgetType): AppInfo? =
    candidateAppsForWidgetType(apps, type).firstOrNull()

/** Convert provider min size (px) into home floating-widget fractions. */
fun providerSizeFractions(
    context: Context,
    info: AppNativeWidgetInfo,
    fallbackW: Float = 0.62f,
    fallbackH: Float = 0.22f,
): Pair<Float, Float> {
    val dm = context.resources.displayMetrics
    val screenW = dm.widthPixels.coerceAtLeast(1).toFloat()
    val screenH = usableHeightPx(context).toFloat().coerceAtLeast(1f)
    val density = dm.density.coerceAtLeast(0.1f)
    // AppWidgetProviderInfo minWidth/Height are in px on modern APIs (complex units resolved).
    var wPx = info.minWidth.toFloat()
    var hPx = info.minHeight.toFloat()
    if (wPx < 48f) wPx = info.minWidth * density
    if (hPx < 48f) hPx = info.minHeight * density
    if (info.targetCellWidth > 0) {
        wPx = max(wPx, info.targetCellWidth * (screenW / 5f))
    }
    if (info.targetCellHeight > 0) {
        hPx = max(hPx, info.targetCellHeight * (screenH / 8f))
    }
    val wFrac = (wPx / screenW).coerceIn(0.35f, 0.95f)
    val hFrac = (hPx / screenH).coerceIn(0.14f, 0.55f)
    return if (wFrac < 0.2f || hFrac < 0.08f) fallbackW to fallbackH else wFrac to hFrac
}

private fun usableHeightPx(context: Context): Int {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        wm.currentWindowMetrics.bounds.height()
    } else {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        metrics.heightPixels
    }
}

private fun scoreProvider(
    packageName: String,
    info: AppWidgetProviderInfo,
    label: String,
    targetW: Int,
    targetH: Int,
): Int {
    val pkg = packageName.lowercase()
    val name = (label + " " + info.provider.className).lowercase()
    var score = info.minWidth * info.minHeight / 100
    if (info.resizeMode != 0) score += 2_000
    if (info.previewImage != 0) score += 500
    score += targetW * targetH * 400

    // Prefer real player / media widgets; deprioritize tiny shortcut tiles.
    when {
        name.contains("shortcut") || name.contains("1x1") -> score -= 5_000
        name.contains("icon") && (targetW <= 1 && targetH <= 1) -> score -= 3_000
    }
    val boostHints = listOf(
        "widget", "player", "now playing", "media", "4x", "3x", "2x",
        "poweramp", "spotify", "youtube", "vlc", "music", "video",
    )
    boostHints.forEach { hint ->
        if (name.contains(hint)) score += 1_200
    }

    // Package-specific class preferences (official themed widgets).
    when {
        pkg.startsWith("com.maxmpz.audioplayer") -> {
            if (name.contains("widget")) score += 5_000
            if (name.contains("legacy")) score += 1_000
        }
        pkg.startsWith("com.spotify") -> {
            if (name.contains("widget") || name.contains("media")) score += 4_000
        }
        pkg.contains("youtube") -> {
            if (name.contains("widget") || name.contains("search")) score += 3_500
        }
        pkg.startsWith("org.videolan.vlc") || pkg.startsWith("com.mxtech.videoplayer") -> {
            if (name.contains("widget") || name.contains("video")) score += 4_500
        }
        pkg.startsWith("com.netflix") || pkg.startsWith("com.amazon.avod") -> {
            if (name.contains("widget")) score += 3_000
        }
    }
    return score
}

fun AppWidgetProviderInfo.flattenProvider(): String = provider.flattenToString()

fun parseProvider(flat: String?): ComponentName? =
    flat?.takeIf { it.isNotBlank() }?.let { ComponentName.unflattenFromString(it) }

fun appWidgetMinSizeDp(context: Context, info: AppWidgetProviderInfo): Pair<Int, Int> {
    val density = context.resources.displayMetrics.density
    val w = (info.minWidth / density).toInt().coerceAtLeast(110)
    val h = (info.minHeight / density).toInt().coerceAtLeast(40)
    return w to h
}

fun describeAppForWidget(context: Context, packageName: String): AppWidgetMeta {
    val pm = context.packageManager
    val appInfo = runCatching {
        pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    }.getOrNull()
    val label = appInfo?.let { pm.getApplicationLabel(it).toString() }.orEmpty()
    val version = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0).versionName
        }
    }.getOrNull().orEmpty()
    val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        appInfo?.loadDescription(pm)?.toString().orEmpty()
    } else {
        ""
    }
    val native = findNativeWidgetsForPackage(context, packageName)
    return AppWidgetMeta(
        packageName = packageName,
        label = label,
        versionName = version,
        description = description,
        hasNativeWidget = native.isNotEmpty(),
        nativeWidgetCount = native.size,
        preferredProvider = native.firstOrNull()?.provider,
        preferredLabel = native.firstOrNull()?.label,
    )
}

data class AppWidgetMeta(
    val packageName: String,
    val label: String,
    val versionName: String,
    val description: String,
    val hasNativeWidget: Boolean,
    val nativeWidgetCount: Int,
    val preferredProvider: ComponentName?,
    val preferredLabel: String? = null,
)

/** Widget types that should auto-bind an installed app's official AppWidget. */
fun WidgetType.shouldAutoBindNative(): Boolean = when (this) {
    WidgetType.BLANK,
    WidgetType.POWERAMP,
    WidgetType.SPOTIFY,
    WidgetType.YOUTUBE,
    WidgetType.TWITCH,
    WidgetType.MUSIC,
    WidgetType.VIDEO,
    WidgetType.GAME,
    WidgetType.CALENDAR,
    WidgetType.NOTES,
    WidgetType.SEARCH,
    -> true
    else -> false
}
