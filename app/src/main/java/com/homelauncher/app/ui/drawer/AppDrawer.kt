package com.homelauncher.app.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

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
    selectionMode: Boolean = false,
    selectedKeys: Set<String> = emptySet(),
    appAliases: Map<String, String> = emptyMap(),
    onToggleSelect: (AppInfo) -> Unit = {},
    onConfirmSelection: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val visibleApps = remember(apps, hiddenApps) { apps.filterNot { it.key in hiddenApps } }
    val microResults = remember(query) { SearchEngine.microResults(query) }
    val filteredApps = remember(visibleApps, query) {
        if (query.isBlank()) visibleApps
        else visibleApps.filter { it.label.contains(query, ignoreCase = true) }
    }

    val tabs = remember(groups) { listOf("All") + groups.map { it.title } }
    val pagerState = rememberPagerState(pageCount = { tabs.size.coerceAtLeast(1) })
    val useTabs = groups.isNotEmpty() && query.isBlank()
    val drawerBg = if (palette.isDark) palette.drawerBackground else Color.White
    val searchBg = if (palette.isDark) palette.searchBackground else Color(0xFFD7ECF8)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 24f) onClose()
                }
            },
        color = drawerBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            if (placementHint != null || selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when {
                            selectionMode -> "Select apps · ${selectedKeys.size} selected"
                            else -> placementHint.orEmpty()
                        },
                        color = palette.accent,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismissPlacement) {
                        Text("Cancel", color = palette.accent)
                    }
                }
            }

            if (settings.searchBarPosition == SearchBarPosition.TOP) {
                SearchField(
                    value = query,
                    background = searchBg,
                    textColor = palette.textPrimary,
                    hintColor = palette.textSecondary,
                    accent = palette.accent,
                    onValueChange = { query = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }

            microResults.forEach { result ->
                MicroResultCard(result, palette)
            }

            if (settings.showDrawerCards && query.isBlank() && placementHint == null && !selectionMode) {
                DrawerMediaCard(palette = palette)
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = palette.textSecondary.copy(alpha = 0.2f),
                )
            }

            if (groups.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = useTabs && pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (selected) palette.accent else palette.searchBackground)
                                .clickable {
                                    query = ""
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = title,
                                color = if (selected) Color.Black else palette.textPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                val tapAction: (AppInfo) -> Unit = { app ->
                    if (selectionMode) onToggleSelect(app) else onLaunch(app)
                }
                when {
                    query.isNotBlank() -> {
                        if (filteredApps.isEmpty() && microResults.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No matches for \"$query\"", color = palette.textSecondary)
                            }
                        } else {
                            AppGrid(filteredApps, settings, palette, selectedKeys, appAliases, tapAction, onLongPress)
                        }
                    }
                    groups.isEmpty() -> {
                        AppGrid(visibleApps, settings, palette, selectedKeys, appAliases, tapAction, onLongPress)
                    }
                    else -> {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            val pageApps = if (page == 0) {
                                visibleApps
                            } else {
                                val group = groups.getOrNull(page - 1)
                                visibleApps.filter { app -> group?.appKeys?.contains(app.key) == true }
                            }
                            if (pageApps.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (page == 0) {
                                            "No apps"
                                        } else {
                                            "No apps in ${tabs.getOrNull(page)}. Long-press an app to add it to this tab."
                                        },
                                        color = palette.textSecondary,
                                        modifier = Modifier.padding(24.dp),
                                    )
                                }
                            } else {
                                AppGrid(pageApps, settings, palette, selectedKeys, appAliases, tapAction, onLongPress)
                            }
                        }
                    }
                }
            }

            if (selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${selectedKeys.size} selected",
                        color = palette.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onConfirmSelection,
                        enabled = selectedKeys.isNotEmpty(),
                    ) {
                        Text("Add selected", color = palette.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (settings.searchBarPosition == SearchBarPosition.BOTTOM) {
                SearchField(
                    value = query,
                    background = searchBg,
                    textColor = palette.textPrimary,
                    hintColor = palette.textSecondary,
                    accent = palette.accent,
                    onValueChange = { query = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun DrawerMediaCard(palette: LauncherPalette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1DB954))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("♪", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Now playing", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Text("Card", color = Color.White.copy(0.8f), fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Something Comforting", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Porter Robinson", color = Color.White.copy(0.85f), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(0.35f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(4.dp)
                    .background(Color.White),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Get suggestions", color = Color.White.copy(0.85f), fontSize = 12.sp)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text("Open", color = Color(0xFF1DB954), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AppGrid(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    palette: LauncherPalette,
    selectedKeys: Set<String>,
    appAliases: Map<String, String>,
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
                AppIconView(
                    app = app,
                    settings = settings,
                    palette = palette,
                    onClick = { onLaunch(app) },
                    onLongClick = { onLongPress(app) },
                    labelOverride = appAliases[app.key],
                    selected = app.key in selectedKeys,
                )
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
                AppIconView(
                    app = app,
                    settings = settings,
                    palette = palette,
                    onClick = { onLaunch(app) },
                    onLongClick = { onLongPress(app) },
                    labelOverride = appAliases[app.key],
                    selected = app.key in selectedKeys,
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    background: Color,
    textColor: Color,
    hintColor: Color,
    accent: Color,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = textColor, fontSize = 16.sp),
        cursorBrush = SolidColor(accent),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(background)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("▦", color = hintColor, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text("Search apps.", color = hintColor, fontSize = 16.sp)
                    }
                    inner()
                }
            }
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
