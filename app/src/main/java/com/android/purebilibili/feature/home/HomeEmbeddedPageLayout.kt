package com.android.purebilibili.feature.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.motion.AppMotionTokens
import com.android.purebilibili.feature.home.policy.resolveHomeEmbeddedPageTopPaddingPx

@Composable
internal fun rememberHomeEmbeddedPageTopPadding(
    expandedTopPadding: Dp,
    statusBarHeight: Dp,
    tabRowHeight: Dp,
    tabsCollapsed: Boolean,
    headerOffsetProvider: () -> Float,
): State<Dp> {
    val density = LocalDensity.current
    val collapsedTabInset = animateDpAsState(
        targetValue = if (tabsCollapsed) tabRowHeight else AppSpacingTokens.None,
        animationSpec = AppMotionTokens.emphasizedSpec(),
        label = "homeEmbeddedTabInset",
    )
    return remember(density, expandedTopPadding, statusBarHeight, collapsedTabInset, headerOffsetProvider) {
        derivedStateOf {
            with(density) {
                resolveHomeEmbeddedPageTopPaddingPx(
                    expandedTopPaddingPx = expandedTopPadding.toPx(),
                    headerOffsetPx = headerOffsetProvider(),
                    collapsedTabInsetPx = collapsedTabInset.value.toPx(),
                    minimumTopPaddingPx = statusBarHeight.toPx(),
                ).toDp()
            }
        }
    }
}

/** Read padding in this restart scope, leaving the pager's page routing untouched. */
@Composable
internal fun HomeEmbeddedPageContent(
    topPaddingState: State<Dp>,
    content: @Composable (Dp) -> Unit,
) {
    content(topPaddingState.value)
}
