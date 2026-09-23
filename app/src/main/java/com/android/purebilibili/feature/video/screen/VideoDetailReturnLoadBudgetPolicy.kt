package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.ui.transition.VideoCardReturnCoverOwnership
import com.android.purebilibili.core.ui.transition.VIDEO_CARD_RETURN_LIVE_CONTENT_YIELD_END
import com.android.purebilibili.core.ui.transition.VideoCardTransitionVisualTimeline

/**
 * Settled 播放态详情 → 列表返回的**运动预算**（纯 Kotlin）。
 *
 * 产品硬门槛：有可绘 live 帧时全程 [VideoDetailReturnPlayerMode.LiveMorph] 一镜到底，
 * 不做 snapshot / 静态帧 / 性能向 forceCover 降级。飞行卡内部形变期间保留弹幕、
 * 次要内容和控制层的 composition，只调整绘制 alpha；不停止实时 surface。
 */

/** 与 live return cover handoff 对齐：末段才允许停播意图。 */
internal const val VIDEO_DETAIL_RETURN_HANDOFF_SETTLE_START =
    VideoCardTransitionVisualTimeline.MEDIA_RETURN_END

internal enum class VideoDetailReturnSessionPhase {
    Idle,
    Commit,
    Morph,
    Handoff,
    Settle,
}

internal enum class VideoDetailReturnPlayerMode {
    /** 实时 surface 跟壳缩（本专项唯一目标路径）。 */
    LiveMorph,

    /** 无可靠 live 帧时的既有兜底，不为性能主动扩大。 */
    ResidentCover,
}

internal enum class VideoDetailReturnSecondaryContentMode {
    Keep,
    /** 保留 composition，靠 alpha/graphicsLayer 让位（壳尺寸稳定）。 */
    Freeze,
    /**
     * 停止绘制次要子树（仍占位时需保证壳高度不变）。
     * 仅在 alpha 已可视为不可见时启用，避免 layout 跳变。
     */
    Detach,
}

internal enum class VideoDetailReturnDanmakuMode {
    Keep,
    PauseHide,
}

internal enum class VideoDetailReturnOverlayControlsMode {
    Keep,
    Suppress,
}

internal enum class VideoDetailReturnDepthBlurMode {
    Full,
    /** 返回段更粗 blur 量化，减少 BlurEffect 更新次数。 */
    QuantizedLite,
    ScrimOnly,
}

internal enum class VideoDetailReturnHomeHeavyWorkMode {
    Allow,
    DeferToSettle,
}

internal data class VideoDetailReturnVisualBudget(
    val phase: VideoDetailReturnSessionPhase,
    val playerMode: VideoDetailReturnPlayerMode,
    val secondaryContentMode: VideoDetailReturnSecondaryContentMode,
    val danmakuMode: VideoDetailReturnDanmakuMode,
    val overlayControlsMode: VideoDetailReturnOverlayControlsMode,
    val depthBlurMode: VideoDetailReturnDepthBlurMode,
    val homeHeavyWorkMode: VideoDetailReturnHomeHeavyWorkMode,
    /** Morph 全程 true；仅 Handoff/Settle 允许发起 pause/释放意图。 */
    val allowPlaybackStopIntent: Boolean,
)

/**
 * 由导航/手势信号解析逻辑相位。
 *
 * - 未提交返回 → Idle（含预测 seek 未松手）
 * - 已提交且 settle 未到 handoff → Commit（刚提交）或 Morph
 * - settle ≥ handoff → Handoff
 * - morph 已结束 → Settle
 */
internal fun resolveVideoDetailReturnSessionPhase(
    isCommittedCardReturn: Boolean,
    isExitTransitionInProgress: Boolean,
    settleProgress: Float,
    handoffSettleStart: Float = VIDEO_DETAIL_RETURN_HANDOFF_SETTLE_START,
): VideoDetailReturnSessionPhase {
    if (!isCommittedCardReturn) {
        return VideoDetailReturnSessionPhase.Idle
    }
    val settle = settleProgress.coerceIn(0f, 1f)
    val handoffStart = handoffSettleStart.coerceIn(0f, 1f)
    if (!isExitTransitionInProgress && settle >= 1f - 1e-3f) {
        return VideoDetailReturnSessionPhase.Settle
    }
    if (settle >= handoffStart) {
        return VideoDetailReturnSessionPhase.Handoff
    }
    // 刚提交、尚未推进 settle：Commit；已在缩回：Morph。
    if (settle <= 1e-3f) {
        return VideoDetailReturnSessionPhase.Commit
    }
    return VideoDetailReturnSessionPhase.Morph
}

/**
 * 有可绘帧 → 强制 LiveMorph；无帧 → ResidentCover（既有兜底）。
 * **禁止**为性能返回其它降级。
 */
