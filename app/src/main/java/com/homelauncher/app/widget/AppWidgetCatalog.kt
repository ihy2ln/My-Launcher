package com.homelauncher.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

data class AppNativeWidgetInfo(
    val provider: ComponentName,
    val label: String,
    val minWidth: Int,
    val minHeight: Int,
    val resizeMode: Int,
    val previewImage: Int,
)

/**
 * Discover whether [packageName] ships Android AppWidget providers.
 * Prefer the largest / most capable provider when multiple exist.
 */
fun findNativeWidgetsForPackage(context: Context, packageName: String): List<AppNativeWidgetInfo> {
    val manager = AppWidgetManager.getInstance(context) ?: return emptyList()
    val pm = context.packageManager
    return manager.installedProviders
        .asSequence()
        .filter { it.provider.packageName.equals(packageName, ignoreCase = true) }
        .map { info ->
            AppNativeWidgetInfo(
                provider = info.provider,
                label = info.loadLabel(pm)?.toString()
                    ?: info.provider.shortClassName.substringAfterLast('.'),
                minWidth = info.minWidth,
                minHeight = info.minHeight,
                resizeMode = info.resizeMode,
                previewImage = info.previewImage,
            )
        }
        .sortedByDescending { it.minWidth * it.minHeight }
        .toList()
}

fun preferredNativeWidget(context: Context, packageName: String): AppNativeWidgetInfo? =
    findNativeWidgetsForPackage(context, packageName).firstOrNull()

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
)
