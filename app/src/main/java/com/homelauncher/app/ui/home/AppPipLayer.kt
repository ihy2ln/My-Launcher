package com.homelauncher.app.ui.home

import android.graphics.Rect
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.homelauncher.app.AppInfo
import com.homelauncher.app.media.MediaNotificationListener
import com.homelauncher.app.media.brandColorForPackage
import com.homelauncher.app.model.AppCategory
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.WidgetType
import com.homelauncher.app.ui.theme.LauncherPalette
import com.homelauncher.app.ui.theme.iconShape
import com.homelauncher.app.widget.NativeAppWidgetView
import com.homelauncher.app.widget.densityScaledRect
import com.homelauncher.app.widget.launchAppInBounds
import com.homelauncher.app.widget.bindNativeWidgetForPackage
import com.homelauncher.app.widget.NativeBindOutcome
import com.homelauncher.app.widget.LauncherAppWidgetHost
import kotlin.math.roundToInt

/**
 * An in-home picture-in-picture style window that keeps the user on the
 * launcher while operating an app (native widget host, media controls,
 * and/or freeform launch into the frame bounds).
 */
data class HomePipSession(
    val id: String,
    val app: AppInfo,
    val label: String,
    val theme: WidgetType? = null,
    val widgetId: String? = null,
    val appWidgetId: Int = -1,
    val providerFlat: String? = null,
    val xFrac: Float = 0.08f,
    val yFrac: Float = 0.22f,
    val widthFrac: Float = 0.72f,
    val heightFrac: Float = 0.42f,
)

