package com.android.purebilibili.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

/** Local physical regions computed from the measured overlay host, before sizing its surface. */
data class HingeSafeOverlayRegions(
    val dialog: ((IntSize, IntOffset) -> List<IntRect>)? = null,
    val sheet: ((IntSize, IntOffset) -> List<IntRect>)? = null,
)

val LocalHingeSafeOverlayRegions = staticCompositionLocalOf { HingeSafeOverlayRegions() }
