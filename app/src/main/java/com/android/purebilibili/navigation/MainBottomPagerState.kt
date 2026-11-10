package com.android.purebilibili.navigation

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * 底栏 HorizontalPager 状态。点击指示器与内容翻页分别连续运动。
 */
@Stable
internal class MainBottomPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    var navigationStartPage by mutableIntStateOf(pagerState.currentPage)
        private set

    /** Continuous position shared by the pager and the bottom-bar indicator. */
    val indicatorPosition: Float
        get() = pagerState.tabIndicatorPosition

    /** True for both user-driven and programmatic pager movement. */
    val isScrollInProgress: Boolean
        get() = pagerState.isScrollInProgress

    /** Stable adapters for UI components that observe values from snapshotFlow. */
    val indicatorPositionProvider: () -> Float = { indicatorPosition }
    val scrollInProgressProvider: () -> Boolean = { isScrollInProgress }
    val programmaticNavigationProvider: () -> Boolean = { isNavigating }
    val targetPositionProvider: () -> Float = {
        (if (isNavigating) selectedPage else pagerState.targetPage).toFloat()
    }

    private var navJob: Job? = null

    /**
     * MainPagerState 目标页切换：
     * - 先更新 [selectedPage]（底栏指示器跟目标页），并让目标页先完成一次组合；
     * - 再驱动 Pager 到目标，远距离切换允许跳过中间页，指示器独立连续移动。
     */
    fun switchToPage(targetIndex: Int) {
        val lastPage = pagerState.pageCount - 1
        if (lastPage < 0) return
        val safeTargetIndex = targetIndex.coerceIn(0, lastPage)
        if (safeTargetIndex == selectedPage) return

        navJob?.cancel()

        navigationStartPage = pagerState.currentPage
        selectedPage = safeTargetIndex
        isNavigating = true

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                // 目标页此刻已成为转场参与者并开始组合。先让出一帧把它挂载 / 测量的成本
                // 付掉，动画再启动，避免动画首帧与冷组合抢同一帧预算而掉帧。
                awaitPagerTargetPagePrewarm()
                animatePagerSelection(pagerState, safeTargetIndex)
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != safeTargetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                    navigationStartPage = pagerState.currentPage
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
internal fun rememberMainBottomPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): MainBottomPagerState {
    return remember(pagerState, coroutineScope) {
        MainBottomPagerState(
            pagerState = pagerState,
            coroutineScope = coroutineScope
        )
    }
}
