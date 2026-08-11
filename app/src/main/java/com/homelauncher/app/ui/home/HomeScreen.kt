package com.homelauncher.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import com.homelauncher.app.ui.components.AppIconView
import com.homelauncher.app.ui.components.EmptySlotView
import com.homelauncher.app.ui.components.FolderIconView
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
    onEmptyHomeSlot: (Int) -> Unit,
    onEmptyDockSlot: (Int) -> Unit,
    onLongPressHome: (Int) -> Unit,
    onLongPressDock: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var cumulativeDragY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.wallpaper)
            .pointerInput(settings) {
                detectTapGestures(
                    onDoubleTap = { onGesture(settings.doubleTap) },
                    onLongPress = { onOpenSettings() },
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
                    onDrag = { _, dragAmount ->
                        cumulativeDragY += dragAmount.y
                    },
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            ClockWidget(palette = palette, modifier = Modifier.padding(top = 20.dp, bottom = 16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(settings.homeColumns),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false,
            ) {
                items(layout.homeSlots.size) { index ->
                    when (val slot = layout.homeSlots[index]) {
                        is HomeSlot.App -> {
                            val app = findApp(apps, slot.key)
                            if (app != null) {
                                AppIconView(
                                    app = app,
                                    settings = settings,
                                    palette = palette,
                                    onClick = { onLaunch(app) },
                                    onLongClick = { onLongPressHome(index) },
                                )
                            } else {
                                EmptySlotView(settings.iconSizeDp.dp, palette) { onEmptyHomeSlot(index) }
                            }
                        }
                        is HomeSlot.Folder -> {
                            val folder = layout.folders[slot.folderId]
                            if (folder != null) {
                                val preview = folder.appKeys.mapNotNull { findApp(apps, it) }
                                FolderIconView(
                                    folder = folder,
                                    previewApps = preview,
                                    settings = settings,
                                    palette = palette,
                                    onClick = { onOpenFolder(folder) },
                                    onLongClick = { onLongPressHome(index) },
                                )
                            } else {
                                EmptySlotView(settings.iconSizeDp.dp, palette) { onEmptyHomeSlot(index) }
                            }
                        }
                        null -> EmptySlotView(settings.iconSizeDp.dp, palette) { onEmptyHomeSlot(index) }
                    }
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
                        } else {
                            EmptySlotView((settings.iconSizeDp - 4).dp, palette) { onEmptySlot(index) }
                        }
                    }
                    else -> EmptySlotView((settings.iconSizeDp - 4).dp, palette) { onEmptySlot(index) }
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
        Text("Swipe up for apps · long-press for settings", color = palette.textSecondary, fontSize = 11.sp)
    }
}
