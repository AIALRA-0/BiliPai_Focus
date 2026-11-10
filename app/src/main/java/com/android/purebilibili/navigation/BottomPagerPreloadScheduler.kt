package com.android.purebilibili.navigation

import com.android.purebilibili.feature.home.components.BottomNavItem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield

/** The caller cancels this work whenever navigation, card motion or feed scrolling begins. */
internal suspend fun preloadBottomPagerPages(
    visibleItems: List<BottomNavItem>,
    preloadedItems: () -> Set<BottomNavItem>,
    awaitFrame: suspend () -> Unit,
    awaitIdle: (suspend () -> Unit)? = null,
    isPreloadAllowed: () -> Boolean = { true },
    onPreload: (BottomNavItem) -> Unit,
) {
    if (!isPreloadAllowed()) return
    delay(BOTTOM_PAGER_PRELOAD_IDLE_MILLIS)
    while (nextBottomPagerPreloadItem(visibleItems, preloadedItems()) != null) {
        if (!isPreloadAllowed()) return
        awaitFrame()
        // A frame callback is not an idle budget: it runs before that frame's UI work.
        // Keep the default suspension in this function's state machine. Kotlin 2.4
        // fails IR lowering for the default suspend lambda containing yield().
        if (awaitIdle != null) awaitIdle() else yield()
        currentCoroutineContext().ensureActive()
        // Input may arrive before LaunchedEffect gets a chance to cancel this coroutine.
        if (!isPreloadAllowed()) return
        // The user may have visited a tab while waiting for the frame; sample again.
        val item = nextBottomPagerPreloadItem(visibleItems, preloadedItems()) ?: break
        onPreload(item)
        delay(BOTTOM_PAGER_PRELOAD_INTERVAL_MILLIS)
    }
}
