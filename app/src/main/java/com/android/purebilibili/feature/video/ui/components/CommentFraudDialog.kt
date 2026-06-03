// 文件路径: feature/video/ui/components/CommentFraudDialog.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.data.model.CommentFraudStatus
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*

/**
 * [新增] 评论反诈检测结果弹窗
 * 发送评论后检测到异常状态时显示
 */
@Composable
fun CommentFraudResultDialog(
    status: CommentFraudStatus,
    onDismiss: () -> Unit,
    onDeleteComment: (() -> Unit)? = null
) {
    val (icon, title, description, color) = remember(status) {
        when (status) {
            CommentFraudStatus.NORMAL -> FraudDialogInfo(
                icon = CupertinoIcons.Default.CheckmarkCircle,
                title = "评论正常",
                description = "您的评论可以被其他用户正常看到。",
                color = FraudStatusColor.GREEN
            )
            CommentFraudStatus.SHADOW_BANNED -> FraudDialogInfo(
                icon = CupertinoIcons.Default.EyeSlash,
                title = "评论被 ShadowBan",
                description = "您的评论仅自己可见，其他用户无法看到。这可能是因为评论内容触发了阿瓦隆风控系统。\n\n建议：删除此评论并修改内容后重新发送。",
                color = FraudStatusColor.RED
            )
            CommentFraudStatus.DELETED -> FraudDialogInfo(
                icon = CupertinoIcons.Default.Trash,
                title = "评论被系统秒删",
                description = "您的评论已被系统自动删除，包括您自己也无法看到。评论内容可能包含严格敏感词。",
                color = FraudStatusColor.RED
            )
            CommentFraudStatus.UNDER_REVIEW -> FraudDialogInfo(
                icon = CupertinoIcons.Default.Clock,
                title = "评论疑似审核中",
                description = "您的评论可能正在等待审核，目前其他用户暂时无法看到。审核通过后将自动显示。",
                color = FraudStatusColor.ORANGE
            )
            CommentFraudStatus.UNKNOWN -> FraudDialogInfo(
                icon = CupertinoIcons.Default.QuestionmarkCircle,
                title = "检测结果未知",
                description = "无法确定评论状态，可能是网络问题导致检测失败。",
                color = FraudStatusColor.GRAY
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = when (color) {
                    FraudStatusColor.GREEN -> MaterialTheme.colorScheme.primary
                    FraudStatusColor.RED -> MaterialTheme.colorScheme.error
                    FraudStatusColor.ORANGE -> MaterialTheme.colorScheme.tertiary
                    FraudStatusColor.GRAY -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        },
        dismissButton = {
            // 如果被 ShadowBan，提供快捷删除操作
            if (status == CommentFraudStatus.SHADOW_BANNED && onDeleteComment != null) {
                TextButton(
                    onClick = {
                        onDeleteComment()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除评论")
                }
            }
        }
    )
}

/**
 * [新增] 评论反诈检测中的 Snackbar 提示条
 */
@Composable
fun CommentFraudDetectingBanner(
    isDetecting: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = isDetecting,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "正在检测评论可见性…",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- 内部数据类 ---

private enum class FraudStatusColor { GREEN, RED, ORANGE, GRAY }

private data class FraudDialogInfo(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val color: FraudStatusColor
)
