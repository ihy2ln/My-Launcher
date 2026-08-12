package com.homelauncher.app.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homelauncher.app.AppInfo
import com.homelauncher.app.model.WidgetType
import com.homelauncher.app.model.displayName
import com.homelauncher.app.ui.components.widgetCatalog
import com.homelauncher.app.ui.theme.LauncherPalette

sealed class LauncherSearchResult {
    data class App(val app: AppInfo) : LauncherSearchResult()
    data class Widget(val type: WidgetType, val label: String) : LauncherSearchResult()
    data class Setting(val title: String, val subtitle: String, val sectionId: String) : LauncherSearchResult()
}

object LauncherSearchIndex {
    val settingsEntries = listOf(
        Triple("home", "Home screen", "Set grid, icon layout, dock settings, and more."),
        Triple("drawer", "App drawer", "Set layout, style, opening gesture, and more."),
        Triple("folders", "Folders", "Set window styles, background colors, and icon layout."),
        Triple("search", "Search", "Search window and bar configuration."),
        Triple("cards", "Cards", "Add and edit cards for the app drawer."),
        Triple("style", "Style", "Theme, accent, wallpaper, icons, and scroll effects."),
        Triple("gestures", "Gestures & inputs", "Swipe, tap, and pinch gestures on the home screen."),
        Triple("integrations", "Integrations", "Connect other apps and community links."),
        Triple("badges", "Notification badges", "Badge visibility preferences."),
        Triple("backup", "Backup & restore", "Export or restore launcher layout and settings."),
    )

    fun search(
        query: String,
        apps: List<AppInfo>,
        hiddenApps: Set<String> = emptySet(),
    ): List<LauncherSearchResult> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val results = mutableListOf<LauncherSearchResult>()

        apps.filterNot { it.key in hiddenApps }
            .filter { it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true) }
            .take(12)
            .forEach { results += LauncherSearchResult.App(it) }

        widgetCatalog()
            .filter { (type, label) ->
                label.contains(q, ignoreCase = true) || type.displayName().contains(q, ignoreCase = true)
            }
            .forEach { (type, label) -> results += LauncherSearchResult.Widget(type, label) }

        settingsEntries
            .filter { (_, title, subtitle) ->
                title.contains(q, ignoreCase = true) || subtitle.contains(q, ignoreCase = true)
            }
            .forEach { (id, title, subtitle) ->
                results += LauncherSearchResult.Setting(title, subtitle, id)
            }

        return results
    }
}

@Composable
fun LauncherSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    palette: LauncherPalette,
    modifier: Modifier = Modifier,
    placeholder: String = "Search apps, widgets, settings…",
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = palette.textPrimary, fontSize = 15.sp),
        cursorBrush = SolidColor(palette.accent),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.searchBackground.copy(alpha = 0.92f))
            .clickable(onClick = onOpenSearch)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⌕", color = palette.textSecondary, fontSize = 16.sp)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                if (query.isEmpty()) {
                    Text(placeholder, color = palette.textSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                } else {
                    Column(modifier = Modifier.weight(1f)) { inner() }
                }
            }
        },
    )
}

@Composable
fun GlobalSearchOverlay(
    visible: Boolean,
    apps: List<AppInfo>,
    hiddenApps: Set<String>,
    palette: LauncherPalette,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenSettingsSection: (String) -> Unit,
    onAddWidget: (WidgetType) -> Unit,
    onClose: () -> Unit,
) {
    var query by remember(visible) { mutableStateOf("") }
    val results = remember(query, apps, hiddenApps) {
        LauncherSearchIndex.search(query, apps, hiddenApps)
    }
    val microResults = remember(query) { SearchEngine.microResults(query) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it / 3 },
        exit = slideOutVertically { -it / 3 },
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = palette.drawerBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = palette.textPrimary, fontSize = 16.sp),
                        cursorBrush = SolidColor(palette.accent),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(palette.searchBackground)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        decorationBox = { inner ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⌕", color = palette.textSecondary)
                                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                                if (query.isEmpty()) {
                                    Text("Search apps, widgets, settings…", color = palette.textSecondary)
                                } else {
                                    inner()
                                }
                            }
                        },
                    )
                    TextButton(onClick = onClose) { Text("Close", color = palette.accent) }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    microResults.forEach { result ->
                        item {
                            MicroResultRow(result, palette)
                        }
                    }
                    if (query.isBlank()) {
                        item {
                            Text(
                                "Type to search installed apps, home widgets, and launcher settings.",
                                color = palette.textSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    } else if (results.isEmpty() && microResults.isEmpty()) {
                        item {
                            Text("No results for \"$query\"", color = palette.textSecondary, modifier = Modifier.padding(12.dp))
                        }
                    } else {
                        items(results, key = {
                            when (it) {
                                is LauncherSearchResult.App -> "app_${it.app.key}"
                                is LauncherSearchResult.Widget -> "widget_${it.type.name}"
                                is LauncherSearchResult.Setting -> "setting_${it.sectionId}"
                            }
                        }) { result ->
                            when (result) {
                                is LauncherSearchResult.App -> SearchResultRow(
                                    title = result.app.label,
                                    subtitle = "App",
                                    palette = palette,
                                    onClick = { onLaunchApp(result.app); onClose() },
                                )
                                is LauncherSearchResult.Widget -> SearchResultRow(
                                    title = result.label,
                                    subtitle = "Widget · add from Edit home",
                                    palette = palette,
                                    onClick = { onAddWidget(result.type); onClose() },
                                )
                                is LauncherSearchResult.Setting -> SearchResultRow(
                                    title = result.title,
                                    subtitle = result.subtitle,
                                    palette = palette,
                                    onClick = { onOpenSettingsSection(result.sectionId); onClose() },
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    palette: LauncherPalette,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.searchBackground)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(title, color = palette.textPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Text(subtitle, color = palette.textSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun MicroResultRow(result: MicroResult, palette: LauncherPalette) {
    val (title, detail) = when (result) {
        is MicroResult.Calculation -> "Calculator" to "${result.expression} = ${result.result}"
        is MicroResult.UnitConversion -> "Convert" to "${result.from} → ${result.result} ${result.to}"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.accent.copy(alpha = 0.15f))
            .padding(14.dp),
    ) {
        Text(title, color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(detail, color = palette.textPrimary, fontSize = 15.sp)
    }
}