@Composable
fun HomePipLayer(
    sessions: List<HomePipSession>,
    settings: LauncherSettings,
    palette: LauncherPalette,
    onClose: (String) -> Unit,
    onExpandFullscreen: (AppInfo) -> Unit,
    onUpdateBounds: (HomePipSession) -> Unit,
    onNativeBound: (String, Int, String) -> Unit = { _, _, _ -> },
) {
    if (sessions.isEmpty()) return
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(30f),
    ) {
        val parentW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val parentH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val density = LocalDensity.current
        val context = LocalContext.current

        sessions.forEach { session ->
            var x by remember(session.id, session.xFrac) { mutableStateOf(session.xFrac * parentW) }
            var y by remember(session.id, session.yFrac) { mutableStateOf(session.yFrac * parentH) }
            var w by remember(session.id, session.widthFrac) { mutableStateOf(session.widthFrac * parentW) }
            var h by remember(session.id, session.heightFrac) { mutableStateOf(session.heightFrac * parentH) }
            var localAppWidgetId by remember(session.id) { mutableStateOf(session.appWidgetId) }
            var localProvider by remember(session.id) { mutableStateOf(session.providerFlat) }

            LaunchedEffect(session.id) {
                // Prefer hosting a native AppWidget inside the PiP frame.
                if (localAppWidgetId == -1) {
                    when (val outcome = bindNativeWidgetForPackage(context, session.app.packageName)) {
                        is NativeBindOutcome.Success -> {
                            localAppWidgetId = outcome.appWidgetId
                            localProvider = outcome.provider.flattenToString()
                            onNativeBound(session.id, outcome.appWidgetId, outcome.provider.flattenToString())
                            outcome.configureIntent?.let { runCatching { context.startActivity(it) } }
                        }
                        is NativeBindOutcome.NeedsUserConsent -> {
                            // Keep interactive fallback; user can bind later from edit home.
                            LauncherAppWidgetHost.deleteId(context, outcome.appWidgetId)
                        }
                        NativeBindOutcome.NoProvider -> Unit
                    }
                }
                // Also try freeform launch into the frame so the real app sits over home.
                val bounds = densityScaledRect(
                    x / parentW, y / parentH, w / parentW, h / parentH,
                    parentW.toInt(), parentH.toInt(),
                )
                // Offset below status / chrome
                val chromePx = with(density) { 36.dp.roundToPx() }
                val contentBounds = Rect(
                    bounds.left,
                    bounds.top + chromePx,
                    bounds.right,
                    bounds.bottom,
                )
                launchAppInBounds(
                    context,
                    session.app.packageName,
                    session.app.activityName,
                    contentBounds,
                )
            }

            val brand = Color(brandColorForPackage(session.app.packageName))
            val wDp = with(density) { w.toDp() }
            val hDp = with(density) { h.toDp() }

            Column(
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .size(wDp, hDp)
                    .shadow(12.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF121212))
                    .border(1.dp, brand.copy(0.7f), RoundedCornerShape(18.dp)),
            ) {
                // Title / drag chrome
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brand.copy(alpha = 0.92f))
                        .pointerInput(session.id) {
                            detectDragGestures(
                                onDragEnd = {
                                    onUpdateBounds(
                                        session.copy(
                                            xFrac = (x / parentW).coerceIn(0f, 0.85f),
                                            yFrac = (y / parentH).coerceIn(0f, 0.85f),
                                            widthFrac = (w / parentW).coerceIn(0.35f, 0.95f),
                                            heightFrac = (h / parentH).coerceIn(0.22f, 0.75f),
                                        ),
                                    )
                                },
                            ) { change, amount ->
                                change.consume()
                                x = (x + amount.x).coerceIn(0f, parentW - w)
                                y = (y + amount.y).coerceIn(0f, parentH - h)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        bitmap = session.app.icon,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(iconShape(settings.iconShape)),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        session.label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "⛶",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { onExpandFullscreen(session.app) }
                            .padding(horizontal = 8.dp),
                    )
                    Text(
                        "✕",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { onClose(session.id) }
                            .padding(horizontal = 4.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A)),
                ) {
                    when {
                        localAppWidgetId != -1 && !localProvider.isNullOrBlank() -> {
                            NativeAppWidgetView(
                                appWidgetId = localAppWidgetId,
                                providerFlat = localProvider,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                            )
                        }
                        session.theme == WidgetType.MUSIC ||
                            session.theme == WidgetType.SPOTIFY ||
                            session.theme == WidgetType.POWERAMP ||
                            session.app.category == AppCategory.MUSIC ||
                            session.app.category == AppCategory.VIDEO ||
                            session.theme == WidgetType.VIDEO ||
                            session.theme == WidgetType.YOUTUBE -> {
                            PipMediaBody(app = session.app, brand = brand)
                        }
                        session.theme == WidgetType.GAME || session.app.category == AppCategory.GAME -> {
                            PipGameBody(app = session.app, onFullscreen = { onExpandFullscreen(session.app) })
                        }
                        else -> {
                            PipGenericBody(
                                app = session.app,
                                label = session.label,
                                onOpen = { onExpandFullscreen(session.app) },
                                onRelaunchBounds = {
                                    val bounds = densityScaledRect(
                                        x / parentW, y / parentH, w / parentW, h / parentH,
                                        parentW.toInt(), parentH.toInt(),
                                    )
                                    launchAppInBounds(
                                        context,
                                        session.app.packageName,
                                        session.app.activityName,
                                        bounds,
                                    )
                                },
                            )
                        }
                    }

                    // Resize handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .pointerInput(session.id) {
                                detectDragGestures(
                                    onDragEnd = {
                                        onUpdateBounds(
                                            session.copy(
                                                xFrac = (x / parentW).coerceIn(0f, 0.85f),
                                                yFrac = (y / parentH).coerceIn(0f, 0.85f),
                                                widthFrac = (w / parentW).coerceIn(0.35f, 0.95f),
                                                heightFrac = (h / parentH).coerceIn(0.22f, 0.75f),
                                            ),
                                        )
                                    },
                                ) { change, amount ->
                                    change.consume()
                                    w = (w + amount.x).coerceIn(parentW * 0.35f, parentW * 0.95f)
                                    h = (h + amount.y).coerceIn(parentH * 0.22f, parentH * 0.75f)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⤡", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PipMediaBody(app: AppInfo, brand: Color) {
    val sessions by MediaNotificationListener.sessions.collectAsState()
    val track = sessions.firstOrNull {
        it.packageName.equals(app.packageName, true) ||
            it.packageName?.startsWith("${app.packageName}.") == true
    } ?: sessions.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brand.copy(alpha = 0.35f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (track?.artwork != null) {
            Image(
                bitmap = track.artwork.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Image(
                bitmap = app.icon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Text(
            track?.title ?: "Not playing",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            track?.artist ?: app.label,
            color = Color.White.copy(0.75f),
            fontSize = 12.sp,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⏮", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable {
                MediaNotificationListener.skipPrevious(app.packageName)
            })
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { MediaNotificationListener.playPause(app.packageName) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (track?.isPlaying == true) "⏸" else "▶",
                    color = brand,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("⏭", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable {
                MediaNotificationListener.skipNext(app.packageName)
            })
        }
    }
}

@Composable
private fun PipGameBody(app: AppInfo, onFullscreen: () -> Unit) {
    var last by remember { mutableStateOf("Ready · open in frame") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1B5E20)),
            contentAlignment = Alignment.Center,
        ) {
            Text(last, color = Color.White, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("▲", "◀", "▶", "▼", "A", "B").forEach { key ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.12f))
                        .clickable { last = key },
                    contentAlignment = Alignment.Center,
                ) { Text(key, color = Color.White, fontSize = 12.sp) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Open full",
            color = Color(0xFF69F0AE),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onFullscreen),
        )
    }
}

@Composable
private fun PipGenericBody(
    app: AppInfo,
    label: String,
    onOpen: () -> Unit,
    onRelaunchBounds: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp)),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "PiP frame · stay on home",
            color = Color.White.copy(0.65f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .clickable(onClick = onRelaunchBounds)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("Open in frame", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.15f))
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("Fullscreen", color = Color.White, fontSize = 12.sp)
            }
        }
        Text(
            "Native app widgets appear here when available. Otherwise the app opens in this frame.",
            color = Color.White.copy(0.45f),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
