package com.homelauncher.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homelauncher.app.data.LauncherRepository
import com.homelauncher.app.findApp
import com.homelauncher.app.launchApp
import com.homelauncher.app.launchPackageOrUrl
import com.homelauncher.app.loadInstalledApps
import com.homelauncher.app.media.MediaNotificationListener
import com.homelauncher.app.model.DrawerGroup
import com.homelauncher.app.model.FloatingWidget
import com.homelauncher.app.model.FolderInfo
import com.homelauncher.app.model.GestureAction
import com.homelauncher.app.model.HomeSlot
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.defaultLayout
import com.homelauncher.app.model.launchPackages
import com.homelauncher.app.model.webFallback
import com.homelauncher.app.ui.components.AppActionSheet
import com.homelauncher.app.ui.components.AppIconView
import com.homelauncher.app.ui.components.openAppInfo
import com.homelauncher.app.ui.components.openUninstall
import com.homelauncher.app.ui.drawer.AppDrawer
import com.homelauncher.app.ui.home.EditHomeScreen
import com.homelauncher.app.ui.home.HomeScreen
import com.homelauncher.app.model.WidgetType
import com.homelauncher.app.model.resolveWidgetTheme
import com.homelauncher.app.ui.home.AppPopoutOverlay
import com.homelauncher.app.ui.search.GlobalSearchOverlay
import com.homelauncher.app.ui.settings.SettingsScreen
import com.homelauncher.app.ui.theme.HomeLauncherTheme
import com.homelauncher.app.ui.theme.rememberPalette
import com.homelauncher.app.widget.LauncherAppWidgetHost
import com.homelauncher.app.widget.NativeBindOutcome
import com.homelauncher.app.widget.bindNativeWidgetForPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LauncherAppWidgetHost.startListening(this)
        enableEdgeToEdge()
        setContent { HomeLauncherApp() }
    }

    override fun onDestroy() {
        if (isFinishing) {
            LauncherAppWidgetHost.stopListening()
        }
        super.onDestroy()
    }
}

private data class PendingNativeBind(
    val widgetId: String,
    val app: AppInfo,
    val linkedType: WidgetType?,
    val appWidgetId: Int,
    val provider: ComponentName,
)

private sealed interface PlacementTarget {
    data class Home(val index: Int) : PlacementTarget
    data class Dock(val index: Int) : PlacementTarget
}

private sealed interface Overlay {
    data object None : Overlay
    data object Drawer : Overlay
    data object GlobalSearch : Overlay
    data object Settings : Overlay
    data object EditHome : Overlay
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLauncherApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { LauncherRepository(context) }

