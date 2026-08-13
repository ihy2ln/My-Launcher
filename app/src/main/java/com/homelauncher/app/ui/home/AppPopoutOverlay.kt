package com.homelauncher.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.homelauncher.app.AppInfo
import com.homelauncher.app.media.MediaNotificationListener
import com.homelauncher.app.model.AppCategory
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.WidgetType
import com.homelauncher.app.ui.theme.LauncherPalette
import com.homelauncher.app.ui.theme.iconShape
import com.homelauncher.app.widget.describeAppForWidget

/**
 * In-widget operate surface used when an app has no native AppWidget.
 * Shows package metadata and interactive controls (media / game pad / launch).
 */
@Composable
fun AppPopoutOverlay(
    app: AppInfo,
    label: String,
    settings: LauncherSettings,
    palette: LauncherPalette,
    theme: WidgetType? = null,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val meta = remember(app.packageName) { describeAppForWidget(context, app.packageName) }
    val isGame = theme == WidgetType.GAME || app.category == AppCategory.GAME
    val isMusic = theme == WidgetType.MUSIC || theme == WidgetType.SPOTIFY ||
        theme == WidgetType.POWERAMP || app.category == AppCategory.MUSIC
    val isVideo = theme == WidgetType.VIDEO || theme == WidgetType.YOUTUBE ||
        theme == WidgetType.TWITCH || app.category == AppCategory.VIDEO

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isGame -> GamePadPopout(
                    app = app,
                    label = label,
                    settings = settings,
                    onOpen = onOpen,
                    onDismiss = onDismiss,
                    modifier = Modifier
                        .padding(20.dp)
                        .clickable(enabled = false, onClick = {}),
                )
                isMusic -> MediaOperatePopout(
                    app = app,
                    label = label,
                    settings = settings,
                    palette = palette,
                    metaLine = buildMetaLine(meta.versionName, meta.description, "Music"),
                    onOpen = onOpen,
                    onDismiss = onDismiss,
                )
                isVideo -> MediaOperatePopout(
                    app = app,
                    label = label,
                    settings = settings,
                    palette = palette,
                    metaLine = buildMetaLine(meta.versionName, meta.description, "Video"),
                    accent = Color(0xFF1A237E),
                    onOpen = onOpen,
                    onDismiss = onDismiss,
                )
                else -> GenericOperatePopout(
                    app = app,
                    label = label,
                    settings = settings,
                    palette = palette,
                    versionName = meta.versionName,
                    description = meta.description,
                    hasNativeHint = meta.hasNativeWidget,
                    onOpen = onOpen,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

private fun buildMetaLine(version: String, description: String, kind: String): String = buildString {
    append(kind)
    if (version.isNotBlank()) append(" · v$version")
    if (description.isNotBlank()) append(" · ${description.take(48)}")
}

@Composable
private fun GenericOperatePopout(
    app: AppInfo,
    label: String,
    settings: LauncherSettings,
    palette: LauncherPalette,
    versionName: String,
    description: String,
    hasNativeHint: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(32.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(palette.surface.copy(alpha = 0.97f))
            .clickable(enabled = false, onClick = {})
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(88.dp)
                .clip(iconShape(settings.iconShape)),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(label, color = palette.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(
            app.packageName,
            color = palette.textSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (versionName.isNotBlank()) {
            Text("Version $versionName", color = palette.textSecondary, fontSize = 12.sp)
        }
        if (description.isNotBlank()) {
            Text(
                description,
                color = palette.textSecondary,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            if (hasNativeHint) {
                "This app provides a native widget — re-bind from Edit Home to host it."
            } else {
                "No native widget · operate here or open the full app"
            },
            color = palette.accent,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
        )
        TextButton(onClick = onOpen) {
            Text("Open in widget frame", color = palette.accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        TextButton(onClick = onDismiss) {
            Text("Close", color = palette.textSecondary)
        }
    }
}

@Composable
private fun MediaOperatePopout(
    app: AppInfo,
    label: String,
    settings: LauncherSettings,
    palette: LauncherPalette,
    metaLine: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    accent: Color = Color(0xFFE91E63),
) {
    val nowPlaying by MediaNotificationListener.state.collectAsState()
    val forApp = nowPlaying.packageName?.let { pkg ->
        pkg.equals(app.packageName, true) || pkg.startsWith("${app.packageName}.")
    } == true
    val track = if (forApp && nowPlaying.hasTrack) nowPlaying else nowPlaying.takeIf { it.hasTrack }

    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(accent.copy(alpha = 0.95f))
            .clickable(enabled = false, onClick = {})
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Image(
                bitmap = app.icon,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(iconShape(settings.iconShape)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(metaLine, color = Color.White.copy(0.75f), fontSize = 11.sp, maxLines = 2)
            }
            TextButton(onClick = onDismiss) { Text("✕", color = Color.White) }
        }

        Spacer(modifier = Modifier.height(18.dp))
        if (track?.artwork != null) {
            Image(
                bitmap = track.artwork.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            track?.title?.takeIf { it.isNotBlank() } ?: "Not playing",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            track?.artist?.takeIf { it.isNotBlank() } ?: "Start playback in $label",
            color = Color.White.copy(0.8f),
            fontSize = 13.sp,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⏮", color = Color.White, fontSize = 22.sp, modifier = Modifier.clickable {
                MediaNotificationListener.skipPrevious()
            })
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { MediaNotificationListener.playPause() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (track?.isPlaying == true) "⏸" else "▶",
                    color = accent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("⏭", color = Color.White, fontSize = 22.sp, modifier = Modifier.clickable {
                MediaNotificationListener.skipNext()
            })
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onOpen) {
            Text("Open full app", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun GamePadPopout(
    app: AppInfo,
    label: String,
    settings: LauncherSettings,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastInput by remember { mutableStateOf("Ready") }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF121212))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                bitmap = app.icon,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(iconShape(settings.iconShape)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Game pad · operate in widget", color = Color.White.copy(0.7f), fontSize = 12.sp)
            }
            TextButton(onClick = onDismiss) { Text("✕", color = Color.White) }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF1B5E20)),
            contentAlignment = Alignment.Center,
        ) {
            Text(lastInput, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PadButton("▲") { lastInput = "Up" }
                Row {
                    PadButton("◀") { lastInput = "Left" }
                    Spacer(modifier = Modifier.width(8.dp))
                    PadButton("▶") { lastInput = "Right" }
                }
                PadButton("▼") { lastInput = "Down" }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("A", Color(0xFF69F0AE)) { lastInput = "A" }
                    ActionButton("B", Color(0xFFFF5252)) { lastInput = "B" }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("X", Color(0xFF40C4FF)) { lastInput = "X" }
                    ActionButton("Y", Color(0xFFFFD740)) { lastInput = "Y" }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onOpen) {
            Text("Launch game", color = Color(0xFF69F0AE), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PadButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
private fun ActionButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.85f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
