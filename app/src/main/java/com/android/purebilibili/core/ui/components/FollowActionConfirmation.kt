package com.android.purebilibili.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.android.purebilibili.core.ui.AppAlertDialog

/**
 * Keep the existing follow action, but require an explicit second step before unfollowing.
 * UI state belongs to this target and relationship; changing either discards a pending choice.
 * Shared interaction only: the adaptive dialog preserves each material's native appearance.
 */
@Composable
internal fun rememberFollowActionWithConfirmation(
    targetKey: Any,
    userName: String,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
): () -> Unit {
    var showConfirmation by remember(targetKey, isFollowing) { mutableStateOf(false) }
    val latestAction = rememberUpdatedState(onFollowClick)
    val latestTarget = rememberUpdatedState(targetKey)
    val latestFollowing = rememberUpdatedState(isFollowing)

    if (showConfirmation && isFollowing) {
        AppAlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { AppText("取消关注") },
            text = {
                val name = userName.ifBlank { "这位 UP 主" }
                AppText("确定取消关注「$name」吗？\n重新关注后，可能需要重新选择关注分组。")
            },
            confirmButton = {
                AppTextButton(onClick = {
                    // Dismiss first to reject repeated taps. A stale dialog must never
                    // toggle a different UP or re-follow someone whose state changed.
                    if (showConfirmation) {
                        showConfirmation = false
                        if (latestTarget.value == targetKey && latestFollowing.value) {
                            latestAction.value()
                        }
                    }
                }) { AppText("继续取关") }
            },
            dismissButton = {
                AppTextButton(onClick = { showConfirmation = false }) { AppText("取消") }
            },
        )
    }
    return {
        if (latestTarget.value == targetKey) {
            if (latestFollowing.value) showConfirmation = true else latestAction.value()
        }
    }
}
