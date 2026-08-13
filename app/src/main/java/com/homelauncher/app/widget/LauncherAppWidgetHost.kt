package com.homelauncher.app.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle

/**
 * Singleton host for embedding third-party AppWidgets on the launcher home screen.
 * Host id is stable so allocated widget ids survive process restarts when persisted.
 */
object LauncherAppWidgetHost {
    const val HOST_ID = 0x4C41554E // "LAUN"
    const val REQUEST_BIND = 0xB17D

    @Volatile
    private var host: AppWidgetHost? = null

    fun get(context: Context): AppWidgetHost {
        host?.let { return it }
        synchronized(this) {
            host?.let { return it }
            val created = AppWidgetHost(context.applicationContext, HOST_ID)
            created.startListening()
            host = created
            return created
        }
    }

    fun startListening(context: Context) {
        get(context).startListening()
    }

    fun stopListening() {
        runCatching { host?.stopListening() }
    }

    fun allocateId(context: Context): Int = get(context).allocateAppWidgetId()

    fun deleteId(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        runCatching { get(context).deleteAppWidgetId(appWidgetId) }
    }

    fun createView(
        context: Context,
        appWidgetId: Int,
        providerInfo: AppWidgetProviderInfo?,
    ): AppWidgetHostView {
        return get(context).createView(context, appWidgetId, providerInfo)
    }

    /**
     * Attempts to bind [provider] to [appWidgetId].
     * Returns true if bound immediately; false if the user must approve via [bindIntent].
     */
    fun tryBind(
        context: Context,
        appWidgetId: Int,
        provider: ComponentName,
        options: Bundle? = null,
    ): BindResult {
        val manager = AppWidgetManager.getInstance(context)
        val bound = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            manager.bindAppWidgetIdIfAllowed(appWidgetId, provider, options)
        } else {
            @Suppress("DEPRECATION")
            manager.bindAppWidgetIdIfAllowed(appWidgetId, provider)
        }
        if (bound) {
            configureIfNeeded(context, appWidgetId)
            return BindResult.Bound
        }
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
            if (options != null) {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options)
            }
        }
        return BindResult.NeedsPermission(intent)
    }

    fun configureIfNeeded(context: Context, appWidgetId: Int): Intent? {
        val manager = AppWidgetManager.getInstance(context)
        val info = manager.getAppWidgetInfo(appWidgetId) ?: return null
        val configure = info.configure ?: return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun providerInfo(context: Context, appWidgetId: Int): AppWidgetProviderInfo? =
        AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)

    fun providerInfo(context: Context, provider: ComponentName): AppWidgetProviderInfo? =
        AppWidgetManager.getInstance(context).installedProviders
            .firstOrNull { it.provider == provider }

    sealed class BindResult {
        data object Bound : BindResult()
        data class NeedsPermission(val intent: Intent) : BindResult()
    }
}

fun Activity.launchBindWidget(intent: Intent) {
    startActivityForResult(intent, LauncherAppWidgetHost.REQUEST_BIND)
}
