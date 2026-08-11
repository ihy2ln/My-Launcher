package com.homelauncher.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homelauncher.app.AppInfo
import com.homelauncher.app.model.FolderInfo
import com.homelauncher.app.model.LauncherSettings
import com.homelauncher.app.model.toComposeColor
import com.homelauncher.app.ui.theme.iconShape
import com.homelauncher.app.ui.theme.LauncherPalette

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconView(
    app: AppInfo,
    settings: LauncherSettings,
    palette: LauncherPalette,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    showLabel: Boolean = settings.showLabels,
    size: Dp = settings.iconSizeDp.dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(iconShape(settings.iconShape)),
        )
        if (showLabel) {
            Text(
                text = app.label,
                color = settings.labelColor.toComposeColor(),
                fontSize = settings.labelSizeSp.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderIconView(
    folder: FolderInfo,
    previewApps: List<AppInfo>,
    settings: LauncherSettings,
    palette: LauncherPalette,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    size: Dp = settings.iconSizeDp.dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(iconShape(settings.iconShape))
                .background(palette.surface.copy(alpha = 0.85f))
                .padding(6.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (row in 0 until 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (col in 0 until 2) {
                            val app = previewApps.getOrNull(row * 2 + col)
                            if (app != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size((size - 16.dp) / 2)
                                        .clip(RoundedCornerShape(4.dp)),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size((size - 16.dp) / 2)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(0.08f)),
                                )
                            }
                        }
                    }
                }
            }
        }
        if (settings.showLabels) {
            Text(
                text = folder.title,
                color = settings.labelColor.toComposeColor(),
                fontSize = settings.labelSizeSp.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
fun EmptySlotView(
    size: Dp,
    palette: LauncherPalette,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "+", color = palette.textSecondary, fontSize = 24.sp)
    }
}
