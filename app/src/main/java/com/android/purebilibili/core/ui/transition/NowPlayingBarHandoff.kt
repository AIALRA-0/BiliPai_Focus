package com.android.purebilibili.core.ui.transition

import kotlin.math.roundToInt

/** Matches RoundedCornerShape(percent = 50), including the compact icon-only bar. */
internal fun resolveNowPlayingBarSourceCornerDp(widthPx: Float, heightPx: Float, density: Float): Int {
    if (!widthPx.isFinite() || !heightPx.isFinite() || !density.isFinite() || density <= 0f) return 0
    return (minOf(widthPx, heightPx).coerceAtLeast(0f) / (2f * density)).roundToInt()
}

/**
 * 小横条（听视频 NowPlaying 条）与视频共享过渡的返回交接协议（纯 Kotlin，无 Compose）。
 *
 * 单一握手状态替代原先散装在调用点的四个布尔
 * （isReturningFromDetail / returningDetailBvid / isSharedTransitionRunning /
 * isSharedTransitionSourceOwner）。真条只向协议问一个问题：「此刻显现多少」。
 *
 * 展开方向（点击小横条 → 详情页）的 bounds/快照记录协议由
 * [CardPositionManager.recordVideoCardPosition] 承担，不在本协议内。
 */
sealed interface NowPlayingBarHandoffState {

    /** 无返回会话；真条完全自主。 */
    data object Idle : NowPlayingBarHandoffState

    /**
     * 详情 → 小横条的返回 morph 进行中。
     *
     * @param targetBvid 返回目标视频；null 表示无明确目标（任一来源条都让位）。
     * @param isSourceOwner 共享过渡宿主当前是否持有源像素。
     */
    data class Returning(
        val targetBvid: String?,
        val isSourceOwner: Boolean,
        val morphDepthProvider: () -> Float = { 1f },
    ) : NowPlayingBarHandoffState
}

/**
 * 返回 morph 中真条的显现度：0 = 飞行 chrome 持有源像素（真条隐藏），
 * 1 = 真条自主显现。最后的源 chrome 窗口结束后，按 morph 进度交接到真条，
 * 在出场 entry 被移除之前完成交接，不再等待会话清理或播第二次进场。
 * （唯一几何时间轴原则）；morph 期间几何不收缩，隐藏只发生在 alpha 层。
 */
internal fun resolveNowPlayingBarReturnVisibility(
    handoff: NowPlayingBarHandoffState,
    currentBvid: String,
): Float {
    val returning = handoff as? NowPlayingBarHandoffState.Returning
        ?: return 1f
    if (currentBvid.isBlank()) return 1f
    val isReturnTarget = returning.targetBvid.isNullOrBlank() ||
        returning.targetBvid == currentBvid
    if (!returning.isSourceOwner || !isReturnTarget) return 1f
    return resolveNowPlayingBarLiveHandoff(returning.morphDepthProvider())
}

/** Transfer pixels before the outgoing entry is disposed, using the morph's final window. */
internal fun resolveNowPlayingBarLiveHandoff(morphDepthProgress: Float): Float =
    resolveVideoCardTimelineWindowProgress(
        progress = resolveVideoCardReturnSettleFromMorphDepth(morphDepthProgress),
        start = VideoCardTransitionVisualTimeline.SOURCE_CHROME_RETURN_END,
        end = 1f,
    )

/**
 * 加载中详情等无 live/封面内容的壳填充窗口（settle 0→[SOURCE_SHELL_FILL_REVEAL_END]）。
 *
 * 详情正文还在加载且无封面接管时，飞行壳会长时间是黑壳；冻结快照在返回 settle 的
 * 前段就接管壳内容。旧的 `/0.18f` 字面量收口于此。
 */
internal const val SOURCE_SHELL_FILL_REVEAL_START = 0f
internal const val SOURCE_SHELL_FILL_REVEAL_END = 0.18f

/** 壳填充显现进度（0 刚开始缩回，1 已完成壳填充）。仅用于黑壳兜底场景。 */
internal fun resolveSourceShellFillReveal(
    morphDepthProgress: Float,
): Float = resolveVideoCardTimelineWindowProgress(
    progress = resolveVideoCardReturnSettleFromMorphDepth(morphDepthProgress),
    start = SOURCE_SHELL_FILL_REVEAL_START,
    end = SOURCE_SHELL_FILL_REVEAL_END,
)

/**
 * 小横条冻结 chrome 的显现进度：走标准 SOURCE_CHROME 窗口（settle 0.82→0.98）。
 *
 * 小横条的标题/UP 文字若在飞行壳还接近详情页大小时就显现，会被 inverse scale
 * 放大十几倍绘制成巨字穿帮；壳内由 live surface 或常驻封面填充，chrome 等
 * 壳接近小横条尺寸后再交接，与普通卡片同窗口。
 */
internal fun resolveNowPlayingBarSourceChromeReveal(
    morphDepthProgress: Float,
): Float = resolveVideoCardTimelineWindowProgress(
    progress = resolveVideoCardReturnSettleFromMorphDepth(morphDepthProgress),
    start = VideoCardTransitionVisualTimeline.SOURCE_CHROME_RETURN_START,
    end = VideoCardTransitionVisualTimeline.SOURCE_CHROME_RETURN_END,
)

/** Finish the snapshot's internal layout before exposing its text and controls. */
internal fun resolveNowPlayingBarMediaLayoutHandoff(morphDepthProgress: Float): Float =
    resolveVideoCardTimelineWindowProgress(
        progress = resolveVideoCardReturnSettleFromMorphDepth(morphDepthProgress),
        start = VideoCardTransitionVisualTimeline.MEDIA_LAYOUT_RETURN_START,
        end = VideoCardTransitionVisualTimeline.SOURCE_CHROME_RETURN_START,
    )
