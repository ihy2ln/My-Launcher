package com.homelauncher.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap,
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
            val drawable = resolveInfo.loadIcon(packageManager)
            val icon = drawable.toBitmap(width = 192, height = 192).asImageBitmap()
            AppInfo(
                label = resolveInfo.loadLabel(packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                activityName = resolveInfo.activityInfo.name,
                icon = icon,
            )
        }
        .distinctBy { it.key }
        .sortedBy { it.label.lowercase() }
}

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
