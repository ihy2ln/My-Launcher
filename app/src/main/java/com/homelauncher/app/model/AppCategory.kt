package com.homelauncher.app.model

/**
 * High-level app kind used to pick a matching home widget theme.
 * Derived from [android.content.pm.ApplicationInfo.category], intent filters, and package heuristics.
 */
enum class AppCategory {
    MUSIC,
    VIDEO,
    GAME,
    IMAGE,
    SOCIAL,
    PRODUCTIVITY,
    NEWS,
    MAPS,
    GENERIC,
}

fun AppCategory.toWidgetType(): WidgetType? = when (this) {
    AppCategory.MUSIC -> WidgetType.MUSIC
    AppCategory.VIDEO -> WidgetType.VIDEO
    AppCategory.GAME -> WidgetType.GAME
    else -> null
}

fun WidgetType.toAppCategory(): AppCategory? = when (this) {
    WidgetType.MUSIC, WidgetType.POWERAMP, WidgetType.SPOTIFY -> AppCategory.MUSIC
    WidgetType.VIDEO, WidgetType.YOUTUBE, WidgetType.TWITCH -> AppCategory.VIDEO
    WidgetType.GAME -> AppCategory.GAME
    else -> null
}
