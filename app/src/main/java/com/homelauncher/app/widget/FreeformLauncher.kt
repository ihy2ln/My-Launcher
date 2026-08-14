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

data class LaunchBoundsResult(
    val launched: Boolean,
    val usedFreeform: Boolean,
    val message: String? = null,
)

fun launchAppInBounds(
    context: Context,
    packageName: String,
    activityName: String?,
    bounds: Rect,
): LaunchBoundsResult {
    val pm = context.packageManager
    val intent = if (!activityName.isNullOrBlank()) {
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(packageName, activityName)
        }
    } else {
        pm.getLaunchIntentForPackage(packageName)
    } ?: return LaunchBoundsResult(false, false, "No launch activity")

    intent.addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
    )

    val options = ActivityOptions.makeBasic()
    var freeform = false
    try {
        val method = ActivityOptions::class.java.getMethod(
            "setLaunchWindowingMode",
            Int::class.javaPrimitiveType,
        )
        method.invoke(options, WINDOWING_MODE_FREEFORM)
        freeform = true
    } catch (t: Throwable) {
        Log.d(TAG, "setLaunchWindowingMode unavailable: ${t.message}")
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        options.setLaunchBounds(bounds)
    }

    return try {
        context.startActivity(intent, options.toBundle())
        LaunchBoundsResult(true, freeform)
    } catch (t: Throwable) {
        Log.w(TAG, "Bounded launch failed — staying in-frame", t)
        LaunchBoundsResult(false, false, t.message)
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
