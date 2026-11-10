package com.android.purebilibili.navigation3

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.android.purebilibili.core.ui.transition.VideoCardSourceChromeSnapshot
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.util.VideoCardPixelOwner

/**
 * A click-time snapshot of the source card used for the complete detail round trip.
 *
 * Feed state is intentionally not consulted after this object is created. Lists can refresh,
 * reorder or leave composition while a detail page is open; using their mutable click globals
 * at return time can otherwise redirect the whole-card morph to a different card.
 */
internal data class VideoCardTransitionSession(
    val bvid: String,
    val sourceRoute: String?,
    val sourceKey: String?,
    val cardBounds: Rect?,
    val coverBounds: Rect?,
    /**
     * 点击时冻结的**信息面**（标题/统计/UP/日期）矩形，供重建来源卡壳的信息板落位。
     *
     * 与 [cardBounds]/[coverBounds] 同一坐标系（Root），因此三者可以互相做相对运算。
     */
    val infoBounds: Rect? = null,
    val sourceCornerDp: Int?,
    val cardSourceDirection: BiliPaiNavCardSourceDirection,
    val coverIdentity: String?,
    val cardFullyVisible: Boolean,
    val isSingleColumnCard: Boolean,
    val sourceLayout: VideoCardSourceLayout,
    val sourceChromeSnapshot: VideoCardSourceChromeSnapshot?,
    /** Navigation-host origin frozen at click, so the landing rect stays in one coordinate space. */
    val hostOriginInRoot: Offset = Offset.Zero,
    val sourcePixelOwner: VideoCardPixelOwner? = null,
) {
    val sourceInstanceId: Long?
        get() = sourcePixelOwner?.sourceInstanceId

    val hasUsableSourceGeometry: Boolean
        get() = cardBounds != null && cardFullyVisible

    companion object {
        fun create(
            bvid: String,
            source: BiliPaiVideoSource,
            recordedSourceKey: String?,
            cardBounds: Rect?,
            coverBounds: Rect? = null,
            infoBounds: Rect? = null,
            sourceCornerDp: Int?,
            cardSourceDirection: BiliPaiNavCardSourceDirection,
            coverIdentity: String?,
            cardFullyVisible: Boolean,
            isSingleColumnCard: Boolean,
            sourceLayout: VideoCardSourceLayout = VideoCardSourceLayout.COVER_ONLY,
            sourceChromeSnapshot: VideoCardSourceChromeSnapshot? = null,
            hostOriginInRoot: Offset = Offset.Zero,
            sourcePixelOwner: VideoCardPixelOwner? = null,
        ): VideoCardTransitionSession {
            val normalizedBvid = bvid.trim()
            val sourceBvid = source.key
                ?.substringAfterLast(':', missingDelimiterValue = "")
                ?.trim()
            val ownsRecordedGeometry = normalizedBvid.isNotEmpty() &&
                sourceBvid == normalizedBvid &&
                source.key != null && source.key == recordedSourceKey
            return VideoCardTransitionSession(
                bvid = normalizedBvid,
                sourceRoute = normalizeBiliPaiVideoSourceRoute(source.route),
                sourceKey = source.key?.trim()?.takeIf(String::isNotEmpty),
                cardBounds = cardBounds?.takeIf { ownsRecordedGeometry }?.let {
                    Rect(it.left, it.top, it.right, it.bottom)
                },
                coverBounds = coverBounds?.takeIf { ownsRecordedGeometry }?.let {
                    Rect(it.left, it.top, it.right, it.bottom)
                },
                infoBounds = infoBounds?.takeIf { ownsRecordedGeometry }?.let {
                    Rect(it.left, it.top, it.right, it.bottom)
                },
                sourceCornerDp = sourceCornerDp
                    ?.takeIf { ownsRecordedGeometry }
                    ?.coerceAtLeast(0),
                cardSourceDirection = cardSourceDirection.takeIf { ownsRecordedGeometry }
                    ?: BiliPaiNavCardSourceDirection.NONE,
                coverIdentity = coverIdentity?.trim()?.takeIf(String::isNotEmpty),
                cardFullyVisible = ownsRecordedGeometry && cardFullyVisible,
                isSingleColumnCard = isSingleColumnCard,
                sourceLayout = sourceLayout,
                sourceChromeSnapshot = sourceChromeSnapshot.takeIf { ownsRecordedGeometry },
                hostOriginInRoot = hostOriginInRoot,
                sourcePixelOwner = sourcePixelOwner.takeIf {
                    ownsRecordedGeometry && it?.sourceKey == source.key
                },
            )
        }
    }
}
