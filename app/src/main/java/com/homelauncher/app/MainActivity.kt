package com.homelauncher.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomeLauncherApp()
        }
    }
}

@Composable
fun HomeLauncherApp() {
    val context = LocalContext.current
    var drawerOpen by remember { mutableStateOf(false) }

    val apps by produceState(initialValue = emptyList<AppInfo>(), context) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            loadInstalledApps(context)
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121318)) {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeScreen(onOpenDrawer = { drawerOpen = true })

                AnimatedVisibility(
                    visible = drawerOpen,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                ) {
                    AppDrawer(
                        apps = apps,
                        onLaunch = { app ->
                            drawerOpen = false
                            launchApp(context, app)
                        },
                    )
                }
            }
        }
    }

    BackHandler(enabled = drawerOpen) {
        drawerOpen = false
    }
}

@Composable
fun HomeScreen(onOpenDrawer: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -12f) onOpenDrawer()
                }
            }
            .clickable(onClick = {}, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier.padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Swipe up for apps",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
            )
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 48.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.5f))
                    .clickable { onOpenDrawer() }
            )
        }
    }
}

@Composable
fun AppDrawer(apps: List<AppInfo>, onLaunch: (AppInfo) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1B1F27),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Apps",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(apps, key = { it.packageName + it.activityName }) { app ->
                    AppIcon(app = app, onClick = { onLaunch(app) })
                }
            }
        }
    }
}

@Composable
fun AppIcon(app: AppInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .aspectRatio(1f)
                .padding(4.dp),
        )
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
