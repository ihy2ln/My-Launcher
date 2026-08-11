package com.homelauncher.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")

private val HOME_APPS_KEY = stringPreferencesKey("home_apps")
private val DOCK_APPS_KEY = stringPreferencesKey("dock_apps")

const val DOCK_SLOT_COUNT = 5
const val HOME_GRID_COLUMNS = 4
const val HOME_GRID_ROWS = 5

data class LauncherLayout(
    val homeApps: List<String?>,
    val dockApps: List<String?>,
)

fun appKey(app: AppInfo): String = "${app.packageName}/${app.activityName}"

fun findApp(apps: List<AppInfo>, key: String?): AppInfo? {
    if (key.isNullOrBlank()) return null
    return apps.firstOrNull { appKey(it) == key }
}

class LauncherPreferences(private val context: Context) {
    val layout: Flow<LauncherLayout> = context.launcherDataStore.data.map { prefs ->
        LauncherLayout(
            homeApps = decodeSlots(prefs[HOME_APPS_KEY], HOME_GRID_COLUMNS * HOME_GRID_ROWS),
            dockApps = decodeSlots(prefs[DOCK_APPS_KEY], DOCK_SLOT_COUNT),
        )
    }

    suspend fun setHomeSlot(index: Int, key: String?) {
        context.launcherDataStore.edit { prefs ->
            val slots = decodeSlots(prefs[HOME_APPS_KEY], HOME_GRID_COLUMNS * HOME_GRID_ROWS).toMutableList()
            if (index in slots.indices) {
                slots[index] = key
                prefs[HOME_APPS_KEY] = encodeSlots(slots)
            }
        }
    }

    suspend fun setDockSlot(index: Int, key: String?) {
        context.launcherDataStore.edit { prefs ->
            val slots = decodeSlots(prefs[DOCK_APPS_KEY], DOCK_SLOT_COUNT).toMutableList()
            if (index in slots.indices) {
                slots[index] = key
                prefs[DOCK_APPS_KEY] = encodeSlots(slots)
            }
        }
    }

    suspend fun clearHomeSlot(index: Int) {
        setHomeSlot(index, null)
    }

    suspend fun clearDockSlot(index: Int) {
        setDockSlot(index, null)
    }

    private fun decodeSlots(raw: String?, size: Int): List<String?> {
        val values = raw?.split("|").orEmpty()
        return List(size) { index -> values.getOrNull(index)?.takeIf { it.isNotBlank() } }
    }

    private fun encodeSlots(slots: List<String?>): String =
        slots.joinToString("|") { it.orEmpty() }
}
