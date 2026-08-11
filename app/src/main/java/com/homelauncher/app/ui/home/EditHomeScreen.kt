package com.homelauncher.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homelauncher.app.AppInfo
import com.homelauncher.app.data.LauncherRepository
import com.homelauncher.app.findApp
import com.homelauncher.app.model.FolderInfo
import com.homelauncher.app.model.HomeSlot
import com.homelauncher.app.model.LauncherLayout
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.ModuleStyle
import com.homelauncher.app.model.WallpaperMode
import com.homelauncher.app.model.WidgetType
import com.homelauncher.app.ui.components.AppIconView
import com.homelauncher.app.ui.components.ColorWheelPicker
import com.homelauncher.app.ui.components.FolderIconView
import com.homelauncher.app.ui.components.HomeWidgetView
import com.homelauncher.app.ui.components.ModulePlate
import com.homelauncher.app.ui.components.WallpaperBackdrop
import com.homelauncher.app.ui.theme.LauncherPalette
import kotlinx.coroutines.launch

enum class AddItemType { APP, WIDGET, GROUP }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditHomeScreen(
    layout: LauncherLayout,
    apps: List<AppInfo>,
    settings: LauncherSettings,
    palette: LauncherPalette,
    repository: LauncherRepository,
    onDone: () -> Unit,
    onPickAppForSlot: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showColorWheel by remember { mutableStateOf(false) }
    var showWallpaperSheet by remember { mutableStateOf(false) }
    var addTargetIndex by remember { mutableStateOf<Int?>(null) }
    var moduleEditIndex by remember { mutableStateOf<Int?>(null) }
    var createGroupIndex by remember { mutableStateOf<Int?>(null) }
    var groupTitle by remember { mutableStateOf("Group") }

    val takePersistable = { uri: Uri ->
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        uri.toString()
    }

    val pickWallpaperImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val path = takePersistable(uri)
            scope.launch {
                repository.updateSettings {
                    it.copy(wallpaperMode = WallpaperMode.IMAGE, wallpaperImageUri = path)
                }
            }
        }
    }
    val pickWallpaperVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val path = takePersistable(uri)
            scope.launch {
                repository.updateSettings {
                    it.copy(wallpaperMode = WallpaperMode.VIDEO, wallpaperVideoUri = path)
                }
            }
        }
    }
    val pickModuleImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val index = moduleEditIndex ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            val path = takePersistable(uri)
            scope.launch {
                val current = layout.moduleStyles[index] ?: ModuleStyle(opacity = settings.moduleOpacity)
                repository.setModuleStyle(index, current.copy(imageUri = path, videoUri = null))
            }
        }
    }
    val pickModuleVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val index = moduleEditIndex ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            val path = takePersistable(uri)
            scope.launch {
                val current = layout.moduleStyles[index] ?: ModuleStyle(opacity = settings.moduleOpacity)
                repository.setModuleStyle(index, current.copy(videoUri = path, imageUri = null))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                .padding(horizontal = 12.dp),
        ) {
            EditToolbar(
                palette = palette,
                onDone = onDone,
                onBackground = { showWallpaperSheet = true },
                onSettings = onOpenSettings,
            )

            Text(
                text = "Edit Home Screen",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            Text(
                text = "Tap + to add apps, widgets, or groups. Long-press a module to style it.",
                color = palette.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Text("Module opacity", color = palette.textSecondary, fontSize = 12.sp)
            Slider(
                value = settings.moduleOpacity,
                onValueChange = { value ->
                    scope.launch { repository.updateSettings { it.copy(moduleOpacity = value) } }
                },
                valueRange = 0.1f..0.85f,
                modifier = Modifier.fillMaxWidth(),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(settings.homeColumns),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = true,
            ) {
                items(layout.homeSlots.size) { index ->
                    val style = layout.moduleStyles[index]
                    val opacity = style?.opacity ?: settings.moduleOpacity
                    when (val slot = layout.homeSlots[index]) {
                        is HomeSlot.App -> {
                            val app = findApp(apps, slot.key)
                            ModulePlate(
                                opacity = opacity,
                                imageUri = style?.imageUri,
                                videoUri = style?.videoUri,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp)
                                    .clickable { addTargetIndex = index },
                            ) {
                                if (app != null) {
                                    AppIconView(
                                        app = app,
                                        settings = settings,
                                        palette = palette,
                                        onClick = { addTargetIndex = index },
                                        onLongClick = { moduleEditIndex = index },
                                        size = settings.iconSizeDp.dp,
                                    )
                                }
                            }
                        }
                        is HomeSlot.Folder -> {
                            val folder = layout.folders[slot.folderId]
                            ModulePlate(
                                opacity = opacity,
                                imageUri = style?.imageUri,
                                videoUri = style?.videoUri,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp),
                            ) {
                                if (folder != null) {
                                    FolderIconView(
                                        folder = folder,
                                        previewApps = folder.appKeys.mapNotNull { findApp(apps, it) },
                                        settings = settings,
                                        palette = palette,
                                        onClick = { addTargetIndex = index },
                                        onLongClick = { moduleEditIndex = index },
                                    )
                                }
                            }
                        }
                        is HomeSlot.Widget -> {
                            ModulePlate(
                                opacity = opacity,
                                imageUri = style?.imageUri,
                                videoUri = style?.videoUri,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp),
                            ) {
                                HomeWidgetView(
                                    type = slot.type,
                                    palette = palette,
                                    size = settings.iconSizeDp.dp,
                                    onClick = { addTargetIndex = index },
                                )
                            }
                        }
                        null -> {
                            ModulePlate(
                                opacity = opacity,
                                imageUri = style?.imageUri,
                                videoUri = style?.videoUri,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp)
                                    .clickable { addTargetIndex = index },
                            ) {
                                Text(
                                    text = "+",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 28.sp,
                                    modifier = Modifier.clickable(
                                        onClick = { addTargetIndex = index },
                                    ),
                                )
                            }
                            // Long-press empty via separate dialog trigger button area
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.dp),
                            )
                        }
                    }
                }
            }

            // Long-press helper row for selected empty modules via tap-and-hold alternative
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Chip("Style last +", palette) {
                    val empty = layout.homeSlots.indexOfFirst { it == null }
                    if (empty >= 0) moduleEditIndex = empty
                }
                Chip("Clear cell", palette) {
                    val filled = layout.homeSlots.indexOfLast { it != null }
                    if (filled >= 0) scope.launch { repository.setHomeSlot(filled, null) }
                }
            }
        }
    }

    if (showWallpaperSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWallpaperSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = palette.drawerBackground,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Background", color = palette.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Color", palette, selected = settings.wallpaperMode == WallpaperMode.COLOR) {
                        scope.launch { repository.updateSettings { it.copy(wallpaperMode = WallpaperMode.COLOR) } }
                        showColorWheel = true
                    }
                    Chip("Picture", palette, selected = settings.wallpaperMode == WallpaperMode.IMAGE) {
                        pickWallpaperImage.launch(arrayOf("image/*"))
                    }
                    Chip("Video", palette, selected = settings.wallpaperMode == WallpaperMode.VIDEO) {
                        pickWallpaperVideo.launch(arrayOf("video/*"))
                    }
                }
                if (showColorWheel || settings.wallpaperMode == WallpaperMode.COLOR) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                    ColorWheelPicker(
                        color = settings.wallpaperColor,
                        onColorChange = { color ->
                            scope.launch {
                                repository.updateSettings {
                                    it.copy(wallpaperMode = WallpaperMode.COLOR, wallpaperColor = color)
                                }
                            }
                        },
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    addTargetIndex?.let { index ->
        AddItemSheet(
            palette = palette,
            onDismiss = { addTargetIndex = null },
            onApp = {
                addTargetIndex = null
                onPickAppForSlot(index)
            },
            onWidget = { type ->
                scope.launch {
                    repository.setHomeSlot(index, HomeSlot.Widget(type))
                    addTargetIndex = null
                }
            },
            onGroup = {
                createGroupIndex = index
                groupTitle = "Group"
                addTargetIndex = null
            },
            onRemove = {
                scope.launch {
                    repository.setHomeSlot(index, null)
                    addTargetIndex = null
                }
            },
            onStyle = {
                moduleEditIndex = index
                addTargetIndex = null
            },
        )
    }

    moduleEditIndex?.let { index ->
        val style = layout.moduleStyles[index] ?: ModuleStyle(opacity = settings.moduleOpacity)
        AlertDialog(
            onDismissRequest = { moduleEditIndex = null },
            title = { Text("Module style") },
            text = {
                Column {
                    Text("Opacity")
                    Slider(
                        value = style.opacity,
                        onValueChange = { value ->
                            scope.launch {
                                repository.setModuleStyle(index, style.copy(opacity = value))
                            }
                        },
                        valueRange = 0.1f..0.9f,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { pickModuleImage.launch(arrayOf("image/*")) }) { Text("Picture") }
                        TextButton(onClick = { pickModuleVideo.launch(arrayOf("video/*")) }) { Text("Video") }
                        TextButton(onClick = {
                            scope.launch {
                                repository.setModuleStyle(index, style.copy(imageUri = null, videoUri = null))
                            }
                        }) { Text("Clear media") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { moduleEditIndex = null }) { Text("Done") }
            },
        )
    }

    createGroupIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { createGroupIndex = null },
            title = { Text("New group") },
            text = {
                androidx.compose.material3.TextField(
                    value = groupTitle,
                    onValueChange = { groupTitle = it },
                    label = { Text("Group name") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.createFolder(groupTitle.ifBlank { "Group" }, emptyList(), index)
                        createGroupIndex = null
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { createGroupIndex = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EditToolbar(
    palette: LauncherPalette,
    onDone: () -> Unit,
    onBackground: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onDone) { Text("Done", color = palette.accent, fontWeight = FontWeight.Bold) }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onBackground) { Text("Background", color = palette.textPrimary) }
            TextButton(onClick = onSettings) { Text("Settings", color = palette.textPrimary) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemSheet(
    palette: LauncherPalette,
    onDismiss: () -> Unit,
    onApp: () -> Unit,
    onWidget: (WidgetType) -> Unit,
    onGroup: () -> Unit,
    onRemove: () -> Unit,
    onStyle: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.drawerBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Add to home", color = palette.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            ActionCard("App", "Pick an installed app", palette, onApp)
            ActionCard("Clock widget", "Live clock module", palette) { onWidget(WidgetType.CLOCK) }
            ActionCard("Weather widget", "Local weather card", palette) { onWidget(WidgetType.WEATHER) }
            ActionCard("App drawer", "Shortcut to open all apps", palette) { onWidget(WidgetType.APP_DRAWER) }
            ActionCard("Group / folder", "Create an empty group", palette, onGroup)
            ActionCard("Module style", "Opacity, picture, or video", palette, onStyle)
            ActionCard("Remove", "Clear this cell", palette, onRemove)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, palette: LauncherPalette, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.searchBackground)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(title, color = palette.textPrimary, fontWeight = FontWeight.Medium)
        Text(subtitle, color = palette.textSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun Chip(
    label: String,
    palette: LauncherPalette,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) palette.accent else palette.searchBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (selected) Color.Black else palette.textPrimary, fontSize = 13.sp)
    }
}
