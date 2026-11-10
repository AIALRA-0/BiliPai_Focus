package com.android.purebilibili.feature.following

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton

@Composable
internal fun RemoveFanConfirmDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { AppText("移除粉丝") },
        text = { AppText("将“${name.ifBlank { "该用户" }}”从你的粉丝中移除？对方仍可再次关注你。此操作不会取消你对对方的关注，也不会将其拉黑。") },
        confirmButton = { AppTextButton(onClick = onConfirm) { AppText("移除") } },
        dismissButton = { AppTextButton(onClick = onDismiss) { AppText("取消") } },
    )
}
