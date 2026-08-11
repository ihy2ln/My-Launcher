package com.homelauncher.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
) {
    var cumulativeDragY by remember { mutableStateOf(0f) }

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
                detectDragGestures(
                    onDragEnd = {
                        when {
                            cumulativeDragY < -80f -> onGesture(settings.swipeUp)
                            cumulativeDragY > 80f -> onGesture(settings.swipeDown)
                        }
                        cumulativeDragY = 0f
                    },
                    onDragCancel = { cumulativeDragY = 0f },
                    onDrag = { _, dragAmount -> cumulativeDragY += dragAmount.y },
                )
            },
    ) {
        WallpaperBackdrop(
            color = settings.wallpaperColor,
            imageUri = settings.wallpaperImageUri,
            videoUri = settings.wallpaperVideoUri,
            useImage = settings.wallpaperMode == WallpaperMode.IMAGE,
            useVideo = settings.wallpaperMode == WallpaperMode.VIDEO,
            gradientFallback = palette.wallpaper,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 14.dp),
        ) {
            ClockWidget(palette = palette, modifier = Modifier.padding(top = 18.dp, bottom = 14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(settings.homeColumns),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false,
            ) {
                items(layout.homeSlots.size) { index ->
                    HomeCell(
                        slot = layout.homeSlots[index],
                        style = layout.moduleStyles[index],
                        defaultOpacity = settings.moduleOpacity,
                        apps = apps,
                        folders = layout.folders,
                        settings = settings,
                        palette = palette,
                        showEmpty = false,
                        onLaunch = onLaunch,
                        onOpenFolder = onOpenFolder,
                        onWidgetClick = onWidgetClick,
                        onEmpty = { onEmptyHomeSlot(index) },
                        onLongPress = { onLongPressHome(index) },
                    )
                }
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
    onLaunch: (AppInfo) -> Unit,
    onOpenFolder: (FolderInfo) -> Unit,
    onWidgetClick: (WidgetType) -> Unit,
    onEmpty: () -> Unit,
    onLongPress: () -> Unit,
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
                )
            } else if (showEmpty) {
                EmptyModule(opacity, style, onEmpty)
            }
        }
        is HomeSlot.Folder -> {
            val folder = folders[slot.folderId]
            if (folder != null) {
                FolderIconView(
                    folder = folder,
                    previewApps = folder.appKeys.mapNotNull { findApp(apps, it) },
                    settings = settings,
                    palette = palette,
                    onClick = { onOpenFolder(folder) },
                    onLongClick = onLongPress,
                )
            } else if (showEmpty) {
                EmptyModule(opacity, style, onEmpty)
            }
        }
        is HomeSlot.Widget -> {
            HomeWidgetView(
                type = slot.type,
                palette = palette,
                size = settings.iconSizeDp.dp,
                onClick = { onWidgetClick(slot.type) },
            )
        }
        null -> if (showEmpty) EmptyModule(opacity, style, onEmpty) else Box(modifier = Modifier.size(settings.iconSizeDp.dp))
    }
}

@Composable
private fun EmptyModule(opacity: Float, style: ModuleStyle?, onClick: () -> Unit) {
    ModulePlate(
        opacity = opacity,
        imageUri = style?.imageUri,
        videoUri = style?.videoUri,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = "+",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 26.sp,
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
        Text("Swipe up for apps · long-press to edit", color = palette.textSecondary, fontSize = 11.sp)
    }
}
