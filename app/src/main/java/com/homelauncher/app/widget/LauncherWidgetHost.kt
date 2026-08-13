package com.homelauncher.app.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle

class LauncherWidgetHost(
    context: Context,
    hostId: Int = HOST_ID,
) : AppWidgetHost(context.applicationContext, hostId) {

    fun createHostView(
        context: Context,
        appWidgetId: Int,
        providerInfo: AppWidgetProviderInfo?,
    ): AppWidgetHostView {
        return createView(context, appWidgetId, providerInfo)
    }

    companion object {
        const val HOST_ID = 0x4C41554E // "LAUN"
        const val REQUEST_BIND_APPWIDGET = 9101
        const val REQUEST_CONFIGURE_APPWIDGET = 9102

        @Volatile private var instance: LauncherWidgetHost? = null

        fun get(context: Context): LauncherWidgetHost {
            return instance ?: synchronized(this) {
                instance ?: LauncherWidgetHost(context.applicationContext).also {
                    instance = it
                    it.startListening()
                }
            }
        }
    }
}

data class AppWidgetOffer(
    val provider: AppWidgetProviderInfo,
    val label: String,
    val minWidth: Int,
    val minHeight: Int,
)

fun findAppWidgetProviders(context: Context, packageName: String): List<AppWidgetOffer> {
    val manager = AppWidgetManager.getInstance(context)
    val pm = context.packageManager
    return manager.installedProviders
        .filter { it.provider.packageName == packageName }
        .map { info ->
            AppWidgetOffer(
                provider = info,
                label = info.loadLabel(pm)?.toString() ?: packageName,
                minWidth = info.minWidth,
                minHeight = info.minHeight,
            )
        }
}

fun hasAppWidgetProvider(context: Context, packageName: String): Boolean =
    findAppWidgetProviders(context, packageName).isNotEmpty()

/**
 * Allocate and bind an app widget for [provider]. Returns the widget id when binding is allowed
 * immediately; otherwise returns null and fills [bindIntentOut] with the system bind prompt.
 */
fun allocateAndBindWidget(
    context: Context,
    host: LauncherWidgetHost,
    provider: ComponentName,
    bindIntentOut: MutableList<Intent> = mutableListOf(),
): Int? {
    val manager = AppWidgetManager.getInstance(context)
    val appWidgetId = host.allocateAppWidgetId()
    val allowed = manager.bindAppWidgetIdIfAllowed(appWidgetId, provider)
    if (allowed) {
        return appWidgetId
    }
    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
    }
    bindIntentOut += intent
    return null
}

fun deleteHostWidget(host: LauncherWidgetHost, appWidgetId: Int?) {
    if (appWidgetId != null && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        runCatching { host.deleteAppWidgetId(appWidgetId) }
    }
}

fun providerOptionsBundle(widthDp: Int, heightDp: Int): Bundle {
    return Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            putInt(AppWidgetManager.OPTION_APPWIDGET_SIZES, 1)
        }
    }
}

fun flattenProvider(component: ComponentName): String = component.flattenToString()

fun unflattenProvider(flat: String?): ComponentName? =
    flat?.takeIf { it.isNotBlank() }?.let { ComponentName.unflattenFromString(it) }
