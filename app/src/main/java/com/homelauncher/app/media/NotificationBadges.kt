package com.homelauncher.app.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Notification badge counts keyed by package name.
 * Updated by [MediaNotificationListener] from active status-bar notifications.
 */
object NotificationBadges {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    fun update(fromPackages: Map<String, Int>) {
        _counts.value = fromPackages.filterValues { it > 0 }
    }

    fun clear() {
        _counts.value = emptyMap()
    }

    fun countFor(packageName: String): Int = _counts.value[packageName] ?: 0
}
