package com.homelauncher.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.homelauncher.app.AppInfo
import com.homelauncher.app.findApp
import com.homelauncher.app.model.FolderInfo
import com.homelauncher.app.model.GestureAction
import com.homelauncher.app.model.HomeSlot
import com.homelauncher.app.model.LauncherLayout
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.ModuleStyle
import com.homelauncher.app.model.ScrollEffect
import com.homelauncher.app.model.WallpaperMode
import com.homelauncher.app.model.WidgetType
import com.homelauncher.app.model.homeCapacity
import com.homelauncher.app.ui.components.AppIconView
import com.homelauncher.app.ui.components.EmptySlotView
import com.homelauncher.app.ui.components.FolderIconView
import com.homelauncher.app.ui.components.HomeWidgetView
import com.homelauncher.app.ui.components.ModulePlate
import com.homelauncher.app.ui.components.WallpaperBackdrop
import com.homelauncher.app.ui.search.LauncherSearchBar
import com.homelauncher.app.ui.theme.LauncherPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class HomeDragState(
    val fromIndex: Int,
    val app: AppInfo,
    val position: Offset,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    layout: LauncherLayout,
    apps: List<AppInfo>,
    settings: LauncherSettings,
    palette: LauncherPalette,
    onGesture: (GestureAction) -> Unit,
    onLaunch: (AppInfo) -> Unit,
    onOpenFolder: (FolderInfo) -> Unit,
    onWidgetClick: (WidgetType) -> Unit,
    onEmptyHomeSlot: (Int) -> Unit,
    onEmptyDockSlot: (Int) -> Unit,
    onEditHome: () -> Unit,
    onOpenSearch: () -> Unit,
    onFloatingWidgetClick: (com.homelauncher.app.model.FloatingWidget) -> Unit = {},
    onFloatingWidgetMove: (com.homelauncher.app.model.FloatingWidget, Float, Float) -> Unit = { _, _, _ -> },
    onFloatingWidgetResize: (com.homelauncher.app.model.FloatingWidget, Float, Float) -> Unit = { _, _, _ -> },
    onAppLongPress: (Int, AppInfo) -> Unit = { _, _ -> },
    badgeCounts: Map<String, Int> = emptyMap(),
) {
    // Touch homeCapacity so multi-page capacity stays wired to settings.
    @Suppress("UNUSED_VARIABLE")
    val capacity = settings.homeCapacity()

    var cumulativeDragY by remember { mutableStateOf(0f) }
    var homeSearchQuery by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperBackdrop(
            color = settings.wallpaperColor,
            imageUri = settings.wallpaperImageUri,
            videoUri = settings.wallpaperVideoUri,
            useImage = settings.wallpaperMode == WallpaperMode.IMAGE,
            useVideo = settings.wallpaperMode == WallpaperMode.VIDEO,
            gradientFallback = palette.wallpaper,
            useGradient = settings.wallpaperMode == WallpaperMode.GRADIENT,
        )

        // Background gestures on wallpaper areas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(settings) {
                    detectTapGestures(onDoubleTap = { onGesture(settings.doubleTap) })
                }
                .pointerInput(settings) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom < 0.92f) onGesture(settings.pinchIn)
                    }
                },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 14.dp)
                .zIndex(1f)
                .pointerInput(settings) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            when {
                                cumulativeDragY < -80f -> onGesture(settings.swipeUp)
                                cumulativeDragY > 80f -> onGesture(settings.swipeDown)
                            }
                            cumulativeDragY = 0f
                        },
                        onDragCancel = { cumulativeDragY = 0f },
                        onVerticalDrag = { _, dragAmount -> cumulativeDragY += dragAmount },
                    )
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LauncherSearchBar(
                    query = homeSearchQuery,
                    onQueryChange = { homeSearchQuery = it },
                    onOpenSearch = onOpenSearch,
                    palette = palette,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "⚙",
                    color = palette.textPrimary,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onEditHome)
                        .padding(6.dp),
                )
            }

            ClockWidget(palette = palette, modifier = Modifier.padding(bottom = 8.dp))

            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val pageSize = settings.homeColumns * settings.homeRows
                val pageCount = settings.homePages.coerceAtLeast(1).coerceAtMost(5).coerceAtLeast(1).let { pages ->
                    maxOf(pages, ((layout.homeSlots.size + pageSize - 1) / pageSize).coerceAtLeast(1))
                }
                val pagerState = rememberPagerState(pageCount = { pageCount })

                Column(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) { page ->
                        val pageOffset =
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(settings.homeColumns),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    when (settings.scrollEffect) {
                                        ScrollEffect.CUBE, ScrollEffect.REVOLVING_DOOR -> {
                                            rotationY = pageOffset * -55f
                                            cameraDistance = 12f * density
                                            alpha = 1f - abs(pageOffset) * 0.35f
                                            scaleX = 1f - abs(pageOffset) * 0.05f
                                            scaleY = 1f - abs(pageOffset) * 0.05f
                                        }
                                        ScrollEffect.CARD_STACK -> {
                                            val t = abs(pageOffset).coerceIn(0f, 1f)
                                            scaleX = 1f - t * 0.12f
                                            scaleY = 1f - t * 0.12f
                                            alpha = 1f - t * 0.45f
                                            translationY = t * 24f
                                        }
                                        ScrollEffect.TABLET -> {
                                            val t = abs(pageOffset).coerceIn(0f, 1f)
                                            scaleX = 1f - t * 0.08f
                                            scaleY = 1f - t * 0.08f
                                            alpha = 1f - t * 0.25f
                                            translationX = pageOffset * size.width * 0.08f
                                        }
                                        ScrollEffect.SIMPLE -> {
                                            alpha = 1f - abs(pageOffset) * 0.2f
                                        }
                                    }
                                },
                            contentPadding = PaddingValues(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = false,
                        ) {
                            items(pageSize) { index ->
                                val globalIndex = page * pageSize + index
                                val slot = layout.homeSlots.getOrNull(globalIndex)
                                val badge = when (slot) {
                                    is HomeSlot.App -> {
                                        val app = findApp(apps, slot.key)
                                        if (app != null) {
                                            badgeCounts[app.packageName]
                                                ?: badgeCounts[app.key]
                                                ?: 0
                                        } else {
                                            0
                                        }
                                    }
                                    else -> 0
                                }
                                HomeCell(
                                    slot = slot,
                                    style = layout.moduleStyles[globalIndex],
                                    defaultOpacity = settings.moduleOpacity,
                                    apps = apps,
                                    folders = layout.folders,
                                    settings = settings,
                                    palette = palette,
                                    showEmpty = false,
                                    appAliases = layout.appAliases,
                                    badgeCount = badge,
                                    onLaunch = onLaunch,
                                    onOpenFolder = onOpenFolder,
                                    onWidgetClick = onWidgetClick,
                                    onEmpty = { onEditHome() },
                                    onLongPress = {
                                        val appSlot = slot as? HomeSlot.App
                                        val app = appSlot?.let { findApp(apps, it.key) }
                                        if (app != null) {
                                            onAppLongPress(globalIndex, app)
                                        } else {
                                            onEditHome()
                                        }
                                    },
                                )
                            }
                        }
                    }

                    if (pageCount > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(pageCount) { i ->
                                val selected = pagerState.currentPage == i
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(if (selected) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) Color.White
                                            else Color.White.copy(alpha = 0.35f),
                                        ),
                                )
                            }
                        }
                    }
                }

                FloatingWidgetsLayer(
                    widgets = layout.floatingWidgets,
                    apps = apps,
                    appAliases = layout.appAliases,
                    settings = settings,
                    palette = palette,
                    editable = false,
                    allowMove = true,
                    onClick = onFloatingWidgetClick,
                    onLongPress = { },
                    onMove = onFloatingWidgetMove,
                    onResize = onFloatingWidgetResize,
                )
            }

            DockBar(
                dockSlots = layout.dockSlots,
                apps = apps,
                settings = settings,
                palette = palette,
                onLaunch = onLaunch,
                onEmptySlot = onEmptyDockSlot,
                onAppLongPress = { index, app -> onAppLongPress(index, app) },
                badgeCounts = badgeCounts,
            )

            DrawerHint(palette)
        }
    }
}

