package com.android.purebilibili.core.ui.transition

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale

/**
 * shell sharedBounds 角色。
 *
 * - 进场：首页等大卡由源卡 Exit.None、详情壳 Enter.None，整卡跟手放大。
 * - 返回（首页等大卡）：详情壳 Exit.None 保住实时画面；源卡 Enter 延后淡入，
 *   避免封面一开始盖住直播画面。
 * - 相关/分区横条卡同样由整卡承载 shared bounds，封面与文字作为一个整体移动。
 */
internal enum class VideoCardShellSharedBoundsRole {
    /** 列表源卡片 */
    SourceCard,

    /** 详情壳：整页放大/缩回 */
    DetailShell,
}

/**
 * 视频卡片/详情 sharedBounds 的缩放对齐。
 *
 * Compose 默认 `Alignment.Center` 会让中间态往屏幕中心挤，表现为非首页预测返回
 * 「封面不往顶部走」。普通进详情/回列表一律 [ContentScale.FillWidth] + [Alignment.TopCenter]，
 * 与顶部播放器落点一致；仅竖全屏直达用 Center。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
internal fun resolveVideoCardSharedBoundsResizeMode(
    fillFullscreenShell: Boolean = false,
): SharedTransitionScope.ResizeMode {
    return if (fillFullscreenShell) {
        scaleToBounds(ContentScale.Crop, Alignment.Center)
    } else {
        scaleToBounds(ContentScale.FillWidth, Alignment.TopCenter)
    }
}

/**
 * 详情侧是否还要挂「封面 ↔ 播放器」cover sharedBounds。
 * shell 已接管整卡 morph 时再挂 cover 会与默认 Center 路径抢戏，中间态往屏幕中心飞。
 */
internal fun shouldAttachVideoDetailCoverSharedBounds(
    coverSharedBoundsEnabled: Boolean,
    detailShellSharedBoundsEnabled: Boolean,
    immediatePlayback: Boolean,
    forceCoverOnlyForReturn: Boolean,
): Boolean {
    if (!coverSharedBoundsEnabled) return false
    if (detailShellSharedBoundsEnabled) return false
    if (!immediatePlayback) return false
    if (forceCoverOnlyForReturn) return false
    return true
}

/**
 * 返回时源卡延后淡入的起点（占 morph 总时长比例）。
 * 与 [VIDEO_CARD_RETURN_SOURCE_ENTER_FADE_DELAY_RATIO] 同源；当前为 0。
 */
internal const val VIDEO_CARD_SHELL_SOURCE_ENTER_FADE_DELAY_RATIO =
    VIDEO_CARD_RETURN_SOURCE_ENTER_FADE_DELAY_RATIO

/** 横条卡进场源卡淡出时长（占 morph 总时长比例）。 */
internal const val VIDEO_CARD_SHELL_SOURCE_EXIT_FADE_RATIO = 0.28f

/** 横条卡返回时，源卡内容在最后一段开始接回播放器内容。 */
internal fun resolveVideoCardShellCrossfadeSourceEnterDelayMillis(
    transitionDurationMillis: Int,
): Int {
    val duration = transitionDurationMillis.coerceAtLeast(0)
    val fadeDuration = resolveVideoCardShellSourceExitFadeDurationMillis(duration)
        .coerceAtMost(duration)
    return (duration - fadeDuration).coerceAtLeast(0)
}

/**
 * 源卡 shell 是否延后 Enter。
 * 一律 false：封面待命 + chrome 独立淡入，见 [canCoexistLiveSurfaceStableCoverAndChromeOnReturn]。
 */
internal fun shouldDelaySourceCardEnterForLiveReturnMorph(
    sourceRoute: String?,
    isQuickReturnFromDetail: Boolean = false,
): Boolean {
    @Suppress("UNUSED_PARAMETER")
    val ignored = sourceRoute
    return shouldDelaySourceCardEnterOnReturn(isQuickReturnFromDetail)
}

/** shell 竖卡进场保持 Exit.None。 */
internal fun shouldFadeOutShellSourceCardOnOpen(sourceRoute: String?): Boolean {
    @Suppress("UNUSED_PARAMETER")
    val ignored = sourceRoute
    return false
}

internal fun resolveVideoCardShellSourceEnterFadeDelayMillis(
    transitionDurationMillis: Int,
): Int {
    val duration = transitionDurationMillis.coerceAtLeast(0)
    return (duration * VIDEO_CARD_SHELL_SOURCE_ENTER_FADE_DELAY_RATIO).toInt().coerceIn(0, duration)
}

internal fun resolveVideoCardShellSourceExitFadeDurationMillis(
    transitionDurationMillis: Int,
): Int {
    val duration = transitionDurationMillis.coerceAtLeast(0)
    return (duration * VIDEO_CARD_SHELL_SOURCE_EXIT_FADE_RATIO).toInt().coerceIn(72, duration.coerceAtLeast(72))
}

internal fun shouldCrossfadeVideoCardSourceContentOnReturn(
    requested: Boolean,
    isQuickReturnFromDetail: Boolean,
    preferWholeCardReturn: Boolean,
): Boolean = requested && !isQuickReturnFromDetail && !preferWholeCardReturn

