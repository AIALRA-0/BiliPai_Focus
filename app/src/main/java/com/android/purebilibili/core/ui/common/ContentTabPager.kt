package com.android.purebilibili.core.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.android.purebilibili.navigation.animatePagerSelection
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** Page content may skip distant tabs; native/glass rails retain independent continuous motion. */
@Composable
internal fun rememberContentTabPager(
    pageCount: Int,
    selectedPage: Int,
    onPageSelected: (Int) -> Unit,
    prepareNeighbors: (Int) -> Unit,
): PagerState {
    // Async tabs can change count while the old layout still owns its key/content lambdas.
    // Replace the pager on a count change so its count never outgrows that layout's list.
    // The caller's selection and per-tab scroll states survive the replacement.
    val pager = key(pageCount) {
        rememberPagerState(initialPage = selectedPage) { pageCount }
    }
    val latestOnSelected = rememberUpdatedState(onPageSelected)
    val latestPrepare = rememberUpdatedState(prepareNeighbors)
    LaunchedEffect(pager, selectedPage) {
        if (pager.currentPage != selectedPage || pager.currentPageOffsetFraction != 0f) {
            latestPrepare.value(pager.settledPage)
            animatePagerSelection(pager, selectedPage)
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.isScrollInProgress }
            .filter { it }
            .collect { latestPrepare.value(pager.settledPage) }
    }
    LaunchedEffect(pager) {
        // Ignore initial restoration. A drag is committed only after settling, so a cancelled
        // swipe leaves selection and data loading untouched.
        snapshotFlow { if (pager.isScrollInProgress) null else pager.settledPage }
            .filterNotNull()
            .distinctUntilChanged()
            .drop(1)
            .collect { latestOnSelected.value(it) }
    }
    return pager
}

/** Keep header/rail pixels stationary while sibling content pages move underneath them. */
internal fun Modifier.stationaryPagerHeader(pager: PagerState, page: Int): Modifier = composed {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    graphicsLayer {
        val distance = pager.currentPage + pager.currentPageOffsetFraction - page
        translationX = distance * pager.layoutInfo.pageSize * if (rtl) -1f else 1f
    }
}

/** Header, tab dragging, nested horizontal lists and two-finger zoom retain their own gestures. */
internal fun Modifier.contentTabPagerGestures(
    pager: PagerState,
    enabled: Boolean,
    contentTopPx: () -> Float,
    dragInteractionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
): Modifier = composed {
    val latestContentTop = rememberUpdatedState(contentTopPx)
    val yieldGesture = remember { booleanArrayOf(false) }
    val scope = rememberCoroutineScope()
    pointerInput(pager, enabled) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            yieldGesture[0] = !enabled || down.position.y <= latestContentTop.value()
            var interrupted = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.size > 1) {
                    yieldGesture[0] = true
                    interrupted = true
                }
            } while (event.changes.any { it.pressed })
            if (interrupted) {
                scope.launch { animatePagerSelection(pager, pager.settledPage) }
            }
        }
    }.verticalPriorityHorizontalPagerSwipe(
        state = pager,
        enabled = enabled,
        forceVerticalPriority = true,
        dragInteractionSource = dragInteractionSource,
        shouldYield = { yieldGesture[0] },
    )
}
