package com.homelauncher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
) {
    when (type) {
        WidgetType.CLOCK -> ClockWidgetCard(palette, size, onClick, modifier)
        WidgetType.WEATHER -> WeatherWidgetCard(palette, size, onClick, modifier)
        WidgetType.APP_DRAWER -> AppDrawerWidgetCard(palette, size, onClick, modifier)
    }
}

@Composable
private fun ClockWidgetCard(
    palette: LauncherPalette,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = time.format(now),
            color = palette.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
        )
        Text(
            text = date.format(now),
            color = palette.textSecondary,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WeatherWidgetCard(
    palette: LauncherPalette,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Demo weather card (no network API key required)
    val temp = remember { 64 + Random.nextInt(0, 12) }
    val condition = remember {
        listOf("Clear", "Cloudy", "Breezy", "Sunny").random()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF4A90A4).copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("$temp°", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        Text(condition, color = Color.White.copy(0.85f), fontSize = 10.sp)
        Text("Weather", color = Color.White.copy(0.65f), fontSize = 9.sp)
    }
}

@Composable
private fun AppDrawerWidgetCard(
    palette: LauncherPalette,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(palette.accent.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("▦", color = Color.Black, fontSize = 22.sp)
        }
        Text(
            text = "Drawer",
            color = palette.textPrimary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
