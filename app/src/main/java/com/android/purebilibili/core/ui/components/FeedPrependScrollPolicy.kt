package com.android.purebilibili.core.ui.components

internal data class FeedVisibleAnchor(
    val key: Any,
    val index: Int,
    val offsetY: Int,
    val height: Int,
)

internal data class FeedPrependScrollTarget(
    val index: Int,
    val scrollOffset: Int,
)

/** Maps a visible content card into the refreshed grid, ignoring chrome and divider anchors. */
internal fun resolveFeedPrependScrollTarget(
    itemKeys: List<String>,
    dividerIndex: Int,
    leadingItemCount: Int,
    visibleItems: List<FeedVisibleAnchor>,
    viewportStartOffset: Int = 0,
): FeedPrependScrollTarget? {
    if (dividerIndex !in 1 until itemKeys.size) return null
    val indicesByKey = itemKeys.withIndex().associate { it.value to it.index }
    val anchor = visibleItems.firstOrNull {
        it.key in indicesByKey && it.offsetY + it.height > viewportStartOffset
    } ?: visibleItems.firstOrNull { it.key in indicesByKey } ?: return null
    val contentIndex = indicesByKey[anchor.key] ?: return null
    val gridIndex = leadingItemCount + contentIndex + if (contentIndex >= dividerIndex) 1 else 0
    // Already measured/restored, ordinary pagination, and replacements must not move the viewport.
    if (gridIndex <= anchor.index) return null
    return FeedPrependScrollTarget(index = gridIndex, scrollOffset = -anchor.offsetY)
}
