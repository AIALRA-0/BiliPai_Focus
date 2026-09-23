@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.android.purebilibili.core.ui.common

import androidx.compose.runtime.Composable

/**
 * 历史旧函数升级：直接重定向至全新的现代沉浸式 [TextSelectionBottomSheet]，
 * 彻底废弃旧版局促的 280dp Alert 弹窗，支持自由选择局部内容与一键复制。
 */
@Composable
fun CopySelectionDialog(
    text: String,
    title: String,
    onDismiss: () -> Unit
) {
    TextSelectionBottomSheet(
        text = text,
        title = title,
        onDismiss = onDismiss
    )
}
