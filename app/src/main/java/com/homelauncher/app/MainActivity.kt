package com.homelauncher.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomeLauncherApp()
        }
    }
}

private sealed interface PlacementTarget {
    data class Home(val index: Int) : PlacementTarget
    data class Dock(val index: Int) : PlacementTarget
}

@Composable
fun HomeLauncherApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { LauncherPreferences(context) }
    val layout by preferences.layout.collectAsState(
        initial = LauncherLayout(
            homeApps = List(HOME_GRID_COLUMNS * HOME_GRID_ROWS) { null },
            dockApps = List(DOCK_SLOT_COUNT) { null },
        ),
    )

    var drawerOpen by remember { mutableStateOf(false) }
    var placementTarget by remember { mutableStateOf<PlacementTarget?>(null) }
    var appMenuTarget by remember { mutableStateOf<AppInfo?>(null) }
    var removeTarget by remember { mutableStateOf<PlacementTarget?>(null) }

    val apps by produceState(initialValue = emptyList<AppInfo>(), context) {
        value = withContext(Dispatchers.Default) { loadInstalledApps(context) }
    }

    val openDrawer: () -> Unit = {
        drawerOpen = true
        placementTarget = null
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeScreen(
                    layout = layout,
                    apps = apps,
                    onOpenDrawer = openDrawer,
                    onLaunch = { launchApp(context, it) },
                    onEmptyHomeSlot = { index ->
                        placementTarget = PlacementTarget.Home(index)
                        drawerOpen = true
                    },
                    onEmptyDockSlot = { index ->
                        placementTarget = PlacementTarget.Dock(index)
                        drawerOpen = true
                    },
                    onLongPressHome = { index ->
                        removeTarget = PlacementTarget.Home(index)
                    },
                    onLongPressDock = { index ->
                        removeTarget = PlacementTarget.Dock(index)
                    },
                )

                AnimatedVisibility(
                    visible = drawerOpen,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                ) {
                    AppDrawer(
                        apps = apps,
                        placementHint = placementTarget?.let { target ->
                            when (target) {
                                is PlacementTarget.Home -> "Tap an app to place on home screen"
                                is PlacementTarget.Dock -> "Tap an app to place in dock"
                            }
                        },
                        onLaunch = { app ->
                            val target = placementTarget
                            if (target == null) {
                                drawerOpen = false
                                launchApp(context, app)
                            } else {
                                scope.launch {
                                    val key = appKey(app)
                                    when (target) {
                                        is PlacementTarget.Home -> preferences.setHomeSlot(target.index, key)
                                        is PlacementTarget.Dock -> preferences.setDockSlot(target.index, key)
                                    }
                                    placementTarget = null
                                    drawerOpen = false
                                }
                            }
                        },
                        onLongPress = { appMenuTarget = it },
                        onDismissPlacement = {
                            placementTarget = null
                        },
                        onClose = {
                            drawerOpen = false
                            placementTarget = null
                        },
                    )
                }
            }
        }
    }

    BackHandler(enabled = drawerOpen) {
        drawerOpen = false
        placementTarget = null
    }

    appMenuTarget?.let { app ->
        AppContextMenu(
            app = app,
            onDismiss = { appMenuTarget = null },
            onAddToHome = {
                scope.launch {
                    val firstEmpty = layout.homeApps.indexOfFirst { it == null }
                    if (firstEmpty >= 0) {
                        preferences.setHomeSlot(firstEmpty, appKey(app))
                    }
                }
                appMenuTarget = null
            },
            onAddToDock = {
                scope.launch {
                    val firstEmpty = layout.dockApps.indexOfFirst { it == null }
                    if (firstEmpty >= 0) {
                        preferences.setDockSlot(firstEmpty, appKey(app))
                    }
                }
                appMenuTarget = null
            },
        )
    }

    removeTarget?.let { target ->
        val key = when (target) {
            is PlacementTarget.Home -> layout.homeApps[target.index]
            is PlacementTarget.Dock -> layout.dockApps[target.index]
        }
        val app = findApp(apps, key)
        val location = if (target is PlacementTarget.Dock) "dock" else "home screen"
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("Remove ${app?.label ?: "app"}?") },
            text = { Text("Remove this shortcut from your $location?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (target) {
                                is PlacementTarget.Home -> preferences.clearHomeSlot(target.index)
                                is PlacementTarget.Dock -> preferences.clearDockSlot(target.index)
                            }
                            removeTarget = null
                        }
                    },
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { removeTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun HomeScreen(
    layout: LauncherLayout,
    apps: List<AppInfo>,
    onOpenDrawer: () -> Unit,
    onLaunch: (AppInfo) -> Unit,
    onEmptyHomeSlot: (Int) -> Unit,
    onEmptyDockSlot: (Int) -> Unit,
    onLongPressHome: (Int) -> Unit,
    onLongPressDock: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LauncherColors.WallpaperBrush)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -16f) onOpenDrawer()
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            ClockWidget(modifier = Modifier.padding(top = 24.dp, bottom = 20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(HOME_GRID_COLUMNS),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false,
            ) {
                items(layout.homeApps.size) { index ->
                    val key = layout.homeApps[index]
                    val app = findApp(apps, key)
                    if (app != null) {
                        LauncherIcon(
                            app = app,
                            onClick = { onLaunch(app) },
                            onLongClick = { onLongPressHome(index) },
                        )
                    } else {
                        EmptySlot(onClick = { onEmptyHomeSlot(index) })
                    }
                }
            }

            DockBar(
                dockApps = layout.dockApps,
                apps = apps,
                onLaunch = onLaunch,
                onEmptySlot = onEmptyDockSlot,
                onLongPress = onLongPressDock,
            )

            DrawerHint(onOpenDrawer = onOpenDrawer)
        }
    }
}

