package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride

/** Miuix owns cross-axis touch recognition; Material keeps Foundation's native pager. */
@Composable
fun AppHorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: TargetedFlingBehavior? = null,
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((Int) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection? = null,
    snapPosition: SnapPosition = SnapPosition.Start,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    shouldYieldGesture: () -> Boolean = { false },
    pageContent: @Composable PagerScope.(Int) -> Unit,
) {
    val miuix = LocalAppUiStyle.current == AppUiStyle.MIUIX
    val enabled = userScrollEnabled && !shouldYieldGesture()
    val resolvedFling = flingBehavior ?: if (miuix) {
        PagerDefaults.flingBehavior(state, snapAnimationSpec = PagerNavigationSpringSpec)
    } else PagerDefaults.flingBehavior(state)
    val resolvedNestedScroll = pageNestedScrollConnection ?: if (miuix) {
        PagerGestureNestedScrollConnection
    } else PagerDefaults.pageNestedScrollConnection(state, Orientation.Horizontal)
    HorizontalPager(
        state = state,
        modifier = if (miuix) modifier.pagerGestureOverride(
            pagerState = state, flingBehavior = resolvedFling, enabled = enabled,
        ) else modifier,
        contentPadding = contentPadding, pageSize = pageSize,
        beyondViewportPageCount = beyondViewportPageCount, pageSpacing = pageSpacing,
        verticalAlignment = verticalAlignment, flingBehavior = resolvedFling,
        userScrollEnabled = !miuix && enabled, reverseLayout = reverseLayout, key = key,
        pageNestedScrollConnection = resolvedNestedScroll, snapPosition = snapPosition,
        overscrollEffect = overscrollEffect, pageContent = pageContent,
    )
}
