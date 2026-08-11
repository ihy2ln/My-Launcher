package com.homelauncher.app.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homelauncher.app.AppInfo
import com.homelauncher.app.model.DrawerGroup
import com.homelauncher.app.model.DrawerScroll
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.SearchBarPosition
import com.homelauncher.app.ui.components.AppIconView
import com.homelauncher.app.ui.search.MicroResult
import com.homelauncher.app.ui.search.SearchEngine
import com.homelauncher.app.ui.theme.LauncherPalette

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    hiddenApps: Set<String>,
    groups: List<DrawerGroup>,
    settings: LauncherSettings,
    palette: LauncherPalette,
    placementHint: String?,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    onDismissPlacement: () -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visibleApps = remember(apps, hiddenApps) { apps.filterNot { it.key in hiddenApps } }
    val microResults = remember(query) { SearchEngine.microResults(query) }
    val filteredApps = remember(visibleApps, query) {
        if (query.isBlank()) visibleApps
        else visibleApps.filter { it.label.contains(query, ignoreCase = true) }
    }

    val tabs = remember(groups) {
        listOf("All") + groups.map { it.title }
    }
    val pagerState = rememberPagerState(pageCount = { tabs.size.coerceAtLeast(1) })

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 24f) onClose()
                }
            },
        color = palette.drawerBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("All apps", color = palette.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                if (placementHint != null) {
                    TextButton(onClick = onDismissPlacement) {
                        Text("Cancel", color = palette.accent)
                    }
                }
            }

            if (placementHint != null) {
                Text(
                    text = placementHint,
                    color = palette.accent,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (settings.searchBarPosition == SearchBarPosition.TOP) {
                SearchField(
                    value = query,
                    palette = palette,
                    onValueChange = { query = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            microResults.forEach { result ->
                MicroResultCard(result, palette)
            }

            if (groups.isNotEmpty() && query.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = pagerState.currentPage == index
                        Text(
                            text = title,
                            color = if (selected) palette.accent else palette.textSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (filteredApps.isEmpty() && microResults.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (query.isBlank()) "No apps found" else "No matches for \"$query\"",
                        color = palette.textSecondary,
                    )
                }
            } else if (query.isNotBlank() || groups.isEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    AppGrid(
                        apps = filteredApps,
                        settings = settings,
                        palette = palette,
                        onLaunch = onLaunch,
                        onLongPress = onLongPress,
                    )
                }
            } else {
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    val pageApps = if (page == 0) {
                        visibleApps
                    } else {
                        val group = groups.getOrNull(page - 1)
                        visibleApps.filter { app -> group?.appKeys?.contains(app.key) == true }
                    }
                    AppGrid(
                        apps = pageApps,
                        settings = settings,
                        palette = palette,
                        onLaunch = onLaunch,
                        onLongPress = onLongPress,
                    )
                }
            }

            if (settings.searchBarPosition == SearchBarPosition.BOTTOM) {
                SearchField(
                    value = query,
                    palette = palette,
                    onValueChange = { query = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun AppGrid(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    palette: LauncherPalette,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
) {
    if (settings.drawerScroll == DrawerScroll.HORIZONTAL) {
        LazyHorizontalGrid(
            rows = GridCells.Fixed(5),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(apps, key = { it.key }) { app ->
                AppIconView(app, settings, palette, onClick = { onLaunch(app) }, onLongClick = { onLongPress(app) })
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(settings.drawerColumns),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(apps, key = { it.key }) { app ->
                AppIconView(app, settings, palette, onClick = { onLaunch(app) }, onLongClick = { onLongPress(app) })
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    palette: LauncherPalette,
    onValueChange: (String) -> Unit,
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
            .clip(RoundedCornerShape(24.dp))
            .background(palette.searchBackground)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text("Search apps, calc, units...", color = palette.textSecondary, fontSize = 16.sp)
            }
            inner()
        },
    )
}

@Composable
private fun MicroResultCard(result: MicroResult, palette: LauncherPalette) {
    val (title, subtitle) = when (result) {
        is MicroResult.Calculation -> "Result" to "${result.expression} = ${result.result}"
        is MicroResult.UnitConversion -> "Unit conversion" to "${result.from} -> ${result.result} ${result.to}"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.searchBackground)
            .padding(14.dp),
    ) {
        Text(title, color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = palette.textPrimary, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
