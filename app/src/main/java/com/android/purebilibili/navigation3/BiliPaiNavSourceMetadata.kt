package com.android.purebilibili.navigation3

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import com.android.purebilibili.core.ui.transition.VideoCardSourceChromeSnapshot
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout

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
    val cardSourceDirection: BiliPaiNavCardSourceDirection = BiliPaiNavCardSourceDirection.NONE,
    val sourceCornerDp: Int? = null,
    val coverIdentity: String? = null,
    val sourceBounds: Rect? = null,
    val sourceCoverBounds: Rect? = null,
    val sourceLayout: VideoCardSourceLayout = VideoCardSourceLayout.COVER_ONLY,
    val sourceChromeSnapshot: VideoCardSourceChromeSnapshot? = null,
) {
    val sharedTransitionEntryReady: Boolean
        get() = clickedBoundsRecorded

    val sharedTransitionReady: Boolean
        get() = clickedBoundsRecorded && cardFullyVisible
}

/** Convert click geometry captured in the app root into the navigation host's local space. */
internal fun BiliPaiNavSourceMetadata.relativeToHost(originInRoot: Offset): BiliPaiNavSourceMetadata {
    if (originInRoot == Offset.Zero) return this
    fun Rect.toHostRect() = translate(Offset(-originInRoot.x, -originInRoot.y))
    return copy(
        sourceBounds = sourceBounds?.toHostRect(),
        sourceCoverBounds = sourceCoverBounds?.toHostRect(),
    )
}

/**
 * Resolve left/right origin of a dual-column video card.
 *
 * Visibility is intentionally not required: when card morph is disabled we still need a
 * reliable left/right exit direction even if the card sits under the top chrome.
 * Single-column (story) cards stay undirected so they keep vertical motion semantics.
 *
 * [cardFullyVisible] remains in the signature for call-site compatibility with shared-element
 * gates; direction itself only needs a recorded dual-column center X.
 *
 * [clickedBoundsRecorded] here means "we have geometry for this click" — not full source-key
 * equality. Prefer bounds + center X over strict key matching so card-disabled motion works.
 */
@Suppress("UNUSED_PARAMETER")
internal fun resolveBiliPaiNavCardSourceDirection(
    clickedBoundsRecorded: Boolean,
    cardFullyVisible: Boolean,
    isSingleColumnCard: Boolean,
    normalizedCenterX: Float?
): BiliPaiNavCardSourceDirection {
    if (!clickedBoundsRecorded || isSingleColumnCard) {
        return BiliPaiNavCardSourceDirection.NONE
    }
    val centerX = normalizedCenterX ?: return BiliPaiNavCardSourceDirection.NONE
    // Dual-column feed: left column ~0.25, right ~0.75. Mid-screen split keeps both sides directed.
    return if (centerX < 0.5f) {
        BiliPaiNavCardSourceDirection.SOURCE_LEFT
    } else {
        BiliPaiNavCardSourceDirection.SOURCE_RIGHT
    }
}

/**
 * Prefer live geometry direction; fall back to the direction frozen at detail enter.
 */
internal fun resolveEffectiveCardSourceDirection(
    liveDirection: BiliPaiNavCardSourceDirection,
    sessionDirection: BiliPaiNavCardSourceDirection
): BiliPaiNavCardSourceDirection {
    return when {
        liveDirection != BiliPaiNavCardSourceDirection.NONE -> liveDirection
        sessionDirection != BiliPaiNavCardSourceDirection.NONE -> sessionDirection
        else -> BiliPaiNavCardSourceDirection.NONE
    }
}

/**
 * Loose video source key match: exact equality or same bvid suffix after the last colon.
 * Avoids home?category=… vs home:BV mismatches that drop directional motion.
 */
internal fun areVideoSourceKeysCompatible(
    cardKey: String?,
    sessionKey: String?
): Boolean {
    if (cardKey.isNullOrBlank() || sessionKey.isNullOrBlank()) return false
    if (cardKey == sessionKey) return true
    val cardBvid = cardKey.substringAfterLast(':', missingDelimiterValue = "")
    val sessionBvid = sessionKey.substringAfterLast(':', missingDelimiterValue = "")
    return cardBvid.isNotBlank() && cardBvid == sessionBvid
}

internal fun resolveBiliPaiNavSourceMetadata(
    sourceKey: String? = null,
    sourceRoute: String? = null,
    clickedBoundsRecorded: Boolean,
    cardFullyVisible: Boolean,
    cardSourceDirection: BiliPaiNavCardSourceDirection = BiliPaiNavCardSourceDirection.NONE,
    sourceCornerDp: Int? = null,
    coverIdentity: String? = null,
    sourceBounds: Rect? = null,
    sourceCoverBounds: Rect? = null,
    sourceLayout: VideoCardSourceLayout = VideoCardSourceLayout.COVER_ONLY,
    sourceChromeSnapshot: VideoCardSourceChromeSnapshot? = null,
): BiliPaiNavSourceMetadata {
    return BiliPaiNavSourceMetadata(
        sourceKey = sourceKey,
        sourceRoute = normalizeBiliPaiVideoSourceRoute(sourceRoute),
        clickedBoundsRecorded = clickedBoundsRecorded,
        cardFullyVisible = cardFullyVisible,
        cardSourceDirection = cardSourceDirection,
        sourceCornerDp = sourceCornerDp?.coerceAtLeast(0),
        coverIdentity = coverIdentity?.trim()?.takeIf(String::isNotEmpty),
        sourceBounds = sourceBounds,
        sourceCoverBounds = sourceCoverBounds,
        sourceLayout = sourceLayout,
        sourceChromeSnapshot = sourceChromeSnapshot,
    )
}
