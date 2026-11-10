// 文件路径: feature/video/ui/components/ReportReasonDialog.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton

/**
 * 举报原因选择对话框。
 *
 * 原定义随已删除的 CommentInputBar.kt 一起被移除，但 ReplyComponents 仍在使用。
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
                            color = AppContentColors.primaryText()
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
