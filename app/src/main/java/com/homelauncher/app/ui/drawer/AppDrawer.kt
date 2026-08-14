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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homelauncher.app.AppInfo
import com.homelauncher.app.media.MediaNotificationListener
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
                DrawerMediaCards(palette = palette)
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
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
private fun DrawerMediaCards(palette: LauncherPalette) {
    val context = LocalContext.current
    val sessions by MediaNotificationListener.sessions.collectAsState()
    val listenerOn by MediaNotificationListener.listenerEnabled.collectAsState()
    var accessKnown by remember {
        mutableStateOf(MediaNotificationListener.isNotificationAccessEnabled(context))
    }

    LaunchedEffect(Unit) {
        while (true) {
            accessKnown = MediaNotificationListener.isNotificationAccessEnabled(context)
            if (accessKnown) MediaNotificationListener.refresh()
            kotlinx.coroutines.delay(2_000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (sessions.isEmpty()) {
            CompactMediaCard(
                brand = Color(0xFF455A64),
                appLabel = "Media",
                title = when {
                    accessKnown || listenerOn -> "Nothing playing"
                    else -> "Enable media access"
                },
                artist = when {
                    accessKnown || listenerOn -> "Play audio or video to see cards here"
                    else -> "Notification access required"
                },
                isPlaying = false,
                progress = 0f,
                artwork = null,
                showControls = false,
                onPlayPause = {},
                onPrev = {},
                onNext = {},
                onOpen = {
                    if (!accessKnown) {
                        MediaNotificationListener.openNotificationAccessSettings(context)
                    }
                },
                openLabel = if (accessKnown) "—" else "Enable",
            )
        } else {
            sessions.forEach { session ->
                CompactMediaCard(
                    brand = Color(session.brandColor),
                    appLabel = session.appLabel ?: session.packageName ?: "Media",
                    title = session.title.ifBlank { "Unknown track" },
                    artist = session.artist.ifBlank { session.appLabel.orEmpty() },
                    isPlaying = session.isPlaying,
                    progress = if (session.hasTrack) session.progress.coerceIn(0.02f, 1f) else 0f,
                    artwork = session.artwork,
                    showControls = true,
                    onPlayPause = { MediaNotificationListener.playPause(session.packageName) },
                    onPrev = { MediaNotificationListener.skipPrevious(session.packageName) },
                    onNext = { MediaNotificationListener.skipNext(session.packageName) },
                    onOpen = {
                        val pkg = session.packageName ?: return@CompactMediaCard
                        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                        if (launch != null) {
                            launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launch)
                        }
                    },
                    openLabel = "Open",
                )
            }
        }
    }
}

@Composable
private fun CompactMediaCard(
    brand: Color,
    appLabel: String,
    title: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    artwork: android.graphics.Bitmap?,
    showControls: Boolean,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
    openLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(brand.copy(alpha = 0.95f))
            .clickable(onClick = onOpen)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            if (artwork != null) {
                androidx.compose.foundation.Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text("♪", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                appLabel,
                color = Color.White.copy(0.75f),
                fontSize = 10.sp,
                maxLines = 1,
                fontWeight = FontWeight.Medium,
            )
            Text(
                title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (artist.isNotBlank()) {
                Text(artist, color = Color.White.copy(0.8f), fontSize = 11.sp, maxLines = 1)
            }
            if (progress > 0f) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White.copy(0.28f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(2.dp)
                            .background(Color.White),
                    )
                }
            }
        }
        if (showControls) {
            Spacer(modifier = Modifier.width(6.dp))
            Text("⏮", color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onPrev))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isPlaying) "⏸" else "▶",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onPlayPause),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("⏭", color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onNext))
        } else if (openLabel != "—") {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(openLabel, color = brand, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
