package com.homelauncher.app.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.homelauncher.app.model.toArgbLong
import com.homelauncher.app.model.toComposeColor
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ColorWheelPicker(
    color: Long,
    onColorChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = color.toComposeColor()
    var hue by remember(color) {
        mutableFloatStateOf(rgbToHsv(current)[0])
    }
    var saturation by remember(color) {
        mutableFloatStateOf(rgbToHsv(current)[1])
    }
    var value by remember(color) {
        mutableFloatStateOf(rgbToHsv(current)[2])
    }

    fun emit() {
        onColorChange(hsvToColor(hue, saturation, value).toArgbLong())
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        fun handle(offset: Offset) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val radius = min(cx, cy)
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val distance = hypot(dx, dy).coerceAtMost(radius)
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f
                            hue = angle
                            saturation = (distance / radius).coerceIn(0f, 1f)
                            emit()
                        }
                        detectTapGestures { handle(it) }
                        detectDragGestures { change, _ ->
                            handle(change.position)
                            change.consume()
                        }
                    },
            ) {
                val radius = size.minDimension / 2f
                val sweep = Brush.sweepGradient(
                    listOf(
                        Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                        Color.Blue, Color.Magenta, Color.Red,
                    ),
                )
                drawCircle(brush = sweep, radius = radius)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                )
                val markerRadius = radius * saturation
                val rad = Math.toRadians(hue.toDouble())
                val mx = center.x + markerRadius * cos(rad).toFloat()
                val my = center.y + markerRadius * sin(rad).toFloat()
                drawCircle(Color.White, radius = 14f, center = Offset(mx, my), style = Stroke(width = 4f))
                drawCircle(hsvToColor(hue, saturation, value), radius = 10f, center = Offset(mx, my))
            }
        }

        Text("Saturation", color = Color.White.copy(0.7f), fontSize = 12.sp)
        Slider(
            value = saturation,
            onValueChange = {
                saturation = it
                emit()
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Brightness", color = Color.White.copy(0.7f), fontSize = 12.sp)
        Slider(
            value = value,
            onValueChange = {
                value = it
                emit()
            },
            valueRange = 0.15f..1f,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(hsvToColor(hue, saturation, value)),
        )
    }
}

@Composable
fun WallpaperBackdrop(
    color: Long,
    imageUri: String?,
    videoUri: String?,
    useImage: Boolean,
    useVideo: Boolean,
    gradientFallback: Brush,
    useGradient: Boolean = false,
) {
    when {
        useVideo && !videoUri.isNullOrBlank() -> {
            VideoWallpaper(uri = videoUri)
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
        }
        useImage && !imageUri.isNullOrBlank() -> {
            UriImage(uri = imageUri, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
        }
        useGradient -> Box(modifier = Modifier.fillMaxSize().background(gradientFallback))
        else -> Box(modifier = Modifier.fillMaxSize().background(color.toComposeColor()))
    }
}

@Composable
fun UriImage(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier.background(Color(0xFF3A4F50)))
    }
}

@Composable
fun VideoWallpaper(uri: String) {
    val context = LocalContext.current
    AndroidView(
        factory = {
            VideoView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setVideoURI(Uri.parse(uri))
                setOnPreparedListener { player ->
                    player.isLooping = true
                    player.setVolume(0f, 0f)
                    start()
                }
            }
        },
        update = { view ->
            if (!view.isPlaying) {
                view.setVideoURI(Uri.parse(uri))
                view.start()
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
    DisposableEffect(uri) {
        onDispose { }
    }
}

@Composable
fun ModulePlate(
    opacity: Float,
    imageUri: String?,
    videoUri: String?,
    modifier: Modifier = Modifier,
    color: Long = 0xFF1A1A1A,
    saturation: Float = 0.2f,
    brightness: Float = 0.35f,
    content: @Composable () -> Unit,
) {
    val base = color.toComposeColor()
    val hsv = rgbToHsv(base)
    val tint = hsvToColor(hsv[0], saturation.coerceIn(0f, 1f), brightness.coerceIn(0.05f, 1f))
        .copy(alpha = opacity.coerceIn(0.15f, 0.95f))

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !videoUri.isNullOrBlank() -> {
                VideoWallpaper(uri = videoUri)
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
            }
            !imageUri.isNullOrBlank() -> {
                UriImage(uri = imageUri, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
            }
        }
        content()
    }
}

/** Public HSV helpers for module styling. */
fun moduleRgbToHsv(color: Color): FloatArray = rgbToHsv(color)
fun moduleHsvToColor(h: Float, s: Float, v: Float): Color = hsvToColor(h, s, v)

private fun rgbToHsv(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val h = when {
        delta == 0f -> 0f
        max == r -> (60 * ((g - b) / delta) + 360) % 360
        max == g -> (60 * ((b - r) / delta) + 120) % 360
        else -> (60 * ((r - g) / delta) + 240) % 360
    }
    val s = if (max == 0f) 0f else delta / max
    return floatArrayOf(h, s, max)
}

private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
    val m = v - c
    val (rp, gp, bp) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(rp + m, gp + m, bp + m)
}
