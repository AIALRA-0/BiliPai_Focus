package com.android.purebilibili.feature.video.ui.overlay

internal data class PiliEpisodeNavigationEntry(
    val bvid: String,
    val cid: Long,
    val pageIndex: Int? = null,
)

/** Traverse the current video's parts before crossing into its external queue or collection. */
// A non-null external playlist owns video boundaries, including when its contents are empty.
internal fun resolvePiliEpisodeNavigation(
    pages: List<PiliEpisodeNavigationEntry>,
    outer: List<PiliEpisodeNavigationEntry>,
    related: List<PiliEpisodeNavigationEntry>,
    externalPlaylist: List<PiliEpisodeNavigationEntry>?,
    currentBvid: String,
    currentCid: Long,
    forward: Boolean,
    reversePages: Boolean,
    reverseOuter: Boolean,
    loop: Boolean,
    autoContinue: Boolean,
): PiliEpisodeNavigationEntry? {
    val direction = if (forward) 1 else -1
    fun candidate(items: List<PiliEpisodeNavigationEntry>, index: Int, step: Int, wrap: Boolean): PiliEpisodeNavigationEntry? {
        if (index !in items.indices) return null
        val next = index + step
        val target = when {
            next in items.indices -> items[next]
            wrap && items.isNotEmpty() -> items[Math.floorMod(next, items.size)]
            else -> null
        }
        return target?.takeUnless { it.bvid == currentBvid && it.cid == currentCid }
    }
    val pageIndex = pages.indexOfFirst { it.bvid == currentBvid && it.cid == currentCid }
    candidate(
        pages, pageIndex, direction * if (reversePages) -1 else 1,
        wrap = loop && externalPlaylist == null && outer.isEmpty(),
    )?.let { return it }
    if (externalPlaylist != null) {
        val exactIndex = externalPlaylist.indexOfFirst { it.bvid == currentBvid && it.cid == currentCid }
        val index = if (exactIndex >= 0) exactIndex else externalPlaylist.indexOfFirst { it.bvid == currentBvid }
        return candidate(externalPlaylist, index, direction, wrap = loop)
    }
    val exactOuter = outer.indexOfFirst { it.bvid == currentBvid && it.cid == currentCid }
    val outerIndex = if (exactOuter >= 0) exactOuter else outer.indexOfFirst { it.bvid == currentBvid }
    candidate(outer, outerIndex, direction * if (reverseOuter) -1 else 1, wrap = loop)?.let { return it }
    return if (forward && autoContinue) related.firstOrNull { it.bvid != currentBvid } else null
}