@Composable
fun HomeCell(
    slot: HomeSlot?,
    style: ModuleStyle?,
    defaultOpacity: Float,
    apps: List<AppInfo>,
    folders: Map<String, FolderInfo>,
    settings: LauncherSettings,
    palette: LauncherPalette,
    showEmpty: Boolean,
    highlighted: Boolean = false,
    appAliases: Map<String, String> = emptyMap(),
    badgeCount: Int = 0,
    onLaunch: (AppInfo) -> Unit,
    onOpenFolder: (FolderInfo) -> Unit,
    onWidgetClick: (WidgetType) -> Unit,
    onEmpty: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onDragStart: ((Offset) -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
) {
    val opacity = style?.opacity ?: defaultOpacity
    when (slot) {
        is HomeSlot.App -> {
            val app = findApp(apps, slot.key)
            if (app != null) {
                AppIconView(
                    app = app,
                    settings = settings,
                    palette = palette,
                    onClick = { onLaunch(app) },
                    onLongClick = onLongPress,
                    onDoubleClick = onDoubleClick,
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                    labelOverride = appAliases[app.key],
                    badgeCount = badgeCount,
                )
            } else {
                HomeEmptyCell(showEmpty = showEmpty, opacity = opacity, style = style, onEmpty = onEmpty)
            }
        }
        is HomeSlot.Folder -> {
            val folder = folders[slot.folderId]
            if (folder != null) {
                Box(
                    modifier = if (highlighted) {
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.accent.copy(alpha = 0.25f))
                    } else {
                        Modifier
                    },
                ) {
                    FolderIconView(
                        folder = folder,
                        previewApps = folder.appKeys.mapNotNull { findApp(apps, it) },
                        settings = settings,
                        palette = palette,
                        onClick = { onOpenFolder(folder) },
                        onLongClick = onLongPress,
                        onDoubleClick = onDoubleClick,
                    )
                }
            } else {
                HomeEmptyCell(showEmpty = showEmpty, opacity = opacity, style = style, onEmpty = onEmpty)
            }
        }
        is HomeSlot.Widget -> {
            // Legacy grid widgets migrate to floating widgets.
            HomeEmptyCell(showEmpty = showEmpty, opacity = opacity, style = style, onEmpty = onEmpty)
        }
        null -> HomeEmptyCell(showEmpty = showEmpty, opacity = opacity, style = style, onEmpty = onEmpty)
    }
}

