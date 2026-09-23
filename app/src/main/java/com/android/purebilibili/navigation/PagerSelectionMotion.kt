package com.android.purebilibili.navigation

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.pager.PagerState
import kotlin.math.abs

/**
 * Moves a tab-backed pager through every intermediate page.
 *
 * Compose's animateScrollToPage may pre-jump for distant targets. That optimization is useful
 * for content carousels, but makes a tab indicator appear to hard-cut. Tab rails use this helper
 * so page content and indicators share one continuous position. The main bottom navigation and
 * every tab rail that opts into progressive page traversal call this same implementation.
 */
internal suspend fun animatePagerSelection(
    pagerState: PagerState,
    targetPage: Int,
    animationSpec: FiniteAnimationSpec<Float>? = null,
) {
    val lastPage = pagerState.pageCount - 1
    if (lastPage < 0) return
    val safeTargetPage = targetPage.coerceIn(0, lastPage)
    if (
        safeTargetPage == pagerState.currentPage &&
        abs(pagerState.currentPageOffsetFraction) <= 0.001f
    ) {
        return
    }

    val pageDistance = abs(safeTargetPage - pagerState.currentPage).coerceAtLeast(1)
    val resolvedSpec = animationSpec ?: tween(
        durationMillis = resolveBottomPagerNavigationDurationMillis(pageDistance),
        easing = EaseInOut,
    )

    pagerState.scroll(MutatePriority.UserInput) {
        val pageSizePx = pagerState.layoutInfo.pageSize + pagerState.layoutInfo.pageSpacing
        if (pageSizePx <= 0) return@scroll
        val remainingPages = safeTargetPage -
            pagerState.currentPage -
            pagerState.currentPageOffsetFraction
        val scrollDistancePx = remainingPages * pageSizePx
        var consumedPx = 0f
        animate(
            initialValue = 0f,
            targetValue = scrollDistancePx,
            animationSpec = resolvedSpec,
        ) { value, _ ->
            consumedPx += scrollBy(value - consumedPx)
        }
    }

    if (pagerState.currentPage != safeTargetPage ||
        abs(pagerState.currentPageOffsetFraction) > 0.001f
    ) {
        pagerState.scrollToPage(safeTargetPage)
    }
}
