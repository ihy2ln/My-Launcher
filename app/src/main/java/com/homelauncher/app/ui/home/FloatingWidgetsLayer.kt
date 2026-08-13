package com.homelauncher.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.homelauncher.app.AppInfo
import com.homelauncher.app.findApp
import com.homelauncher.app.model.FloatingWidget
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.WidgetType
import com.homelauncher.app.model.displayAppLabel
import com.homelauncher.app.model.displayName
import com.homelauncher.app.ui.components.HomeWidgetView
import com.homelauncher.app.ui.theme.LauncherPalette
import kotlin.math.roundToInt

@Composable
fun FloatingWidgetsLayer(
    widgets: List<FloatingWidget>,
    apps: List<AppInfo>,
    appAliases: Map<String, String>,
    settings: LauncherSettings,
    palette: LauncherPalette,
    editable: Boolean,
    onClick: (FloatingWidget) -> Unit,
    onDoubleTap: (FloatingWidget) -> Unit = {},
    onMove: (FloatingWidget, xFrac: Float, yFrac: Float) -> Unit,
    onResize: (FloatingWidget, widthFrac: Float, heightFrac: Float) -> Unit,
) {
    if (widgets.isEmpty()) return
    val context = LocalContext.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(2f),
    ) {
        val density = LocalDensity.current
        val parentW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val parentH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        widgets.forEach { widget ->
            var dragX by remember(widget.id, widget.xFrac) { mutableStateOf(widget.xFrac * parentW) }
            var dragY by remember(widget.id, widget.yFrac) { mutableStateOf(widget.yFrac * parentH) }
            var widthPx by remember(widget.id, widget.widthFrac) { mutableStateOf(widget.widthFrac * parentW) }
            var heightPx by remember(widget.id, widget.heightFrac) { mutableStateOf(widget.heightFrac * parentH) }

            val wDp = with(density) { widthPx.toDp() }
            val hDp = with(density) { heightPx.toDp() }
            val boundApp = widget.appKey?.let { findApp(apps, it) }
            val displayType = widget.effectiveType()
            val displayLabel = when {
                widget.title.isNotBlank() -> widget.title
                boundApp != null -> displayAppLabel(boundApp.key, boundApp.label, appAliases)
                else -> widget.type.displayName()
            }
            val metadataLine = boundApp?.let { app ->
                val version = runCatching {
                    context.packageManager.getPackageInfo(app.packageName, 0).versionName
                }.getOrNull()
                buildString {
                    append(com.homelauncher.app.widget.categoryLabel(app.category))
                    if (!version.isNullOrBlank()) append(" · v$version")
                    when {
                        widget.hasHostedAppWidget() -> append(" · Live widget")
                        widget.embedSession -> append(" · In-widget session")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
                    .size(wDp, hDp)
                    .alpha(widget.opacity.coerceIn(0.15f, 1f))
                    .then(
                        if (editable) {
                            Modifier.border(1.dp, palette.accent.copy(0.7f), RoundedCornerShape(16.dp))
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        if (editable) {
                            Modifier.pointerInput(widget.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragEnd = {
                                        onMove(
                                            widget,
                                            (dragX / parentW).coerceIn(0f, 0.92f),
                                            (dragY / parentH).coerceIn(0f, 0.92f),
                                        )
                                    },
                                ) { change, dragAmount ->
                                    change.consume()
                                    dragX = (dragX + dragAmount.x).coerceIn(0f, parentW - widthPx)
                                    dragY = (dragY + dragAmount.y).coerceIn(0f, parentH - heightPx)
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                HomeWidgetView(
                    type = when {
                        widget.hasHostedAppWidget() -> WidgetType.BLANK
                        widget.type == WidgetType.BLANK && widget.appKey == null -> WidgetType.BLANK
                        else -> displayType
                    },
                    palette = palette,
                    size = hDp,
                    title = displayLabel,
                    app = boundApp,
                    appLabel = displayLabel,
                    appWidgetId = widget.appWidgetId,
                    appWidgetProvider = widget.appWidgetProvider,
                    metadataLine = metadataLine,
                    onClick = { if (!editable) onClick(widget) },
                    onDoubleClick = if (editable) {{ onDoubleTap(widget) }} else null,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (editable) {
                                Modifier.pointerInput(widget.id) {
                                    detectTapGestures(onTap = { /* drag layer handles move */ })
                                }
                            } else {
                                Modifier
                            },
                        ),
                )

                if (editable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp))
                            .background(palette.accent)
                            .pointerInput(widget.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragEnd = {
                                        onResize(
                                            widget,
                                            (widthPx / parentW).coerceIn(0.15f, 0.95f),
                                            (heightPx / parentH).coerceIn(0.08f, 0.6f),
                                        )
                                    },
                                ) { change, dragAmount ->
                                    change.consume()
                                    widthPx = (widthPx + dragAmount.x).coerceIn(parentW * 0.15f, parentW * 0.95f)
                                    heightPx = (heightPx + dragAmount.y).coerceIn(parentH * 0.08f, parentH * 0.6f)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⤡", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
