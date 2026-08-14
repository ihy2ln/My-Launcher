package com.homelauncher.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.homelauncher.app.AppInfo
import com.homelauncher.app.media.MediaNotificationListener
import com.homelauncher.app.media.NowPlayingState
import com.homelauncher.app.model.AppCategory
import com.homelauncher.app.model.WidgetType
import com.homelauncher.app.model.brandColor
import com.homelauncher.app.model.displayName
import com.homelauncher.app.model.toComposeColor
import com.homelauncher.app.ui.theme.LauncherPalette
import com.homelauncher.app.widget.describeAppForWidget
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeWidgetView(
    type: WidgetType,
    palette: LauncherPalette,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    app: AppInfo? = null,
    appLabel: String? = null,
    enableGestures: Boolean = true,
) {
    val click = if (enableGestures) onClick else ({})
    val longClick = if (enableGestures) onLongClick else null
    val doubleClick = if (enableGestures) onDoubleClick else null
    when (type) {
        WidgetType.BLANK -> BlankAppWidgetCard(
            palette = palette,
            app = app,
            label = appLabel ?: title ?: "App widget",
            onClick = click,
            onLongClick = longClick,
            onDoubleClick = doubleClick,
            enableGestures = enableGestures,
            modifier = modifier,
        )
        WidgetType.CLOCK -> ClockWidgetCard(palette, click, modifier, title, longClick, doubleClick)
        WidgetType.WEATHER -> WeatherWidgetCard(palette, click, modifier, title, longClick, doubleClick)
        WidgetType.APP_DRAWER -> AppDrawerWidgetCard(palette, click, modifier, title, longClick, doubleClick)
        WidgetType.YOUTUBE -> LiveMediaWidgetCard(
            brand = Color(0xFFFF0000),
            glyph = "▶",
            fallbackHeadline = title ?: "YouTube",
            fallbackSubtitle = "Watch · Subscribe",
            packageFilter = listOf("com.google.android.youtube", "com.vanced.android.youtube"),
            onClick = click,
            onLongClick = longClick,
            modifier = modifier,
        )
        WidgetType.POWERAMP -> LiveMediaWidgetCard(
            brand = Color(0xFFF5A623),
            glyph = "♫",
            fallbackHeadline = title ?: "Poweramp",
            fallbackSubtitle = "Local library",
            packageFilter = listOf("com.maxmpz.audioplayer"),
            onClick = click,
            onLongClick = longClick,
            modifier = modifier,
            showProgress = true,
        )
        WidgetType.TWITCH -> MediaWidgetCard(
            brand = Color(0xFF9146FF),
            glyph = "◉",
            headline = title ?: "Twitch",
            subtitle = "Live channels",
            detail = "Browse streams",
            onClick = click,
            onLongClick = longClick,
            modifier = modifier,
        )
        WidgetType.SPOTIFY -> LiveMediaWidgetCard(
            brand = Color(0xFF1DB954),
            glyph = "♪",
            fallbackHeadline = title ?: "Spotify",
            fallbackSubtitle = "Tap to open",
            packageFilter = listOf("com.spotify.music"),
            onClick = click,
            onLongClick = longClick,
            modifier = modifier,
            showProgress = true,
        )
        WidgetType.MUSIC -> MusicPlayerWidgetCard(
            palette = palette,
            app = app,
            title = title ?: appLabel ?: "Music",
            onClick = click,
            onLongClick = longClick,
            onDoubleClick = doubleClick,
            modifier = modifier,
        )
        WidgetType.VIDEO -> VideoPlayerWidgetCard(
            palette = palette,
            app = app,
            title = title ?: appLabel ?: "Video",
            onClick = click,
            onLongClick = longClick,
            onDoubleClick = doubleClick,
            modifier = modifier,
        )
        WidgetType.GAME -> GameWidgetCard(
            palette = palette,
            app = app,
            title = title ?: appLabel ?: "Game",
            onClick = click,
            onLongClick = longClick,
            onDoubleClick = doubleClick,
            modifier = modifier,
        )
        WidgetType.SEARCH -> SearchWidgetCard(palette, click, modifier, title, longClick, doubleClick)
        WidgetType.CALENDAR -> CalendarWidgetCard(palette, click, modifier, title, longClick, doubleClick)
        WidgetType.NOTES -> NotesWidgetCard(palette, click, modifier, title, longClick, doubleClick)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BlankAppWidgetCard(
    palette: LauncherPalette,
    app: AppInfo?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    enableGestures: Boolean = true,
) {
    val context = LocalContext.current
    val meta = remember(app?.packageName) {
        app?.let { describeAppForWidget(context, it.packageName) }
    }
    val categoryLabel = when (app?.category) {
        AppCategory.MUSIC -> "Music"
        AppCategory.VIDEO -> "Video"
        AppCategory.GAME -> "Game"
        AppCategory.SOCIAL -> "Social"
        AppCategory.PRODUCTIVITY -> "Productivity"
        AppCategory.NEWS -> "News"
        AppCategory.MAPS -> "Maps"
        AppCategory.IMAGE -> "Photos"
        else -> "App"
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface.copy(alpha = 0.72f))
            .then(
                when {
                    !enableGestures -> Modifier
                    onDoubleClick != null -> Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onDoubleTap = { onDoubleClick() },
                            onLongPress = { onLongClick?.invoke() },
                        )
                    }
                    else -> Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                },
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (app != null) {
            Image(
                bitmap = app.icon,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                label,
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(categoryLabel)
                    if (!meta?.versionName.isNullOrBlank()) append(" · v${meta!!.versionName}")
                },
                color = palette.textSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when {
                    meta?.hasNativeWidget == true -> "Native widget available"
                    else -> "Tap to operate in widget"
                },
                color = palette.accent.copy(alpha = 0.9f),
                fontSize = 10.sp,
                maxLines = 1,
            )
        } else {
            Text("+", color = palette.textSecondary, fontSize = 28.sp)
            Text("Choose app", color = palette.textSecondary, fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.widgetClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
): Modifier = if (onDoubleClick != null) {
    pointerInput(onClick, onLongClick, onDoubleClick) {
        detectTapGestures(
            onTap = { onClick() },
            onDoubleTap = { onDoubleClick() },
            onLongPress = { onLongClick?.invoke() },
        )
    }
} else {
    combinedClickable(onClick = onClick, onLongClick = onLongClick)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClockWidgetCard(
    palette: LauncherPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            kotlinx.coroutines.delay(15_000)
        }
    }
    val time = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val date = remember { SimpleDateFormat("EEE d", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface.copy(alpha = 0.55f))
            .widgetClickable(onClick, onLongClick, onDoubleClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (!title.isNullOrBlank()) {
            Text(title, color = palette.textSecondary, fontSize = 10.sp)
        }
        Text(
            text = time.format(now),
            color = palette.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
        )
        Text(
            text = date.format(now),
            color = palette.textSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeatherWidgetCard(
    palette: LauncherPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val weather by produceState<com.homelauncher.app.data.WeatherSnapshot?>(initialValue = null, context) {
        value = runCatching {
            com.homelauncher.app.data.WeatherRepository.current(context)
        }.getOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF4A90A4).copy(alpha = 0.55f))
            .widgetClickable(onClick, onLongClick, onDoubleClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (weather == null) {
            Text("…", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium)
            Text("Loading", color = Color.White.copy(0.85f), fontSize = 12.sp)
        } else {
            Text("${weather!!.tempF}°", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium)
            Text(weather!!.condition, color = Color.White.copy(0.85f), fontSize = 12.sp)
            Text(
                if (weather!!.isLive) weather!!.locationLabel else "Offline",
                color = Color.White.copy(0.65f),
                fontSize = 10.sp,
            )
        }
        if (!title.isNullOrBlank()) {
            Text(title, color = Color.White.copy(0.55f), fontSize = 9.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppDrawerWidgetCard(
    palette: LauncherPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.accent.copy(alpha = 0.35f))
            .widgetClickable(onClick, onLongClick, onDoubleClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(palette.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text("∷", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            title ?: "Apps",
            color = palette.textPrimary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LiveMediaWidgetCard(
    brand: Color,
    glyph: String,
    fallbackHeadline: String,
    fallbackSubtitle: String,
    packageFilter: List<String>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
) {
    val nowPlaying by MediaNotificationListener.state.collectAsState()
    val matched = remember(nowPlaying, packageFilter) {
        val pkg = nowPlaying.packageName.orEmpty()
        nowPlaying.takeIf { state ->
            state.hasTrack && packageFilter.any { pkg.equals(it, true) || pkg.startsWith("$it.") }
        } ?: nowPlaying.takeIf { it.hasTrack && packageFilter.isEmpty() }
    }
    val headline = matched?.title?.takeIf { it.isNotBlank() } ?: fallbackHeadline
    val subtitle = when {
        matched?.artist?.isNotBlank() == true -> matched.artist
        matched?.isPlaying == true -> "Now playing"
        else -> fallbackSubtitle
    }
    val detail = matched?.appLabel
    val progress = matched?.progress?.coerceIn(0.02f, 1f) ?: 0.0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(brand.copy(alpha = 0.92f))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                if (matched?.artwork != null) {
                    Image(
                        bitmap = matched.artwork.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(glyph, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    headline,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(subtitle, color = Color.White.copy(0.85f), fontSize = 11.sp, maxLines = 1)
                if (detail != null) {
                    Text(detail, color = Color.White.copy(0.65f), fontSize = 10.sp, maxLines = 1)
                }
            }
            if (matched != null) {
                Text(
                    if (matched.isPlaying) "⏸" else "▶",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { MediaNotificationListener.playPause(matched.packageName) },
                )
            }
        }
        if (showProgress) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(0.3f)),
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(3.dp)
                            .background(Color.White),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaWidgetCard(
    brand: Color,
    glyph: String,
    headline: String,
    subtitle: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    detail: String? = null,
    showProgress: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(brand.copy(alpha = 0.92f))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    headline,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(subtitle, color = Color.White.copy(0.85f), fontSize = 11.sp, maxLines = 1)
                if (detail != null) {
                    Text(detail, color = Color.White.copy(0.65f), fontSize = 10.sp, maxLines = 1)
                }
            }
        }
        if (showProgress) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(0.3f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(3.dp)
                        .background(Color.White),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchWidgetCard(
    palette: LauncherPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .widgetClickable(onClick, onLongClick, onDoubleClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⌕", color = Color(0xFF5F6368), fontSize = 16.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            title ?: "Search",
            color = Color(0xFF9AA0A6),
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        Text("G", color = Color(0xFF4285F4), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarWidgetCard(
    palette: LauncherPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
) {
    val cal = remember { Calendar.getInstance() }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = remember { SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time) }
    val weekday = remember { SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .widgetClickable(onClick, onLongClick, onDoubleClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEA4335))
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(month.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text("$day", color = Color(0xFF202124), fontSize = 28.sp, fontWeight = FontWeight.Light)
        Text(weekday, color = Color(0xFF5F6368), fontSize = 11.sp)
        if (!title.isNullOrBlank()) {
            Text(title, color = Color(0xFF9AA0A6), fontSize = 9.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotesWidgetCard(
    palette: LauncherPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF59D).copy(alpha = 0.95f))
            .widgetClickable(onClick, onLongClick, onDoubleClick)
            .padding(12.dp),
    ) {
        Text(title ?: "Notes", color = Color(0xFF5D4037), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Quick note…", color = Color(0xFF6D4C41).copy(0.7f), fontSize = 12.sp)
        Text("Tap to open", color = Color(0xFF6D4C41).copy(0.45f), fontSize = 10.sp)
    }
}

/** Catalog used by edit-home widget pickers. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicPlayerWidgetCard(
    palette: LauncherPalette,
    app: AppInfo?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
) {
    val nowPlaying by MediaNotificationListener.state.collectAsState()
    val matched = remember(nowPlaying, app?.packageName) {
        val pkg = nowPlaying.packageName
        when {
            app != null && pkg != null &&
                (pkg.equals(app.packageName, true) || pkg.startsWith("${app.packageName}.")) -> nowPlaying
            nowPlaying.hasTrack && (app == null || app.category == AppCategory.MUSIC) -> nowPlaying
            else -> NowPlayingState.Empty
        }
    }
    val headline = matched.title.takeIf { it.isNotBlank() } ?: title
    val subtitle = when {
        matched.artist.isNotBlank() -> matched.artist
        matched.isPlaying -> "Music · Now playing"
        else -> "Music · Tap to operate"
    }
    val progress = if (matched.hasTrack) matched.progress.coerceIn(0.02f, 1f) else 0.45f

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFE91E63).copy(alpha = 0.9f))
            .widgetClickable(onClick, onLongClick, onDoubleClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (matched.artwork != null) {
                Image(
                    bitmap = matched.artwork.asImageBitmap(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else if (app != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.2f)),
                    contentAlignment = Alignment.Center,
                ) { Text("♪", color = Color.White, fontSize = 18.sp) }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(headline, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = Color.White.copy(0.85f), fontSize = 11.sp, maxLines = 1)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "⏮",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.clickable { MediaNotificationListener.skipPrevious(matched.packageName ?: app?.packageName) },
            )
            Text(
                if (matched.isPlaying) "⏸" else "▶",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { MediaNotificationListener.playPause(matched.packageName ?: app?.packageName) },
            )
            Text(
                "⏭",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.clickable { MediaNotificationListener.skipNext(matched.packageName ?: app?.packageName) },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(0.3f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(Color.White),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoPlayerWidgetCard(
    palette: LauncherPalette,
    app: AppInfo?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A237E))
            .widgetClickable(onClick, onLongClick, onDoubleClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.35f)),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.9f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("▶", color = Color(0xFF1A237E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Video player", color = Color.White.copy(0.75f), fontSize = 10.sp)
        }
        if (app != null) {
            androidx.compose.foundation.Image(
                bitmap = app.icon,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.White.copy(0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .height(4.dp)
                    .background(Color(0xFFFF5252)),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameWidgetCard(
    palette: LauncherPalette,
    app: AppInfo?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF00C853).copy(alpha = 0.88f))
            .widgetClickable(onClick, onLongClick, onDoubleClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (app != null) {
            androidx.compose.foundation.Image(
                bitmap = app.icon,
                contentDescription = title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        } else {
            Text("🎮", color = Color.White, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Tap for controls", color = Color.White.copy(0.85f), fontSize = 10.sp)
    }
}

/** Catalog used by edit-home widget pickers. */
fun widgetCatalog(): List<Pair<WidgetType, String>> = listOf(
    WidgetType.BLANK to "App widget",
    WidgetType.CLOCK to "Clock",
    WidgetType.WEATHER to "Weather",
    WidgetType.APP_DRAWER to "App drawer",
    WidgetType.MUSIC to "Music player",
    WidgetType.VIDEO to "Video player",
    WidgetType.GAME to "Game pad",
    WidgetType.YOUTUBE to "YouTube",
    WidgetType.POWERAMP to "Poweramp",
    WidgetType.TWITCH to "Twitch",
    WidgetType.SPOTIFY to "Spotify",
    WidgetType.SEARCH to "Search bar",
    WidgetType.CALENDAR to "Calendar",
    WidgetType.NOTES to "Notes",
)
