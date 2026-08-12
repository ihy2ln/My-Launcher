package com.homelauncher.app.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homelauncher.app.AppInfo
import com.homelauncher.app.model.IconShape
import com.homelauncher.app.ui.theme.iconShape
import com.homelauncher.app.ui.theme.LauncherPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionSheet(
    app: AppInfo,
    palette: LauncherPalette,
    iconShape: IconShape,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onAddToCategory: () -> Unit,
    onMoveToFolder: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onRemoveFromHome: () -> Unit,
    onLauncherSettings: () -> Unit,
    onRename: () -> Unit = {},
    showRemove: Boolean = true,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(iconShape(iconShape)),
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            ActionRow(SheetIcon.Open, "Open", "Launch ${app.label}", onOpen)
            ActionRow(SheetIcon.Favorite, "Favorite", "Pin to favorites / dock", onFavorite)
            ActionRow(SheetIcon.Info, "App info", "System application details", onAppInfo)
            ActionRow(SheetIcon.Category, "Add to category", "Put in a drawer tab / group", onAddToCategory)
            ActionRow(SheetIcon.Folder, "Move to folder", "Add to a home screen folder", onMoveToFolder)
            ActionRow(SheetIcon.Favorite, "Rename", "Custom name on this launcher", onRename)
            if (showRemove) {
                ActionRow(SheetIcon.Remove, "Remove from home", "Keep installed, remove shortcut", onRemoveFromHome)
            }
            ActionRow(SheetIcon.Uninstall, "Uninstall", "Open system uninstall screen", onUninstall)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Color.White.copy(alpha = 0.12f),
            )

            ActionRow(SheetIcon.Settings, "Launcher settings", null, onLauncherSettings)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private enum class SheetIcon {
    Open, Favorite, Info, Category, Folder, Remove, Uninstall, Settings
}

@Composable
private fun ActionRow(
    icon: SheetIcon,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SheetGlyph(icon)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = Color.White.copy(0.45f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SheetGlyph(icon: SheetIcon) {
    val color = Color.White.copy(alpha = 0.9f)
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = Stroke(width = 2.2f, cap = StrokeCap.Round)
        when (icon) {
            SheetIcon.Open -> {
                drawRect(
                    color = color,
                    topLeft = Offset(size.width * 0.18f, size.height * 0.18f),
                    size = Size(size.width * 0.48f, size.height * 0.48f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.45f, size.height * 0.45f),
                    end = Offset(size.width * 0.82f, size.height * 0.18f),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.82f, size.height * 0.18f),
                    end = Offset(size.width * 0.82f, size.height * 0.42f),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.82f, size.height * 0.18f),
                    end = Offset(size.width * 0.58f, size.height * 0.18f),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round,
                )
            }
            SheetIcon.Favorite -> {
                val path = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.12f)
                    lineTo(size.width * 0.61f, size.height * 0.38f)
                    lineTo(size.width * 0.88f, size.height * 0.38f)
                    lineTo(size.width * 0.66f, size.height * 0.55f)
                    lineTo(size.width * 0.74f, size.height * 0.84f)
                    lineTo(size.width * 0.5f, size.height * 0.68f)
                    lineTo(size.width * 0.26f, size.height * 0.84f)
                    lineTo(size.width * 0.34f, size.height * 0.55f)
                    lineTo(size.width * 0.12f, size.height * 0.38f)
                    lineTo(size.width * 0.39f, size.height * 0.38f)
                    close()
                }
                drawPath(path, color = color, style = stroke)
            }
            SheetIcon.Info -> {
                drawCircle(color = color, radius = size.minDimension * 0.38f, style = stroke)
                drawCircle(color = color, radius = 2.2f, center = Offset(size.width * 0.5f, size.height * 0.32f))
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.5f, size.height * 0.46f),
                    end = Offset(size.width * 0.5f, size.height * 0.72f),
                    strokeWidth = 2.4f,
                    cap = StrokeCap.Round,
                )
            }
            SheetIcon.Category -> {
                drawCircle(color = color, radius = size.minDimension * 0.12f, center = Offset(size.width * 0.28f, size.height * 0.55f))
                drawRect(
                    color = color,
                    topLeft = Offset(size.width * 0.48f, size.height * 0.42f),
                    size = Size(size.width * 0.22f, size.height * 0.22f),
                    style = stroke,
                )
                val tri = Path().apply {
                    moveTo(size.width * 0.78f, size.height * 0.28f)
                    lineTo(size.width * 0.9f, size.height * 0.52f)
                    lineTo(size.width * 0.66f, size.height * 0.52f)
                    close()
                }
                drawPath(tri, color = color, style = stroke)
            }
            SheetIcon.Folder -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.34f),
                    size = Size(size.width * 0.68f, size.height * 0.42f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.16f, size.height * 0.4f),
                    end = Offset(size.width * 0.42f, size.height * 0.4f),
                    strokeWidth = 2.2f,
                )
            }
            SheetIcon.Remove -> {
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.2f, size.height * 0.2f),
                    end = Offset(size.width * 0.8f, size.height * 0.8f),
                    strokeWidth = 2.4f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.8f, size.height * 0.2f),
                    end = Offset(size.width * 0.2f, size.height * 0.8f),
                    strokeWidth = 2.4f,
                    cap = StrokeCap.Round,
                )
            }
            SheetIcon.Uninstall -> {
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.28f, size.height * 0.28f),
                    end = Offset(size.width * 0.72f, size.height * 0.28f),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round,
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.3f, size.height * 0.34f),
                    size = Size(size.width * 0.4f, size.height * 0.46f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
                    style = stroke,
                )
            }
            SheetIcon.Settings -> {
                drawCircle(color = color, radius = size.minDimension * 0.16f, style = stroke)
                drawCircle(color = color, radius = size.minDimension * 0.36f, style = stroke)
            }
        }
    }
}

fun openAppInfo(context: android.content.Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

fun openUninstall(context: android.content.Context, packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE).apply {
        data = Uri.parse("package:$packageName")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
