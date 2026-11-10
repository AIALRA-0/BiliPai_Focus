package com.android.purebilibili.core.ui.transition

import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/** 来源 entry 可被详情覆盖，但补充读回只允许真实窗口仍在前台且持有焦点。 */
@Composable
internal fun rememberVideoCardPixelCaptureAllowed(): State<Boolean> {
    val lifecycle = (LocalActivity.current as? LifecycleOwner)?.lifecycle
        ?: LocalLifecycleOwner.current.lifecycle
    val lifecycleState = lifecycle.currentStateAsState()
    val window = LocalWindowInfo.current
    return remember(lifecycleState, window) {
        derivedStateOf {
            lifecycleState.value.isAtLeast(Lifecycle.State.RESUMED) && window.isWindowFocused
        }
    }
}

/**
 * 返回末段使用的补充像素不参与点击和入场关键帧。
 * 让导航先发布相位，再等待当次详情的首轮正文/媒体/评论/推荐挂载许可释放。
 * 释放后再让出一个完整绘制帧；快速返回、换卡或页面销毁均不补抓。
 * 调用方提供页面 scope，并在开始等待前冻结点击身份，不能在等待结束后重新认领身份。
 */
internal suspend fun captureVideoCardPixelsAfterEntry(
    phaseProvider: () -> VideoCardTransitionBackgroundPhase,
    isReturnGestureInProgress: () -> Boolean,
    ownsCapture: () -> Boolean,
    captureAllowed: () -> Boolean = { true },
    entryWorkReleased: () -> Boolean = { true },
    capture: suspend () -> Unit,
) {
    yield()
    if (!ownsCapture() || !captureAllowed()) return
    snapshotFlow {
        !ownsCapture() || !captureAllowed() || phaseProvider() != VideoCardTransitionBackgroundPhase.OPENING
    }
        .first { it }
    fun canCapture() = ownsCapture() && captureAllowed() &&
        phaseProvider() == VideoCardTransitionBackgroundPhase.HELD &&
        !isReturnGestureInProgress()
    if (!canCapture()) return
    coroutineScope {
        val captureJob = launch(start = CoroutineStart.UNDISPATCHED) {
            // 等待许可时也受下方撤销监听保护；正文迟到不能被固定两帧假定为已挂载。
            snapshotFlow { entryWorkReleased() }.first { it }
            repeat(2) { withFrameNanos { } }
            if (canCapture()) capture()
        }
        // 一旦发生返回/换卡便永久撤销本次任务；取消返回后不复活旧捕获。
        // 监听覆盖等待帧和异步像素读回整个区间，而非只在读回之前检查。
        val revokeJob = launch(start = CoroutineStart.UNDISPATCHED) {
            snapshotFlow { canCapture() }.first { !it }
            captureJob.cancel()
        }
        try {
            captureJob.join()
        } finally {
            revokeJob.cancel()
        }
    }
}
