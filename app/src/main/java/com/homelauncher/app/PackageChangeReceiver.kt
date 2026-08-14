package com.homelauncher.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Broadcasts when packages are installed/removed/changed so the launcher
 * can reload the app list without a process restart.
 */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_PACKAGE_ADDED ||
            action == Intent.ACTION_PACKAGE_REMOVED ||
            action == Intent.ACTION_PACKAGE_CHANGED ||
            action == Intent.ACTION_PACKAGE_REPLACED
        ) {
            AppsChangedBus.emit()
        }
    }
}

object AppsChangedBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()
    fun emit() {
        _events.tryEmit(Unit)
    }
}