internal fun resolveVideoCardShellSharedBoundsEnter(
    role: VideoCardShellSharedBoundsRole,
    transitionDurationMillis: Int,
    delaySourceCardEnterForLiveReturn: Boolean = true,
    crossfadeSourceContent: Boolean = false,
): EnterTransition {
    if (
        role == VideoCardShellSharedBoundsRole.SourceCard &&
        crossfadeSourceContent
    ) {
        val duration = transitionDurationMillis.coerceAtLeast(0)
        val delay = resolveVideoCardShellCrossfadeSourceEnterDelayMillis(duration)
        return fadeIn(
            animationSpec = tween(
                durationMillis = (duration - delay).coerceAtLeast(0),
                delayMillis = delay,
            ),
        )
    }
    if (
        role == VideoCardShellSharedBoundsRole.SourceCard &&
        delaySourceCardEnterForLiveReturn
    ) {
        val duration = transitionDurationMillis.coerceAtLeast(0)
        val delay = resolveVideoCardShellSourceEnterFadeDelayMillis(duration)
        return fadeIn(
            animationSpec = tween(
                durationMillis = (duration - delay).coerceAtLeast(0),
                delayMillis = delay,
            ),
        )
    }
    return EnterTransition.None
}

internal fun resolveVideoCardShellSharedBoundsExit(
    role: VideoCardShellSharedBoundsRole,
    fadeOutSourceCardOnOpen: Boolean = false,
    transitionDurationMillis: Int = 0,
): ExitTransition {
    if (
        role == VideoCardShellSharedBoundsRole.SourceCard &&
        fadeOutSourceCardOnOpen
    ) {
        return fadeOut(
            animationSpec = tween(
                durationMillis = resolveVideoCardShellSourceExitFadeDurationMillis(
                    transitionDurationMillis,
                ),
            ),
        )
    }
    return ExitTransition.None
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.videoCardShellSharedBoundsOrEmpty(
    enabled: Boolean,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    bvid: String,
    sourceRoute: String?,
    motionSpec: VideoSharedTransitionMotionSpec,
    clipShape: Shape,
    role: VideoCardShellSharedBoundsRole = VideoCardShellSharedBoundsRole.SourceCard,
    /**
     * 详情页顶部播放器：FillWidth + TopCenter。
     * 竖屏直达 Story 全屏：FillBounds + Center，卡片从列表位整卡展开。
     */
    fillFullscreenShell: Boolean = false,
    /**
     * 横卡整壳：打开前段让源卡内容淡出并由详情播放器接管；
     * 返回时在落位末段恢复源卡内容。几何仍只由 shared bounds 驱动。
     */
    crossfadeSourceContent: Boolean = false,
): Modifier {
    if (!enabled || sharedTransitionScope == null || animatedVisibilityScope == null || bvid.isBlank()) {
        return this
    }
    val bgState = LocalVideoCardTransitionBackgroundState.current
    // 快速返回：源卡 Enter.None，标题/UP 与封面同步落位，避免先占位后出字。
    val isQuickReturnFromDetail = bgState.isQuickReturnFromDetailProvider()
    val crossfadeSourceContentOnReturn = shouldCrossfadeVideoCardSourceContentOnReturn(
        requested = crossfadeSourceContent,
        isQuickReturnFromDetail = isQuickReturnFromDetail,
        preferWholeCardReturn = bgState.preferWholeCardReturnProvider(),
    )
    val delaySourceCardEnter = shouldDelaySourceCardEnterForLiveReturnMorph(
        sourceRoute = sourceRoute,
        isQuickReturnFromDetail = isQuickReturnFromDetail,
    )
    val fadeOutSourceOnOpen = remember(sourceRoute, crossfadeSourceContent) {
        crossfadeSourceContent || shouldFadeOutShellSourceCardOnOpen(sourceRoute)
    }
    val enter = remember(
        role,
        motionSpec.durationMillis,
        delaySourceCardEnter,
        crossfadeSourceContentOnReturn,
    ) {
        resolveVideoCardShellSharedBoundsEnter(
            role = role,
            transitionDurationMillis = motionSpec.durationMillis,
            delaySourceCardEnterForLiveReturn = delaySourceCardEnter,
            crossfadeSourceContent = crossfadeSourceContentOnReturn,
        )
    }
    val exit = remember(role, motionSpec.durationMillis, fadeOutSourceOnOpen) {
        resolveVideoCardShellSharedBoundsExit(
            role = role,
            fadeOutSourceCardOnOpen = fadeOutSourceOnOpen,
            transitionDurationMillis = motionSpec.durationMillis,
        )
    }
    val resizeMode = remember(fillFullscreenShell) {
        resolveVideoCardSharedBoundsResizeMode(fillFullscreenShell = fillFullscreenShell)
    }
    return then(
        with(sharedTransitionScope) {
            val sharedContentState = rememberSharedContentState(
                key = videoCardShellSharedElementKey(
                    bvid = bvid,
                    sourceRoute = sourceRoute
                )
            )
            Modifier.sharedBounds(
                sharedContentState = sharedContentState,
                animatedVisibilityScope = animatedVisibilityScope,
                enter = enter,
                exit = exit,
                boundsTransform = { initialBounds, targetBounds ->
                    if (motionSpec.enabled) {
                        // duration/easing 与 VideoCardTransitionTimelineSpec /
                        // 详情 AVS morph clock 强制同源（进 Continuity / 回 Linear）。
                        videoSharedElementBoundsTransformSpec(
                            motion = motionSpec,
                            initialBounds = initialBounds,
                            targetBounds = targetBounds,
                            durationMillis = motionSpec.durationMillis,
                        )
                    } else {
                        com.android.purebilibili.core.ui.motion.AppMotionTokens.spatialSpec()
                    }
                },
                resizeMode = resizeMode,
                clipInOverlayDuringTransition = OverlayClip(clipShape)
            )
        }
    )
}
