// 文件路径: feature/video/ui/components/CommentFraudDialog.kt
package com.android.purebilibili.feature.video.ui.components
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.data.model.CommentFraudStatus
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*

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
                icon = Icons.Outlined.CheckCircle,
                title = "评论正常",
                description = "您的评论可以被其他用户正常看到。",
                color = FraudStatusColor.GREEN
            )
            CommentFraudStatus.SHADOW_BANNED -> FraudDialogInfo(
                icon = Icons.Outlined.VisibilityOff,
                title = "评论被 ShadowBan",
                description = "您的评论仅自己可见，其他用户无法看到。这可能是因为评论内容触发了阿瓦隆风控系统。\n\n建议：删除此评论并修改内容后重新发送。",
                color = FraudStatusColor.RED
            )
            CommentFraudStatus.DELETED -> FraudDialogInfo(
                icon = Icons.Outlined.Delete,
                title = "评论被系统秒删",
                description = "您的评论已被系统自动删除，包括您自己也无法看到。评论内容可能包含严格敏感词。",
                color = FraudStatusColor.RED
            )
            CommentFraudStatus.UNDER_REVIEW -> FraudDialogInfo(
                icon = Icons.Outlined.Schedule,
                title = "评论疑似审核中",
                description = "您的评论可能正在等待审核，目前其他用户暂时无法看到。审核通过后将自动显示。",
                color = FraudStatusColor.ORANGE
            )
            CommentFraudStatus.INVISIBLE -> FraudDialogInfo(
                icon = Icons.Outlined.Visibility,
                title = "评论被前端隐藏",
                description = "您的评论数据存在，但已被前端隐藏，其他用户看不到。这通常是因为被 UP 主拉黑或评论被标记为隐身。\n\n建议：删除此评论。",
                color = FraudStatusColor.ORANGE
            )
            CommentFraudStatus.UNKNOWN -> FraudDialogInfo(
                icon = Icons.Outlined.HelpOutline,
                title = "检测结果未知",
                description = "无法确定评论状态，可能是网络问题导致检测失败。",
                color = FraudStatusColor.GRAY
            )
        }
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            AppIcon(
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
            AppText(
                text = title,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            AppText(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            AppTextButton(onClick = onDismiss) {
                AppText("知道了")
            }
        },
        dismissButton = {
            // 如果被 ShadowBan，提供快捷删除操作
            if (status == CommentFraudStatus.SHADOW_BANNED && onDeleteComment != null) {
                AppTextButton(
                    onClick = {
                        onDeleteComment()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    AppText("删除评论")
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
            AdaptiveLoadingIndicator(
                size = 12.dp,
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(8.dp))
            AppText(
                text = "正在检测评论可见性…",
                style = MaterialTheme.typography.bodySmall,
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
