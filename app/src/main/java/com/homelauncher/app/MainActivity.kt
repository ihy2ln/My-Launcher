package com.homelauncher.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.homelauncher.app.data.LauncherRepository
import com.homelauncher.app.model.DrawerGroup
import com.homelauncher.app.model.FolderInfo
import com.homelauncher.app.model.GestureAction
import com.homelauncher.app.model.HomeSlot
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.defaultLayout
import com.homelauncher.app.ui.components.AppIconView
import com.homelauncher.app.ui.drawer.AppDrawer
import com.homelauncher.app.ui.home.EditHomeScreen
import com.homelauncher.app.ui.home.HomeScreen
import com.homelauncher.app.ui.settings.SettingsScreen
import com.homelauncher.app.ui.theme.HomeLauncherTheme
import com.homelauncher.app.ui.theme.rememberPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { HomeLauncherApp() }
    }
}

private sealed interface PlacementTarget {
    data class Home(val index: Int) : PlacementTarget
    data class Dock(val index: Int) : PlacementTarget
}

private sealed interface Overlay {
    data object None : Overlay
    data object Drawer : Overlay
    data object SearchDrawer : Overlay
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
    var openFolder by remember { mutableStateOf<FolderInfo?>(null) }
    var createFolderDraft by remember { mutableStateOf<Pair<Int, AppInfo>?>(null) }
    var folderTitle by remember { mutableStateOf("Folder") }
    var groupDialog by remember { mutableStateOf(false) }
    var newGroupTitle by remember { mutableStateOf("") }

    val apps by produceState(initialValue = emptyList<AppInfo>(), context) {
        value = withContext(Dispatchers.Default) { loadInstalledApps(context) }
    }

    fun runGesture(action: GestureAction) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.OPEN_DRAWER -> {
                placementTarget = null
                overlay = Overlay.Drawer
            }
            GestureAction.OPEN_SEARCH -> {
                placementTarget = null
                overlay = Overlay.SearchDrawer
            }
            GestureAction.OPEN_SETTINGS -> overlay = Overlay.Settings
            GestureAction.EXPAND_NOTIFICATIONS -> expandNotifications(context)
            GestureAction.EDIT_HOME -> overlay = Overlay.EditHome
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
                    onLongPressHome = { removeTarget = PlacementTarget.Home(it) },
                    onLongPressDock = { removeTarget = PlacementTarget.Dock(it) },
                    onEditHome = { overlay = Overlay.EditHome },
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
                        onOpenSettings = { overlay = Overlay.Settings },
                    )
                }

                AnimatedVisibility(
                    visible = overlay == Overlay.Drawer || overlay == Overlay.SearchDrawer,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                ) {
                    AppDrawer(
                        apps = apps,
                        hiddenApps = layout.hiddenApps,
                        groups = layout.drawerGroups,
                        settings = settings,
                        palette = palette,
                        placementHint = placementTarget?.let { target ->
                            when (target) {
                                is PlacementTarget.Home -> "Tap an app to place on home screen"
                                is PlacementTarget.Dock -> "Tap an app to place in dock"
                            }
                        },
                        onLaunch = { app ->
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
                        },
                        onLongPress = { appMenuTarget = it },
                        onDismissPlacement = {
                            placementTarget = null
                            if (returnToEditAfterPick) {
                                returnToEditAfterPick = false
                                overlay = Overlay.EditHome
                            }
                        },
                        onClose = {
                            placementTarget = null
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
                        onBack = { overlay = Overlay.None },
                    )
                }
            }
        }

        BackHandler(enabled = overlay != Overlay.None || openFolder != null) {
            when {
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
                        scope.launch {
                            val title = newGroupTitle.ifBlank { "Group" }
                            val existing = layout.drawerGroups.firstOrNull { it.title.equals(title, true) }
                            val groups = if (existing != null) {
                                layout.drawerGroups.map {
                                    if (it.id == existing.id) it.copy(appKeys = (it.appKeys + app.key).distinct())
                                    else it
                                }
                            } else {
                                layout.drawerGroups + DrawerGroup(
                                    id = "group_${System.currentTimeMillis()}",
                                    title = title,
                                    appKeys = listOf(app.key),
                                )
                            }
                            repository.saveDrawerGroups(groups)
                            newGroupTitle = ""
                            groupDialog = false
                            appMenuTarget = null
                        }
                    }) { Text("Save") }
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
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val folderApps = folder.appKeys.mapNotNull { findApp(apps, it) }
            ModalBottomSheet(
                onDismissRequest = { openFolder = null },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = palette.drawerBackground,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(folder.title, color = palette.textPrimary)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
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
                            )
                        }
                    }
                    TextButton(onClick = {
                        scope.launch {
                            repository.deleteFolder(folder.id)
                            openFolder = null
                        }
                    }) { Text("Delete folder", color = palette.accent) }
                }
            }
        }
    }
}