internal fun resolveVideoDetailReturnPlayerMode(
    hasRenderableLiveFrame: Boolean,
): VideoDetailReturnPlayerMode {
    return if (hasRenderableLiveFrame) {
        VideoDetailReturnPlayerMode.LiveMorph
    } else {
        VideoDetailReturnPlayerMode.ResidentCover
    }
}

/**
 * 是否允许发起停播/释放意图（非强制立刻 release codec）。
 * LiveMorph 下 Commit/Morph 必须 false，保证一镜到底 surface。
 */
internal fun shouldAllowPlaybackStopIntentForReturnBudget(
    phase: VideoDetailReturnSessionPhase,
    playerMode: VideoDetailReturnPlayerMode,
): Boolean {
    if (playerMode != VideoDetailReturnPlayerMode.LiveMorph) {
        // 封面路径可在提交后停播，避免无效解码。
        return phase != VideoDetailReturnSessionPhase.Idle
    }
    return when (phase) {
        VideoDetailReturnSessionPhase.Handoff,
        VideoDetailReturnSessionPhase.Settle -> true
        VideoDetailReturnSessionPhase.Idle,
        VideoDetailReturnSessionPhase.Commit,
        VideoDetailReturnSessionPhase.Morph -> false
    }
}

internal fun resolveVideoDetailReturnVisualBudget(
    phase: VideoDetailReturnSessionPhase,
    hasRenderableLiveFrame: Boolean,
    reduceMotion: Boolean = false,
    secondaryContentAlpha: Float = 1f,
): VideoDetailReturnVisualBudget {
    val playerMode = resolveVideoDetailReturnPlayerMode(hasRenderableLiveFrame)
    if (phase == VideoDetailReturnSessionPhase.Idle) {
        return VideoDetailReturnVisualBudget(
            phase = phase,
            playerMode = playerMode,
            secondaryContentMode = VideoDetailReturnSecondaryContentMode.Keep,
            danmakuMode = VideoDetailReturnDanmakuMode.Keep,
            overlayControlsMode = VideoDetailReturnOverlayControlsMode.Keep,
            depthBlurMode = if (reduceMotion) {
                VideoDetailReturnDepthBlurMode.ScrimOnly
            } else {
                VideoDetailReturnDepthBlurMode.Full
            },
            homeHeavyWorkMode = VideoDetailReturnHomeHeavyWorkMode.Allow,
            allowPlaybackStopIntent = false,
        )
    }

    val preserveLiveMorphContentTrees =
        playerMode == VideoDetailReturnPlayerMode.LiveMorph &&
            phase != VideoDetailReturnSessionPhase.Settle
    val secondaryMode = if (preserveLiveMorphContentTrees) {
        VideoDetailReturnSecondaryContentMode.Keep
    } else {
        resolveVideoDetailReturnSecondaryContentMode(
            phase = phase,
            secondaryContentAlpha = secondaryContentAlpha,
        )
    }
    val depthMode = when {
        reduceMotion -> VideoDetailReturnDepthBlurMode.ScrimOnly
        phase == VideoDetailReturnSessionPhase.Settle -> VideoDetailReturnDepthBlurMode.Full
        else -> VideoDetailReturnDepthBlurMode.QuantizedLite
    }

    return VideoDetailReturnVisualBudget(
        phase = phase,
        playerMode = playerMode,
        secondaryContentMode = secondaryMode,
        danmakuMode = if (preserveLiveMorphContentTrees) {
            VideoDetailReturnDanmakuMode.Keep
        } else {
            when (phase) {
                VideoDetailReturnSessionPhase.Settle -> VideoDetailReturnDanmakuMode.Keep
                else -> VideoDetailReturnDanmakuMode.PauseHide
            }
        },
        overlayControlsMode = if (preserveLiveMorphContentTrees) {
            VideoDetailReturnOverlayControlsMode.Keep
        } else {
            when (phase) {
                VideoDetailReturnSessionPhase.Settle -> VideoDetailReturnOverlayControlsMode.Keep
                else -> VideoDetailReturnOverlayControlsMode.Suppress
            }
        },
        depthBlurMode = depthMode,
        homeHeavyWorkMode = when (phase) {
            VideoDetailReturnSessionPhase.Settle -> VideoDetailReturnHomeHeavyWorkMode.Allow
            else -> VideoDetailReturnHomeHeavyWorkMode.DeferToSettle
        },
        allowPlaybackStopIntent = shouldAllowPlaybackStopIntentForReturnBudget(
            phase = phase,
            playerMode = playerMode,
        ),
    )
}

/**
 * 无实时帧的封面回退路径：Commit/Morph 优先 Freeze（保壳尺寸）；alpha 已近 0 时可
 * Detach 省 composition。LiveMorph 会在上层预算中直接 Keep，不进入这里。
 */
