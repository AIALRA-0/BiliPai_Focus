package com.android.purebilibili.feature.video.ui.components

import androidx.compose.runtime.Composable
import com.android.purebilibili.core.ui.ModalWindowBackHandler

/** 播放器与其他独立弹窗共用当前窗口的返回手势注册与清理。 */
@Composable
internal fun PlayerPanelPopupBackHandler(
    predictive: Boolean,
    onStarted: () -> Unit,
    onProgress: (Float) -> Unit,
    onCancelled: () -> Unit,
    onCompleted: () -> Unit,
) = ModalWindowBackHandler(
    predictive = predictive,
    onStarted = onStarted,
    onProgress = onProgress,
    onCancelled = onCancelled,
    onCompleted = onCompleted,
)
