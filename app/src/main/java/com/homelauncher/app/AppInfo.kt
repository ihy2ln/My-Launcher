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
)

fun loadInstalledApps(context: Context): List<AppInfo> {
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    val resolvedActivities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)

    return resolvedActivities
        .map { resolveInfo ->
            val icon = resolveInfo.loadIcon(packageManager).toBitmap().asImageBitmap()
            AppInfo(
                label = resolveInfo.loadLabel(packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                activityName = resolveInfo.activityInfo.name,
                icon = icon,
            )
        }
        .distinctBy { it.packageName + it.activityName }
        .sortedBy { it.label.lowercase() }
}

fun launchApp(context: Context, app: AppInfo) {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        component = ComponentName(app.packageName, app.activityName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
