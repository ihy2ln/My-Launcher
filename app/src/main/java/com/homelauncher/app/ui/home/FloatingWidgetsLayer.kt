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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
        // Always derive from *current* constraints. Caching first-measure pixels
        // was shrinking widgets when the home layout settled larger.
        val parentW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val parentH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        widgets.forEach { widget ->
            var interacting by remember(widget.id) { mutableStateOf(false) }
            var dragX by remember(widget.id) { mutableFloatStateOf(widget.xFrac * parentW) }
            var dragY by remember(widget.id) { mutableFloatStateOf(widget.yFrac * parentH) }
            var widthPx by remember(widget.id) { mutableFloatStateOf(widget.widthFrac * parentW) }
            var heightPx by remember(widget.id) { mutableFloatStateOf(widget.heightFrac * parentH) }

            // Re-sync whenever the model or parent size changes (unless mid-gesture).
            LaunchedEffect(
                widget.id,
                widget.xFrac,
                widget.yFrac,
                widget.widthFrac,
                widget.heightFrac,
                parentW,
                parentH,
                interacting,
            ) {
                if (!interacting) {
                    val w = widget.widthFrac.coerceIn(MIN_WIDTH_FRAC, MAX_WIDTH_FRAC) * parentW
                    val h = widget.heightFrac.coerceIn(MIN_HEIGHT_FRAC, MAX_HEIGHT_FRAC) * parentH
                    widthPx = w
                    heightPx = h
                    dragX = (widget.xFrac * parentW).coerceIn(0f, (parentW - w).coerceAtLeast(0f))
                    dragY = (widget.yFrac * parentH).coerceIn(0f, (parentH - h).coerceAtLeast(0f))
                }
            }

            val latestParentW by rememberUpdatedState(parentW)
            val latestParentH by rememberUpdatedState(parentH)
            val latestWidget by rememberUpdatedState(widget)

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
                        if (editable || interacting) {
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
                                            interacting = true
                                            onLongPress(latestWidget)
                                        },
                                        onDragEnd = {
                                            val pw = latestParentW
                                            val ph = latestParentH
                                            onMove(
                                                latestWidget,
                                                (dragX / pw).coerceIn(0f, 0.92f),
                                                (dragY / ph).coerceIn(0f, 0.92f),
                                            )
                                            interacting = false
                                        },
                                        onDragCancel = { interacting = false },
                                    ) { change, dragAmount ->
                                        change.consume()
                                        val pw = latestParentW
                                        val ph = latestParentH
                                        dragX = (dragX + dragAmount.x)
                                            .coerceIn(0f, (pw - widthPx).coerceAtLeast(0f))
                                        dragY = (dragY + dragAmount.y)
                                            .coerceIn(0f, (ph - heightPx).coerceAtLeast(0f))
                                    }
                                }
                                .pointerInput(widget.id, editable) {
                                    detectTapGestures(
                                        onTap = {
                                            if (!interacting && !editable) onClick(latestWidget)
                                        },
                                        onDoubleTap = {
                                            if (editable) onDoubleTap(latestWidget)
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
                        onClick = { if (!editable && !interacting) onClick(widget) },
                        onLongClick = null,
                        onDoubleClick = null,
                        enableGestures = !allowMove && !editable,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (editable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp))
                            .background(palette.accent)
                            .pointerInput(widget.id) {
                                detectDragGestures(
                                    onDragStart = { interacting = true },
                                    onDragEnd = {
                                        val pw = latestParentW
                                        val ph = latestParentH
                                        onResize(
                                            latestWidget,
                                            (widthPx / pw).coerceIn(MIN_WIDTH_FRAC, MAX_WIDTH_FRAC),
                                            (heightPx / ph).coerceIn(MIN_HEIGHT_FRAC, MAX_HEIGHT_FRAC),
                                        )
                                        interacting = false
                                    },
                                    onDragCancel = { interacting = false },
                                ) { change, dragAmount ->
                                    change.consume()
                                    val pw = latestParentW
                                    val ph = latestParentH
                                    widthPx = (widthPx + dragAmount.x)
                                        .coerceIn(pw * MIN_WIDTH_FRAC, pw * MAX_WIDTH_FRAC)
                                    heightPx = (heightPx + dragAmount.y)
                                        .coerceIn(ph * MIN_HEIGHT_FRAC, ph * MAX_HEIGHT_FRAC)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⤡", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private const val MIN_WIDTH_FRAC = 0.22f
private const val MAX_WIDTH_FRAC = 0.95f
private const val MIN_HEIGHT_FRAC = 0.12f
private const val MAX_HEIGHT_FRAC = 0.72f
