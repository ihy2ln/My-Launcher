package com.homelauncher.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WallpaperBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        WallpaperGradientTop,
                        WallpaperGradientMid,
                        WallpaperGradientBottom,
                    ),
                ),
            ),
    )
}

@Composable
fun ClockWidget(modifier: Modifier = Modifier) {
    val timeFormat = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val now = remember { System.currentTimeMillis() }
    val timeText = remember(now) { timeFormat.format(Date(now)) }
    val dateText = remember(now) { dateFormat.format(Date(now)) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = timeText,
            color = LauncherTextPrimary,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1).sp,
        )
        Text(
            text = dateText,
            color = LauncherTextSecondary,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) {
                            LauncherTextPrimary
                        } else {
                            LauncherTextPrimary.copy(alpha = 0.35f)
                        },
                    ),
            )
        }
    }
}

@Composable
fun LauncherAppIcon(
    app: AppInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    iconSize: androidx.compose.ui.unit.Dp = 56.dp,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(iconSize),
        )
        if (showLabel) {
            Text(
                text = app.label,
                color = LauncherTextPrimary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
fun DockBar(
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LauncherTextPrimary.copy(alpha = 0.45f))
                .clickable(onClick = onOpenDrawer),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(LauncherDockBackground)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            apps.forEach { app ->
                LauncherAppIcon(
                    app = app,
                    onClick = { onLaunch(app) },
                    showLabel = false,
                    iconSize = 48.dp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    homePageApps: List<List<AppInfo>>,
    dockApps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { homePageApps.size })

    Box(modifier = modifier.fillMaxSize()) {
        WallpaperBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            ClockWidget(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                val pageApps = homePageApps[page]
                if (pageApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize())
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        pageApps.take(4).forEach { app ->
                            LauncherAppIcon(
                                app = app,
                                onClick = { onLaunch(app) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            PageIndicator(
                pageCount = homePageApps.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .padding(horizontal = 16.dp),
            )

            DockBar(
                apps = dockApps,
                onLaunch = onLaunch,
                onOpenDrawer = onOpenDrawer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )
        }
    }
}
