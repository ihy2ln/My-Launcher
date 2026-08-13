package com.homelauncher.app.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Renders a bound Android AppWidget inside Compose.
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
        modifier = modifier.fillMaxSize(),
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
        },
    )
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