internal fun resolveVideoDetailReturnSecondaryContentMode(
    phase: VideoDetailReturnSessionPhase,
    secondaryContentAlpha: Float,
    detachAlphaThreshold: Float = 0.02f,
): VideoDetailReturnSecondaryContentMode {
    return when (phase) {
        VideoDetailReturnSessionPhase.Idle,
        VideoDetailReturnSessionPhase.Settle ->
            VideoDetailReturnSecondaryContentMode.Keep
        VideoDetailReturnSessionPhase.Commit,
        VideoDetailReturnSessionPhase.Morph,
        VideoDetailReturnSessionPhase.Handoff -> {
            if (secondaryContentAlpha <= detachAlphaThreshold) {
                VideoDetailReturnSecondaryContentMode.Detach
            } else {
                VideoDetailReturnSecondaryContentMode.Freeze
            }
        }
    }
}

/**
 * Live 返回保留详情与来源两套内容树以完成飞行卡内部形变；无实时帧时才可以提前
 * 卸载次要内容。
 */
internal fun resolveVideoDetailReturnSecondaryContentAlphaPreview(
    isCommittedCardReturn: Boolean,
    hasRenderableLiveFrame: Boolean = false,
): Float = if (isCommittedCardReturn && !hasRenderableLiveFrame) 0f else 1f

/** 返回预算是否要求隐藏/暂停弹幕绘制。 */
internal fun shouldPauseHideDanmakuForReturnBudget(
    budget: VideoDetailReturnVisualBudget,
): Boolean = budget.danmakuMode == VideoDetailReturnDanmakuMode.PauseHide

/** 返回预算是否抑制播放器控制层交互/动画。 */
internal fun shouldSuppressOverlayControlsForReturnBudget(
    budget: VideoDetailReturnVisualBudget,
): Boolean = budget.overlayControlsMode == VideoDetailReturnOverlayControlsMode.Suppress

/** 返回预算是否跳过次要内容子树绘制。 */
internal fun shouldDetachSecondaryContentForReturnBudget(
    budget: VideoDetailReturnVisualBudget,
): Boolean = budget.secondaryContentMode == VideoDetailReturnSecondaryContentMode.Detach

/**
 * 景深 return 段 blur 量化步长（px）。
 * QuantizedLite 用更大步长，减少 GPU effect 更新；Full 保持 1px 细腻。
 */
internal fun resolveVideoDetailReturnDepthBlurQuantumPx(
    depthBlurMode: VideoDetailReturnDepthBlurMode,
    fullQuantumPx: Float = 1f,
    liteQuantumPx: Float = 4f,
): Float {
    return when (depthBlurMode) {
        VideoDetailReturnDepthBlurMode.Full -> fullQuantumPx.coerceAtLeast(0.5f)
        VideoDetailReturnDepthBlurMode.QuantizedLite -> liteQuantumPx.coerceAtLeast(fullQuantumPx)
        VideoDetailReturnDepthBlurMode.ScrimOnly -> Float.MAX_VALUE // 调用方应跳过 blur
    }
}

/**
 * settle 进度：1 - morphDepth（与 return timeline 同语义）。
 * morphDepth 1=详情全屏，0=列表落位。
 */
internal fun resolveVideoDetailReturnSettleProgressFromMorphDepth(
    morphDepthProgress: Float,
): Float = (1f - morphDepthProgress.coerceIn(0f, 1f)).coerceIn(0f, 1f)

/**
 * LiveMorph 预算与 ownership 一致性：有可绘帧时 budget 必须是 LiveMorph。
 */
internal fun shouldExpectLiveSurfaceOwnershipForReturnBudget(
    budget: VideoDetailReturnVisualBudget,
): Boolean = budget.playerMode == VideoDetailReturnPlayerMode.LiveMorph

internal fun mapReturnPlayerModeToCoverOwnershipHint(
    playerMode: VideoDetailReturnPlayerMode,
): VideoCardReturnCoverOwnership {
    return when (playerMode) {
        VideoDetailReturnPlayerMode.LiveMorph ->
            VideoCardReturnCoverOwnership.LIVE_SURFACE
        VideoDetailReturnPlayerMode.ResidentCover ->
            VideoCardReturnCoverOwnership.RESIDENT_COVER
    }
}

/**
 * 次要内容 yield 终点（与 timeline 对齐），供测试与接线引用。
 */
internal val VIDEO_DETAIL_RETURN_SECONDARY_YIELD_END: Float =
    VIDEO_CARD_RETURN_LIVE_CONTENT_YIELD_END


/**
 * shared 卡片返回是否延后停播（保 live surface 跟壳）。
 * 与 [MiniPlayerManager.markLeavingByNavigation] deferPlaybackStop 对齐。
 */
internal fun shouldDeferPlaybackStopForSharedLiveReturn(
    cardTransitionEnabled: Boolean,
    hasSourceRoute: Boolean,
): Boolean = cardTransitionEnabled && hasSourceRoute
