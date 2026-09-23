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
 * 底栏 HorizontalPager 状态。切页由 UserInput 优先级接管 Pager。
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
        get() = pagerState.currentPage + pagerState.currentPageOffsetFraction

    /** True for both user-driven and programmatic pager movement. */
    val isScrollInProgress: Boolean
        get() = pagerState.isScrollInProgress

    /** Stable adapters for UI components that observe values from snapshotFlow. */
    val indicatorPositionProvider: () -> Float = { indicatorPosition }
    val scrollInProgressProvider: () -> Boolean = { isScrollInProgress }

    private var navJob: Job? = null

    /**
     * MainPagerState 目标页切换：
     * - 先更新 [selectedPage]（底栏指示器跟目标页）；
     * - 再在 Pager 的 UserInput mutation 中逐帧滚动到目标，避免反向切页被旧状态取消。
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
