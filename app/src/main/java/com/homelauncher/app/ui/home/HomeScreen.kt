package com.homelauncher.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import com.homelauncher.app.model.WallpaperMode
import com.homelauncher.app.model.WidgetType
import com.homelauncher.app.ui.components.AppIconView
import com.homelauncher.app.ui.components.FolderIconView
import com.homelauncher.app.ui.components.HomeWidgetView
import com.homelauncher.app.ui.components.ModulePlate
import com.homelauncher.app.ui.components.WallpaperBackdrop
import com.homelauncher.app.ui.theme.LauncherPalette
import com.homelauncher.app.ui.theme.iconShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class HomeDragState(
    val fromIndex: Int,
    val app: AppInfo,
    val position: Offset,
)

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
    onLongPressHome: (Int) -> Unit,
    onLongPressDock: (Int) -> Unit,
    onEditHome: () -> Unit,
    onDropApp: (fromIndex: Int, toIndex: Int) -> Unit,
    onFloatingWidgetClick: (com.homelauncher.app.model.FloatingWidget) -> Unit = {},
    onFloatingWidgetLongPress: (com.homelauncher.app.model.FloatingWidget) -> Unit = {},
    onFloatingWidgetMove: (com.homelauncher.app.model.FloatingWidget, Float, Float) -> Unit = { _, _, _ -> },
    onFloatingWidgetResize: (com.homelauncher.app.model.FloatingWidget, Float, Float) -> Unit = { _, _, _ -> },
) {
    var cumulativeDragY by remember { mutableStateOf(0f) }
    var dragState by remember { mutableStateOf<HomeDragState?>(null) }
    var hoverIndex by remember { mutableStateOf<Int?>(null) }
    val cellBounds = remember { mutableStateMapOf<Int, Rect>() }

    fun hitTest(point: Offset): Int? =
        cellBounds.entries.firstOrNull { (_, rect) -> rect.contains(point) }?.key

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

        // Background gestures only (behind interactive content)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(settings) {
                    detectTapGestures(
                        onDoubleTap = { onGesture(settings.doubleTap) },
                        onLongPress = { onEditHome() },
                    )
                }
                .pointerInput(settings) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom < 0.92f) onGesture(settings.pinchIn)
                    }
                }
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
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 14.dp)
                .zIndex(1f),
        ) {
            ClockWidget(palette = palette, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))

            if (dragState != null) {
                Text(
                    text = "Drop on a folder to add · empty cell to move · another app to swap",
                    color = palette.accent,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(settings.homeColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false,
                ) {
                    items(layout.homeSlots.size) { index ->
                        val isHoverTarget = hoverIndex == index && dragState != null && dragState?.fromIndex != index
                        val slot = layout.homeSlots[index]
                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { coords ->
                                    cellBounds[index] = coords.boundsInRoot()
                                }
                                .then(
                                    if (isHoverTarget) {
                                        Modifier.border(2.dp, palette.accent, RoundedCornerShape(16.dp))
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            if (dragState?.fromIndex == index) {
                                Box(modifier = Modifier.height(76.dp).fillMaxWidth())
                            } else {
                                HomeCell(
                                    slot = slot,
                                    style = layout.moduleStyles[index],
                                    defaultOpacity = settings.moduleOpacity,
                                    apps = apps,
                                    folders = layout.folders,
                                    settings = settings,
                                    palette = palette,
                                    showEmpty = false,
                                    highlighted = isHoverTarget && slot is HomeSlot.Folder,
                                    appAliases = layout.appAliases,
                                    onLaunch = onLaunch,
                                    onOpenFolder = onOpenFolder,
                                    onWidgetClick = onWidgetClick,
                                    onEmpty = { onEditHome() },
                                    onLongPress = { onLongPressHome(index) },
                                    onDragStart = { pos ->
                                        val appKey = (slot as? HomeSlot.App)?.key ?: return@HomeCell
                                        val app = findApp(apps, appKey) ?: return@HomeCell
                                        dragState = HomeDragState(index, app, pos)
                                        hoverIndex = index
                                    },
                                    onDrag = { pos ->
                                        dragState = dragState?.copy(position = pos)
                                        hoverIndex = hitTest(pos)
                                    },
                                    onDragEnd = {
                                        val target = hoverIndex
                                        val from = dragState?.fromIndex
                                        if (from != null && target != null && target != from) {
                                            onDropApp(from, target)
                                        }
                                        dragState = null
                                        hoverIndex = null
                                    },
                                    onDragCancel = {
                                        dragState = null
                                        hoverIndex = null
                                    },
                                )
                            }
                        }
                    }
                }

                FloatingWidgetsLayer(
                    widgets = layout.floatingWidgets,
                    palette = palette,
                    editable = false,
                    onClick = onFloatingWidgetClick,
                    onLongPress = onFloatingWidgetLongPress,
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
                onLongPress = onLongPressDock,
            )

            DrawerHint(palette)
        }

        dragState?.let { drag ->
            Image(
                bitmap = drag.app.icon,
                contentDescription = drag.app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (drag.position.x - 36).roundToInt(),
                            (drag.position.y - 36).roundToInt(),
                        )
                    }
                    .size(72.dp)
                    .clip(iconShape(settings.iconShape))
                    .zIndex(20f),
            )
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
    onLaunch: (AppInfo) -> Unit,
    onOpenFolder: (FolderInfo) -> Unit,
    onWidgetClick: (WidgetType) -> Unit,
    onEmpty: () -> Unit,
    onLongPress: () -> Unit,
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
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                    labelOverride = appAliases[app.key],
                )
            } else if (showEmpty) {
                EmptyModule(opacity, style, onEmpty)
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
                    )
                }
            } else if (showEmpty) {
                EmptyModule(opacity, style, onEmpty)
            }
        }
        is HomeSlot.Widget -> {
            // Legacy grid widgets are migrated to floating widgets; keep cell empty.
            if (showEmpty) EmptyModule(opacity, style, onEmpty)
        }
        null -> if (showEmpty) {
            EmptyModule(opacity, style, onEmpty)
        } else {
            // Invisible spacer — keeps grid alignment without "+" clutter
            Box(modifier = Modifier.height(76.dp).fillMaxWidth())
        }
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
    onLongPress: (Int) -> Unit,
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
                                onLongClick = { onLongPress(index) },
                                showLabel = false,
                                size = (settings.iconSizeDp - 4).dp,
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
                    else -> { }
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
                .background(Color.White.copy(alpha = 0.45f)),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Swipe up for apps",
            color = palette.textSecondary,
            fontSize = 11.sp,
        )
    }
}
