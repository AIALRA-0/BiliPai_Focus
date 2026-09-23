// 文件路径: feature/dynamic/components/RepostDialog.kt
package com.android.purebilibili.feature.dynamic.components
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import com.android.purebilibili.core.ui.AppSpacingTokens

import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.rememberAppShareIcon
import com.android.purebilibili.core.ui.components.AppTextField

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/**
 *  动态转发对话框
 */
@Composable
fun RepostDialog(
    onDismiss: () -> Unit,
    onRepost: (content: String, onComplete: (Boolean) -> Unit) -> Unit
) {
    var repostText by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }
    
    AppAlertDialog(
        onDismissRequest = { if (!isPosting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(
                    rememberAppShareIcon(),
                    contentDescription = null,
                    modifier = Modifier.size(AppSpacingTokens.ExtraLarge),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(AppSpacingTokens.Medium))
                AppText(
                    "转发动态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            AppTextField(
                value = repostText,
                onValueChange = { repostText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacingTokens.TripleExtraLarge * 2 + AppSpacingTokens.ExtraLarge),
                placeholder = "说点什么吧...(可选)",
                singleLine = false,
                minLines = 3,
                maxLines = 5,
            )
        },
        dismissButton = {
            AppDialogAction(onClick = { if (!isPosting) onDismiss() }) {
                AppText("取消")
            }
        },
        confirmButton = {
            AppDialogAction(
                onClick = {
                    if (!isPosting) {
                        isPosting = true
                        onRepost(repostText) { success ->
                            if (!success) isPosting = false
                        }
                    }
                }
            ) {
                if (isPosting) {
                    AdaptiveLoadingIndicator(size = AppSpacingTokens.Large)
                } else {
                    AppText("转发")
                }
            }
        }
    )
}
