package com.homelauncher.app

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "launcher_prefs"
private const val KEY_DOCK_APPS = "dock_apps"
private const val KEY_HOME_PAGE_APPS = "home_page_apps"

class LauncherPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDockAppKeys(): List<String> {
        val stored = prefs.getString(KEY_DOCK_APPS, null)
        return if (stored.isNullOrBlank()) emptyList() else stored.split("|")
    }

    fun setDockAppKeys(keys: List<String>) {
        prefs.edit().putString(KEY_DOCK_APPS, keys.joinToString("|")).apply()
    }

    fun getHomePageAppKeys(pageIndex: Int): List<String> {
        val stored = prefs.getString("$KEY_HOME_PAGE_APPS$pageIndex", null)
        return if (stored.isNullOrBlank()) emptyList() else stored.split("|")
    }

    fun setHomePageAppKeys(pageIndex: Int, keys: List<String>) {
        prefs.edit().putString("$KEY_HOME_PAGE_APPS$pageIndex", keys.joinToString("|")).apply()
    }
}

fun AppInfo.uniqueKey(): String = "$packageName/$activityName"

fun List<AppInfo>.findByKey(key: String): AppInfo? =
    firstOrNull { it.uniqueKey() == key }
