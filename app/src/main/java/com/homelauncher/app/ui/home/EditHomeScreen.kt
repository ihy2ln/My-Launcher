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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.homelauncher.app.model.FloatingWidget
import com.homelauncher.app.model.displayAppLabel
import com.homelauncher.app.ui.search.LauncherSearchBar
import com.homelauncher.app.ui.theme.LauncherPalette
import com.homelauncher.app.ui.theme.iconShape
import kotlin.math.roundToInt
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
    onPickAppForBlankWidget: (String) -> Unit,
    onAddAppsToFolder: (FolderInfo) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showColorWheel by remember { mutableStateOf(false) }
    var showWallpaperSheet by remember { mutableStateOf(false) }
    var addTargetIndex by remember { mutableStateOf<Int?>(null) }
    var moduleEditIndex by remember { mutableStateOf<Int?>(null) }
    var moduleStyleIndex by remember { mutableStateOf<Int?>(null) }
    var createGroupIndex by remember { mutableStateOf<Int?>(null) }
    var groupTitle by remember { mutableStateOf("Group") }
    var widgetTitleDraft by remember { mutableStateOf("") }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var editSearchQuery by remember { mutableStateOf("") }
    var dragState by remember { mutableStateOf<HomeDragState?>(null) }
    var hoverIndex by remember { mutableStateOf<Int?>(null) }
    val cellBounds = remember { mutableStateMapOf<Int, Rect>() }
    var widgetEditTarget by remember { mutableStateOf<FloatingWidget?>(null) }
    var widgetOpacityDraft by remember { mutableStateOf(1f) }
    var moduleOpacityDraft by remember { mutableStateOf(0.45f) }
    var renameModuleDraft by remember { mutableStateOf("") }

    fun hitTest(point: Offset): Int? =
        cellBounds.entries.firstOrNull { (_, rect) -> rect.contains(point) }?.key

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
        val index = moduleStyleIndex ?: moduleEditIndex ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            val path = takePersistable(uri)
            scope.launch {
                val current = layout.moduleStyles[index] ?: ModuleStyle(opacity = settings.moduleOpacity)
                repository.setModuleStyle(index, current.copy(imageUri = path, videoUri = null))
            }
        }
    }
    val pickModuleVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val index = moduleStyleIndex ?: moduleEditIndex ?: return@rememberLauncherForActivityResult
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
                    .padding(top = 8.dp, bottom = 6.dp),
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
                    modifier = Modifier.weight(1f),
                )
            }
            LauncherSearchBar(
                query = editSearchQuery,
                onQueryChange = { editSearchQuery = it },
                onOpenSearch = onOpenSearch,
                palette = palette,
                modifier = Modifier.padding(bottom = 8.dp),
            )

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
                        text = "Tap + to add · Long-press drag to move · Double-tap for options",
                        color = Color.White.copy(0.75f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    )

                    if (dragState != null) {
                        Text(
                            "Drop on another cell to move or swap",
                            color = palette.accent,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }

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
                            val isHoverTarget = hoverIndex == index && dragState != null && dragState?.fromIndex != index
                            val slot = layout.homeSlots[index]
                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { coords ->
                                        cellBounds[index] = coords.boundsInRoot()
                                    },
                            ) {
                                if (dragState?.fromIndex == index) {
                                    Box(modifier = Modifier.height(84.dp).fillMaxWidth())
                                } else {
                                    val style = layout.moduleStyles[index]
                                    val opacity = style?.opacity ?: settings.moduleOpacity
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
                                        HomeCell(
                                            slot = slot,
                                            style = style,
                                            defaultOpacity = settings.moduleOpacity,
                                            apps = apps,
                                            folders = layout.folders,
                                            settings = settings,
                                            palette = palette,
                                            showEmpty = true,
                                            highlighted = isHoverTarget && slot is HomeSlot.Folder,
                                            appAliases = layout.appAliases,
                                            onLaunch = { addTargetIndex = index },
                                            onOpenFolder = { addTargetIndex = index },
                                            onWidgetClick = { },
                                            onEmpty = { addTargetIndex = index },
                                            onDoubleClick = {
                                                moduleEditIndex = index
                                                val currentStyle = layout.moduleStyles[index] ?: ModuleStyle(opacity = settings.moduleOpacity)
                                                renameModuleDraft = currentStyle.title.orEmpty()
                                                moduleOpacityDraft = currentStyle.opacity
                                                when (slot) {
                                                    is HomeSlot.App -> {
                                                        val app = findApp(apps, slot.key)
                                                        if (app != null) {
                                                            renameModuleDraft = layout.appAliases[app.key] ?: app.label
                                                        }
                                                    }
                                                    is HomeSlot.Folder -> {
                                                        renameModuleDraft = layout.folders[slot.folderId]?.title.orEmpty()
                                                    }
                                                    else -> Unit
                                                }
                                            },
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
                                                    scope.launch { repository.moveHomeSlot(from, target) }
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
                    }

                    FloatingWidgetsLayer(
                        widgets = layout.floatingWidgets,
                        apps = apps,
                        appAliases = layout.appAliases,
                        settings = settings,
                        palette = palette,
                        editable = true,
                        onClick = { widget ->
                            if (widget.type == WidgetType.BLANK && widget.appKey == null) {
                                onPickAppForBlankWidget(widget.id)
                            }
                        },
                        onDoubleTap = { widget ->
                            widgetEditTarget = widget
                            widgetTitleDraft = widget.title
                            widgetOpacityDraft = widget.opacity
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
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("Add widget", color = palette.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Widgets float freely — long-press drag to move, corner handle to resize.",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(com.homelauncher.app.ui.components.widgetCatalog()) { (type, label) ->
                        ActionCard(label, "Free-form size and position", palette) {
                            scope.launch {
                                val id = repository.addFloatingWidget(type)
                                showWidgetPicker = false
                                if (type == WidgetType.BLANK) {
                                    onPickAppForBlankWidget(id)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    widgetEditTarget?.let { widget ->
        AlertDialog(
            onDismissRequest = { widgetEditTarget = null },
            title = { Text("Widget options") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Rename, opacity, move (long-press drag), or remove.")
                    androidx.compose.material3.TextField(
                        value = widgetTitleDraft,
                        onValueChange = { widgetTitleDraft = it },
                        label = { Text("Name") },
                    )
                    Text("Opacity")
                    Slider(
                        value = widgetOpacityDraft,
                        onValueChange = { widgetOpacityDraft = it },
                        valueRange = 0.15f..1f,
                    )
                    if (widget.type == WidgetType.BLANK) {
                        TextButton(onClick = {
                            widgetEditTarget = null
                            onPickAppForBlankWidget(widget.id)
                        }) { Text("Choose app") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.updateFloatingWidget(
                            widget.copy(
                                title = widgetTitleDraft.ifBlank { widget.title },
                                opacity = widgetOpacityDraft,
                            ),
                        )
                        widgetEditTarget = null
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        scope.launch {
                            repository.removeFloatingWidgetAndHost(widget.id) { hostId ->
                                com.homelauncher.app.widget.deleteHostWidget(
                                    com.homelauncher.app.widget.LauncherWidgetHost.get(context),
                                    hostId,
                                )
                            }
                            widgetEditTarget = null
                        }
                    }) { Text("Remove") }
                    TextButton(onClick = { widgetEditTarget = null }) { Text("Cancel") }
                }
            },
        )
    }

    addTargetIndex?.let { index ->
        val slot = layout.homeSlots.getOrNull(index)
        val existingFolder = (slot as? HomeSlot.Folder)?.let { layout.folders[it.folderId] }
        AddItemSheet(
            palette = palette,
            isFolder = existingFolder != null,
            onDismiss = { addTargetIndex = null },
            onApp = {
                addTargetIndex = null
                onPickAppForSlot(index)
            },
            onAddAppsToFolder = {
                existingFolder?.let {
                    addTargetIndex = null
                    onAddAppsToFolder(it)
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
                moduleStyleIndex = index
                addTargetIndex = null
            },
        )
    }

    moduleEditIndex?.let { index ->
        val style = layout.moduleStyles[index] ?: ModuleStyle(opacity = settings.moduleOpacity)
        val slot = layout.homeSlots[index]
        val folder = (slot as? HomeSlot.Folder)?.let { layout.folders[it.folderId] }
        AlertDialog(
            onDismissRequest = { moduleEditIndex = null },
            title = { Text(if (folder != null) "Folder options" else "Item options") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Rename, opacity, long-press drag to move, or remove.")
                    androidx.compose.material3.TextField(
                        value = renameModuleDraft,
                        onValueChange = { renameModuleDraft = it },
                        label = { Text("Name") },
                    )
                    Text("Opacity")
                    Slider(
                        value = moduleOpacityDraft,
                        onValueChange = { moduleOpacityDraft = it },
                        valueRange = 0.15f..0.95f,
                    )
                    if (folder != null) {
                        TextButton(onClick = {
                            moduleEditIndex = null
                            onAddAppsToFolder(folder)
                        }) { Text("Add apps to folder") }
                    }
                    TextButton(onClick = { moduleEditIndex = null }) {
                        Text("Move: long-press and drag this icon")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        when (slot) {
                            is HomeSlot.App -> {
                                val app = findApp(apps, slot.key)
                                if (app != null) {
                                    repository.setAppAlias(app.key, renameModuleDraft.ifBlank { null })
                                }
                            }
                            is HomeSlot.Folder -> {
                                val live = layout.folders[slot.folderId]
                                if (live != null) {
                                    repository.updateFolder(live.copy(title = renameModuleDraft.ifBlank { live.title }))
                                }
                            }
                            else -> Unit
                        }
                        repository.setModuleStyle(index, style.copy(opacity = moduleOpacityDraft, title = renameModuleDraft.ifBlank { null }))
                        moduleEditIndex = null
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        scope.launch {
                            repository.setHomeSlot(index, null)
                            moduleEditIndex = null
                        }
                    }) { Text("Remove") }
                    TextButton(onClick = {
                        moduleStyleIndex = index
                        moduleEditIndex = null
                    }) { Text("Style") }
                    TextButton(onClick = { moduleEditIndex = null }) { Text("Cancel") }
                }
            },
        )
    }

    moduleStyleIndex?.let { index ->
        val style = layout.moduleStyles[index] ?: ModuleStyle(opacity = settings.moduleOpacity)
        AlertDialog(
            onDismissRequest = { moduleStyleIndex = null },
            title = { Text("Module style") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ColorWheelPicker(
                        color = style.color,
                        onColorChange = { color ->
                            scope.launch {
                                repository.setModuleStyle(index, style.copy(color = color))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
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
                TextButton(onClick = { moduleStyleIndex = null }) { Text("Done") }
            },
        )
    }

    createGroupIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { createGroupIndex = null },
            title = { Text("New group") },
            text = {
                Column {
                    Text("Create a folder, then pick apps to add.", fontSize = 13.sp)
                    androidx.compose.material3.TextField(
                        value = groupTitle,
                        onValueChange = { groupTitle = it },
                        label = { Text("Group name") },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val title = groupTitle.ifBlank { "Group" }
                        val folderId = repository.createFolder(title, emptyList(), index)
                        createGroupIndex = null
                        onAddAppsToFolder(FolderInfo(folderId, title, emptyList()))
                    }
                }) { Text("Create & add apps") }
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
    isFolder: Boolean = false,
    onDismiss: () -> Unit,
    onApp: () -> Unit,
    onAddAppsToFolder: () -> Unit = {},
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (isFolder) "Folder" else "Add to home",
                color = palette.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Widgets are added from the Widgets bar below.",
                color = palette.textSecondary,
                fontSize = 12.sp,
            )
            if (isFolder) {
                ActionCard("Add apps", "Pick apps to put in this folder", palette, onAddAppsToFolder)
            } else {
                ActionCard("App", "Pick an installed app", palette, onApp)
                ActionCard("Group / folder", "Create a group, then add apps", palette, onGroup)
            }
            ActionCard("Module style", "Opacity, picture, video, rename", palette, onStyle)
            ActionCard("Remove", "Clear this cell", palette, onRemove)
            Spacer(modifier = Modifier.height(16.dp))
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
