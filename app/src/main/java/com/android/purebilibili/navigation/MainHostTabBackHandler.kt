package com.android.purebilibili.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * 主页底栏 Tab 二级返回：栈顶为 [com.android.purebilibili.navigation3.BiliPaiNavKey.MainHost]
 * 且当前不在首页 Tab 时，边缘返回手势回到首页 Tab（而非直接退出应用）。
 *
 * 对齐 BiliPai [MainScreenBackHandler]：
 * - [NavigationBackHandler] 拦截系统返回；
 * - 仅在 [onBackCompleted] 时切回首页，**不做** progress seek / 跟手预览；
 * - 切页动画由 [MainBottomPagerState.switchToPage]（BiliPai `animateToPage` +
 *   `animateScrollBy`）负责，保证跨页有连续横向滚动过渡。
 */
@Composable
internal fun MainHostTabBackHandler(
    enabled: Boolean,
    onReturnToHomeTab: () -> Unit,
) {
    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = enabled,
        onBackCompleted = onReturnToHomeTab,
    )
}

/** Retained pager pages keep their UI state, but only the settled page owns Back. */
@Composable
internal fun MainHostTabBackScope(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val parent = LocalNavigationEventDispatcherOwner.current
    if (parent == null) {
        content()
        return
    }
    val dispatcher = remember(parent) {
        NavigationEventDispatcher(parent = parent.navigationEventDispatcher).also {
            it.isEnabled = enabled
        }
    }
    SideEffect {
        // NavDisplay can dispose the parent before this retained page leaves composition.
        try {
            dispatcher.isEnabled = enabled
        } catch (_: IllegalStateException) {
            // The parent already disposed this dispatcher.
        }
    }
    DisposableEffect(dispatcher) {
        onDispose {
            try {
                dispatcher.dispose()
            } catch (_: IllegalStateException) {
                // Parent disposal cascades to children before their own onDispose.
            }
        }
    }
    val owner = remember(dispatcher) {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = dispatcher
        }
    }
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner, content = content)
}
