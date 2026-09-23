package com.android.purebilibili.core.plugin.feed

private val allocatedCoverRatios = floatArrayOf(0.75f, 0.8f, 1f)

/** 封面进列表前就定好宽高比，图片解码后不再改格子。 */
fun allocateCoverAspectRatio(imageUrl: String?): Float {
    if (imageUrl.isNullOrBlank()) return 0.75f
    val bucket = imageUrl.hashCode().ushr(1) % allocatedCoverRatios.size
    return allocatedCoverRatios[bucket]
}

/**
 * 已经滑离顶部时，保持屏幕上的顺序，新到的条目只接在末尾。
 * 停在顶部时才按发布时间重排。
 */
fun stabilizeFeedOrder(
    previous: List<ParsedFeedItem>,
    latest: List<ParsedFeedItem>,
    preserveVisibleOrder: Boolean,
): List<ParsedFeedItem> {
    if (!preserveVisibleOrder || previous.isEmpty()) return latest
    val latestByKey = latest.associateBy { it.layoutKey() }
    val kept = previous.mapNotNull { latestByKey[it.layoutKey()] }
    val keptKeys = kept.map { it.layoutKey() }.toSet()
    return kept + latest.filter { it.layoutKey() !in keptKeys }
}

internal fun ParsedFeedItem.withAllocatedCover(): ParsedFeedItem {
    return copy(coverAspectRatio = allocateCoverAspectRatio(imageUrl))
}

private fun ParsedFeedItem.layoutKey(): String = "$sourceId:$id"
