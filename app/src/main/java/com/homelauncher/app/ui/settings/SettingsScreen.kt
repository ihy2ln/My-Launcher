package com.homelauncher.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.homelauncher.app.data.LauncherRepository
import com.homelauncher.app.model.DrawerScroll
import com.homelauncher.app.model.GestureAction
import com.homelauncher.app.model.IconShape
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.SearchBarPosition
import com.homelauncher.app.model.ThemeMode
import com.homelauncher.app.ui.theme.LauncherPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.drawerBackground)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Nova Settings", color = palette.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onBack) {
                Text("Done", color = palette.accent)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle("Look & feel", palette)
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
            Text("Accent color", color = palette.textPrimary, fontSize = 14.sp)
            AccentPicker(selected = settings.accentColor) { color ->
                update { it.copy(accentColor = color) }
            }
            Text("Wallpaper style", color = palette.textPrimary, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            WallpaperPicker(selected = settings.wallpaperStyle) { style ->
                update { it.copy(wallpaperStyle = style) }
            }

            SectionTitle("Icons", palette)
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

            SectionTitle("Desktop", palette)
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

            SectionTitle("App drawer", palette)
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
            ChoiceRow(
                title = "Search bar",
                options = listOf("Top", "Bottom"),
                selected = settings.searchBarPosition.ordinal,
                palette = palette,
                onSelect = { index -> update { it.copy(searchBarPosition = SearchBarPosition.entries[index]) } },
            )

            SectionTitle("Gestures (Prime)", palette)
            GesturePicker("Swipe up", settings.swipeUp, palette) { update { s -> s.copy(swipeUp = it) } }
            GesturePicker("Swipe down", settings.swipeDown, palette) { update { s -> s.copy(swipeDown = it) } }
            GesturePicker("Double tap", settings.doubleTap, palette) { update { s -> s.copy(doubleTap = it) } }
            GesturePicker("Pinch in", settings.pinchIn, palette) { update { s -> s.copy(pinchIn = it) } }

            SectionTitle("Backup & restore", palette)
            ActionRow("Backup to file", palette) { exportLauncher.launch("HomeLauncher-backup.json") }
            ActionRow("Restore from file", palette) { importLauncher.launch(arrayOf("application/json", "*/*")) }

            SectionTitle("Support", palette)
            ActionRow("Open Discord community", palette) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/novalauncher")))
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Home Launcher - Nova-inspired\nv0.3.0",
                color = palette.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
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

@Composable
private fun SectionTitle(title: String, palette: LauncherPalette) {
    Text(
        text = title,
        color = palette.accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
    HorizontalDivider(color = palette.textSecondary.copy(alpha = 0.2f))
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
private fun AccentPicker(selected: Long, onSelect: (Long) -> Unit) {
    val colors = listOf(
        0xFF82B1FF, 0xFF80CBC4, 0xFFFFAB91, 0xFFCE93D8,
        0xFFFFF59D, 0xFF90CAF9, 0xFFA5D6A7, 0xFFEF9A9A,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(if (color == selected) 40.dp else 36.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .clickable { onSelect(color) },
            )
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
                    .clickable { onSelect(index) }
                    .then(if (index == selected) Modifier.padding(2.dp) else Modifier),
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
        options = listOf("None", "App drawer", "Search", "Settings", "Notifications"),
        selected = value.ordinal,
        palette = palette,
        onSelect = { onSelect(GestureAction.entries[it]) },
    )
}

@Composable
private fun ActionRow(title: String, palette: LauncherPalette, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.searchBackground)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(title, color = palette.textPrimary, fontSize = 15.sp)
    }
}
