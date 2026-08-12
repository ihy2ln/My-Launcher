package com.homelauncher.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.homelauncher.app.model.AppCategory

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap,
    val category: AppCategory = AppCategory.GENERIC,
) {
    val key: String get() = "$packageName/$activityName"
}

fun appKey(packageName: String, activityName: String): String = "$packageName/$activityName"

fun loadInstalledApps(context: Context): List<AppInfo> {
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolvedActivities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)

    return resolvedActivities
        .map { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val drawable = resolveInfo.loadIcon(packageManager)
            val icon = drawable.toBitmap(width = 192, height = 192).asImageBitmap()
            AppInfo(
                label = resolveInfo.loadLabel(packageManager).toString(),
                packageName = packageName,
                activityName = resolveInfo.activityInfo.name,
                icon = icon,
                category = detectAppCategory(context, packageName, resolveInfo.loadLabel(packageManager).toString()),
            )
        }
        .distinctBy { it.key }
        .sortedBy { it.label.lowercase() }
}

/**
 * Inspect ApplicationInfo.category, intent filters, and package/label heuristics
 * to classify apps for themed home widgets.
 */
fun detectAppCategory(context: Context, packageName: String, label: String = ""): AppCategory {
    val pm = context.packageManager
    val appInfo = runCatching {
        pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    }.getOrNull()

    if (appInfo != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAME
                ApplicationInfo.CATEGORY_AUDIO -> return AppCategory.MUSIC
                ApplicationInfo.CATEGORY_VIDEO -> return AppCategory.VIDEO
                ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.IMAGE
                ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_NEWS -> return AppCategory.NEWS
                ApplicationInfo.CATEGORY_MAPS -> return AppCategory.MAPS
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
            }
        } else {
            @Suppress("DEPRECATION")
            if ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
                return AppCategory.GAME
            }
        }
    }

    if (hasIntentFilter(pm, packageName, Intent.ACTION_MEDIA_BUTTON) ||
        hasIntentFilter(pm, packageName, "android.intent.action.MUSIC_PLAYER") ||
        hasCategory(pm, packageName, Intent.CATEGORY_APP_MUSIC)
    ) {
        return AppCategory.MUSIC
    }

    if (canHandleMime(pm, packageName, "audio/*")) return AppCategory.MUSIC
    if (canHandleMime(pm, packageName, "video/*")) return AppCategory.VIDEO

    val haystack = "$packageName $label".lowercase()
    when {
        MUSIC_HINTS.any { haystack.contains(it) } -> return AppCategory.MUSIC
        VIDEO_HINTS.any { haystack.contains(it) } -> return AppCategory.VIDEO
        GAME_HINTS.any { haystack.contains(it) } -> return AppCategory.GAME
    }

    return AppCategory.GENERIC
}

private fun hasIntentFilter(pm: PackageManager, packageName: String, action: String): Boolean {
    val intent = Intent(action)
    return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        .any { it.activityInfo.packageName == packageName }
}

private fun hasCategory(pm: PackageManager, packageName: String, category: String): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
    return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        .any { it.activityInfo.packageName == packageName }
}

private fun canHandleMime(pm: PackageManager, packageName: String, mime: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW).setType(mime)
    return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        .any { it.activityInfo.packageName == packageName }
}

private val MUSIC_HINTS = listOf(
    "spotify", "music", "audio", "podcast", "tidal", "deezer", "pandora",
    "soundcloud", "poweramp", "shazam", "radio", "ytmusic", "youtube.music",
)

private val VIDEO_HINTS = listOf(
    "youtube", "netflix", "hulu", "disney", "twitch", "video", "stream",
    "plex", "vlc", "player", "primevideo", "hbo", "max.android", "tubi",
)

private val GAME_HINTS = listOf(
    "game", "play.games", "roblox", "minecraft", "fortnite", "genshin",
    "candy", "puzzle", "racing", "rpg", "unity", "gameloft", "supercell",
)

fun findApp(apps: List<AppInfo>, key: String?): AppInfo? {
    if (key.isNullOrBlank()) return null
    return apps.firstOrNull { it.key == key }
}

fun launchApp(context: Context, app: AppInfo) {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        component = ComponentName(app.packageName, app.activityName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

fun launchPackageOrUrl(context: Context, packages: List<String>, webFallback: String?): Boolean {
    val pm = context.packageManager
    for (pkg in packages) {
        val launch = pm.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
            return true
        }
    }
    if (!webFallback.isNullOrBlank()) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(webFallback)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            return true
        }
    }
    return false
}

fun expandNotifications(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
    } catch (_: Exception) {
        // Not available on all devices / without permission
    }
}
