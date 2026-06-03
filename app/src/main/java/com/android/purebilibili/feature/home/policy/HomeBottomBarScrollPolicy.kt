package com.android.purebilibili.feature.home.policy

internal data class HomeBottomBarScrollState(
    val firstVisibleItem: Int,
    val scrollOffset: Int
)

internal data class HomeBottomBarScrollUpdate(
    val state: HomeBottomBarScrollState,
    val visibilityIntent: BottomBarVisibilityIntent?
)

internal fun reduceHomeBottomBarListScroll(
    previousState: HomeBottomBarScrollState,
    firstVisibleItem: Int,
    scrollOffset: Int,
    isVideoNavigating: Boolean,
    contentInteractionRestored: Boolean = false,
    topRevealThresholdPx: Int = 100,
    offsetHysteresisPx: Int = 200
): HomeBottomBarScrollUpdate {
    val nextState = HomeBottomBarScrollState(
        firstVisibleItem = firstVisibleItem,
        scrollOffset = scrollOffset
    )
    if (isVideoNavigating && !contentInteractionRestored) {
        return HomeBottomBarScrollUpdate(
            state = nextState,
            visibilityIntent = null
        )
    }

    val intent = when {
        firstVisibleItem == 0 && scrollOffset < topRevealThresholdPx -> BottomBarVisibilityIntent.SHOW
        firstVisibleItem > previousState.firstVisibleItem -> BottomBarVisibilityIntent.HIDE
        firstVisibleItem < previousState.firstVisibleItem -> BottomBarVisibilityIntent.SHOW
        scrollOffset > previousState.scrollOffset + offsetHysteresisPx -> BottomBarVisibilityIntent.HIDE
        scrollOffset < previousState.scrollOffset - offsetHysteresisPx -> BottomBarVisibilityIntent.SHOW
        else -> null
    }

    return HomeBottomBarScrollUpdate(
        state = nextState,
        visibilityIntent = intent
    )
}

internal fun resolveBottomBarChromeScrollOffset(
    firstVisibleItem: Int,
    scrollOffset: Int,
    offscreenOffset: Float = 1000f
): Float {
    return if (firstVisibleItem == 0) {
        scrollOffset.coerceAtLeast(0).toFloat()
    } else {
        offscreenOffset
    }
}
