package com.android.purebilibili.feature.video.share

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.IOSModalBottomSheet
import com.android.purebilibili.core.ui.common.copyPlainTextToClipboard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoShareSheet(
    payload: VideoSharePayload,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()
    var sharingTarget by remember { mutableStateOf<VideoShareTarget?>(null) }
    val neutralIconBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val neutralIconContent = MaterialTheme.colorScheme.onSurface
    val items = listOf(
        VideoShareSheetItem(
            target = VideoShareTarget.WECHAT,
            label = "微信",
            iconText = "微",
            iconVector = null,
            backgroundColor = Color(0xFF31C95B),
            contentColor = Color.White
        ),
        VideoShareSheetItem(
            target = VideoShareTarget.QQ,
            label = "QQ",
            iconText = "Q",
            iconVector = null,
            backgroundColor = Color(0xFF25A9F2),
            contentColor = Color.White
        ),
        VideoShareSheetItem(
            target = VideoShareTarget.COPY_LINK,
            label = "复制链接",
            iconText = null,
            iconVector = Icons.Outlined.Link,
            backgroundColor = neutralIconBackground,
            contentColor = neutralIconContent
        ),
        VideoShareSheetItem(
            target = VideoShareTarget.MORE,
            label = "更多",
            iconText = null,
            iconVector = Icons.Outlined.MoreHoriz,
            backgroundColor = neutralIconBackground,
            contentColor = neutralIconContent
        )
    )

    IOSModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Text(
                text = "分享",
                modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 18.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                items.forEach { item ->
                    VideoShareSheetItemView(
                        item = item,
                        onClick = {
                            when (item.target) {
                                VideoShareTarget.WECHAT,
                                VideoShareTarget.QQ -> {
                                    val packageName = item.target.packageName ?: return@VideoShareSheetItemView
                                    if (sharingTarget != null) return@VideoShareSheetItemView
                                    sharingTarget = item.target
                                    copyPlainTextToClipboard(context, payload.url, "视频链接")
                                    Toast.makeText(context, "链接已复制，正在准备视频封面", Toast.LENGTH_SHORT).show()
                                    shareScope.launch {
                                        val coverFile = prepareVideoShareCoverFile(context, payload)
                                        if (coverFile == null && payload.coverUrl.isNotBlank()) {
                                            Toast.makeText(
                                                context,
                                                "封面加载失败，已改用链接分享",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        context.startTargetedVideoShare(
                                            payload = payload,
                                            packageName = packageName,
                                            appName = item.label,
                                            coverFile = coverFile,
                                            onSuccess = onDismiss
                                        )
                                        sharingTarget = null
                                    }
                                }
                                VideoShareTarget.COPY_LINK -> {
                                    copyPlainTextToClipboard(context, payload.url, "视频链接")
                                    Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                VideoShareTarget.MORE -> {
                                    if (sharingTarget != null) return@VideoShareSheetItemView
                                    sharingTarget = item.target
                                    copyPlainTextToClipboard(context, payload.url, "视频链接")
                                    Toast.makeText(context, "链接已复制，正在准备视频封面", Toast.LENGTH_SHORT).show()
                                    shareScope.launch {
                                        val coverFile = prepareVideoShareCoverFile(context, payload)
                                        if (coverFile == null && payload.coverUrl.isNotBlank()) {
                                            Toast.makeText(
                                                context,
                                                "封面加载失败，已改用链接分享",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        context.startMoreVideoShare(
                                            payload = payload,
                                            coverFile = coverFile,
                                            onSuccess = onDismiss
                                        )
                                        sharingTarget = null
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "取消",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Immutable
private data class VideoShareSheetItem(
    val target: VideoShareTarget,
    val label: String,
    val iconText: String?,
    val iconVector: ImageVector?,
    val backgroundColor: Color,
    val contentColor: Color
)

@Composable
private fun VideoShareSheetItemView(
    item: VideoShareSheetItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(item.backgroundColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (item.iconVector != null) {
                Icon(
                    imageVector = item.iconVector,
                    contentDescription = item.label,
                    modifier = Modifier.size(28.dp),
                    tint = item.contentColor
                )
            } else {
                Text(
                    text = item.iconText.orEmpty(),
                    color = item.contentColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun Context.startTargetedVideoShare(
    payload: VideoSharePayload,
    packageName: String,
    appName: String,
    coverFile: VideoShareCoverFile?,
    onSuccess: () -> Unit
) {
    try {
        val intent = if (coverFile != null) {
            buildVideoCoverShareIntent(
                payload = payload,
                coverUri = coverFile.uri,
                mimeType = coverFile.mimeType,
                packageName = packageName,
                contentResolver = contentResolver
            )
        } else {
            buildTargetedShareIntent(payload, packageName)
        }
        startActivityWithTaskFlag(intent)
        onSuccess()
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "未安装$appName", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(this, "无法打开$appName", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.startMoreVideoShare(
    payload: VideoSharePayload,
    coverFile: VideoShareCoverFile?,
    onSuccess: () -> Unit
) {
    try {
        val sendIntent = if (coverFile != null) {
            buildVideoCoverShareIntent(
                payload = payload,
                coverUri = coverFile.uri,
                mimeType = coverFile.mimeType,
                contentResolver = contentResolver
            )
        } else {
            buildVideoShareIntent(payload)
        }
        val chooser = Intent.createChooser(sendIntent, "分享视频到")
        if (coverFile != null) {
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityWithTaskFlag(chooser)
        onSuccess()
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "无法打开分享面板", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(this, "分享失败", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.startActivityWithTaskFlag(intent: Intent) {
    if (this !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}
