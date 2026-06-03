package com.android.purebilibili.navigation3

internal enum class BiliPaiNavCardSourceDirection {
    NONE,
    SOURCE_LEFT,
    SOURCE_RIGHT
}

internal data class BiliPaiNavSourceMetadata(
    val sourceKey: String? = null,
    val sourceRoute: String? = null,
    val clickedBoundsRecorded: Boolean = false,
    val cardFullyVisible: Boolean = false,
    val cardSourceDirection: BiliPaiNavCardSourceDirection = BiliPaiNavCardSourceDirection.NONE
) {
    val sharedTransitionReady: Boolean
        get() = clickedBoundsRecorded && cardFullyVisible
}

internal fun resolveBiliPaiNavCardSourceDirection(
    clickedBoundsRecorded: Boolean,
    cardFullyVisible: Boolean,
    isSingleColumnCard: Boolean,
    normalizedCenterX: Float?
): BiliPaiNavCardSourceDirection {
    if (!clickedBoundsRecorded || !cardFullyVisible || isSingleColumnCard) {
        return BiliPaiNavCardSourceDirection.NONE
    }
    val centerX = normalizedCenterX ?: return BiliPaiNavCardSourceDirection.NONE
    return when {
        centerX < 0.4f -> BiliPaiNavCardSourceDirection.SOURCE_LEFT
        centerX > 0.6f -> BiliPaiNavCardSourceDirection.SOURCE_RIGHT
        else -> BiliPaiNavCardSourceDirection.NONE
    }
}

internal fun resolveBiliPaiNavSourceMetadata(
    sourceKey: String? = null,
    sourceRoute: String? = null,
    clickedBoundsRecorded: Boolean,
    cardFullyVisible: Boolean,
    cardSourceDirection: BiliPaiNavCardSourceDirection = BiliPaiNavCardSourceDirection.NONE
): BiliPaiNavSourceMetadata {
    return BiliPaiNavSourceMetadata(
        sourceKey = sourceKey,
        sourceRoute = sourceRoute?.substringBefore("?"),
        clickedBoundsRecorded = clickedBoundsRecorded,
        cardFullyVisible = cardFullyVisible,
        cardSourceDirection = cardSourceDirection
    )
}