@Composable
fun ClockWidget(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            kotlinx.coroutines.delay(30_000)
        }
    }

    val timeFormat = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    Column(modifier = modifier) {
        Text(
            text = timeFormat.format(now),
            color = LauncherColors.TextPrimary,
            fontSize = 56.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1).sp,
        )
        Text(
            text = dateFormat.format(now),
            color = LauncherColors.TextSecondary,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockBar(
    dockApps: List<String?>,
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onEmptySlot: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(LauncherColors.DockBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dockApps.forEachIndexed { index, key ->
            val app = findApp(apps, key)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (app != null) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = { onLaunch(app) },
                                onLongClick = { onLongPress(index) },
                            ),
                    )
                } else {
                    EmptySlot(
                        compact = true,
                        onClick = { onEmptySlot(index) },
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerHint(onOpenDrawer: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.45f))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Swipe up for all apps",
            color = LauncherColors.TextSecondary,
            fontSize = 12.sp,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherIcon(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .size(if (compact) 48.dp else 58.dp)
                .clip(RoundedCornerShape(if (compact) 14.dp else 16.dp)),
        )
        if (!compact) {
            Text(
                text = app.label,
                color = LauncherColors.TextPrimary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
fun EmptySlot(compact: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(if (compact) 54.dp else 58.dp)
            .clip(RoundedCornerShape(if (compact) 16.dp else 18.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            color = LauncherColors.TextSecondary,
            fontSize = if (compact) 22.sp else 26.sp,
        )
    }
}

@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    placementHint: String?,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    onDismissPlacement: () -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.label.contains(query, ignoreCase = true) }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 24f) onClose()
                }
            },
        color = LauncherColors.DrawerBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "All apps",
                    color = LauncherColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (placementHint != null) {
                    TextButton(onClick = onDismissPlacement) {
                        Text("Cancel", color = LauncherColors.Accent)
                    }
                }
            }

            if (placementHint != null) {
                Text(
                    text = placementHint,
                    color = LauncherColors.Accent,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            SearchField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (query.isBlank()) "No apps found" else "No matches for \"$query\"",
                        color = LauncherColors.TextSecondary,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filteredApps, key = { it.packageName + it.activityName }) { app ->
                        LauncherIcon(
                            app = app,
                            onClick = { onLaunch(app) },
                            onLongClick = { onLongPress(app) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = LauncherColors.TextPrimary, fontSize = 16.sp),
        cursorBrush = SolidColor(LauncherColors.Accent),
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(LauncherColors.SearchBackground)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text("Search apps", color = LauncherColors.TextSecondary, fontSize = 16.sp)
            }
            innerTextField()
        },
    )
}

@Composable
fun AppContextMenu(
    app: AppInfo,
    onDismiss: () -> Unit,
    onAddToHome: () -> Unit,
    onAddToDock: () -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    if (expanded) {
        AlertDialog(
            onDismissRequest = {
                expanded = false
                onDismiss()
            },
            title = { Text(app.label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Choose where to add this app.")
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            expanded = false
                            onAddToDock()
                        },
                    ) {
                        Text("Add to dock")
                    }
                    TextButton(
                        onClick = {
                            expanded = false
                            onAddToHome()
                        },
                    ) {
                        Text("Add to home")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        expanded = false
                        onDismiss()
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}
