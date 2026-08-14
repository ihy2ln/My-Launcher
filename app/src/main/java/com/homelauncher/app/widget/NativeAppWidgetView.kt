package com.homelauncher.app.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Renders a bound Android AppWidget inside Compose and keeps the provider
 * informed of the real host size so widgets do not stay stuck at min size.
 */
@Composable
fun NativeAppWidgetView(
    appWidgetId: Int,
    providerFlat: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val provider = remember(providerFlat) { parseProvider(providerFlat) }
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    DisposableEffect(Unit) {
        LauncherAppWidgetHost.startListening(context)
        onDispose { /* keep host listening for other widgets */ }
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                if (size.width > 0 && size.height > 0) {
                    updateAppWidgetHostSize(context, appWidgetId, size.width, size.height)
                }
            },
        factory = { ctx ->
            val info = LauncherAppWidgetHost.providerInfo(ctx, appWidgetId)
                ?: provider?.let { LauncherAppWidgetHost.providerInfo(ctx, it) }
            val hostView: AppWidgetHostView = LauncherAppWidgetHost.createView(ctx, appWidgetId, info)
            hostView.setPadding(0, 0, 0, 0)
            hostView.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            hostView
        },
        update = { hostView ->
            val info = LauncherAppWidgetHost.providerInfo(hostView.context, appWidgetId)
                ?: provider?.let { LauncherAppWidgetHost.providerInfo(hostView.context, it) }
            if (info != null) {
                hostView.setAppWidget(appWidgetId, info)
            }
            val w = hostView.width
            val h = hostView.height
            if (w > 0 && h > 0) {
                updateAppWidgetHostSize(hostView.context, appWidgetId, w, h, hostView)
            }
        },
    )
}

fun updateAppWidgetHostSize(
    context: android.content.Context,
    appWidgetId: Int,
    widthPx: Int,
    heightPx: Int,
    hostView: AppWidgetHostView? = null,
) {
    if (widthPx <= 0 || heightPx <= 0) return
    val density = context.resources.displayMetrics.density.coerceAtLeast(0.1f)
    val widthDp = (widthPx / density).toInt().coerceAtLeast(1)
    val heightDp = (heightPx / density).toInt().coerceAtLeast(1)
    val options = Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
    }
    runCatching {
        AppWidgetManager.getInstance(context).updateAppWidgetOptions(appWidgetId, options)
    }
    if (hostView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching {
            hostView.updateAppWidgetSize(
                options,
                listOf(SizeF(widthDp.toFloat(), heightDp.toFloat())),
            )
        }
    }
}

fun bindNativeWidgetForPackage(
    context: android.content.Context,
    packageName: String,
): NativeBindOutcome {
    val preferred = preferredNativeWidget(context, packageName)
        ?: return NativeBindOutcome.NoProvider
    val id = LauncherAppWidgetHost.allocateId(context)
    return when (val result = LauncherAppWidgetHost.tryBind(context, id, preferred.provider)) {
        is LauncherAppWidgetHost.BindResult.Bound -> {
            val configure = LauncherAppWidgetHost.configureIfNeeded(context, id)
            NativeBindOutcome.Success(
                appWidgetId = id,
                provider = preferred.provider,
                configureIntent = configure,
            )
        }
        is LauncherAppWidgetHost.BindResult.NeedsPermission -> NativeBindOutcome.NeedsUserConsent(
            appWidgetId = id,
            provider = preferred.provider,
            bindIntent = result.intent,
        )
    }
}

sealed class NativeBindOutcome {
    data object NoProvider : NativeBindOutcome()
    data class Success(
        val appWidgetId: Int,
        val provider: ComponentName,
        val configureIntent: android.content.Intent?,
    ) : NativeBindOutcome()
    data class NeedsUserConsent(
        val appWidgetId: Int,
        val provider: ComponentName,
        val bindIntent: android.content.Intent,
    ) : NativeBindOutcome()
}
