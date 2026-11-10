package com.android.purebilibili.feature.video.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.purebilibili.core.ui.AppHapticScene
import com.android.purebilibili.core.ui.LocalVisualWorkAllowed
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback

/** 成功事件即时确认；页面隐藏、后台或失焦时丢弃触感，不在恢复页面后补振。 */
@Composable
internal fun rememberVideoActionSuccessHaptic(isVisible: Boolean = true): () -> Unit {
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val visible = rememberUpdatedState(isVisible && LocalVisualWorkAllowed.current)
    val haptic = rememberHapticFeedback(AppHapticScene.VIDEO_ACTIONS)
    val latestHaptic = rememberUpdatedState(haptic)
    return remember(view, lifecycle) {
        {
            if (visible.value && view.hasWindowFocus() &&
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            ) {
                latestHaptic.value(HapticType.LIGHT)
            }
        }
    }
}
