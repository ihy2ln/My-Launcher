package com.homelauncher.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homelauncher.app.model.WidgetType
import com.homelauncher.app.ui.theme.LauncherPalette
import java.text.SimpleDateFormat
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
) {
    when (type) {
        WidgetType.CLOCK -> ClockWidgetCard(palette, onClick, modifier, title, onLongClick)
        WidgetType.WEATHER -> WeatherWidgetCard(palette, onClick, modifier, title, onLongClick)
        WidgetType.APP_DRAWER -> AppDrawerWidgetCard(palette, onClick, modifier, title, onLongClick)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClockWidgetCard(
    palette: LauncherPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onLongClick: (() -> Unit)? = null,
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.accent.copy(alpha = 0.35f))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
