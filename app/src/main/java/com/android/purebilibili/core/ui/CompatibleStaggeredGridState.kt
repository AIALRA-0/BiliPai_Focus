@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.android.purebilibili.core.ui

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Non-home callers retain the conservative prefetch-off policy for issue #880.
 * Home opts back into normal line prefetch together with ComposeDetachedOwnerGuard in :app.
 * Keep the independent cache-window lane-index workaround in PureApplication.
 */
internal fun createCompatibleStaggeredGridState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    prefetchEnabled: Boolean = false,
): LazyStaggeredGridState = LazyStaggeredGridState(
    initialFirstVisibleItemIndex,
    initialFirstVisibleItemScrollOffset,
).withOffscreenPrefetch(prefetchEnabled)

private fun LazyStaggeredGridState.withOffscreenPrefetch(enabled: Boolean): LazyStaggeredGridState = apply {
    prefetchingEnabled = enabled
}

/** Delegate lane indices and offsets to Foundation's saver, including restored states. */
private fun staggeredGridStateSaver(prefetchEnabled: Boolean): Saver<LazyStaggeredGridState, Any> = Saver(
    save = { state -> with(LazyStaggeredGridState.Saver) { save(state) } },
    restore = { saved ->
        LazyStaggeredGridState.Saver.restore(saved)?.withOffscreenPrefetch(prefetchEnabled)
    },
)

internal val CompatibleStaggeredGridStateSaver = staggeredGridStateSaver(prefetchEnabled = false)
internal val HomeStaggeredGridStateSaver = staggeredGridStateSaver(prefetchEnabled = true)

internal fun createHomeStaggeredGridState(): LazyStaggeredGridState =
    createCompatibleStaggeredGridState(prefetchEnabled = true)

@Composable
internal fun rememberHomeStaggeredGridState(): LazyStaggeredGridState =
    rememberSaveable(saver = HomeStaggeredGridStateSaver) { createHomeStaggeredGridState() }

@Composable
internal fun rememberCompatibleStaggeredGridState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): LazyStaggeredGridState = rememberSaveable(saver = CompatibleStaggeredGridStateSaver) {
    createCompatibleStaggeredGridState(
        initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset,
    )
}
