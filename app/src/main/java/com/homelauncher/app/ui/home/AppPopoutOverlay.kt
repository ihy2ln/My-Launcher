package com.homelauncher.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.homelauncher.app.AppInfo
import com.homelauncher.app.model.AppCategory
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.WidgetType
import com.homelauncher.app.ui.theme.LauncherPalette
import com.homelauncher.app.ui.theme.iconShape

@Composable
fun AppPopoutOverlay(
    app: AppInfo,
    label: String,
    settings: LauncherSettings,
    palette: LauncherPalette,
    theme: WidgetType? = null,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isGame = theme == WidgetType.GAME || app.category == AppCategory.GAME
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            if (isGame) {
                GamePadPopout(
                    app = app,
                    label = label,
                    settings = settings,
                    onOpen = onOpen,
                    onDismiss = onDismiss,
                    modifier = Modifier
                        .padding(20.dp)
                        .clickable(enabled = false, onClick = {}),
                )
            } else {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(palette.surface.copy(alpha = 0.95f))
                        .clickable(enabled = false, onClick = {})
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(iconShape(settings.iconShape)),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(label, color = palette.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            theme == WidgetType.MUSIC || app.category == AppCategory.MUSIC -> "Music widget · open to play"
                            theme == WidgetType.VIDEO || app.category == AppCategory.VIDEO -> "Video player · open to watch"
                            else -> "Pop-out preview · tap Open to launch"
                        },
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                    )
                    TextButton(onClick = onOpen) {
                        Text("Open app", color = palette.accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = palette.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun GamePadPopout(
    app: AppInfo,
    label: String,
    settings: LauncherSettings,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastInput by remember { mutableStateOf("Ready") }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF121212))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                bitmap = app.icon,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(iconShape(settings.iconShape)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Game pad pop-out", color = Color.White.copy(0.7f), fontSize = 12.sp)
            }
            TextButton(onClick = onDismiss) { Text("✕", color = Color.White) }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF1B5E20)),
            contentAlignment = Alignment.Center,
        ) {
            Text(lastInput, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // D-pad
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PadButton("▲") { lastInput = "Up" }
                Row {
                    PadButton("◀") { lastInput = "Left" }
                    Spacer(modifier = Modifier.width(8.dp))
                    PadButton("▶") { lastInput = "Right" }
                }
                PadButton("▼") { lastInput = "Down" }
            }
            // Action buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("A", Color(0xFF69F0AE)) { lastInput = "A" }
                    ActionButton("B", Color(0xFFFF5252)) { lastInput = "B" }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("X", Color(0xFF40C4FF)) { lastInput = "X" }
                    ActionButton("Y", Color(0xFFFFD740)) { lastInput = "Y" }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onOpen) {
            Text("Launch game", color = Color(0xFF69F0AE), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PadButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
private fun ActionButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.85f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