@Composable
private fun HomeEmptyCell(
    showEmpty: Boolean,
    opacity: Float,
    style: ModuleStyle?,
    onEmpty: () -> Unit,
) {
    if (showEmpty) {
        EmptyModule(opacity, style, onEmpty)
    } else {
        // Fully invisible — no plate, no "+", no module style chrome on the main home.
        Box(modifier = Modifier.height(76.dp).fillMaxWidth())
    }
}

@Composable
private fun EmptyModule(opacity: Float, style: ModuleStyle?, onClick: () -> Unit) {
    ModulePlate(
        opacity = style?.opacity ?: opacity.coerceAtLeast(0.4f),
        imageUri = style?.imageUri,
        videoUri = style?.videoUri,
        color = style?.color ?: 0xFF1A1A1A,
        saturation = style?.saturation ?: 0.15f,
        brightness = style?.brightness ?: 0.4f,
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = "+",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Composable
fun ClockWidget(palette: LauncherPalette, modifier: Modifier = Modifier) {
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
            color = palette.textPrimary,
            fontSize = 56.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1).sp,
        )
        Text(
            text = dateFormat.format(now),
            color = palette.textSecondary,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockBar(
    dockSlots: List<HomeSlot?>,
    apps: List<AppInfo>,
    settings: LauncherSettings,
    palette: LauncherPalette,
    onLaunch: (AppInfo) -> Unit,
    onEmptySlot: (Int) -> Unit,
    onAppLongPress: (Int, AppInfo) -> Unit = { _, _ -> },
    badgeCounts: Map<String, Int> = emptyMap(),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(palette.dockBackground)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dockSlots.forEachIndexed { index, slot ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                when (slot) {
                    is HomeSlot.App -> {
                        val app = findApp(apps, slot.key)
                        if (app != null) {
                            AppIconView(
                                app = app,
                                settings = settings,
                                palette = palette,
                                onClick = { onLaunch(app) },
                                onLongClick = { onAppLongPress(index, app) },
                                showLabel = false,
                                size = (settings.iconSizeDp - 4).dp,
                                badgeCount = badgeCounts[app.packageName]
                                    ?: badgeCounts[app.key]
                                    ?: 0,
                            )
                        } else {
                            EmptySlotView(
                                size = (settings.iconSizeDp - 4).dp,
                                palette = palette,
                                onClick = { onEmptySlot(index) },
                            )
                        }
                    }
                    is HomeSlot.Widget -> {
                        HomeWidgetView(
                            type = slot.type,
                            palette = palette,
                            size = (settings.iconSizeDp - 4).dp,
                            onClick = { },
                        )
                    }
                    else -> {
                        EmptySlotView(
                            size = (settings.iconSizeDp - 4).dp,
                            palette = palette,
                            onClick = { onEmptySlot(index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerHint(palette: LauncherPalette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.35f)),
        )
    }
}
