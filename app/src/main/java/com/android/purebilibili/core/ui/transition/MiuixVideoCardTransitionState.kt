package com.android.purebilibili.core.ui.transition

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Rect

/**
 * Miuix Nav card-morph state exposed to video-detail children.
 *
 * Both geometry and depth effects read the same deferred Miuix navigation driver. This keeps
 * secondary content, source-page blur, predictive back, and cancellation on one timeline without
 * restoring the removed AndroidX Navigation3 bridge.
 */
@Immutable
internal data class MiuixVideoCardTransitionState(
    val enabled: Boolean = false,
    val motionSpec: VideoHeroMotionSpec? = null,
    val progressProvider: () -> Float = { 1f },
    val isGestureInProgressProvider: () -> Boolean = { false },
    /**
     * Nav host width matching outer morph (`layoutSize.width`). Landing inverse scale must
     * use this, not [androidx.compose.ui.platform.LocalConfiguration] screen width.
     */
    val layoutWidthProvider: () -> Float = { 1f },
    /**
     * Nav host height matching outer morph (`layoutSize.height`). Inverse Y must use this
     * together with [layoutWidthProvider] or stacked info drifts off the frozen card.
     */
    val layoutHeightProvider: () -> Float = { 1f },
    /** 点击时冻结的整卡落点，供飞行详情壳内构造来源卡内容。 */
    val sourceBoundsProvider: () -> Rect? = { null },
    /** 点击时冻结的真实封面落点；不可由卡宽或固定宽高比推算。 */
    val sourceCoverBoundsProvider: () -> Rect? = { null },
    /**
     * 点击时冻结的**信息面**落点（与 [sourceBoundsProvider] 同坐标系）。
     *
     * 重建来源卡壳的信息板按它落位，避免用「卡底 − 封面底」推导时因卡片测量高度与列表
     * 实际信息面不等价而在卡槽上段露出一块盖不住的近白带。
     */
    val sourceInfoBoundsProvider: () -> Rect? = { null },
    val sourceLayout: VideoCardSourceLayout = VideoCardSourceLayout.COVER_ONLY,
    val sourceChromeSnapshot: VideoCardSourceChromeSnapshot? = null,
)

internal val LocalMiuixVideoCardTransitionState = compositionLocalOf {
    MiuixVideoCardTransitionState()
}
