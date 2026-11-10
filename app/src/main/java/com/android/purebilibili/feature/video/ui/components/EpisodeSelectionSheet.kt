package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.navigationevent.findViewTreeNavigationEventDispatcherOwner
import com.android.purebilibili.core.ui.AppModalBottomSheet

/** 分 P 与合集共用窗口、安全区、材质和返回收起过程，内容由各入口提供。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EpisodeSelectionSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    inlinePanel: Boolean = false,
    content: @Composable ColumnScope.(requestDismiss: () -> Unit) -> Unit,
) {
    if (inlinePanel) {
        val visible = remember { MutableTransitionState(false).apply { targetState = true } }
        val backState = rememberNavigationEventState(NavigationEventInfo.None)
        val backTransition = backState.transitionState as? NavigationEventTransitionState.InProgress
        val motionEnabled = com.android.purebilibili.core.ui.LocalComponentMotionEnabled.current &&
            !com.android.purebilibili.core.ui.LocalSystemReduceMotion.current
        val gestureEnabled = motionEnabled &&
            com.android.purebilibili.core.ui.LocalPredictiveBackGestureEnabled.current
        var committedProgress by remember { mutableFloatStateOf(0f) }
        val backProgress = animateFloatAsState(
            targetValue = if (!gestureEnabled) 0f else if (!visible.targetState) committedProgress else
                backTransition?.takeIf {
                    it.direction == NavigationEventTransitionState.TRANSITIONING_BACK
                }?.latestEvent?.progress?.coerceIn(0f, 1f) ?: 0f,
            animationSpec = if (backTransition != null || !gestureEnabled) snap() else tween(180),
            label = "inlineEpisodeSelectionBack",
        )
        val requestDismiss = {
            if (visible.targetState) {
                committedProgress = backProgress.value
                visible.targetState = false
            }
        }
        LaunchedEffect(visible.currentState, visible.targetState, visible.isIdle) {
            if (visible.isIdle && !visible.currentState && !visible.targetState) onDismiss()
        }
        NavigationBackHandler(
            state = backState,
            isBackEnabled = true,
            onBackCompleted = requestDismiss,
        )
        AnimatedVisibility(
            visibleState = visible,
            modifier = modifier.fillMaxSize(),
            enter = if (motionEnabled) slideInVertically(tween(260)) { it } + fadeIn(tween(180))
                else EnterTransition.None,
            exit = if (motionEnabled) slideOutVertically(tween(220)) { it } + fadeOut(tween(180))
                else ExitTransition.None,
        ) {
            com.android.purebilibili.core.ui.AppPopupSurface(
                type = com.android.purebilibili.core.ui.AppPopupSurfaceType.SHEET,
                shape = androidx.compose.ui.graphics.RectangleShape,
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val progress = backProgress.value
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    scaleX = 1f - 0.06f * progress
                    scaleY = 1f - 0.06f * progress
                    translationY = size.height * 0.12f * progress
                },
            ) {
                Column(Modifier.fillMaxSize()) { content(requestDismiss) }
            }
        }
        return
    }
    var sheetVisible by remember { mutableStateOf(true) }
    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    var committedBackProgress by remember { mutableFloatStateOf(0f) }
    val backTransition = backState.transitionState as? NavigationEventTransitionState.InProgress
    val motionEnabled = com.android.purebilibili.core.ui.LocalPredictiveBackGestureEnabled.current &&
        com.android.purebilibili.core.ui.LocalComponentMotionEnabled.current &&
        !com.android.purebilibili.core.ui.LocalSystemReduceMotion.current
    val backProgress = animateFloatAsState(
        targetValue = if (!motionEnabled) 0f else if (!sheetVisible) committedBackProgress else
            backTransition?.takeIf {
                it.direction == NavigationEventTransitionState.TRANSITIONING_BACK
            }?.latestEvent?.progress?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = if (backTransition != null || !motionEnabled) snap() else tween(180),
        label = "episodeSelectionPredictiveBack",
    )
    val requestDismiss: () -> Unit = {
        committedBackProgress = backProgress.value
        sheetVisible = false
    }
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        visible = sheetVisible,
        useTimedSlideMotion = true,
        dismissOnBackPress = false,
        modifier = modifier.graphicsLayer {
            val progress = backProgress.value
            transformOrigin = TransformOrigin(0.5f, 1f)
            scaleX = 1f - 0.06f * progress
            scaleY = 1f - 0.06f * progress
            translationY = size.height * 0.12f * progress
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        windowInsets = WindowInsets(0.dp),
    ) {
        // 处理器绑定面板所在窗口，避免关闭时返回事件穿透到视频页面。
        val windowOwner = checkNotNull(
            LocalView.current.findViewTreeNavigationEventDispatcherOwner()
                ?: LocalNavigationEventDispatcherOwner.current
        ) { "Episode selection sheet must provide a window navigation dispatcher" }
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides windowOwner) {
            NavigationBackHandler(
                state = backState,
                isBackEnabled = true,
                onBackCompleted = { if (sheetVisible) requestDismiss() },
            )
        }
        content(requestDismiss)
    }
}
