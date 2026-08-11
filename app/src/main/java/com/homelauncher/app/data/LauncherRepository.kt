package com.homelauncher.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.homelauncher.app.model.DrawerGroup
import com.homelauncher.app.model.DrawerScroll
import com.homelauncher.app.model.FolderInfo
import com.homelauncher.app.model.GestureAction
import com.homelauncher.app.model.HomeSlot
import com.homelauncher.app.model.IconShape
import com.homelauncher.app.model.LauncherLayout
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.SearchBarPosition
import com.homelauncher.app.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_launcher_prefs")

class LauncherRepository(private val context: Context) {

    val settings: Flow<LauncherSettings> = context.launcherDataStore.data.map { prefs ->
        LauncherSettings(
            themeMode = ThemeMode.entries.getOrElse(prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.ordinal) { ThemeMode.DARK },
            accentColor = prefs[Keys.ACCENT] ?: 0xFF82B1FF,
            useMaterialYou = prefs[Keys.MATERIAL_YOU] ?: false,
            iconShape = IconShape.entries.getOrElse(prefs[Keys.ICON_SHAPE] ?: IconShape.SQUIRCLE.ordinal) { IconShape.SQUIRCLE },
            iconSizeDp = prefs[Keys.ICON_SIZE] ?: 58,
            labelSizeSp = prefs[Keys.LABEL_SIZE] ?: 12,
            showLabels = prefs[Keys.SHOW_LABELS] ?: true,
            labelColor = prefs[Keys.LABEL_COLOR] ?: 0xFFFFFFFF,
            homeColumns = prefs[Keys.HOME_COLS] ?: 4,
            homeRows = prefs[Keys.HOME_ROWS] ?: 5,
            dockSlots = prefs[Keys.DOCK_SLOTS] ?: 5,
            dockBackgroundAlpha = prefs[Keys.DOCK_ALPHA] ?: 0.4f,
            drawerColumns = prefs[Keys.DRAWER_COLS] ?: 4,
            drawerScroll = DrawerScroll.entries.getOrElse(prefs[Keys.DRAWER_SCROLL] ?: 0) { DrawerScroll.VERTICAL },
            searchBarPosition = SearchBarPosition.entries.getOrElse(prefs[Keys.SEARCH_POS] ?: 0) { SearchBarPosition.TOP },
            wallpaperStyle = prefs[Keys.WALLPAPER] ?: 0,
            swipeUp = GestureAction.entries.getOrElse(prefs[Keys.SWIPE_UP] ?: GestureAction.OPEN_DRAWER.ordinal) { GestureAction.OPEN_DRAWER },
            swipeDown = GestureAction.entries.getOrElse(prefs[Keys.SWIPE_DOWN] ?: GestureAction.OPEN_SEARCH.ordinal) { GestureAction.OPEN_SEARCH },
            doubleTap = GestureAction.entries.getOrElse(prefs[Keys.DOUBLE_TAP] ?: GestureAction.NONE.ordinal) { GestureAction.NONE },
            pinchIn = GestureAction.entries.getOrElse(prefs[Keys.PINCH_IN] ?: GestureAction.OPEN_SETTINGS.ordinal) { GestureAction.OPEN_SETTINGS },
        )
    }

    val layout: Flow<LauncherLayout> = context.launcherDataStore.data.map { prefs ->
        val settingsSnapshot = LauncherSettings(
            homeColumns = prefs[Keys.HOME_COLS] ?: 4,
            homeRows = prefs[Keys.HOME_ROWS] ?: 5,
            dockSlots = prefs[Keys.DOCK_SLOTS] ?: 5,
        )
        val homeSize = settingsSnapshot.homeColumns * settingsSnapshot.homeRows
        val dockSize = settingsSnapshot.dockSlots
        LauncherLayout(
            homeSlots = decodeSlots(prefs[Keys.HOME_SLOTS], homeSize),
            dockSlots = decodeSlots(prefs[Keys.DOCK_SLOTS_DATA], dockSize),
            folders = decodeFolders(prefs[Keys.FOLDERS]),
            hiddenApps = decodeSet(prefs[Keys.HIDDEN]),
            drawerGroups = decodeGroups(prefs[Keys.GROUPS]),
        )
    }

    suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        context.launcherDataStore.edit { prefs ->
            val current = LauncherSettings(
                themeMode = ThemeMode.entries.getOrElse(prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.ordinal) { ThemeMode.DARK },
                accentColor = prefs[Keys.ACCENT] ?: 0xFF82B1FF,
                useMaterialYou = prefs[Keys.MATERIAL_YOU] ?: false,
                iconShape = IconShape.entries.getOrElse(prefs[Keys.ICON_SHAPE] ?: IconShape.SQUIRCLE.ordinal) { IconShape.SQUIRCLE },
                iconSizeDp = prefs[Keys.ICON_SIZE] ?: 58,
                labelSizeSp = prefs[Keys.LABEL_SIZE] ?: 12,
                showLabels = prefs[Keys.SHOW_LABELS] ?: true,
                labelColor = prefs[Keys.LABEL_COLOR] ?: 0xFFFFFFFF,
                homeColumns = prefs[Keys.HOME_COLS] ?: 4,
                homeRows = prefs[Keys.HOME_ROWS] ?: 5,
                dockSlots = prefs[Keys.DOCK_SLOTS] ?: 5,
                dockBackgroundAlpha = prefs[Keys.DOCK_ALPHA] ?: 0.4f,
                drawerColumns = prefs[Keys.DRAWER_COLS] ?: 4,
                drawerScroll = DrawerScroll.entries.getOrElse(prefs[Keys.DRAWER_SCROLL] ?: 0) { DrawerScroll.VERTICAL },
                searchBarPosition = SearchBarPosition.entries.getOrElse(prefs[Keys.SEARCH_POS] ?: 0) { SearchBarPosition.TOP },
                wallpaperStyle = prefs[Keys.WALLPAPER] ?: 0,
                swipeUp = GestureAction.entries.getOrElse(prefs[Keys.SWIPE_UP] ?: GestureAction.OPEN_DRAWER.ordinal) { GestureAction.OPEN_DRAWER },
                swipeDown = GestureAction.entries.getOrElse(prefs[Keys.SWIPE_DOWN] ?: GestureAction.OPEN_SEARCH.ordinal) { GestureAction.OPEN_SEARCH },
                doubleTap = GestureAction.entries.getOrElse(prefs[Keys.DOUBLE_TAP] ?: GestureAction.NONE.ordinal) { GestureAction.NONE },
                pinchIn = GestureAction.entries.getOrElse(prefs[Keys.PINCH_IN] ?: GestureAction.OPEN_SETTINGS.ordinal) { GestureAction.OPEN_SETTINGS },
            )
            val next = transform(current)
            prefs[Keys.THEME_MODE] = next.themeMode.ordinal
            prefs[Keys.ACCENT] = next.accentColor
            prefs[Keys.MATERIAL_YOU] = next.useMaterialYou
            prefs[Keys.ICON_SHAPE] = next.iconShape.ordinal
            prefs[Keys.ICON_SIZE] = next.iconSizeDp
            prefs[Keys.LABEL_SIZE] = next.labelSizeSp
            prefs[Keys.SHOW_LABELS] = next.showLabels
            prefs[Keys.LABEL_COLOR] = next.labelColor
            prefs[Keys.HOME_COLS] = next.homeColumns
            prefs[Keys.HOME_ROWS] = next.homeRows
            prefs[Keys.DOCK_SLOTS] = next.dockSlots
            prefs[Keys.DOCK_ALPHA] = next.dockBackgroundAlpha
            prefs[Keys.DRAWER_COLS] = next.drawerColumns
            prefs[Keys.DRAWER_SCROLL] = next.drawerScroll.ordinal
            prefs[Keys.SEARCH_POS] = next.searchBarPosition.ordinal
            prefs[Keys.WALLPAPER] = next.wallpaperStyle
            prefs[Keys.SWIPE_UP] = next.swipeUp.ordinal
            prefs[Keys.SWIPE_DOWN] = next.swipeDown.ordinal
            prefs[Keys.DOUBLE_TAP] = next.doubleTap.ordinal
            prefs[Keys.PINCH_IN] = next.pinchIn.ordinal

            // Resize grids when dimensions change
            val home = decodeSlots(prefs[Keys.HOME_SLOTS], current.homeColumns * current.homeRows)
            val dock = decodeSlots(prefs[Keys.DOCK_SLOTS_DATA], current.dockSlots)
            prefs[Keys.HOME_SLOTS] = encodeSlots(resizeSlots(home, next.homeColumns * next.homeRows))
            prefs[Keys.DOCK_SLOTS_DATA] = encodeSlots(resizeSlots(dock, next.dockSlots))
        }
    }

    suspend fun setHomeSlot(index: Int, slot: HomeSlot?) {
        mutateLayout { layout ->
            val home = layout.homeSlots.toMutableList()
            if (index in home.indices) home[index] = slot
            layout.copy(homeSlots = home)
        }
    }

    suspend fun setDockSlot(index: Int, slot: HomeSlot?) {
        mutateLayout { layout ->
            val dock = layout.dockSlots.toMutableList()
            if (index in dock.indices) dock[index] = slot
            layout.copy(dockSlots = dock)
        }
    }

    suspend fun createFolder(title: String, appKeys: List<String>, homeIndex: Int) {
        mutateLayout { layout ->
            val id = "folder_${System.currentTimeMillis()}"
            val folder = FolderInfo(id, title, appKeys)
            val home = layout.homeSlots.toMutableList()
            if (homeIndex in home.indices) home[homeIndex] = HomeSlot.Folder(id)
            layout.copy(
                homeSlots = home,
                folders = layout.folders + (id to folder),
            )
        }
    }

    suspend fun updateFolder(folder: FolderInfo) {
        mutateLayout { layout ->
            layout.copy(folders = layout.folders + (folder.id to folder))
        }
    }

    suspend fun deleteFolder(folderId: String) {
        mutateLayout { layout ->
            val home = layout.homeSlots.map {
                if (it is HomeSlot.Folder && it.folderId == folderId) null else it
            }
            layout.copy(homeSlots = home, folders = layout.folders - folderId)
        }
    }

    suspend fun hideApp(key: String) {
        mutateLayout { it.copy(hiddenApps = it.hiddenApps + key) }
    }

    suspend fun unhideApp(key: String) {
        mutateLayout { it.copy(hiddenApps = it.hiddenApps - key) }
    }

    suspend fun saveDrawerGroups(groups: List<DrawerGroup>) {
        mutateLayout { it.copy(drawerGroups = groups) }
    }

    suspend fun exportBackup(): String = buildBackupJson()

    private suspend fun buildBackupJson(): String {
        var settingsJson = JSONObject()
        var layoutJson = JSONObject()
        context.launcherDataStore.edit { prefs ->
            settingsJson = JSONObject().apply {
                put("themeMode", prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.ordinal)
                put("accentColor", prefs[Keys.ACCENT] ?: 0xFF82B1FF)
                put("useMaterialYou", prefs[Keys.MATERIAL_YOU] ?: false)
                put("iconShape", prefs[Keys.ICON_SHAPE] ?: IconShape.SQUIRCLE.ordinal)
                put("iconSizeDp", prefs[Keys.ICON_SIZE] ?: 58)
                put("labelSizeSp", prefs[Keys.LABEL_SIZE] ?: 12)
                put("showLabels", prefs[Keys.SHOW_LABELS] ?: true)
                put("labelColor", prefs[Keys.LABEL_COLOR] ?: 0xFFFFFFFF)
                put("homeColumns", prefs[Keys.HOME_COLS] ?: 4)
                put("homeRows", prefs[Keys.HOME_ROWS] ?: 5)
                put("dockSlots", prefs[Keys.DOCK_SLOTS] ?: 5)
                put("dockBackgroundAlpha", (prefs[Keys.DOCK_ALPHA] ?: 0.4f).toDouble())
                put("drawerColumns", prefs[Keys.DRAWER_COLS] ?: 4)
                put("drawerScroll", prefs[Keys.DRAWER_SCROLL] ?: 0)
                put("searchBarPosition", prefs[Keys.SEARCH_POS] ?: 0)
                put("wallpaperStyle", prefs[Keys.WALLPAPER] ?: 0)
                put("swipeUp", prefs[Keys.SWIPE_UP] ?: GestureAction.OPEN_DRAWER.ordinal)
                put("swipeDown", prefs[Keys.SWIPE_DOWN] ?: GestureAction.OPEN_SEARCH.ordinal)
                put("doubleTap", prefs[Keys.DOUBLE_TAP] ?: GestureAction.NONE.ordinal)
                put("pinchIn", prefs[Keys.PINCH_IN] ?: GestureAction.OPEN_SETTINGS.ordinal)
            }
            layoutJson = JSONObject().apply {
                put("homeSlots", prefs[Keys.HOME_SLOTS] ?: "")
                put("dockSlots", prefs[Keys.DOCK_SLOTS_DATA] ?: "")
                put("folders", prefs[Keys.FOLDERS] ?: "[]")
                put("hidden", prefs[Keys.HIDDEN] ?: "")
                put("groups", prefs[Keys.GROUPS] ?: "[]")
            }
        }
        return JSONObject().apply {
            put("version", 1)
            put("settings", settingsJson)
            put("layout", layoutJson)
        }.toString(2)
    }

    suspend fun importBackup(json: String) {
        val root = JSONObject(json)
        val settingsObj = root.getJSONObject("settings")
        val layoutObj = root.getJSONObject("layout")
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = settingsObj.optInt("themeMode", ThemeMode.DARK.ordinal)
            prefs[Keys.ACCENT] = settingsObj.optLong("accentColor", 0xFF82B1FF)
            prefs[Keys.MATERIAL_YOU] = settingsObj.optBoolean("useMaterialYou", false)
            prefs[Keys.ICON_SHAPE] = settingsObj.optInt("iconShape", IconShape.SQUIRCLE.ordinal)
            prefs[Keys.ICON_SIZE] = settingsObj.optInt("iconSizeDp", 58)
            prefs[Keys.LABEL_SIZE] = settingsObj.optInt("labelSizeSp", 12)
            prefs[Keys.SHOW_LABELS] = settingsObj.optBoolean("showLabels", true)
            prefs[Keys.LABEL_COLOR] = settingsObj.optLong("labelColor", 0xFFFFFFFF)
            prefs[Keys.HOME_COLS] = settingsObj.optInt("homeColumns", 4)
            prefs[Keys.HOME_ROWS] = settingsObj.optInt("homeRows", 5)
            prefs[Keys.DOCK_SLOTS] = settingsObj.optInt("dockSlots", 5)
            prefs[Keys.DOCK_ALPHA] = settingsObj.optDouble("dockBackgroundAlpha", 0.4).toFloat()
            prefs[Keys.DRAWER_COLS] = settingsObj.optInt("drawerColumns", 4)
            prefs[Keys.DRAWER_SCROLL] = settingsObj.optInt("drawerScroll", 0)
            prefs[Keys.SEARCH_POS] = settingsObj.optInt("searchBarPosition", 0)
            prefs[Keys.WALLPAPER] = settingsObj.optInt("wallpaperStyle", 0)
            prefs[Keys.SWIPE_UP] = settingsObj.optInt("swipeUp", GestureAction.OPEN_DRAWER.ordinal)
            prefs[Keys.SWIPE_DOWN] = settingsObj.optInt("swipeDown", GestureAction.OPEN_SEARCH.ordinal)
            prefs[Keys.DOUBLE_TAP] = settingsObj.optInt("doubleTap", GestureAction.NONE.ordinal)
            prefs[Keys.PINCH_IN] = settingsObj.optInt("pinchIn", GestureAction.OPEN_SETTINGS.ordinal)
            prefs[Keys.HOME_SLOTS] = layoutObj.optString("homeSlots", "")
            prefs[Keys.DOCK_SLOTS_DATA] = layoutObj.optString("dockSlots", "")
            prefs[Keys.FOLDERS] = layoutObj.optString("folders", "[]")
            prefs[Keys.HIDDEN] = layoutObj.optString("hidden", "")
            prefs[Keys.GROUPS] = layoutObj.optString("groups", "[]")
        }
    }

    private suspend fun mutateLayout(transform: (LauncherLayout) -> LauncherLayout) {
        context.launcherDataStore.edit { prefs ->
            val homeSize = (prefs[Keys.HOME_COLS] ?: 4) * (prefs[Keys.HOME_ROWS] ?: 5)
            val dockSize = prefs[Keys.DOCK_SLOTS] ?: 5
            val current = LauncherLayout(
                homeSlots = decodeSlots(prefs[Keys.HOME_SLOTS], homeSize),
                dockSlots = decodeSlots(prefs[Keys.DOCK_SLOTS_DATA], dockSize),
                folders = decodeFolders(prefs[Keys.FOLDERS]),
                hiddenApps = decodeSet(prefs[Keys.HIDDEN]),
                drawerGroups = decodeGroups(prefs[Keys.GROUPS]),
            )
            val next = transform(current)
            prefs[Keys.HOME_SLOTS] = encodeSlots(next.homeSlots)
            prefs[Keys.DOCK_SLOTS_DATA] = encodeSlots(next.dockSlots)
            prefs[Keys.FOLDERS] = encodeFolders(next.folders)
            prefs[Keys.HIDDEN] = encodeSet(next.hiddenApps)
            prefs[Keys.GROUPS] = encodeGroups(next.drawerGroups)
        }
    }

    private object Keys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val ACCENT = longPreferencesKey("accent")
        val MATERIAL_YOU = booleanPreferencesKey("material_you")
        val ICON_SHAPE = intPreferencesKey("icon_shape")
        val ICON_SIZE = intPreferencesKey("icon_size")
        val LABEL_SIZE = intPreferencesKey("label_size")
        val SHOW_LABELS = booleanPreferencesKey("show_labels")
        val LABEL_COLOR = longPreferencesKey("label_color")
        val HOME_COLS = intPreferencesKey("home_cols")
        val HOME_ROWS = intPreferencesKey("home_rows")
        val DOCK_SLOTS = intPreferencesKey("dock_slots")
        val DOCK_ALPHA = floatPreferencesKey("dock_alpha")
        val DRAWER_COLS = intPreferencesKey("drawer_cols")
        val DRAWER_SCROLL = intPreferencesKey("drawer_scroll")
        val SEARCH_POS = intPreferencesKey("search_pos")
        val WALLPAPER = intPreferencesKey("wallpaper")
        val SWIPE_UP = intPreferencesKey("swipe_up")
        val SWIPE_DOWN = intPreferencesKey("swipe_down")
        val DOUBLE_TAP = intPreferencesKey("double_tap")
        val PINCH_IN = intPreferencesKey("pinch_in")
        val HOME_SLOTS = stringPreferencesKey("home_slots_v2")
        val DOCK_SLOTS_DATA = stringPreferencesKey("dock_slots_v2")
        val FOLDERS = stringPreferencesKey("folders")
        val HIDDEN = stringPreferencesKey("hidden")
        val GROUPS = stringPreferencesKey("groups")
    }

    companion object {
        private fun resizeSlots(slots: List<HomeSlot?>, size: Int): List<HomeSlot?> =
            List(size) { index -> slots.getOrNull(index) }

        private fun encodeSlots(slots: List<HomeSlot?>): String =
            slots.joinToString("|") {
                when (it) {
                    is HomeSlot.App -> "a:${it.key}"
                    is HomeSlot.Folder -> "f:${it.folderId}"
                    null -> ""
                }
            }

        private fun decodeSlots(raw: String?, size: Int): List<HomeSlot?> {
            val parts = raw?.split("|").orEmpty()
            return List(size) { index ->
                val value = parts.getOrNull(index).orEmpty()
                when {
                    value.startsWith("a:") -> HomeSlot.App(value.removePrefix("a:"))
                    value.startsWith("f:") -> HomeSlot.Folder(value.removePrefix("f:"))
                    value.isNotBlank() && !value.contains(":") -> HomeSlot.App(value) // legacy
                    else -> null
                }
            }
        }

        private fun encodeFolders(folders: Map<String, FolderInfo>): String {
            val array = JSONArray()
            folders.values.forEach { folder ->
                array.put(JSONObject().apply {
                    put("id", folder.id)
                    put("title", folder.title)
                    put("apps", JSONArray(folder.appKeys))
                })
            }
            return array.toString()
        }

        private fun decodeFolders(raw: String?): Map<String, FolderInfo> {
            if (raw.isNullOrBlank()) return emptyMap()
            return try {
                val array = JSONArray(raw)
                buildMap {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val apps = obj.getJSONArray("apps")
                        val keys = List(apps.length()) { apps.getString(it) }
                        val folder = FolderInfo(obj.getString("id"), obj.getString("title"), keys)
                        put(folder.id, folder)
                    }
                }
            } catch (_: Exception) {
                emptyMap()
            }
        }

        private fun encodeGroups(groups: List<DrawerGroup>): String {
            val array = JSONArray()
            groups.forEach { group ->
                array.put(JSONObject().apply {
                    put("id", group.id)
                    put("title", group.title)
                    put("apps", JSONArray(group.appKeys))
                })
            }
            return array.toString()
        }

        private fun decodeGroups(raw: String?): List<DrawerGroup> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val array = JSONArray(raw)
                List(array.length()) { i ->
                    val obj = array.getJSONObject(i)
                    val apps = obj.getJSONArray("apps")
                    DrawerGroup(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        appKeys = List(apps.length()) { apps.getString(it) },
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun encodeSet(set: Set<String>): String = set.joinToString("|")
        private fun decodeSet(raw: String?): Set<String> =
            raw?.split("|")?.filter { it.isNotBlank() }?.toSet().orEmpty()
    }
}