    val settings by repository.settings.collectAsState(initial = LauncherSettings())
    val layout by repository.layout.collectAsState(
        initial = defaultLayout(settings.homeColumns * settings.homeRows, settings.dockSlots),
    )

    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    var returnToEditAfterPick by remember { mutableStateOf(false) }
    var placementTarget by remember { mutableStateOf<PlacementTarget?>(null) }
    var appMenuTarget by remember { mutableStateOf<AppInfo?>(null) }
    var removeTarget by remember { mutableStateOf<PlacementTarget?>(null) }
    var appActionTarget by remember { mutableStateOf<Pair<Int, AppInfo>?>(null) }
    var moveToFolderApp by remember { mutableStateOf<Pair<Int, AppInfo>?>(null) }
    var openFolder by remember { mutableStateOf<FolderInfo?>(null) }
    var folderAddTarget by remember { mutableStateOf<FolderInfo?>(null) }
    var groupPickTarget by remember { mutableStateOf<DrawerGroup?>(null) }
    var groupPickTitle by remember { mutableStateOf("Group") }
    var multiSelectKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var renameAppTarget by remember { mutableStateOf<AppInfo?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var renameFolderTarget by remember { mutableStateOf<FolderInfo?>(null) }
    var renameGroupTarget by remember { mutableStateOf<DrawerGroup?>(null) }
    var createFolderDraft by remember { mutableStateOf<Pair<Int, AppInfo>?>(null) }
    var folderTitle by remember { mutableStateOf("Folder") }
    var groupDialog by remember { mutableStateOf(false) }
    var newGroupTitle by remember { mutableStateOf("") }
    var settingsSection by remember { mutableStateOf<String?>(null) }
    var popoutTarget by remember { mutableStateOf<Pair<AppInfo, WidgetType?>?>(null) }
    var blankWidgetBindTarget by remember { mutableStateOf<String?>(null) }
    var pendingNativeBind by remember { mutableStateOf<PendingNativeBind?>(null) }
    var mediaAccessPrompt by remember { mutableStateOf(false) }
    var pipSessions by remember { mutableStateOf<List<com.homelauncher.app.ui.home.HomePipSession>>(emptyList()) }

    val apps by produceState(initialValue = emptyList<AppInfo>(), context) {
        value = withContext(Dispatchers.Default) { loadInstalledApps(context) }
    }

    DisposableEffect(Unit) {
        LauncherAppWidgetHost.startListening(context)
        if (!MediaNotificationListener.isNotificationAccessEnabled(context)) {
            mediaAccessPrompt = settings.showDrawerCards
        }
        onDispose { }
    }

    fun applyBoundWidget(
        widgetId: String,
        app: AppInfo,
        linked: WidgetType?,
        appWidgetId: Int = -1,
        providerFlat: String? = null,
    ) {
        scope.launch {
            val widget = layout.floatingWidgets.firstOrNull { it.id == widgetId }
            if (widget != null) {
                val hostsNative = appWidgetId != -1 && !providerFlat.isNullOrBlank()
                repository.updateFloatingWidget(
                    widget.copy(
                        appKey = app.key,
                        linkedType = if (hostsNative) null else linked,
                        title = app.label,
                        appWidgetId = appWidgetId,
                        providerFlat = providerFlat,
                        widthFrac = when {
                            hostsNative -> 0.55f
                            linked == WidgetType.VIDEO || linked == WidgetType.YOUTUBE || linked == WidgetType.TWITCH -> 0.58f
                            linked == WidgetType.MUSIC || linked == WidgetType.SPOTIFY || linked == WidgetType.POWERAMP -> 0.55f
                            linked == WidgetType.GAME -> 0.4f
                            else -> widget.widthFrac
                        },
                        heightFrac = when {
                            hostsNative -> 0.22f
                            linked == WidgetType.VIDEO || linked == WidgetType.YOUTUBE -> 0.18f
                            linked == WidgetType.MUSIC || linked == WidgetType.SPOTIFY || linked == WidgetType.POWERAMP -> 0.16f
                            linked == WidgetType.GAME -> 0.16f
                            else -> widget.heightFrac
                        },
                    ),
                )
            } else {
                repository.bindBlankWidget(widgetId, app.key, linked, appWidgetId, providerFlat)
            }
            blankWidgetBindTarget = null
            overlay = if (returnToEditAfterPick) {
                returnToEditAfterPick = false
                Overlay.EditHome
            } else {
                Overlay.None
            }
        }
    }

    val bindWidgetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val pending = pendingNativeBind
        pendingNativeBind = null
        if (pending == null) return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            val configure = LauncherAppWidgetHost.configureIfNeeded(context, pending.appWidgetId)
            if (configure != null) {
                runCatching { context.startActivity(configure) }
            }
            applyBoundWidget(
                widgetId = pending.widgetId,
                app = pending.app,
                linked = pending.linkedType,
                appWidgetId = pending.appWidgetId,
                providerFlat = pending.provider.flattenToString(),
            )
        } else {
            LauncherAppWidgetHost.deleteId(context, pending.appWidgetId)
            // Fall back to metadata / in-widget operate mode
            applyBoundWidget(pending.widgetId, pending.app, pending.linkedType)
        }
    }

    fun bindAppToBlankWidget(widgetId: String, app: AppInfo) {
        val linked = resolveWidgetTheme(app.packageName, app.category)
        when (val outcome = bindNativeWidgetForPackage(context, app.packageName)) {
            is NativeBindOutcome.Success -> {
                if (outcome.configureIntent != null) {
                    runCatching { context.startActivity(outcome.configureIntent) }
                }
                applyBoundWidget(
                    widgetId = widgetId,
                    app = app,
                    linked = linked,
                    appWidgetId = outcome.appWidgetId,
                    providerFlat = outcome.provider.flattenToString(),
                )
            }
            is NativeBindOutcome.NeedsUserConsent -> {
                pendingNativeBind = PendingNativeBind(
                    widgetId = widgetId,
                    app = app,
                    linkedType = linked,
                    appWidgetId = outcome.appWidgetId,
                    provider = outcome.provider,
                )
                bindWidgetLauncher.launch(outcome.bindIntent)
            }
            NativeBindOutcome.NoProvider -> applyBoundWidget(widgetId, app, linked)
        }
    }

    fun runGesture(action: GestureAction) {
        if (overlay != Overlay.None && overlay != Overlay.EditHome) return
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.OPEN_DRAWER -> {
                placementTarget = null
                overlay = Overlay.Drawer
            }
            GestureAction.OPEN_SEARCH -> {
                placementTarget = null
                overlay = Overlay.GlobalSearch
            }
            GestureAction.OPEN_SETTINGS -> overlay = Overlay.Settings
            GestureAction.EXPAND_NOTIFICATIONS -> expandNotifications(context)
            GestureAction.EDIT_HOME -> overlay = Overlay.EditHome
        }
    }

    fun openSearch() {
        placementTarget = null
        overlay = Overlay.GlobalSearch
    }

    fun openAppPip(
        app: AppInfo,
        theme: WidgetType? = null,
        widget: FloatingWidget? = null,
    ) {
        val existing = pipSessions.firstOrNull { it.app.key == app.key }
        if (existing != null) {
            // Bring to front by reordering
            pipSessions = (pipSessions.filterNot { it.id == existing.id }) + existing
            return
        }
        val session = com.homelauncher.app.ui.home.HomePipSession(
            id = "pip_${app.packageName}_${System.currentTimeMillis()}",
            app = app,
            label = layout.appAliases[app.key] ?: app.label,
            theme = theme ?: widget?.effectiveType()?.takeIf { it != WidgetType.BLANK },
            widgetId = widget?.id,
            appWidgetId = widget?.appWidgetId ?: -1,
            providerFlat = widget?.providerFlat,
            xFrac = widget?.xFrac?.coerceIn(0.04f, 0.3f) ?: 0.1f,
            yFrac = widget?.yFrac?.coerceIn(0.12f, 0.4f) ?: 0.2f,
            widthFrac = (widget?.widthFrac?.coerceAtLeast(0.55f) ?: 0.72f).coerceIn(0.55f, 0.92f),
            heightFrac = (widget?.heightFrac?.coerceAtLeast(0.32f) ?: 0.42f).coerceIn(0.32f, 0.7f),
        )
        pipSessions = pipSessions + session
    }

    fun handleFloatingWidgetClick(widget: FloatingWidget) {
        if (widget.hostsNativeWidget) {
            // Expand native widget into a larger PiP frame for easier use
            val boundApp = widget.appKey?.let { findApp(apps, it) }
            if (boundApp != null) openAppPip(boundApp, widget.effectiveType(), widget)
            return
        }
        val boundApp = widget.appKey?.let { findApp(apps, it) }
        when (val type = widget.effectiveType()) {
            WidgetType.APP_DRAWER -> overlay = Overlay.Drawer
            WidgetType.CLOCK, WidgetType.WEATHER -> Unit
            WidgetType.BLANK -> {
                if (boundApp != null) openAppPip(boundApp, null, widget)
            }
            WidgetType.GAME,
            WidgetType.MUSIC, WidgetType.VIDEO, WidgetType.POWERAMP, WidgetType.SPOTIFY,
            WidgetType.YOUTUBE, WidgetType.TWITCH,
            -> {
                if (boundApp != null) {
                    openAppPip(boundApp, type, widget)
                } else {
                    launchPackageOrUrl(context, type.launchPackages(), type.webFallback())
                }
            }
            else -> {
                if (boundApp != null) {
                    openAppPip(boundApp, type, widget)
                } else {
                    launchPackageOrUrl(context, type.launchPackages(), type.webFallback())
                }
            }
        }
    }

    HomeLauncherTheme(settings) {
        val palette = rememberPalette(settings)
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeScreen(
                    layout = layout,
                    apps = apps,
                    settings = settings,
                    palette = palette,
                    onGesture = ::runGesture,
                    onLaunch = { launchApp(context, it) },
                    onOpenFolder = { openFolder = it },
                    onWidgetClick = { type ->
                        when (type) {
                            com.homelauncher.app.model.WidgetType.APP_DRAWER -> overlay = Overlay.Drawer
                            com.homelauncher.app.model.WidgetType.CLOCK,
                            com.homelauncher.app.model.WidgetType.WEATHER -> Unit
                            else -> launchPackageOrUrl(context, type.launchPackages(), type.webFallback())
                        }
                    },
                    onEmptyHomeSlot = { index ->
                        overlay = Overlay.EditHome
                        placementTarget = PlacementTarget.Home(index)
                    },
                    onEmptyDockSlot = { index ->
                        placementTarget = PlacementTarget.Dock(index)
                        overlay = Overlay.Drawer
                    },
                    onEditHome = { overlay = Overlay.EditHome },
                    onOpenSearch = ::openSearch,
                    onFloatingWidgetClick = ::handleFloatingWidgetClick,
                    onFloatingWidgetMove = { widget, x, y ->
                        scope.launch { repository.updateFloatingWidget(widget.copy(xFrac = x, yFrac = y)) }
                    },
                    onFloatingWidgetResize = { widget, w, h ->
                        scope.launch { repository.updateFloatingWidget(widget.copy(widthFrac = w, heightFrac = h)) }
                    },
                )

                if (overlay == Overlay.EditHome) {
                    EditHomeScreen(
                        layout = layout,
                        apps = apps,
                        settings = settings,
                        palette = palette,
                        repository = repository,
                        onDone = {
                            overlay = Overlay.None
                            placementTarget = null
                        },
                        onPickAppForSlot = { index ->
                            placementTarget = PlacementTarget.Home(index)
                            returnToEditAfterPick = true
                            overlay = Overlay.Drawer
                        },
                        onPickAppForBlankWidget = { widgetId ->
                            blankWidgetBindTarget = widgetId
                            returnToEditAfterPick = true
                            overlay = Overlay.Drawer
                        },
                        onAddAppsToFolder = { folder ->
                            folderAddTarget = folder
                            multiSelectKeys = emptySet()
                            returnToEditAfterPick = true
                            overlay = Overlay.Drawer
                        },
                        onOpenSettings = {
                            returnToEditAfterPick = true
                            overlay = Overlay.Settings
                        },
                        onOpenSearch = ::openSearch,
                    )
                }

                GlobalSearchOverlay(
                    visible = overlay == Overlay.GlobalSearch,
                    apps = apps,
                    hiddenApps = layout.hiddenApps,
                    palette = palette,
                    onLaunchApp = { launchApp(context, it) },
                    onOpenSettingsSection = { sectionId ->
                        settingsSection = sectionId
                        overlay = Overlay.Settings
                    },
                    onAddWidget = { type ->
                        scope.launch { repository.addFloatingWidget(type) }
                        overlay = Overlay.EditHome
                    },
                    onClose = { overlay = Overlay.None },
                )

                AnimatedVisibility(
                    visible = overlay == Overlay.Drawer,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                ) {
                    AppDrawer(
                        apps = apps,
                        hiddenApps = layout.hiddenApps,
                        groups = layout.drawerGroups,
                        settings = settings,
                        palette = palette,
                        placementHint = when {
                            blankWidgetBindTarget != null -> "Tap an app to bind to this widget"
                            folderAddTarget != null -> "Select apps for ${folderAddTarget!!.title}"
                            groupPickTarget != null -> "Select apps for ${groupPickTitle.ifBlank { "group" }}"
                            else -> placementTarget?.let { target ->
                                when (target) {
                                    is PlacementTarget.Home -> "Tap an app to place on home screen"
                                    is PlacementTarget.Dock -> "Tap an app to place in dock"
                                }
                            }
                        },
                        selectionMode = folderAddTarget != null || groupPickTarget != null,
                        selectedKeys = multiSelectKeys,
                        appAliases = layout.appAliases,
                        onToggleSelect = { app ->
                            multiSelectKeys = if (app.key in multiSelectKeys) {
                                multiSelectKeys - app.key
                            } else {
                                multiSelectKeys + app.key
                            }
                        },
                        onConfirmSelection = {
                            val keys = multiSelectKeys
                            scope.launch {
                                when {
                                    folderAddTarget != null -> {
                                        val folder = folderAddTarget!!
                                        val updated = folder.copy(appKeys = (folder.appKeys + keys).distinct())
                                        repository.updateFolder(updated)
                                        folderAddTarget = null
                                        if (returnToEditAfterPick) {
                                            returnToEditAfterPick = false
                                            overlay = Overlay.EditHome
                                        } else {
                                            openFolder = updated
                                            overlay = Overlay.None
                                        }
                                    }
                                    groupPickTarget != null -> {
                                        val title = groupPickTitle.ifBlank { "Group" }
                                        val existing = groupPickTarget
                                        val seed = appMenuTarget?.key
                                        val allKeys = (keys + listOfNotNull(seed)).distinct()
                                        val groups = if (existing != null && layout.drawerGroups.any { it.id == existing.id }) {
                                            layout.drawerGroups.map {
                                                if (it.id == existing.id) {
                                                    it.copy(title = title, appKeys = (it.appKeys + allKeys).distinct())
                                                } else it
                                            }
                                        } else {
                                            layout.drawerGroups + DrawerGroup(
                                                id = existing?.id ?: "group_${System.currentTimeMillis()}",
                                                title = title,
                                                appKeys = allKeys,
                                            )
                                        }
                                        repository.saveDrawerGroups(groups)
                                        groupPickTarget = null
                                        groupDialog = false
                                        appMenuTarget = null
                                        overlay = Overlay.None
                                    }
                                }
                                multiSelectKeys = emptySet()
                            }
                        },
                        onLaunch = { app ->
                            if (folderAddTarget != null || groupPickTarget != null) {
                                multiSelectKeys = if (app.key in multiSelectKeys) {
                                    multiSelectKeys - app.key
                                } else {
                                    multiSelectKeys + app.key
                                }
                            } else {
                            val bindWidgetId = blankWidgetBindTarget
                            if (bindWidgetId != null) {
                                bindAppToBlankWidget(bindWidgetId, app)
                            } else {
                            val target = placementTarget
                            if (target == null) {
                                overlay = Overlay.None
                                launchApp(context, app)
                            } else {
                                scope.launch {
                                    when (target) {
                                        is PlacementTarget.Home -> {
                                            val existing = layout.homeSlots.getOrNull(target.index)
                                            if (existing is HomeSlot.App) {
                                                createFolderDraft = target.index to app
                                                folderTitle = "Folder"
                                            } else {
                                                repository.setHomeSlot(target.index, HomeSlot.App(app.key))
                                            }
                                        }
                                        is PlacementTarget.Dock -> {
                                            repository.setDockSlot(target.index, HomeSlot.App(app.key))
                                        }
                                    }
                                    placementTarget = null
                                    overlay = if (returnToEditAfterPick) {
                                        returnToEditAfterPick = false
                                        Overlay.EditHome
                                    } else {
                                        Overlay.None
                                    }
                                }
                            }
                            }
                            }
                        },
                        onLongPress = { appMenuTarget = it },
                        onDismissPlacement = {
                            placementTarget = null
                            folderAddTarget = null
                            groupPickTarget = null
                            groupDialog = false
                            blankWidgetBindTarget = null
                            multiSelectKeys = emptySet()
                            if (returnToEditAfterPick) {
                                returnToEditAfterPick = false
                                overlay = Overlay.EditHome
                            }
                        },
                        onClose = {
                            placementTarget = null
                            folderAddTarget = null
                            groupPickTarget = null
                            blankWidgetBindTarget = null
                            multiSelectKeys = emptySet()
                            overlay = if (returnToEditAfterPick) {
                                returnToEditAfterPick = false
                                Overlay.EditHome
                            } else {
                                Overlay.None
                            }
                        },
                    )
                }

                if (overlay == Overlay.Settings) {
                    SettingsScreen(
                        settings = settings,
                        palette = palette,
                        repository = repository,
                        initialSection = settingsSection,
                        onOpenSearch = ::openSearch,
                        onBack = {
                            settingsSection = null
                            overlay = if (returnToEditAfterPick) {
                                returnToEditAfterPick = false
                                Overlay.EditHome
                            } else {
                                Overlay.None
                            }
                        },
                    )
                }
            }
        }

        popoutTarget?.let { (app, theme) ->
            AppPopoutOverlay(
                app = app,
                label = layout.appAliases[app.key] ?: app.label,
                settings = settings,
                palette = palette,
                theme = theme,
                onOpen = {
                    popoutTarget = null
                    launchApp(context, app)
                },
                onDismiss = { popoutTarget = null },
            )
        }

        if (pipSessions.isNotEmpty() && overlay == Overlay.None) {
            com.homelauncher.app.ui.home.HomePipLayer(
                sessions = pipSessions,
                settings = settings,
                palette = palette,
                onClose = { id -> pipSessions = pipSessions.filterNot { it.id == id } },
                onExpandFullscreen = { app ->
                    pipSessions = emptyList()
                    launchApp(context, app)
                },
                onUpdateBounds = { updated ->
                    pipSessions = pipSessions.map { if (it.id == updated.id) updated else it }
                },
                onNativeBound = { id, appWidgetId, providerFlat ->
                    pipSessions = pipSessions.map {
                        if (it.id == id) it.copy(appWidgetId = appWidgetId, providerFlat = providerFlat) else it
                    }
                    // Persist onto the source floating widget when present
                    val session = pipSessions.firstOrNull { it.id == id }
                    val widgetId = session?.widgetId
                    if (widgetId != null) {
                        scope.launch {
                            val widget = layout.floatingWidgets.firstOrNull { it.id == widgetId } ?: return@launch
                            repository.updateFloatingWidget(
                                widget.copy(appWidgetId = appWidgetId, providerFlat = providerFlat),
                            )
                        }
                    }
                },
            )
        }

        if (mediaAccessPrompt) {
            AlertDialog(
                onDismissRequest = { mediaAccessPrompt = false },
                title = { Text("Show real media?") },
                text = {
                    Text(
                        "Allow notification access so the drawer Now Playing card and music widgets " +
                            "can read the active media session and respond to play/pause.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        mediaAccessPrompt = false
                        MediaNotificationListener.openNotificationAccessSettings(context)
                    }) { Text("Enable") }
                },
                dismissButton = {
                    TextButton(onClick = { mediaAccessPrompt = false }) { Text("Later") }
                },
            )
        }

        BackHandler(enabled = overlay != Overlay.None || openFolder != null || popoutTarget != null || pipSessions.isNotEmpty()) {
            when {
                pipSessions.isNotEmpty() -> pipSessions = pipSessions.dropLast(1)
                popoutTarget != null -> popoutTarget = null
                openFolder != null -> openFolder = null
                overlay != Overlay.None -> {
                    overlay = Overlay.None
                    placementTarget = null
                }
            }
        }

        appMenuTarget?.let { app ->
            AlertDialog(
                onDismissRequest = { appMenuTarget = null },
                title = { Text(app.label) },
                text = { Text("Add to home, dock, hide from drawer, or add to a group.") },
                confirmButton = {
                    Column {
                        TextButton(onClick = {
                            scope.launch {
                                val index = layout.homeSlots.indexOfFirst { it == null }
                                if (index >= 0) repository.setHomeSlot(index, HomeSlot.App(app.key))
                            }
                            appMenuTarget = null
                        }) { Text("Add to home") }
                        TextButton(onClick = {
                            scope.launch {
                                val index = layout.dockSlots.indexOfFirst { it == null }
                                if (index >= 0) repository.setDockSlot(index, HomeSlot.App(app.key))
                            }
                            appMenuTarget = null
                        }) { Text("Add to dock") }
                        TextButton(onClick = {
                            scope.launch { repository.hideApp(app.key) }
                            appMenuTarget = null
                        }) { Text("Hide app") }
                        TextButton(onClick = {
                            groupDialog = true
                        }) { Text("Add to group…") }
                        TextButton(onClick = {
                            renameAppTarget = app
                            renameDraft = layout.appAliases[app.key] ?: app.label
                            appMenuTarget = null
                        }) { Text("Rename") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { appMenuTarget = null }) { Text("Cancel") }
                },
            )
        }

        if (groupDialog && appMenuTarget != null) {
            val app = appMenuTarget!!
            AlertDialog(
                onDismissRequest = { groupDialog = false },
                title = { Text("Drawer group") },
                text = {
                    Column {
                        Text("Create or add to a custom drawer tab.")
                        TextField(
                            value = newGroupTitle,
                            onValueChange = { newGroupTitle = it },
                            label = { Text("Group name") },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val title = newGroupTitle.ifBlank { "Group" }
                        groupPickTitle = title
                        val existing = layout.drawerGroups.firstOrNull { it.title.equals(title, true) }
                        groupPickTarget = existing ?: DrawerGroup(
                            id = "group_${System.currentTimeMillis()}",
                            title = title,
                            appKeys = emptyList(),
                        )
                        multiSelectKeys = setOf(app.key)
                        groupDialog = false
                        overlay = Overlay.Drawer
                    }) { Text("Choose apps") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        groupDialog = false
                        appMenuTarget = null
                    }) { Text("Cancel") }
                },
            )
        }

        createFolderDraft?.let { (index, newApp) ->
            val existingKey = (layout.homeSlots[index] as? HomeSlot.App)?.key
            AlertDialog(
                onDismissRequest = { createFolderDraft = null },
                title = { Text("Create folder?") },
                text = {
                    Column {
                        Text("Stacking apps creates a folder (Nova-style).")
                        TextField(
                            value = folderTitle,
                            onValueChange = { folderTitle = it },
                            label = { Text("Folder name") },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val keys = listOfNotNull(existingKey, newApp.key)
                            repository.createFolder(folderTitle.ifBlank { "Folder" }, keys, index)
                            createFolderDraft = null
                        }
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        scope.launch {
                            repository.setHomeSlot(index, HomeSlot.App(newApp.key))
                            createFolderDraft = null
                        }
                    }) { Text("Replace") }
                },
            )
        }

        appActionTarget?.let { (index, app) ->
            val folders = layout.folders.values.toList()
            AppActionSheet(
                app = app,
                palette = palette,
                iconShape = settings.iconShape,
                onDismiss = { appActionTarget = null },
                onOpen = {
                    appActionTarget = null
                    launchApp(context, app)
                },
                onFavorite = {
                    scope.launch {
                        val dockIndex = layout.dockSlots.indexOfFirst { it == null }
                        if (dockIndex >= 0) repository.setDockSlot(dockIndex, HomeSlot.App(app.key))
                    }
                    appActionTarget = null
                },
                onAddToCategory = {
                    appMenuTarget = app
                    newGroupTitle = ""
                    groupDialog = true
                    appActionTarget = null
                },
                onRename = {
                    renameAppTarget = app
                    renameDraft = layout.appAliases[app.key] ?: app.label
                    appActionTarget = null
                },
                onMoveToFolder = {
                    appActionTarget = null
                    if (folders.isEmpty()) {
                        scope.launch {
                            repository.createFolder(app.label, listOf(app.key), index)
                        }
                    } else {
                        moveToFolderApp = index to app
                    }
                },
                onAppInfo = {
                    appActionTarget = null
                    openAppInfo(context, app.packageName)
                },
                onUninstall = {
                    appActionTarget = null
                    openUninstall(context, app.packageName)
                },
                onRemoveFromHome = {
                    appActionTarget = null
                    removeTarget = PlacementTarget.Home(index)
                },
                onLauncherSettings = {
                    appActionTarget = null
                    overlay = Overlay.Settings
                },
                showRemove = true,
            )
        }

        renameAppTarget?.let { app ->
            AlertDialog(
                onDismissRequest = { renameAppTarget = null },
                title = { Text("Rename app") },
                text = {
                    Column {
                        Text("Custom launcher name for ${app.label}")
                        TextField(
                            value = renameDraft,
                            onValueChange = { renameDraft = it },
                            label = { Text("Name") },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            repository.setAppAlias(app.key, renameDraft)
                            renameAppTarget = null
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            scope.launch {
                                repository.setAppAlias(app.key, null)
                                renameAppTarget = null
                            }
                        }) { Text("Reset") }
                        TextButton(onClick = { renameAppTarget = null }) { Text("Cancel") }
                    }
                },
            )
        }

        renameFolderTarget?.let { folder ->
            AlertDialog(
                onDismissRequest = { renameFolderTarget = null },
                title = { Text("Rename folder") },
                text = {
                    TextField(
                        value = renameDraft,
                        onValueChange = { renameDraft = it },
                        label = { Text("Folder name") },
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val updated = folder.copy(title = renameDraft.ifBlank { folder.title })
                            repository.updateFolder(updated)
                            openFolder = updated
                            renameFolderTarget = null
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { renameFolderTarget = null }) { Text("Cancel") }
                },
            )
        }

        renameGroupTarget?.let { group ->
            AlertDialog(
                onDismissRequest = { renameGroupTarget = null },
                title = { Text("Rename group") },
                text = {
                    TextField(
                        value = renameDraft,
                        onValueChange = { renameDraft = it },
                        label = { Text("Group name") },
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val groups = layout.drawerGroups.map {
                                if (it.id == group.id) it.copy(title = renameDraft.ifBlank { group.title }) else it
                            }
                            repository.saveDrawerGroups(groups)
                            renameGroupTarget = null
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { renameGroupTarget = null }) { Text("Cancel") }
                },
            )
        }

        moveToFolderApp?.let { (index, app) ->
            val folders = layout.folders.values.toList()
            AlertDialog(
                onDismissRequest = { moveToFolderApp = null },
                title = { Text("Move ${app.label}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Choose a folder, or create a new one.")
                        folders.forEach { folder ->
                            TextButton(onClick = {
                                scope.launch {
                                    repository.moveAppIntoFolder(index, folder.id)
                                    moveToFolderApp = null
                                    openFolder = layout.folders[folder.id]?.copy(
                                        appKeys = (folder.appKeys + app.key).distinct(),
                                    ) ?: folder.copy(appKeys = folder.appKeys + app.key)
                                }
                            }) { Text(folder.title) }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            repository.createFolder("Folder", listOf(app.key), index)
                            moveToFolderApp = null
                        }
                    }) { Text("New folder here") }
                },
                dismissButton = {
                    TextButton(onClick = { moveToFolderApp = null }) { Text("Cancel") }
                },
            )
        }

        removeTarget?.let { target ->
            val slot = when (target) {
                is PlacementTarget.Home -> layout.homeSlots.getOrNull(target.index)
                is PlacementTarget.Dock -> layout.dockSlots.getOrNull(target.index)
            }
            val label = when (slot) {
                is HomeSlot.App -> findApp(apps, slot.key)?.label ?: "app"
                is HomeSlot.Folder -> layout.folders[slot.folderId]?.title ?: "folder"
                is HomeSlot.Widget -> slot.type.name.lowercase().replace('_', ' ')
                null -> "item"
            }
            AlertDialog(
                onDismissRequest = { removeTarget = null },
                title = { Text("Remove $label?") },
                text = {
                    Column {
                        Text("Remove this shortcut from your ${if (target is PlacementTarget.Dock) "dock" else "home screen"}?")
                        if (slot is HomeSlot.App && slot.key in layout.hiddenApps) {
                            TextButton(onClick = {
                                scope.launch { repository.unhideApp(slot.key) }
                            }) { Text("Unhide from drawer") }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            when (target) {
                                is PlacementTarget.Home -> {
                                    if (slot is HomeSlot.Folder) repository.deleteFolder(slot.folderId)
                                    else repository.setHomeSlot(target.index, null)
                                }
                                is PlacementTarget.Dock -> repository.setDockSlot(target.index, null)
                            }
                            removeTarget = null
                        }
                    }) { Text("Remove") }
                },
                dismissButton = {
                    TextButton(onClick = { removeTarget = null }) { Text("Cancel") }
                },
            )
        }

        openFolder?.let { folder ->
            // Keep sheet in sync with latest folder data
            val liveFolder = layout.folders[folder.id] ?: folder
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val folderApps = liveFolder.appKeys.mapNotNull { findApp(apps, it) }
            ModalBottomSheet(
                onDismissRequest = { openFolder = null },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = palette.drawerBackground,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(liveFolder.title, color = palette.textPrimary, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            renameFolderTarget = liveFolder
                            renameDraft = liveFolder.title
                        }) { Text("Rename", color = palette.accent) }
                    }
                    Text(
                        "${folderApps.size} apps · tap to launch · long-press to remove · select multiple via Add apps",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(folderApps, key = { it.key }) { app ->
                            AppIconView(
                                app = app,
                                settings = settings,
                                palette = palette,
                                onClick = {
                                    openFolder = null
                                    launchApp(context, app)
                                },
                                onLongClick = {
                                    scope.launch {
                                        val updated = liveFolder.copy(
                                            appKeys = liveFolder.appKeys.filterNot { it == app.key },
                                        )
                                        repository.updateFolder(updated)
                                        openFolder = updated
                                    }
                                },
                            )
                        }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        folderAddTarget = liveFolder
                                        multiSelectKeys = emptySet()
                                        openFolder = null
                                        overlay = Overlay.Drawer
                                    },
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(settings.iconSizeDp.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(palette.searchBackground),
                                    contentAlignment = androidx.compose.ui.Alignment.Center,
                                ) {
                                    Text("+", color = palette.textPrimary, fontSize = 28.sp)
                                }
                                Text(
                                    "Add",
                                    color = palette.textPrimary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(onClick = {
                            folderAddTarget = liveFolder
                            multiSelectKeys = emptySet()
                            openFolder = null
                            overlay = Overlay.Drawer
                        }) { Text("Add apps", color = palette.accent) }
                        TextButton(onClick = {
                            scope.launch {
                                repository.deleteFolder(liveFolder.id)
                                openFolder = null
                            }
                        }) { Text("Delete folder") }
                    }
                }
            }
        }
    }
}
