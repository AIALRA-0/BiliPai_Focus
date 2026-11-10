package com.android.purebilibili.core.ui.transition

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/** Frozen page pixels must not resurrect the clicked card underneath its flying owner. */
internal fun resolveVideoCardSnapshotSourceExclusionBounds(
    sourceBounds: Rect?,
    canvasOriginInRoot: Offset,
    canvasSize: Size,
): Rect? {
    val source = sourceBounds?.takeIf { it.width > 1f && it.height > 1f } ?: return null
    val local = source.translate(-canvasOriginInRoot)
        .intersect(Rect(Offset.Zero, canvasSize))
    return local.takeIf { it.width > 0f && it.height > 0f }
}
