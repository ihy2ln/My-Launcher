package com.homelauncher.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import com.homelauncher.app.widget.NativeAppWidgetView
import kotlin.math.roundToInt

@Composable
fun FloatingWidgetsLayer(
    widgets: List<FloatingWidget>,
    apps: List<AppInfo>,
    appAliases: Map<String, String>,
    settings: LauncherSettings,
    palette: LauncherPalette,
    editable: Boolean,
    /** When true, long-press moves widgets even outside full edit chrome. */
    allowMove: Boolean = editable,
    onClick: (FloatingWidget) -> Unit,
    onDoubleTap: (FloatingWidget) -> Unit = {},
    onLongPress: (FloatingWidget) -> Unit = {},
    onMove: (FloatingWidget, xFrac: Float, yFrac: Float) -> Unit,
    onResize: (FloatingWidget, widthFrac: Float, heightFrac: Float) -> Unit,
) {
    if (widgets.isEmpty()) return
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
            var moving by remember(widget.id) { mutableStateOf(false) }

            val wDp = with(density) { widthPx.toDp() }
            val hDp = with(density) { heightPx.toDp() }
            val boundApp = widget.appKey?.let { findApp(apps, it) }
            val displayType = widget.effectiveType()
            val displayLabel = when {
                widget.title.isNotBlank() -> widget.title
                boundApp != null -> displayAppLabel(boundApp.key, boundApp.label, appAliases)
                else -> widget.type.displayName()
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
                    .size(wDp, hDp)
                    .alpha(widget.opacity.coerceIn(0.15f, 1f))
                    .then(
                        if (editable || moving) {
                            Modifier.border(1.dp, palette.accent.copy(0.7f), RoundedCornerShape(16.dp))
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        if (allowMove || editable) {
                            Modifier
                                .pointerInput(widget.id, editable, allowMove) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            moving = true
                                            onLongPress(widget)
                                        },
                                        onDragEnd = {
                                            moving = false
                                            onMove(
                                                widget,
                                                (dragX / parentW).coerceIn(0f, 0.92f),
                                                (dragY / parentH).coerceIn(0f, 0.92f),
                                            )
                                        },
                                        onDragCancel = { moving = false },
                                    ) { change, dragAmount ->
                                        change.consume()
                                        dragX = (dragX + dragAmount.x).coerceIn(0f, parentW - widthPx)
                                        dragY = (dragY + dragAmount.y).coerceIn(0f, parentH - heightPx)
                                    }
                                }
                                .pointerInput(widget.id, editable) {
                                    detectTapGestures(
                                        onTap = {
                                            if (!moving && !editable) onClick(widget)
                                        },
                                        onDoubleTap = {
                                            if (editable) onDoubleTap(widget)
                                        },
                                    )
                                }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                if (widget.hostsNativeWidget && !editable) {
                    NativeAppWidgetView(
                        appWidgetId = widget.appWidgetId,
                        providerFlat = widget.providerFlat,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                    )
                } else {
                    HomeWidgetView(
                        type = if (widget.type == WidgetType.BLANK && widget.appKey == null && !widget.hostsNativeWidget) {
                            WidgetType.BLANK
                        } else if (widget.hostsNativeWidget) {
                            WidgetType.BLANK
                        } else {
                            displayType
                        },
                        palette = palette,
                        size = hDp,
                        title = displayLabel,
                        app = boundApp,
                        appLabel = displayLabel,
                        onClick = { if (!editable && !moving) onClick(widget) },
                        onLongClick = null,
                        onDoubleClick = null,
                        // Outer box owns gestures whenever move/edit is enabled.
                        enableGestures = !allowMove && !editable,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (editable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp))
                            .background(palette.accent)
                            .pointerInput(widget.id) {
                                detectDragGestures(
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
