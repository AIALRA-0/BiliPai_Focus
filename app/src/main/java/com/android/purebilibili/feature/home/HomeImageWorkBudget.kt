package com.android.purebilibili.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** Null outside the home feed: other lists retain their existing page-level policy. */
internal val LocalHomeCardCoverLoadingAllowed = androidx.compose.runtime.compositionLocalOf<Boolean?> { null }

internal const val HOME_CARD_AUXILIARY_WORK_SETTLE_MS = 180L
internal const val HOME_COVER_PRELOAD_SETTLE_MS = 360L

/** Visible covers resume immediately; optional work waits for a quiet page. */
@Composable
internal fun rememberHomeCardAuxiliaryWorkAllowed(
    pageActive: Boolean,
    isScrolling: Boolean,
    visualWorkActive: Boolean = true,
): Boolean {
    val workActive = pageActive && visualWorkActive
    // New motion invalidates readiness synchronously, before effects are cancelled.
    // A subsequent idle period always gets its own cancellable settling window.
    var ready by remember(workActive, isScrolling) { mutableStateOf(false) }
    LaunchedEffect(workActive, isScrolling) {
        if (workActive && !isScrolling) {
            delay(HOME_CARD_AUXILIARY_WORK_SETTLE_MS)
            ready = true
        }
    }
    return workActive && !isScrolling && ready
}

/** Only already-composed neighbours may load, at most one card row on either side. */
internal fun resolveHomeCoverLoadRange(
    firstVisibleVideoIndex: Int?,
    lastVisibleVideoIndex: Int?,
    itemCount: Int,
    columns: Int,
    isDataSaverActive: Boolean,
): IntRange? {
    val first = firstVisibleVideoIndex ?: return null
    val last = lastVisibleVideoIndex ?: return null
    if (itemCount <= 0 || first !in 0 until itemCount || last !in first until itemCount) return null
    val ahead = if (isDataSaverActive) 0 else columns.coerceAtLeast(1)
    return (first - ahead).coerceAtLeast(0)..(last + ahead).coerceAtMost(itemCount - 1)
}
