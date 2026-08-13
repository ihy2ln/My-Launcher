package com.homelauncher.app.widget

import android.appwidget.AppWidgetManager
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.homelauncher.app.AppInfo
import com.homelauncher.app.model.AppCategory
import com.homelauncher.app.ui.theme.LauncherPalette

@Composable
fun HostedAppWidget(
    appWidgetId: Int,
    providerFlat: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val host = remember { LauncherWidgetHost.get(context) }
    val manager = remember { AppWidgetManager.getInstance(context) }
    val providerInfo = remember(appWidgetId, providerFlat) {
        manager.getAppWidgetInfo(appWidgetId)
            ?: unflattenProvider(providerFlat)?.let { component ->
                manager.installedProviders.firstOrNull { it.provider == component }
            }
    }

    AndroidView(
        factory = { ctx ->
            host.createHostView(ctx, appWidgetId, providerInfo).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { view ->
            if (providerInfo != null) {
                view.setAppWidget(appWidgetId, providerInfo)
            }
        },
        modifier = modifier.fillMaxSize(),
    )
}

data class AppShortcutAction(
    val id: String,
    val label: String,
    val onClick: () -> Unit,
)

@Composable
fun rememberAppShortcuts(app: AppInfo): List<AppShortcutAction> {
    val context = LocalContext.current
    return remember(app.packageName) {
        runCatching {
            val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return@runCatching emptyList()
            val user: UserHandle = Process.myUserHandle()
            val query = android.content.pm.LauncherApps.ShortcutQuery().apply {
                setPackage(app.packageName)
                setQueryFlags(
                    android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                )
            }
            launcherApps.getShortcuts(query, user).orEmpty().take(8).mapNotNull { shortcut ->
                val label = shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString() ?: return@mapNotNull null
                AppShortcutAction(
                    id = shortcut.id,
                    label = label,
                ) {
                    runCatching {
                        launcherApps.startShortcut(app.packageName, shortcut.id, null, null, user)
                    }
                }
            }
        }.getOrDefault(emptyList())
    }
}

fun resolveAppWebUrl(context: android.content.Context, packageName: String): String? {
    val pm = context.packageManager
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        addCategory(android.content.Intent.CATEGORY_BROWSABLE)
        data = android.net.Uri.parse("https://")
    }
    val matches = pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName == packageName }
    // Prefer https app links from package intent filters via PackageManager
    val packageInfo = runCatching {
        pm.getPackageInfo(
            packageName,
            android.content.pm.PackageManager.GET_ACTIVITIES or android.content.pm.PackageManager.GET_META_DATA,
        )
    }.getOrNull()
    val known = knownWebFrontends[packageName]
    if (known != null) return known
    if (matches.isNotEmpty()) {
        return "https://www.google.com/search?q=${android.net.Uri.encode(packageName)}"
    }
    packageInfo ?: return null
    return null
}

private val knownWebFrontends = mapOf(
    "com.amazon.mShop.android.shopping" to "https://www.amazon.com",
    "com.spotify.music" to "https://open.spotify.com",
    "com.google.android.youtube" to "https://www.youtube.com",
    "com.netflix.mediaclient" to "https://www.netflix.com",
    "com.instagram.android" to "https://www.instagram.com",
    "com.twitter.android" to "https://x.com",
    "com.zhiliaoapp.musically" to "https://www.tiktok.com",
    "com.facebook.katana" to "https://www.facebook.com",
    "com.reddit.frontpage" to "https://www.reddit.com",
    "com.discord" to "https://discord.com/app",
    "com.whatsapp" to "https://web.whatsapp.com",
    "org.telegram.messenger" to "https://web.telegram.org",
    "com.android.chrome" to "https://www.google.com",
    "com.brave.browser" to "https://search.brave.com",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmbeddedAppWorkspace(
    app: AppInfo,
    metadataSummary: String,
    palette: LauncherPalette,
    onOpenFullApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val shortcuts = rememberAppShortcuts(app)
    val webUrl = remember(app.packageName) { resolveAppWebUrl(context, app.packageName) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surface.copy(alpha = 0.96f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.label, color = palette.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(metadataSummary, color = palette.textSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = onOpenFullApp) {
                    Text("Open", color = palette.accent, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = palette.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (shortcuts.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surface.copy(alpha = 0.9f))
                        .padding(10.dp),
                ) {
                    shortcuts.forEach { shortcut ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(palette.accent.copy(alpha = 0.25f))
                                .clickable(onClick = shortcut.onClick)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(shortcut.label, color = palette.textPrimary, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surface),
            ) {
                if (webUrl != null) {
                    keyWebView(url = webUrl)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("In-widget session", color = palette.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text(
                            "This app does not publish an Android widget. Use shortcuts above or open the full app. Metadata from the package is shown so the home tile stays useful.",
                            color = palette.textSecondary,
                            fontSize = 13.sp,
                        )
                        MetaRow("Package", app.packageName, palette)
                        MetaRow("Category", app.category.name.lowercase().replaceFirstChar { it.titlecase() }, palette)
                        MetaRow("Activity", app.activityName.substringAfterLast('.'), palette)
                        TextButton(onClick = onOpenFullApp) {
                            Text("Launch inside launcher", color = palette.accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String, palette: LauncherPalette) {
    Column {
        Text(label, color = palette.textSecondary, fontSize = 11.sp)
        Text(value, color = palette.textPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun keyWebView(url: String) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        onDispose { /* WebView cleaned by AndroidView */ }
    }
    AndroidView(
        factory = {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

fun categoryLabel(category: AppCategory): String = when (category) {
    AppCategory.MUSIC -> "Music"
    AppCategory.VIDEO -> "Video"
    AppCategory.GAME -> "Game"
    AppCategory.IMAGE -> "Photos"
    AppCategory.SOCIAL -> "Social"
    AppCategory.NEWS -> "News"
    AppCategory.MAPS -> "Maps"
    AppCategory.PRODUCTIVITY -> "Productivity"
    AppCategory.GENERIC -> "App"
}
