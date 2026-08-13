package com.homelauncher.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.homelauncher.app.model.FloatingWidget
import com.homelauncher.app.model.FolderInfo
import com.homelauncher.app.model.GestureAction
import com.homelauncher.app.model.HomeSlot
import com.homelauncher.app.model.IconShape
import com.homelauncher.app.model.LauncherLayout
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.ModuleStyle
import com.homelauncher.app.model.SearchBarPosition
import com.homelauncher.app.model.ScrollEffect
import com.homelauncher.app.model.ThemeMode
import com.homelauncher.app.model.WallpaperMode
import com.homelauncher.app.model.WidgetType
import com.homelauncher.app.model.DrawerGroup
import com.homelauncher.app.model.DrawerScroll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_launcher_prefs")

class LauncherRepository(private val context: Context) {

    val settings: Flow<LauncherSettings> = context.launcherDataStore.data.map { prefs -> prefs.toSettings() }

    val layout: Flow<LauncherLayout> = context.launcherDataStore.data.map { prefs ->
        val s = prefs.toSettings()
        val decoded = decodeSlots(prefs[Keys.HOME_SLOTS], s.homeColumns * s.homeRows)
        val migrated = migrateGridWidgets(
            homeSlots = decoded,
            existing = decodeFloatingWidgets(prefs[Keys.FLOATING_WIDGETS]),
        )
        LauncherLayout(
            homeSlots = migrated.homeSlots,
            dockSlots = decodeSlots(prefs[Keys.DOCK_SLOTS_DATA], s.dockSlots),
            folders = decodeFolders(prefs[Keys.FOLDERS]),
            hiddenApps = decodeSet(prefs[Keys.HIDDEN]),
            drawerGroups = decodeGroups(prefs[Keys.GROUPS]),
            moduleStyles = decodeModuleStyles(prefs[Keys.MODULE_STYLES]),
            floatingWidgets = migrated.widgets,
            appAliases = decodeAliases(prefs[Keys.APP_ALIASES]),
        )
    }

    suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        context.launcherDataStore.edit { prefs ->
            val current = prefs.toSettings()
            val next = transform(current)
            writeSettings(prefs, next)
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

    suspend fun setModuleStyle(index: Int, style: ModuleStyle?) {
        mutateLayout { layout ->
            val styles = layout.moduleStyles.toMutableMap()
            if (style == null) styles.remove(index) else styles[index] = style
            layout.copy(moduleStyles = styles)
        }
    }

    suspend fun createFolder(title: String, appKeys: List<String>, homeIndex: Int): String {
        var createdId = ""
        mutateLayout { layout ->
            val id = "folder_${System.currentTimeMillis()}"
            createdId = id
            val folder = FolderInfo(id, title, appKeys)
            val home = layout.homeSlots.toMutableList()
            if (homeIndex in home.indices) home[homeIndex] = HomeSlot.Folder(id)
            layout.copy(homeSlots = home, folders = layout.folders + (id to folder))
        }
        return createdId
    }

    suspend fun updateFolder(folder: FolderInfo) {
        mutateLayout { it.copy(folders = it.folders + (folder.id to folder)) }
    }

    suspend fun deleteFolder(folderId: String) {
        mutateLayout { layout ->
            val home = layout.homeSlots.map {
                if (it is HomeSlot.Folder && it.folderId == folderId) null else it
            }
            layout.copy(homeSlots = home, folders = layout.folders - folderId)
        }
    }

    suspend fun moveAppIntoFolder(fromHomeIndex: Int, folderId: String) {
        mutateLayout { layout ->
            val slot = layout.homeSlots.getOrNull(fromHomeIndex) as? HomeSlot.App ?: return@mutateLayout layout
            val folder = layout.folders[folderId] ?: return@mutateLayout layout
            val home = layout.homeSlots.toMutableList()
            home[fromHomeIndex] = null
            val updated = folder.copy(appKeys = (folder.appKeys + slot.key).distinct())
            layout.copy(
                homeSlots = home,
                folders = layout.folders + (folderId to updated),
            )
        }
    }

    suspend fun moveHomeSlot(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        mutateLayout { layout ->
            val home = layout.homeSlots.toMutableList()
            if (fromIndex !in home.indices || toIndex !in home.indices) return@mutateLayout layout
            val moving = home[fromIndex] ?: return@mutateLayout layout
            val target = home[toIndex]
            when {
                target == null -> {
                    home[toIndex] = moving
                    home[fromIndex] = null
                }
                target is HomeSlot.Folder && moving is HomeSlot.App -> {
                    val folder = layout.folders[target.folderId] ?: return@mutateLayout layout
                    home[fromIndex] = null
                    val updated = folder.copy(appKeys = (folder.appKeys + moving.key).distinct())
                    return@mutateLayout layout.copy(
                        homeSlots = home,
                        folders = layout.folders + (folder.id to updated),
                    )
                }
                target is HomeSlot.App && moving is HomeSlot.App -> {
                    // swap
                    home[fromIndex] = target
                    home[toIndex] = moving
                }
                else -> {
                    // swap generic
                    home[fromIndex] = target
                    home[toIndex] = moving
                }
            }
            layout.copy(homeSlots = home)
        }
    }

    suspend fun hideApp(key: String) = mutateLayout { it.copy(hiddenApps = it.hiddenApps + key) }
    suspend fun unhideApp(key: String) = mutateLayout { it.copy(hiddenApps = it.hiddenApps - key) }
    suspend fun saveDrawerGroups(groups: List<DrawerGroup>) = mutateLayout { it.copy(drawerGroups = groups) }

    suspend fun setAppAlias(key: String, alias: String?) = mutateLayout { layout ->
        val aliases = layout.appAliases.toMutableMap()
        if (alias.isNullOrBlank()) aliases.remove(key) else aliases[key] = alias.trim()
        layout.copy(appAliases = aliases)
    }

    suspend fun addFloatingWidget(type: WidgetType): String {
        var createdId = ""
        mutateLayout { layout ->
            val widget = FloatingWidget.defaultsFor(type, layout.floatingWidgets.size)
            createdId = widget.id
            layout.copy(floatingWidgets = layout.floatingWidgets + widget)
        }
        return createdId
    }

    suspend fun updateFloatingWidget(widget: FloatingWidget) = mutateLayout { layout ->
        layout.copy(floatingWidgets = layout.floatingWidgets.map { if (it.id == widget.id) widget else it })
    }

    suspend fun removeFloatingWidget(id: String) = mutateLayout { layout ->
        val victim = layout.floatingWidgets.firstOrNull { it.id == id }
        if (victim != null && victim.appWidgetId != -1) {
            com.homelauncher.app.widget.LauncherAppWidgetHost.deleteId(context, victim.appWidgetId)
        }
        layout.copy(floatingWidgets = layout.floatingWidgets.filterNot { it.id == id })
    }

    suspend fun bindBlankWidget(
        widgetId: String,
        appKey: String,
        linkedType: WidgetType?,
        appWidgetId: Int = -1,
        providerFlat: String? = null,
    ) = mutateLayout { layout ->
        layout.copy(
            floatingWidgets = layout.floatingWidgets.map { widget ->
                if (widget.id == widgetId) {
                    widget.copy(
                        appKey = appKey,
                        linkedType = linkedType,
                        appWidgetId = appWidgetId,
                        providerFlat = providerFlat,
                    )
                } else widget
            },
        )
    }

    suspend fun exportBackup(): String = buildBackupJson()

    private suspend fun buildBackupJson(): String {
        var settingsJson = JSONObject()
        var layoutJson = JSONObject()
        context.launcherDataStore.edit { prefs ->
            val s = prefs.toSettings()
            settingsJson = JSONObject().apply {
                put("themeMode", s.themeMode.ordinal)
                put("accentColor", s.accentColor)
                put("useMaterialYou", s.useMaterialYou)
                put("iconShape", s.iconShape.ordinal)
                put("iconSizeDp", s.iconSizeDp)
                put("labelSizeSp", s.labelSizeSp)
                put("showLabels", s.showLabels)
                put("labelColor", s.labelColor)
                put("homeColumns", s.homeColumns)
                put("homeRows", s.homeRows)
                put("dockSlots", s.dockSlots)
                put("dockBackgroundAlpha", s.dockBackgroundAlpha.toDouble())
                put("drawerColumns", s.drawerColumns)
                put("drawerScroll", s.drawerScroll.ordinal)
                put("searchBarPosition", s.searchBarPosition.ordinal)
                put("showDrawerCards", s.showDrawerCards)
                put("wallpaperStyle", s.wallpaperStyle)
                put("wallpaperMode", s.wallpaperMode.ordinal)
                put("wallpaperColor", s.wallpaperColor)
                put("wallpaperImageUri", s.wallpaperImageUri ?: "")
                put("wallpaperVideoUri", s.wallpaperVideoUri ?: "")
                put("moduleOpacity", s.moduleOpacity.toDouble())
                put("scrollEffect", s.scrollEffect.ordinal)
                put("swipeUp", s.swipeUp.ordinal)
                put("swipeDown", s.swipeDown.ordinal)
                put("doubleTap", s.doubleTap.ordinal)
                put("pinchIn", s.pinchIn.ordinal)
            }
            layoutJson = JSONObject().apply {
                put("homeSlots", prefs[Keys.HOME_SLOTS] ?: "")
                put("dockSlots", prefs[Keys.DOCK_SLOTS_DATA] ?: "")
                put("folders", prefs[Keys.FOLDERS] ?: "[]")
                put("hidden", prefs[Keys.HIDDEN] ?: "")
                put("groups", prefs[Keys.GROUPS] ?: "[]")
                put("moduleStyles", prefs[Keys.MODULE_STYLES] ?: "{}")
                put("floatingWidgets", prefs[Keys.FLOATING_WIDGETS] ?: "[]")
                put("appAliases", prefs[Keys.APP_ALIASES] ?: "{}")
            }
        }
        return JSONObject().apply {
            put("version", 2)
            put("settings", settingsJson)
            put("layout", layoutJson)
        }.toString(2)
    }

    suspend fun importBackup(json: String) {
        val root = JSONObject(json)
        val settingsObj = root.getJSONObject("settings")
        val layoutObj = root.getJSONObject("layout")
        context.launcherDataStore.edit { prefs ->
            writeSettings(
                prefs,
                LauncherSettings(
                    themeMode = ThemeMode.entries.getOrElse(settingsObj.optInt("themeMode", ThemeMode.DARK.ordinal)) { ThemeMode.DARK },
                    accentColor = settingsObj.optLong("accentColor", 0xFF82B1FF),
                    useMaterialYou = settingsObj.optBoolean("useMaterialYou", false),
                    iconShape = IconShape.entries.getOrElse(settingsObj.optInt("iconShape", IconShape.CIRCLE.ordinal)) { IconShape.CIRCLE },
                    iconSizeDp = settingsObj.optInt("iconSizeDp", 56),
                    labelSizeSp = settingsObj.optInt("labelSizeSp", 11),
                    showLabels = settingsObj.optBoolean("showLabels", true),
                    labelColor = settingsObj.optLong("labelColor", 0xFFFFFFFF),
                    homeColumns = settingsObj.optInt("homeColumns", 5),
                    homeRows = settingsObj.optInt("homeRows", 6),
                    dockSlots = settingsObj.optInt("dockSlots", 6),
                    dockBackgroundAlpha = settingsObj.optDouble("dockBackgroundAlpha", 0.45).toFloat(),
                    drawerColumns = settingsObj.optInt("drawerColumns", 4),
                    drawerScroll = DrawerScroll.entries.getOrElse(settingsObj.optInt("drawerScroll", 0)) { DrawerScroll.VERTICAL },
                    searchBarPosition = SearchBarPosition.entries.getOrElse(settingsObj.optInt("searchBarPosition", 0)) { SearchBarPosition.TOP },
                    showDrawerCards = settingsObj.optBoolean("showDrawerCards", true),
                    wallpaperStyle = settingsObj.optInt("wallpaperStyle", 4),
                    wallpaperMode = WallpaperMode.entries.getOrElse(settingsObj.optInt("wallpaperMode", WallpaperMode.COLOR.ordinal)) { WallpaperMode.COLOR },
                    wallpaperColor = settingsObj.optLong("wallpaperColor", 0xFF3A4F50),
                    wallpaperImageUri = settingsObj.optString("wallpaperImageUri").ifBlank { null },
                    wallpaperVideoUri = settingsObj.optString("wallpaperVideoUri").ifBlank { null },
                    moduleOpacity = settingsObj.optDouble("moduleOpacity", 0.45).toFloat(),
                    scrollEffect = ScrollEffect.entries.getOrElse(settingsObj.optInt("scrollEffect", ScrollEffect.CUBE.ordinal)) { ScrollEffect.CUBE },
                    swipeUp = GestureAction.entries.getOrElse(settingsObj.optInt("swipeUp", GestureAction.OPEN_DRAWER.ordinal)) { GestureAction.OPEN_DRAWER },
                    swipeDown = GestureAction.entries.getOrElse(settingsObj.optInt("swipeDown", GestureAction.OPEN_SEARCH.ordinal)) { GestureAction.OPEN_SEARCH },
                    doubleTap = GestureAction.entries.getOrElse(settingsObj.optInt("doubleTap", GestureAction.NONE.ordinal)) { GestureAction.NONE },
                    pinchIn = GestureAction.entries.getOrElse(settingsObj.optInt("pinchIn", GestureAction.EDIT_HOME.ordinal)) { GestureAction.EDIT_HOME },
                ),
            )
            prefs[Keys.HOME_SLOTS] = layoutObj.optString("homeSlots", "")
            prefs[Keys.DOCK_SLOTS_DATA] = layoutObj.optString("dockSlots", "")
            prefs[Keys.FOLDERS] = layoutObj.optString("folders", "[]")
            prefs[Keys.HIDDEN] = layoutObj.optString("hidden", "")
            prefs[Keys.GROUPS] = layoutObj.optString("groups", "[]")
            prefs[Keys.MODULE_STYLES] = layoutObj.optString("moduleStyles", "{}")
            prefs[Keys.FLOATING_WIDGETS] = layoutObj.optString("floatingWidgets", "[]")
            prefs[Keys.APP_ALIASES] = layoutObj.optString("appAliases", "{}")
        }
    }

    private suspend fun mutateLayout(transform: (LauncherLayout) -> LauncherLayout) {
        context.launcherDataStore.edit { prefs ->
            val s = prefs.toSettings()
            val decoded = decodeSlots(prefs[Keys.HOME_SLOTS], s.homeColumns * s.homeRows)
            val migrated = migrateGridWidgets(
                homeSlots = decoded,
                existing = decodeFloatingWidgets(prefs[Keys.FLOATING_WIDGETS]),
            )
            val current = LauncherLayout(
                homeSlots = migrated.homeSlots,
                dockSlots = decodeSlots(prefs[Keys.DOCK_SLOTS_DATA], s.dockSlots),
                folders = decodeFolders(prefs[Keys.FOLDERS]),
                hiddenApps = decodeSet(prefs[Keys.HIDDEN]),
                drawerGroups = decodeGroups(prefs[Keys.GROUPS]),
                moduleStyles = decodeModuleStyles(prefs[Keys.MODULE_STYLES]),
                floatingWidgets = migrated.widgets,
                appAliases = decodeAliases(prefs[Keys.APP_ALIASES]),
            )
            val next = transform(current)
            prefs[Keys.HOME_SLOTS] = encodeSlots(next.homeSlots)
            prefs[Keys.DOCK_SLOTS_DATA] = encodeSlots(next.dockSlots)
            prefs[Keys.FOLDERS] = encodeFolders(next.folders)
            prefs[Keys.HIDDEN] = encodeSet(next.hiddenApps)
            prefs[Keys.GROUPS] = encodeGroups(next.drawerGroups)
            prefs[Keys.MODULE_STYLES] = encodeModuleStyles(next.moduleStyles)
            prefs[Keys.FLOATING_WIDGETS] = encodeFloatingWidgets(next.floatingWidgets)
            prefs[Keys.APP_ALIASES] = encodeAliases(next.appAliases)
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
        val SHOW_DRAWER_CARDS = booleanPreferencesKey("show_drawer_cards")
        val WALLPAPER = intPreferencesKey("wallpaper")
        val WALLPAPER_MODE = intPreferencesKey("wallpaper_mode")
        val WALLPAPER_COLOR = longPreferencesKey("wallpaper_color")
        val WALLPAPER_IMAGE = stringPreferencesKey("wallpaper_image")
        val WALLPAPER_VIDEO = stringPreferencesKey("wallpaper_video")
        val MODULE_OPACITY = floatPreferencesKey("module_opacity")
        val SCROLL_EFFECT = intPreferencesKey("scroll_effect")
        val SWIPE_UP = intPreferencesKey("swipe_up")
        val SWIPE_DOWN = intPreferencesKey("swipe_down")
        val DOUBLE_TAP = intPreferencesKey("double_tap")
        val PINCH_IN = intPreferencesKey("pinch_in")
        val HOME_SLOTS = stringPreferencesKey("home_slots_v2")
        val DOCK_SLOTS_DATA = stringPreferencesKey("dock_slots_v2")
        val FOLDERS = stringPreferencesKey("folders")
        val HIDDEN = stringPreferencesKey("hidden")
        val GROUPS = stringPreferencesKey("groups")
        val MODULE_STYLES = stringPreferencesKey("module_styles")
        val FLOATING_WIDGETS = stringPreferencesKey("floating_widgets")
        val APP_ALIASES = stringPreferencesKey("app_aliases")
    }

    companion object {
        private fun Preferences.toSettings(): LauncherSettings = LauncherSettings(
            themeMode = ThemeMode.entries.getOrElse(this[Keys.THEME_MODE] ?: ThemeMode.DARK.ordinal) { ThemeMode.DARK },
            accentColor = this[Keys.ACCENT] ?: 0xFF82B1FF,
            useMaterialYou = this[Keys.MATERIAL_YOU] ?: false,
            iconShape = IconShape.entries.getOrElse(this[Keys.ICON_SHAPE] ?: IconShape.CIRCLE.ordinal) { IconShape.CIRCLE },
            iconSizeDp = this[Keys.ICON_SIZE] ?: 56,
            labelSizeSp = this[Keys.LABEL_SIZE] ?: 11,
            showLabels = this[Keys.SHOW_LABELS] ?: true,
            labelColor = this[Keys.LABEL_COLOR] ?: 0xFFFFFFFF,
            homeColumns = this[Keys.HOME_COLS] ?: 5,
            homeRows = this[Keys.HOME_ROWS] ?: 6,
            dockSlots = this[Keys.DOCK_SLOTS] ?: 6,
            dockBackgroundAlpha = this[Keys.DOCK_ALPHA] ?: 0.45f,
            drawerColumns = this[Keys.DRAWER_COLS] ?: 4,
            drawerScroll = DrawerScroll.entries.getOrElse(this[Keys.DRAWER_SCROLL] ?: 0) { DrawerScroll.VERTICAL },
            searchBarPosition = SearchBarPosition.entries.getOrElse(this[Keys.SEARCH_POS] ?: 0) { SearchBarPosition.TOP },
            showDrawerCards = this[Keys.SHOW_DRAWER_CARDS] ?: true,
            wallpaperStyle = this[Keys.WALLPAPER] ?: 4,
            wallpaperMode = WallpaperMode.entries.getOrElse(this[Keys.WALLPAPER_MODE] ?: WallpaperMode.COLOR.ordinal) { WallpaperMode.COLOR },
            wallpaperColor = this[Keys.WALLPAPER_COLOR] ?: 0xFF3A4F50,
            wallpaperImageUri = this[Keys.WALLPAPER_IMAGE],
            wallpaperVideoUri = this[Keys.WALLPAPER_VIDEO],
            moduleOpacity = this[Keys.MODULE_OPACITY] ?: 0.45f,
            scrollEffect = ScrollEffect.entries.getOrElse(this[Keys.SCROLL_EFFECT] ?: ScrollEffect.CUBE.ordinal) { ScrollEffect.CUBE },
            swipeUp = GestureAction.entries.getOrElse(this[Keys.SWIPE_UP] ?: GestureAction.OPEN_DRAWER.ordinal) { GestureAction.OPEN_DRAWER },
            swipeDown = GestureAction.entries.getOrElse(this[Keys.SWIPE_DOWN] ?: GestureAction.OPEN_SEARCH.ordinal) { GestureAction.OPEN_SEARCH },
            doubleTap = GestureAction.entries.getOrElse(this[Keys.DOUBLE_TAP] ?: GestureAction.NONE.ordinal) { GestureAction.NONE },
            pinchIn = GestureAction.entries.getOrElse(this[Keys.PINCH_IN] ?: GestureAction.EDIT_HOME.ordinal) { GestureAction.EDIT_HOME },
        )

        private fun writeSettings(prefs: androidx.datastore.preferences.core.MutablePreferences, s: LauncherSettings) {
            prefs[Keys.THEME_MODE] = s.themeMode.ordinal
            prefs[Keys.ACCENT] = s.accentColor
            prefs[Keys.MATERIAL_YOU] = s.useMaterialYou
            prefs[Keys.ICON_SHAPE] = s.iconShape.ordinal
            prefs[Keys.ICON_SIZE] = s.iconSizeDp
            prefs[Keys.LABEL_SIZE] = s.labelSizeSp
            prefs[Keys.SHOW_LABELS] = s.showLabels
            prefs[Keys.LABEL_COLOR] = s.labelColor
            prefs[Keys.HOME_COLS] = s.homeColumns
            prefs[Keys.HOME_ROWS] = s.homeRows
            prefs[Keys.DOCK_SLOTS] = s.dockSlots
            prefs[Keys.DOCK_ALPHA] = s.dockBackgroundAlpha
            prefs[Keys.DRAWER_COLS] = s.drawerColumns
            prefs[Keys.DRAWER_SCROLL] = s.drawerScroll.ordinal
            prefs[Keys.SEARCH_POS] = s.searchBarPosition.ordinal
            prefs[Keys.SHOW_DRAWER_CARDS] = s.showDrawerCards
            prefs[Keys.WALLPAPER] = s.wallpaperStyle
            prefs[Keys.WALLPAPER_MODE] = s.wallpaperMode.ordinal
            prefs[Keys.WALLPAPER_COLOR] = s.wallpaperColor
            if (s.wallpaperImageUri == null) prefs.remove(Keys.WALLPAPER_IMAGE) else prefs[Keys.WALLPAPER_IMAGE] = s.wallpaperImageUri
            if (s.wallpaperVideoUri == null) prefs.remove(Keys.WALLPAPER_VIDEO) else prefs[Keys.WALLPAPER_VIDEO] = s.wallpaperVideoUri
            prefs[Keys.MODULE_OPACITY] = s.moduleOpacity
            prefs[Keys.SCROLL_EFFECT] = s.scrollEffect.ordinal
            prefs[Keys.SWIPE_UP] = s.swipeUp.ordinal
            prefs[Keys.SWIPE_DOWN] = s.swipeDown.ordinal
            prefs[Keys.DOUBLE_TAP] = s.doubleTap.ordinal
            prefs[Keys.PINCH_IN] = s.pinchIn.ordinal
        }

        private fun resizeSlots(slots: List<HomeSlot?>, size: Int): List<HomeSlot?> =
            List(size) { index -> slots.getOrNull(index) }

        private fun encodeSlots(slots: List<HomeSlot?>): String =
            slots.joinToString("|") {
                when (it) {
                    is HomeSlot.App -> "a:${it.key}"
                    is HomeSlot.Folder -> "f:${it.folderId}"
                    is HomeSlot.Widget -> "w:${it.type.name}:${it.id}"
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
                    value.startsWith("w:") -> {
                        val bits = value.removePrefix("w:").split(":")
                        val type = runCatching { WidgetType.valueOf(bits[0]) }.getOrNull()
                        if (type != null) HomeSlot.Widget(type, bits.getOrNull(1) ?: "w_${type.name.lowercase()}") else null
                    }
                    value.isNotBlank() && !value.contains(":") -> HomeSlot.App(value)
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
                    DrawerGroup(obj.getString("id"), obj.getString("title"), List(apps.length()) { apps.getString(it) })
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun encodeModuleStyles(styles: Map<Int, ModuleStyle>): String {
            val obj = JSONObject()
            styles.forEach { (index, style) ->
                obj.put(
                    index.toString(),
                    JSONObject().apply {
                        put("opacity", style.opacity.toDouble())
                        put("color", style.color)
                        put("saturation", style.saturation.toDouble())
                        put("brightness", style.brightness.toDouble())
                        put("imageUri", style.imageUri ?: "")
                        put("videoUri", style.videoUri ?: "")
                        put("title", style.title ?: "")
                    },
                )
            }
            return obj.toString()
        }

        private fun decodeModuleStyles(raw: String?): Map<Int, ModuleStyle> {
            if (raw.isNullOrBlank()) return emptyMap()
            return try {
                val obj = JSONObject(raw)
                buildMap {
                    obj.keys().forEach { key ->
                        val item = obj.getJSONObject(key)
                        put(
                            key.toInt(),
                            ModuleStyle(
                                opacity = item.optDouble("opacity", 0.45).toFloat(),
                                color = item.optLong("color", 0xFF1A1A1A),
                                saturation = item.optDouble("saturation", 0.2).toFloat(),
                                brightness = item.optDouble("brightness", 0.35).toFloat(),
                                imageUri = item.optString("imageUri").ifBlank { null },
                                videoUri = item.optString("videoUri").ifBlank { null },
                                title = item.optString("title").ifBlank { null },
                            ),
                        )
                    }
                }
            } catch (_: Exception) {
                emptyMap()
            }
        }

        private fun encodeFloatingWidgets(widgets: List<FloatingWidget>): String {
            val array = JSONArray()
            widgets.forEach { w ->
                array.put(JSONObject().apply {
                    put("id", w.id)
                    put("type", w.type.name)
                    put("title", w.title)
                    put("xFrac", w.xFrac.toDouble())
                    put("yFrac", w.yFrac.toDouble())
                    put("widthFrac", w.widthFrac.toDouble())
                    put("heightFrac", w.heightFrac.toDouble())
                    if (w.appKey != null) put("appKey", w.appKey)
                    if (w.linkedType != null) put("linkedType", w.linkedType.name)
                    put("opacity", w.opacity.toDouble())
                    put("appWidgetId", w.appWidgetId)
                    if (w.providerFlat != null) put("providerFlat", w.providerFlat)
                })
            }
            return array.toString()
        }

        private fun decodeFloatingWidgets(raw: String?): List<FloatingWidget> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val array = JSONArray(raw)
                List(array.length()) { i ->
                    val obj = array.getJSONObject(i)
                    val type = runCatching { WidgetType.valueOf(obj.getString("type")) }.getOrDefault(WidgetType.CLOCK)
                    val linkedRaw = if (obj.has("linkedType")) obj.optString("linkedType") else ""
                    val linkedType = linkedRaw.takeIf { it.isNotBlank() }?.let {
                        runCatching { WidgetType.valueOf(it) }.getOrNull()
                    }
                    val appKeyRaw = if (obj.has("appKey")) obj.optString("appKey") else ""
                    val providerFlat = if (obj.has("providerFlat")) obj.optString("providerFlat") else ""
                    FloatingWidget(
                        id = obj.getString("id"),
                        type = type,
                        title = obj.optString("title"),
                        xFrac = obj.optDouble("xFrac", 0.08).toFloat(),
                        yFrac = obj.optDouble("yFrac", 0.22).toFloat(),
                        widthFrac = obj.optDouble("widthFrac", 0.42).toFloat(),
                        heightFrac = obj.optDouble("heightFrac", 0.16).toFloat(),
                        appKey = appKeyRaw.takeIf { it.isNotBlank() },
                        linkedType = linkedType,
                        opacity = obj.optDouble("opacity", 1.0).toFloat(),
                        appWidgetId = obj.optInt("appWidgetId", -1),
                        providerFlat = providerFlat.takeIf { it.isNotBlank() },
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun encodeAliases(aliases: Map<String, String>): String {
            val obj = JSONObject()
            aliases.forEach { (k, v) -> obj.put(k, v) }
            return obj.toString()
        }

        private fun decodeAliases(raw: String?): Map<String, String> {
            if (raw.isNullOrBlank()) return emptyMap()
            return try {
                val obj = JSONObject(raw)
                buildMap {
                    obj.keys().forEach { key -> put(key, obj.getString(key)) }
                }
            } catch (_: Exception) {
                emptyMap()
            }
        }

        private data class MigratedWidgets(
            val homeSlots: List<HomeSlot?>,
            val widgets: List<FloatingWidget>,
        )

        private fun migrateGridWidgets(
            homeSlots: List<HomeSlot?>,
            existing: List<FloatingWidget>,
        ): MigratedWidgets {
            val extras = mutableListOf<FloatingWidget>()
            val cleaned = homeSlots.mapIndexed { index, slot ->
                if (slot is HomeSlot.Widget) {
                    val already = existing.any { it.id == slot.id } || extras.any { it.id == slot.id }
                    if (!already) {
                        extras += FloatingWidget.defaultsFor(slot.type, index).copy(
                            id = slot.id,
                            title = slot.type.name.lowercase().replaceFirstChar { it.titlecase() },
                        )
                    }
                    null
                } else {
                    slot
                }
            }
            return MigratedWidgets(cleaned, existing + extras)
        }

        private fun encodeSet(set: Set<String>): String = set.joinToString("|")
        private fun decodeSet(raw: String?): Set<String> =
            raw?.split("|")?.filter { it.isNotBlank() }?.toSet().orEmpty()
    }
}
