package com.homelauncher.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homelauncher.app.AppInfo
import com.homelauncher.app.model.WidgetType
import com.homelauncher.app.model.brandColor
import com.homelauncher.app.model.displayName
import com.homelauncher.app.model.toComposeColor
import com.homelauncher.app.ui.theme.LauncherPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

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
) {
    when (type) {
        WidgetType.BLANK -> BlankAppWidgetCard(
            palette = palette,
            app = app,
            label = appLabel ?: title ?: "App widget",
            onClick = onClick,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            modifier = modifier,
        )
        WidgetType.CLOCK -> ClockWidgetCard(palette, onClick, modifier, title, onLongClick, onDoubleClick)
        WidgetType.WEATHER -> WeatherWidgetCard(palette, onClick, modifier, title, onLongClick, onDoubleClick)
        WidgetType.APP_DRAWER -> AppDrawerWidgetCard(palette, onClick, modifier, title, onLongClick, onDoubleClick)
        WidgetType.YOUTUBE -> MediaWidgetCard(
            brand = Color(0xFFFF0000),
            glyph = "▶",
            headline = title ?: "YouTube",
            subtitle = "Watch · Subscribe",
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
        )
        WidgetType.POWERAMP -> MediaWidgetCard(
            brand = Color(0xFFF5A623),
            glyph = "♫",
            headline = title ?: "Poweramp",
            subtitle = "Now playing",
            detail = "Local library",
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
            showProgress = true,
        )
        WidgetType.TWITCH -> MediaWidgetCard(
            brand = Color(0xFF9146FF),
            glyph = "◉",
            headline = title ?: "Twitch",
            subtitle = "Live channels",
            detail = "Browse streams",
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
        )
        WidgetType.SPOTIFY -> MediaWidgetCard(
            brand = Color(0xFF1DB954),
            glyph = "♪",
            headline = title ?: "Spotify",
            subtitle = "Something Comforting",
            detail = "Porter Robinson",
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
            showProgress = true,
        )
        WidgetType.SEARCH -> SearchWidgetCard(palette, onClick, modifier, title, onLongClick, onDoubleClick)
        WidgetType.CALENDAR -> CalendarWidgetCard(palette, onClick, modifier, title, onLongClick, onDoubleClick)
        WidgetType.NOTES -> NotesWidgetCard(palette, onClick, modifier, title, onLongClick, onDoubleClick)
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
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface.copy(alpha = 0.6f))
            .then(
                if (onDoubleClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onDoubleTap = { onDoubleClick() },
                            onLongPress = { onLongClick?.invoke() },
                        )
                    }
                } else {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                },
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (app != null) {
            androidx.compose.foundation.Image(
                bitmap = app.icon,
                contentDescription = label,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
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
            Text("Tap to open", color = palette.textSecondary, fontSize = 10.sp)
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
    val temp = remember { 64 + Random.nextInt(0, 12) }
    val condition = remember {
        listOf("Clear", "Cloudy", "Breezy", "Sunny").random()
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
        Text("$temp°", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium)
        Text(condition, color = Color.White.copy(0.85f), fontSize = 12.sp)
        Text(title ?: "Weather", color = Color.White.copy(0.65f), fontSize = 10.sp)
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
fun widgetCatalog(): List<Pair<WidgetType, String>> = listOf(
    WidgetType.BLANK to "App widget",
    WidgetType.CLOCK to "Clock",
    WidgetType.WEATHER to "Weather",
    WidgetType.APP_DRAWER to "App drawer",
    WidgetType.YOUTUBE to "YouTube",
    WidgetType.POWERAMP to "Poweramp",
    WidgetType.TWITCH to "Twitch",
    WidgetType.SPOTIFY to "Spotify",
    WidgetType.SEARCH to "Search bar",
    WidgetType.CALENDAR to "Calendar",
    WidgetType.NOTES to "Notes",
)
