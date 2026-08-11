package com.homelauncher.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LauncherTheme {
                HomeLauncherApp()
            }
        }
    }
}

private const val HOME_PAGE_COUNT = 2

@Composable
fun HomeLauncherApp() {
    val context = LocalContext.current
    val preferences = remember { LauncherPreferences(context) }
    var drawerOpen by remember { mutableStateOf(false) }

    val apps by produceState(initialValue = emptyList<AppInfo>(), context) {
        value = withContext(Dispatchers.Default) {
            loadInstalledApps(context)
        }
    }

    val dockApps by produceState(initialValue = emptyList<AppInfo>(), apps) {
        if (apps.isEmpty()) {
            value = emptyList()
            return@produceState
        }
        val storedKeys = preferences.getDockAppKeys()
        val resolved = storedKeys.mapNotNull { apps.findByKey(it) }
        value = if (resolved.isNotEmpty()) {
            resolved
        } else {
            val defaults = defaultDockApps(context, apps)
            preferences.setDockAppKeys(defaults.map { it.uniqueKey() })
            defaults
        }
    }

    val homePageApps by produceState(initialValue = List(HOME_PAGE_COUNT) { emptyList<AppInfo>() }, apps) {
        if (apps.isEmpty()) {
            value = List(HOME_PAGE_COUNT) { emptyList() }
            return@produceState
        }
        value = (0 until HOME_PAGE_COUNT).map { pageIndex ->
            preferences.getHomePageAppKeys(pageIndex).mapNotNull { apps.findByKey(it) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreenContent(
            homePageApps = homePageApps,
            dockApps = dockApps,
            onLaunch = { launchApp(context, it) },
            onOpenDrawer = { drawerOpen = true },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(drawerOpen) {
                    if (!drawerOpen) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -16f) drawerOpen = true
                        }
                    }
                },
        )

        AppDrawerOverlay(
            visible = drawerOpen,
            apps = apps,
            onLaunch = { app ->
                drawerOpen = false
                launchApp(context, app)
            },
            onDismiss = { drawerOpen = false },
        )
    }

    BackHandler(enabled = drawerOpen) {
        drawerOpen = false
    }
}
