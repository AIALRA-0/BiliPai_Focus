package com.android.purebilibili.feature.home.components.cards

import com.android.purebilibili.core.ui.AppSpacingTokens

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.transition.LocalVideoCardTransitionBackgroundState
import com.android.purebilibili.core.util.CardPositionManager

/**
 * 源卡信息区在 shell morph 时的 chrome 视觉。
 *
 * 列表真卡与飞行 entry 使用同一 shared-bounds 几何；返回时由 shared transition 自己完成
 * 像素交接，避免额外的整卡 alpha 层造成播放器 Surface 黑帧。
 * 横卡可选择随主进度短距离移动。进度在绘制阶段读取，避免整卡重组。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.videoCardShellReturnChromeAlpha(
    enabled: Boolean,
    bvid: String,
    sourceRoute: String?,
    isReturningFromDetail: Boolean = false,
    isQuickReturnFromDetail: Boolean = false,
    followShellMotion: Boolean = false,
    resolveSourceOwnershipAtDraw: Boolean = false,
): Modifier {
    if (!enabled || bvid.isBlank()) return this
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val bgState = LocalVideoCardTransitionBackgroundState.current
    val followDistancePx = with(LocalDensity.current) { AppSpacingTokens.Small.toPx() }
    val isSharedMorphSourceCard = remember(
        bvid,
        sourceRoute,
        CardPositionManager.lastClickedVideoSourceKey,
    ) {
        isVideoCardSharedReturnTarget(
            bvid = bvid,
            sourceRoute = sourceRoute,
            lastClickedVideoSourceKey = CardPositionManager.lastClickedVideoSourceKey,
        )
    }
    return graphicsLayer {
        val effectiveSharedMorphSourceCard = if (resolveSourceOwnershipAtDraw) {
            isVideoCardSharedReturnTarget(
                bvid = bvid,
                sourceRoute = sourceRoute,
                lastClickedVideoSourceKey = CardPositionManager.lastClickedVideoSourceKey,
            )
        } else {
            isSharedMorphSourceCard
        }
        val phase = bgState.phaseProvider()
        val returnGestureInProgress = bgState.isReturnGestureInProgressProvider() ||
            bgState.isGestureRestoreInProgressProvider()
        val transitionActive = sharedTransitionScope?.isTransitionActive == true
        val progress = bgState.progressProvider()
        val quickReturn = isQuickReturnFromDetail ||
            bgState.isQuickReturnFromDetailProvider()
        val preferWholeCardReturn = bgState.preferWholeCardReturnProvider()
        if (followShellMotion) {
            val frame = resolveHorizontalCardChromeMotionFrame(
                useCardContainerSharedBounds = enabled,
                isSharedMorphSourceCard = effectiveSharedMorphSourceCard,
                isReturningFromDetail = isReturningFromDetail,
                transitionBackgroundPhase = phase,
                isVideoCardReturnGestureInProgress = returnGestureInProgress,
                isSharedTransitionActive = transitionActive,
                transitionBackgroundProgress = progress,
                isQuickReturnFromDetail = quickReturn,
                preferWholeCardReturn = preferWholeCardReturn,
            )
            alpha = frame.alpha
            translationY = if (bgState.motionTierProvider() == MotionTier.Reduced) {
                0f
            } else {
                -followDistancePx * frame.translationProgress
            }
        } else {
            alpha = resolveHomeCardChromeAlphaDuringShellReturnMorph(
                useCardContainerSharedBounds = enabled,
                isSharedMorphSourceCard = effectiveSharedMorphSourceCard,
                isReturningFromDetail = isReturningFromDetail,
                transitionBackgroundPhase = phase,
                isVideoCardReturnGestureInProgress = returnGestureInProgress,
                isSharedTransitionActive = transitionActive,
                transitionBackgroundProgress = progress,
                isQuickReturnFromDetail = quickReturn,
                preferWholeCardReturn = preferWholeCardReturn,
            )
            translationY = 0f
        }
    }
}

/**
 * 保留来源卡封面资源，但让其像素与详情 live surface 使用同一返回交接窗口。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.videoCardShellReturnCoverAlpha(
    enabled: Boolean,
    bvid: String,
    sourceRoute: String?,
    isReturningFromDetail: Boolean = false,
): Modifier {
    if (!enabled || bvid.isBlank()) return this
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val bgState = LocalVideoCardTransitionBackgroundState.current
    val isSharedMorphSourceCard = remember(
        bvid,
        sourceRoute,
        CardPositionManager.lastClickedVideoSourceKey,
    ) {
        isVideoCardSharedReturnTarget(
            bvid = bvid,
            sourceRoute = sourceRoute,
            lastClickedVideoSourceKey = CardPositionManager.lastClickedVideoSourceKey,
        )
    }
    return graphicsLayer {
        alpha = resolveHomeCardReturnSourceVisualAlpha(
            useCardContainerSharedBounds = enabled,
            isSharedMorphSourceCard = isSharedMorphSourceCard,
            isReturningFromDetail = isReturningFromDetail,
            transitionBackgroundPhase = bgState.phaseProvider(),
            isVideoCardReturnGestureInProgress =
                bgState.isReturnGestureInProgressProvider() ||
                    bgState.isGestureRestoreInProgressProvider(),
            isSharedTransitionActive = sharedTransitionScope?.isTransitionActive == true,
            transitionBackgroundProgress = bgState.progressProvider(),
            // 来源封面位于 sharedBounds 飞行层，在最后 82%–98% 把播放器变为封面。
            preferWholeCardReturn = bgState.preferWholeCardReturnProvider(),
        )
    }
}
