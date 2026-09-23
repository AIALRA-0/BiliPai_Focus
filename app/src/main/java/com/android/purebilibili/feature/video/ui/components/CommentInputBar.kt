// 文件路径: feature/video/ui/components/CommentInputBar.kt
package com.android.purebilibili.feature.video.ui.components
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppHorizontalDivider

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.rememberAppClearIcon
import com.android.purebilibili.core.ui.components.AppCircularProgressIndicator
import com.android.purebilibili.core.ui.components.AppDropdownMenu
import com.android.purebilibili.core.ui.components.AppDropdownMenuItem
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppTextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.*
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

/**
 * [新增] 评论输入栏组件
 * 固定在评论列表底部，支持发送评论和回复评论
 */
@Composable
fun CommentInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean = false,
    replyToName: String? = null,  // 回复目标用户名
    onCancelReply: () -> Unit = {},
    onEmoteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clearIcon = rememberAppClearIcon()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        // 回复提示条
        AnimatedVisibility(
            visible = replyToName != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = "回复 @${replyToName ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                AppIconButton(onClick = onCancelReply) {
                    AppIcon(
                        imageVector = clearIcon,
                        contentDescription = "取消回复",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        
        AppHorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )
        
        // 输入栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 表情按钮
            AppIconButton(
                onClick = onEmoteClick,
                modifier = Modifier.size(36.dp)
            ) {
                AppIcon(
                    imageVector = Icons.Outlined.SentimentSatisfied,
                    contentDescription = "表情",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // 输入框
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = AppShapes.container(ContainerLevel.Card)
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    AppText(
                        text = if (replyToName != null) "回复 @$replyToName" else "发一条友善的评论",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (value.isNotBlank() && !isSending) {
                                onSend()
                                focusManager.clearFocus()
                            }
                        }
                    )
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 发送按钮
            val canSend = value.isNotBlank() && !isSending
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(enabled = canSend) {
                        onSend()
                        focusManager.clearFocus()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    AppCircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    AppIcon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = "发送",
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary 
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * [新增] 评论长按菜单
 */
@Composable
fun CommentContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onReply: () -> Unit,
    onDelete: (() -> Unit)? = null,  // 只有自己的评论才显示删除
    onReport: () -> Unit
) {
    AppDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        AppDropdownMenuItem(
            text = { AppText("复制") },
            onClick = {
                onCopy()
                onDismiss()
            },
            leadingIcon = {
                AppIcon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        AppDropdownMenuItem(
            text = { AppText("回复") },
            onClick = {
                onReply()
                onDismiss()
            },
            leadingIcon = {
                AppIcon(Icons.AutoMirrored.Outlined.Reply, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        if (onDelete != null) {
            AppDropdownMenuItem(
                text = { AppText("删除", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    onDelete()
                    onDismiss()
                },
                leadingIcon = {
                    AppIcon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            )
        }
        AppDropdownMenuItem(
            text = { AppText("举报") },
            onClick = {
                onReport()
                onDismiss()
            },
            leadingIcon = {
                AppIcon(Icons.Outlined.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
    }
}

/**
 * [新增] 举报原因选择对话框
 */
@Composable
fun ReportReasonDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onReport: (Int) -> Unit
) {
    if (!visible) return
    
    val reasons = listOf(
        1 to "垃圾广告",
        2 to "色情",
        3 to "刷屏",
        4 to "引战",
        5 to "剧透",
        7 to "人身攻击",
        8 to "内容不相关",
        0 to "其他"
    )
    
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("举报原因", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                reasons.forEach { (code, label) ->
                    AppTextButton(
                        onClick = { onReport(code) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppText(
                            text = label,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                AppText("取消")
            }
        }
    )
}
