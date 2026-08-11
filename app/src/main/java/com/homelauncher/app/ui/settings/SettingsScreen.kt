package com.homelauncher.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homelauncher.app.data.LauncherRepository
import com.homelauncher.app.model.DrawerScroll
import com.homelauncher.app.model.GestureAction
import com.homelauncher.app.model.IconShape
import com.homelauncher.app.model.DrawerGroup
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.ScrollEffect
import com.homelauncher.app.model.SearchBarPosition
import com.homelauncher.app.model.ThemeMode
import com.homelauncher.app.model.WallpaperMode
import com.homelauncher.app.ui.components.ColorWheelPicker
import com.homelauncher.app.ui.theme.LauncherPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsTab { SETTINGS, STYLE }
private enum class SettingsSection {
    HOME, DRAWER, FOLDERS, SEARCH, CARDS, LOOK, GESTURES, INTEGRATIONS, BADGES, BACKUP
}

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    palette: LauncherPalette,
    repository: LauncherRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(SettingsTab.SETTINGS) }
    var section by remember { mutableStateOf<SettingsSection?>(null) }
    var query by remember { mutableStateOf("") }
    var showScrollEffect by remember { mutableStateOf(false) }
    val layout by repository.layout.collectAsState(initial = com.homelauncher.app.model.defaultLayout(30, 6))
    var renameGroupId by remember { mutableStateOf<String?>(null) }
    var renameGroupDraft by remember { mutableStateOf("") }

    val takePersistable = { uri: Uri ->
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
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

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = repository.exportBackup()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }
                message = "Backup saved"
            } catch (e: Exception) {
                message = "Backup failed: ${e.message}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
                }
                repository.importBackup(json)
                message = "Backup restored"
            } catch (e: Exception) {
                message = "Restore failed: ${e.message}"
            }
        }
    }

    fun update(block: (LauncherSettings) -> LauncherSettings) {
        scope.launch { repository.updateSettings(block) }
    }

    BackHandler {
        when {
            showScrollEffect -> showScrollEffect = false
            section != null -> section = null
            else -> onBack()
        }
    }

    val hubItems = listOf(
        SettingsHubItem(SettingsSection.HOME, "Home screen", "Set grid, icon layout, dock settings, and more.", HubGlyph.Phone),
        SettingsHubItem(SettingsSection.DRAWER, "App drawer", "Set layout, style, opening gesture, and more.", HubGlyph.Drawer),
        SettingsHubItem(SettingsSection.FOLDERS, "Folders", "Set window styles, background colors, and icon layout.", HubGlyph.Folder),
        SettingsHubItem(SettingsSection.SEARCH, "Search", "Search window and bar configuration.", HubGlyph.Search),
        SettingsHubItem(SettingsSection.CARDS, "Cards", "Add and edit cards for the app drawer.", HubGlyph.Cards),
        SettingsHubItem(SettingsSection.LOOK, "Look & feel", "Icon preferences, popup menu, and wallpaper options.", HubGlyph.Palette),
        SettingsHubItem(SettingsSection.GESTURES, "Gestures & inputs", "Swipe, tap, and pinch gestures on the home screen.", HubGlyph.Gestures),
        SettingsHubItem(SettingsSection.INTEGRATIONS, "Integrations", "Connect other apps and community links.", HubGlyph.Puzzle),
        SettingsHubItem(SettingsSection.BADGES, "Notification badges", "Badge visibility preferences.", HubGlyph.Badge),
        SettingsHubItem(SettingsSection.BACKUP, "Backup & restore", "Export or restore launcher layout and settings.", HubGlyph.Backup),
    )

    val filtered = remember(query, hubItems) {
        if (query.isBlank()) hubItems
        else hubItems.filter {
            it.title.contains(query, true) || it.subtitle.contains(query, true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (palette.isDark) Color(0xFF0F141C) else Color(0xFFF2F5FA))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when {
            section != null -> {
                SectionHeader(
                    title = hubItems.first { it.section == section }.title,
                    palette = palette,
                    onBack = { section = null },
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (section) {
                        SettingsSection.HOME -> {
                            SliderRow("Columns", settings.homeColumns.toFloat(), 3f, 6f, palette) {
                                update { s -> s.copy(homeColumns = it.toInt()) }
                            }
                            SliderRow("Rows", settings.homeRows.toFloat(), 4f, 7f, palette) {
                                update { s -> s.copy(homeRows = it.toInt()) }
                            }
                            SliderRow("Dock icons", settings.dockSlots.toFloat(), 3f, 7f, palette) {
                                update { s -> s.copy(dockSlots = it.toInt()) }
                            }
                            SliderRow("Dock opacity", settings.dockBackgroundAlpha, 0.1f, 0.9f, palette) {
                                update { s -> s.copy(dockBackgroundAlpha = it) }
                            }
                            ActionRow("Scroll effect", "${settings.scrollEffect.label()} selected", palette) {
                                showScrollEffect = true
                            }
                        }
                        SettingsSection.DRAWER -> {
                            SliderRow("Columns", settings.drawerColumns.toFloat(), 3f, 6f, palette) {
                                update { s -> s.copy(drawerColumns = it.toInt()) }
                            }
                            ChoiceRow(
                                title = "Scroll",
                                options = listOf("Vertical", "Horizontal"),
                                selected = settings.drawerScroll.ordinal,
                                palette = palette,
                                onSelect = { index -> update { it.copy(drawerScroll = DrawerScroll.entries[index]) } },
                            )
                            Text("Drawer groups", color = palette.textPrimary, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
                            if (layout.drawerGroups.isEmpty()) {
                                Text("No groups yet. Long-press an app and choose Add to group.", color = palette.textSecondary, fontSize = 13.sp)
                            } else {
                                layout.drawerGroups.forEach { group ->
                                    ActionRow(group.title, "${group.appKeys.size} apps · tap to rename", palette) {
                                        renameGroupId = group.id
                                        renameGroupDraft = group.title
                                    }
                                }
                            }
                        }
                        SettingsSection.FOLDERS -> {
                            Text(
                                "Folders open as bottom sheets. Style module backgrounds from Edit Home.",
                                color = palette.textSecondary,
                                fontSize = 14.sp,
                            )
                            SliderRow("Default module opacity", settings.moduleOpacity, 0.15f, 0.9f, palette) {
                                update { s -> s.copy(moduleOpacity = it) }
                            }
                        }
                        SettingsSection.SEARCH -> {
                            ChoiceRow(
                                title = "Search bar",
                                options = listOf("Top", "Bottom"),
                                selected = settings.searchBarPosition.ordinal,
                                palette = palette,
                                onSelect = { index -> update { it.copy(searchBarPosition = SearchBarPosition.entries[index]) } },
                            )
                        }
                        SettingsSection.CARDS -> {
                            Text(
                                "Drawer cards appear above the app grid. Media-style cards help shortcuts stand out.",
                                color = palette.textSecondary,
                                fontSize = 14.sp,
                            )
                            SettingSwitch("Show drawer cards", settings.showDrawerCards, palette) {
                                update { s -> s.copy(showDrawerCards = it) }
                            }
                        }
                        SettingsSection.LOOK -> {
                            ChoiceRow(
                                title = "Theme",
                                options = listOf("System", "Light", "Dark"),
                                selected = settings.themeMode.ordinal,
                                palette = palette,
                                onSelect = { index -> update { it.copy(themeMode = ThemeMode.entries[index]) } },
                            )
                            SettingSwitch("Material You colors", settings.useMaterialYou, palette) {
                                update { s -> s.copy(useMaterialYou = it) }
                            }
                            Text("Accent color", color = palette.textPrimary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                            ColorWheelPicker(
                                color = settings.accentColor,
                                onColorChange = { color -> update { it.copy(accentColor = color) } },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text("Background color", color = palette.textPrimary, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                            ColorWheelPicker(
                                color = settings.wallpaperColor,
                                onColorChange = { color ->
                                    update {
                                        it.copy(
                                            wallpaperMode = WallpaperMode.COLOR,
                                            wallpaperColor = color,
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text("Background media", color = palette.textPrimary, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionChip("Picture", palette) { pickWallpaperImage.launch(arrayOf("image/*")) }
                                ActionChip("Video", palette) { pickWallpaperVideo.launch(arrayOf("video/*")) }
                                ActionChip("Solid color", palette) {
                                    update { it.copy(wallpaperMode = WallpaperMode.COLOR) }
                                }
                            }
                            Text("Gradient presets", color = palette.textPrimary, fontSize = 14.sp)
                            WallpaperPicker(selected = settings.wallpaperStyle) { style ->
                                update { it.copy(wallpaperStyle = style, wallpaperMode = WallpaperMode.GRADIENT) }
                            }
                            ChoiceRow(
                                title = "Icon shape",
                                options = listOf("System", "Circle", "Squircle", "Square", "Teardrop"),
                                selected = settings.iconShape.ordinal,
                                palette = palette,
                                onSelect = { index -> update { it.copy(iconShape = IconShape.entries[index]) } },
                            )
                            SliderRow("Icon size", settings.iconSizeDp.toFloat(), 40f, 72f, palette) {
                                update { s -> s.copy(iconSizeDp = it.toInt()) }
                            }
                            SettingSwitch("Show labels", settings.showLabels, palette) {
                                update { s -> s.copy(showLabels = it) }
                            }
                            SliderRow("Label size", settings.labelSizeSp.toFloat(), 10f, 16f, palette) {
                                update { s -> s.copy(labelSizeSp = it.toInt()) }
                            }
                        }
                        SettingsSection.GESTURES -> {
                            GesturePicker("Swipe up", settings.swipeUp, palette) { update { s -> s.copy(swipeUp = it) } }
                            GesturePicker("Swipe down", settings.swipeDown, palette) { update { s -> s.copy(swipeDown = it) } }
                            GesturePicker("Double tap", settings.doubleTap, palette) { update { s -> s.copy(doubleTap = it) } }
                            GesturePicker("Pinch in", settings.pinchIn, palette) { update { s -> s.copy(pinchIn = it) } }
                        }
                        SettingsSection.INTEGRATIONS -> {
                            ActionRow("Open Discord community", "Nova community link", palette) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/novalauncher")))
                            }
                        }
                        SettingsSection.BADGES -> {
                            Text(
                                "Notification badges follow the system unread counts when available.",
                                color = palette.textSecondary,
                                fontSize = 14.sp,
                            )
                        }
                        SettingsSection.BACKUP -> {
                            ActionRow("Backup to file", "Export layout and settings JSON", palette) {
                                exportLauncher.launch("HomeLauncher-backup.json")
                            }
                            ActionRow("Restore from file", "Import a previous backup", palette) {
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                        }
                        null -> Unit
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            tab == SettingsTab.STYLE -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Style", color = palette.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onBack) { Text("Done", color = Color(0xFF1A3A6B)) }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Quick look & feel", color = palette.textSecondary, fontSize = 13.sp)
                    ChoiceRow(
                        title = "Theme",
                        options = listOf("System", "Light", "Dark"),
                        selected = settings.themeMode.ordinal,
                        palette = palette,
                        onSelect = { index -> update { it.copy(themeMode = ThemeMode.entries[index]) } },
                    )
                    Text("Accent", color = palette.textSecondary, fontSize = 13.sp)
                    ColorWheelPicker(
                        color = settings.accentColor,
                        onColorChange = { color -> update { it.copy(accentColor = color) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Background", color = palette.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    ColorWheelPicker(
                        color = settings.wallpaperColor,
                        onColorChange = { color ->
                            update { it.copy(wallpaperMode = WallpaperMode.COLOR, wallpaperColor = color) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ChoiceRow(
                        title = "Icon shape",
                        options = listOf("System", "Circle", "Squircle", "Square", "Teardrop"),
                        selected = settings.iconShape.ordinal,
                        palette = palette,
                        onSelect = { index -> update { it.copy(iconShape = IconShape.entries[index]) } },
                    )
                    ActionRow("Scroll effect", settings.scrollEffect.label(), palette) {
                        showScrollEffect = true
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Home Launcher", color = Color(0xFF1A3A6B).takeIf { !palette.isDark } ?: palette.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onBack) { Text("Done", color = Color(0xFF1A3A6B).takeIf { !palette.isDark } ?: palette.accent) }
                }

                HubSearchField(
                    value = query,
                    onValueChange = { query = it },
                    palette = palette,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    filtered.forEach { item ->
                        HubRow(item = item, palette = palette) { section = item.section }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Home Launcher · Nova-inspired · v0.8.0",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }

        BottomSettingsTabs(
            selected = tab,
            palette = palette,
            onSelect = {
                tab = it
                section = null
            },
        )
    }

    renameGroupId?.let { id ->
        val group = layout.drawerGroups.firstOrNull { it.id == id }
        if (group != null) {
            AlertDialog(
                onDismissRequest = { renameGroupId = null },
                title = { Text("Rename group") },
                text = {
                    androidx.compose.material3.TextField(
                        value = renameGroupDraft,
                        onValueChange = { renameGroupDraft = it },
                        label = { Text("Group name") },
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val groups = layout.drawerGroups.map {
                                if (it.id == id) it.copy(title = renameGroupDraft.ifBlank { it.title }) else it
                            }
                            repository.saveDrawerGroups(groups)
                            renameGroupId = null
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { renameGroupId = null }) { Text("Cancel") }
                },
            )
        }
    }

    if (showScrollEffect) {
        ScrollEffectDialog(
            selected = settings.scrollEffect,
            accent = Color(0xFFE53935),
            onSelect = { effect ->
                update { it.copy(scrollEffect = effect) }
            },
            onDismiss = { showScrollEffect = false },
        )
    }

    message?.let {
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } },
            title = { Text("Backup") },
            text = { Text(it) },
        )
    }
}

private data class SettingsHubItem(
    val section: SettingsSection,
    val title: String,
    val subtitle: String,
    val glyph: HubGlyph,
)

private enum class HubGlyph { Phone, Drawer, Folder, Search, Cards, Palette, Gestures, Puzzle, Badge, Backup }

private fun ScrollEffect.label(): String = when (this) {
    ScrollEffect.SIMPLE -> "Simple"
    ScrollEffect.CUBE -> "Cube"
    ScrollEffect.CARD_STACK -> "Card Stack"
    ScrollEffect.TABLET -> "Tablet"
    ScrollEffect.REVOLVING_DOOR -> "Revolving Door"
}

@Composable
private fun SectionHeader(title: String, palette: LauncherPalette, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text("Back", color = Color(0xFF1A3A6B).takeIf { !palette.isDark } ?: palette.accent) }
        Text(title, color = palette.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HubSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    palette: LauncherPalette,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = palette.textPrimary, fontSize = 16.sp),
        cursorBrush = SolidColor(palette.accent),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, palette.textSecondary.copy(0.25f), RoundedCornerShape(28.dp))
            .background(palette.surface)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                HubIcon(HubGlyph.Search, Color(0xFF1A3A6B).takeIf { !palette.isDark } ?: palette.textSecondary)
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text("Search", color = palette.textSecondary, fontSize = 16.sp)
                    }
                    inner()
                }
            }
        },
    )
}

@Composable
private fun HubRow(item: SettingsHubItem, palette: LauncherPalette, onClick: () -> Unit) {
    val iconColor = Color(0xFF1A3A6B).takeIf { !palette.isDark } ?: palette.accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            HubIcon(item.glyph, iconColor)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, color = palette.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(item.subtitle, color = palette.textSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun HubIcon(glyph: HubGlyph, color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = Stroke(width = 2.2f, cap = StrokeCap.Round)
        when (glyph) {
            HubGlyph.Phone -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.28f, size.height * 0.12f),
                    size = Size(size.width * 0.44f, size.height * 0.76f),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = stroke,
                )
            }
            HubGlyph.Drawer -> {
                drawCircle(color = color, radius = size.minDimension * 0.38f, style = stroke)
                drawLine(color, Offset(size.width * 0.32f, size.height * 0.4f), Offset(size.width * 0.68f, size.height * 0.4f), 2.2f)
                drawLine(color, Offset(size.width * 0.32f, size.height * 0.52f), Offset(size.width * 0.68f, size.height * 0.52f), 2.2f)
                drawLine(color, Offset(size.width * 0.32f, size.height * 0.64f), Offset(size.width * 0.68f, size.height * 0.64f), 2.2f)
            }
            HubGlyph.Folder -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.34f),
                    size = Size(size.width * 0.68f, size.height * 0.42f),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = stroke,
                )
            }
            HubGlyph.Search -> {
                drawCircle(color = color, radius = size.minDimension * 0.28f, center = Offset(size.width * 0.42f, size.height * 0.42f), style = stroke)
                drawLine(color, Offset(size.width * 0.62f, size.height * 0.62f), Offset(size.width * 0.82f, size.height * 0.82f), 2.4f, StrokeCap.Round)
            }
            HubGlyph.Cards -> {
                drawRoundRect(color, Offset(size.width * 0.22f, size.height * 0.28f), Size(size.width * 0.42f, size.height * 0.5f), CornerRadius(4f, 4f), style = stroke)
                drawRoundRect(color, Offset(size.width * 0.36f, size.height * 0.18f), Size(size.width * 0.42f, size.height * 0.5f), CornerRadius(4f, 4f), style = stroke)
            }
            HubGlyph.Palette -> {
                drawCircle(color = color, radius = size.minDimension * 0.36f, style = stroke)
                drawCircle(color = color, radius = 2.5f, center = Offset(size.width * 0.38f, size.height * 0.4f))
                drawCircle(color = color, radius = 2.5f, center = Offset(size.width * 0.55f, size.height * 0.32f))
                drawCircle(color = color, radius = 2.5f, center = Offset(size.width * 0.62f, size.height * 0.5f))
            }
            HubGlyph.Gestures -> {
                val path = Path().apply {
                    moveTo(size.width * 0.25f, size.height * 0.35f)
                    cubicTo(size.width * 0.4f, size.height * 0.1f, size.width * 0.6f, size.height * 0.9f, size.width * 0.75f, size.height * 0.65f)
                }
                drawPath(path, color = color, style = stroke)
            }
            HubGlyph.Puzzle -> {
                drawRoundRect(color, Offset(size.width * 0.22f, size.height * 0.22f), Size(size.width * 0.56f, size.height * 0.56f), CornerRadius(4f, 4f), style = stroke)
            }
            HubGlyph.Badge -> {
                drawCircle(color = color, radius = size.minDimension * 0.34f, style = stroke)
                drawCircle(color = color, radius = size.minDimension * 0.12f)
            }
            HubGlyph.Backup -> {
                drawRoundRect(color, Offset(size.width * 0.28f, size.height * 0.2f), Size(size.width * 0.44f, size.height * 0.6f), CornerRadius(4f, 4f), style = stroke)
                drawLine(color, Offset(size.width * 0.5f, size.height * 0.35f), Offset(size.width * 0.5f, size.height * 0.62f), 2.2f)
            }
        }
    }
}

@Composable
private fun BottomSettingsTabs(
    selected: SettingsTab,
    palette: LauncherPalette,
    onSelect: (SettingsTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (palette.isDark) Color(0xFF151A22) else Color(0xFFE8EEF7))
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(SettingsTab.SETTINGS to "Settings", SettingsTab.STYLE to "Style").forEach { (tab, label) ->
            val active = selected == tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) Color(0xFF1A3A6B).copy(alpha = if (palette.isDark) 0.35f else 0.15f) else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 28.dp, vertical = 8.dp),
            ) {
                HubIcon(
                    if (tab == SettingsTab.SETTINGS) HubGlyph.Backup else HubGlyph.Palette,
                    if (active) Color(0xFF1A3A6B).takeIf { !palette.isDark } ?: palette.accent else palette.textSecondary,
                )
                Text(
                    label,
                    color = if (active) Color(0xFF1A3A6B).takeIf { !palette.isDark } ?: palette.accent else palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun ScrollEffectDialog(
    selected: ScrollEffect,
    accent: Color,
    onSelect: (ScrollEffect) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scroll effect", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ScrollEffectPreview(selected)
                }
                ScrollEffect.entries.forEach { effect ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(effect) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(effect.label(), fontSize = 16.sp)
                        RadioButton(
                            selected = selected == effect,
                            onClick = { onSelect(effect) },
                            colors = RadioButtonDefaults.colors(selectedColor = accent),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DONE", color = accent, fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
private fun ScrollEffectPreview(effect: ScrollEffect) {
    Canvas(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF3F3F3)),
    ) {
        drawRect(Color(0xFFFFFFFF), size = Size(size.width, size.height * 0.18f))
        // striped mid
        var x = 0f
        var flip = false
        while (x < size.width) {
            drawRect(
                color = if (flip) Color(0xFF6D6D6D) else Color(0xFFD2B48C),
                topLeft = Offset(x, size.height * 0.18f),
                size = Size(12f, size.height * 0.42f),
            )
            x += 12f
            flip = !flip
        }
        // icon grid
        val gridTop = size.height * 0.62f
        val cell = size.width / 4.5f
        for (row in 0 until 2) {
            for (col in 0 until 4) {
                drawRoundRect(
                    color = Color(0xFF90CAF9),
                    topLeft = Offset(cell * 0.25f + col * cell, gridTop + row * (cell * 0.7f)),
                    size = Size(cell * 0.55f, cell * 0.55f),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }
        }
        // subtle cue of selected effect
        if (effect == ScrollEffect.CUBE || effect == ScrollEffect.REVOLVING_DOOR) {
            drawLine(Color(0xFFE53935).copy(0.5f), Offset(size.width * 0.75f, 0f), Offset(size.width, size.height), 2f)
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    checked: Boolean,
    palette: LauncherPalette,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = palette.textPrimary, fontSize = 15.sp)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    min: Float,
    max: Float,
    palette: LauncherPalette,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = palette.textPrimary, fontSize = 15.sp)
            Text(
                if (max <= 1f) String.format("%.2f", value) else value.toInt().toString(),
                color = palette.textSecondary,
            )
        }
        Slider(value = value.coerceIn(min, max), onValueChange = onChange, valueRange = min..max)
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    options: List<String>,
    selected: Int,
    palette: LauncherPalette,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = palette.textPrimary, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            options.forEachIndexed { index, label ->
                val active = index == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) palette.accent else palette.searchBackground)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(label, color = if (active) Color.Black else palette.textPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}


@Composable
private fun WallpaperPicker(selected: Int, onSelect: (Int) -> Unit) {
    val previews = listOf(
        Color(0xFF4527A0), Color(0xFF1565C0), Color(0xFF388E3C),
        Color(0xFF5D4037), Color(0xFF37474F), Color(0xFFFFB300),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        previews.forEachIndexed { index, color ->
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .clickable { onSelect(index) },
            )
        }
    }
}

@Composable
private fun GesturePicker(
    title: String,
    value: GestureAction,
    palette: LauncherPalette,
    onSelect: (GestureAction) -> Unit,
) {
    ChoiceRow(
        title = title,
        options = listOf("None", "App drawer", "Search", "Settings", "Notifications", "Edit home"),
        selected = value.ordinal,
        palette = palette,
        onSelect = { onSelect(GestureAction.entries[it]) },
    )
}

@Composable
private fun ActionRow(title: String, subtitle: String? = null, palette: LauncherPalette, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.searchBackground)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(title, color = palette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        if (subtitle != null) {
            Text(subtitle, color = palette.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ActionChip(label: String, palette: LauncherPalette, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(palette.searchBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = palette.textPrimary, fontSize = 13.sp)
    }
}
