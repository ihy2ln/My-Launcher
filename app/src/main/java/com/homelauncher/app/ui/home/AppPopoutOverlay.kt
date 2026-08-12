package com.homelauncher.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.ui.theme.LauncherPalette
import com.homelauncher.app.ui.theme.iconShape

@Composable
fun AppPopoutOverlay(
    app: AppInfo,
    label: String,
    settings: LauncherSettings,
    palette: LauncherPalette,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
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
                    "Pop-out preview · tap Open to launch",
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
