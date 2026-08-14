package com.homelauncher.app.widget

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.util.Log

private const val TAG = "FreeformLaunch"
private const val WINDOWING_MODE_FREEFORM = 5

/**
 * Launch [packageName] into a freeform / bounded window matching [bounds]
 * so it can sit over the home screen like a PiP frame.
 *
 * Returns true if a launch was attempted successfully.
 */
fun launchAppInBounds(
    context: Context,
    packageName: String,
    activityName: String?,
    bounds: Rect,
): Boolean {
    val pm = context.packageManager
    val intent = if (!activityName.isNullOrBlank()) {
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(packageName, activityName)
        }
    } else {
        pm.getLaunchIntentForPackage(packageName)
    } ?: return false

    intent.addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
    )

    val options = ActivityOptions.makeBasic()
    try {
        // Prefer freeform windowing when the platform exposes it.
        val method = ActivityOptions::class.java.getMethod(
            "setLaunchWindowingMode",
            Int::class.javaPrimitiveType,
        )
        method.invoke(options, WINDOWING_MODE_FREEFORM)
    } catch (t: Throwable) {
        Log.d(TAG, "setLaunchWindowingMode unavailable: ${t.message}")
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        options.setLaunchBounds(bounds)
    }

    return try {
        context.startActivity(intent, options.toBundle())
        true
    } catch (t: Throwable) {
        Log.w(TAG, "Bounded launch failed, falling back", t)
        runCatching {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}

fun densityScaledRect(
    leftFrac: Float,
    topFrac: Float,
    widthFrac: Float,
    heightFrac: Float,
    screenW: Int,
    screenH: Int,
): Rect {
    val left = (leftFrac * screenW).toInt().coerceIn(0, screenW - 80)
    val top = (topFrac * screenH).toInt().coerceIn(0, screenH - 80)
    val right = (left + widthFrac * screenW).toInt().coerceAtMost(screenW)
    val bottom = (top + heightFrac * screenH).toInt().coerceAtMost(screenH)
    return Rect(left, top, right.coerceAtLeast(left + 80), bottom.coerceAtLeast(top + 80))
}
