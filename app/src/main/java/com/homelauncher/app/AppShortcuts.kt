package com.homelauncher.app

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppShortcutItem(
    val id: String,
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val shortcutInfo: ShortcutInfo,
)

fun loadAppShortcuts(context: Context, packageName: String): List<AppShortcutItem> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
    val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return emptyList()
    return try {
        val query = LauncherApps.ShortcutQuery()
            .setPackage(packageName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        val shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle()).orEmpty()
        shortcuts
            .filter { it.isEnabled }
            .take(6)
            .map { sc ->
                val drawable = runCatching { launcherApps.getShortcutIconDrawable(sc, 0) }.getOrNull()
                AppShortcutItem(
                    id = sc.id,
                    packageName = packageName,
                    label = sc.shortLabel?.toString()
                        ?: sc.longLabel?.toString()
                        ?: "Shortcut",
                    icon = drawable?.toBitmap(96, 96)?.asImageBitmap(),
                    shortcutInfo = sc,
                )
            }
    } catch (_: SecurityException) {
        emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

fun startAppShortcut(context: Context, item: AppShortcutItem) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
    val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return
    runCatching {
        launcherApps.startShortcut(item.packageName, item.id, null, null, Process.myUserHandle())
    }
}
