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
    var editingWidgetId by remember { mutableStateOf<String?>(null) }
    var widgetTitleDraft by remember { mutableStateOf("") }
    var showWidgetPicker by remember { mutableStateOf(false) }

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
            useGradient = settings.wallpaperMode == WallpaperMode.GRADIENT,
        )

        // Dim overlay for edit mode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDone) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Edit home",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.size(64.dp))
            }

            // Zoomed panel chrome (Nova-style)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(10.dp),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⌂", color = Color.Black, fontSize = 14.sp)
                    }

                    Text(
                        text = "Tap + for apps/groups · Widgets float freely — drag to move, corner to resize",
                        color = Color.White.copy(0.75f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(settings.homeColumns),
                        modifier = Modifier.fillMaxSize(),
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
                                        color = style?.color ?: 0xFF1A1A1A,
                                        saturation = style?.saturation ?: 0.2f,
                                        brightness = style?.brightness ?: 0.4f,
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
                                        color = style?.color ?: 0xFF1A1A1A,
                                        saturation = style?.saturation ?: 0.2f,
                                        brightness = style?.brightness ?: 0.4f,
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
                                is HomeSlot.Widget, null -> {
                                    ModulePlate(
                                        opacity = opacity,
                                        imageUri = style?.imageUri,
                                        videoUri = style?.videoUri,
                                        color = style?.color ?: 0xFF1A1A1A,
                                        saturation = style?.saturation ?: 0.2f,
                                        brightness = style?.brightness ?: 0.4f,
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
                                }
                            }
                        }
                    }

                    FloatingWidgetsLayer(
                        widgets = layout.floatingWidgets,
                        palette = palette,
                        editable = true,
                        onClick = { /* keep selected via long-press rename */ },
                        onLongPress = { widget ->
                            editingWidgetId = widget.id
                            widgetTitleDraft = widget.title
                        },
                        onMove = { widget, x, y ->
                            scope.launch {
                                repository.updateFloatingWidget(widget.copy(xFrac = x, yFrac = y))
                            }
                        },
                        onResize = { widget, w, h ->
                            scope.launch {
                                repository.updateFloatingWidget(widget.copy(widthFrac = w, heightFrac = h))
                            }
                        },
                    )
                    } // end Box
                }
            }

            // Nova-style bottom customization bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 22.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EditBarAction("Wallpapers", "▣") { showWallpaperSheet = true }
                EditBarAction("Widgets") { showWidgetPicker = true }
                EditBarAction("Settings", "⚙", onOpenSettings)
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

    if (showWidgetPicker) {
        ModalBottomSheet(
            onDismissRequest = { showWidgetPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = palette.drawerBackground,
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add widget", color = palette.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Widgets float freely — drag to move, use the corner handle to resize.", color = palette.textSecondary, fontSize = 12.sp)
                listOf(
                    WidgetType.CLOCK to "Clock",
                    WidgetType.WEATHER to "Weather",
                    WidgetType.APP_DRAWER to "App drawer button",
                ).forEach { (type, label) ->
                    ActionCard(label, "Free-form size and position", palette) {
                        scope.launch { repository.addFloatingWidget(type) }
                        showWidgetPicker = false
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    editingWidgetId?.let { id ->
        val widget = layout.floatingWidgets.firstOrNull { it.id == id }
        if (widget == null) {
            editingWidgetId = null
        } else {
            AlertDialog(
                onDismissRequest = { editingWidgetId = null },
                title = { Text("Widget") },
                text = {
                    Column {
                        Text("Rename and manage this floating widget.")
                        androidx.compose.material3.TextField(
                            value = widgetTitleDraft,
                            onValueChange = { widgetTitleDraft = it },
                            label = { Text("Name") },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            repository.updateFloatingWidget(widget.copy(title = widgetTitleDraft.ifBlank { widget.title }))
                            editingWidgetId = null
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            scope.launch {
                                repository.removeFloatingWidget(id)
                                editingWidgetId = null
                            }
                        }) { Text("Remove") }
                        TextButton(onClick = { editingWidgetId = null }) { Text("Cancel") }
                    }
                },
            )
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
                    repository.addFloatingWidget(type)
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Name this module", fontSize = 12.sp)
                    androidx.compose.material3.TextField(
                        value = style.title.orEmpty(),
                        onValueChange = { value ->
                            scope.launch {
                                repository.setModuleStyle(index, style.copy(title = value.ifBlank { null }))
                            }
                        },
                        label = { Text("Module name") },
                    )
                    Text("Color, saturation & brightness", fontSize = 12.sp)
                    ColorWheelPicker(
                        color = style.color,
                        onColorChange = { color ->
                            scope.launch {
                                repository.setModuleStyle(index, style.copy(color = color))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                    )
                    Text("Saturation")
                    Slider(
                        value = style.saturation,
                        onValueChange = { value ->
                            scope.launch {
                                repository.setModuleStyle(index, style.copy(saturation = value))
                            }
                        },
                        valueRange = 0f..1f,
                    )
                    Text("Brightness")
                    Slider(
                        value = style.brightness,
                        onValueChange = { value ->
                            scope.launch {
                                repository.setModuleStyle(index, style.copy(brightness = value))
                            }
                        },
                        valueRange = 0.1f..1f,
                    )
                    Text("Opacity")
                    Slider(
                        value = style.opacity,
                        onValueChange = { value ->
                            scope.launch {
                                repository.setModuleStyle(index, style.copy(opacity = value))
                            }
                        },
                        valueRange = 0.15f..0.95f,
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
private fun EditBarAction(label: String, glyph: String = "◇", onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 4.dp),
    ) {
        Text(glyph, color = Color.White, fontSize = 22.sp)
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
            ActionCard("Clock widget", "Free-form floating clock", palette) { onWidget(WidgetType.CLOCK) }
            ActionCard("Weather widget", "Free-form weather card", palette) { onWidget(WidgetType.WEATHER) }
            ActionCard("App drawer", "Floating shortcut to all apps", palette) { onWidget(WidgetType.APP_DRAWER) }
            ActionCard("Group / folder", "Create an empty group", palette, onGroup)
            ActionCard("Module style", "Opacity, picture, video, rename", palette, onStyle)
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
