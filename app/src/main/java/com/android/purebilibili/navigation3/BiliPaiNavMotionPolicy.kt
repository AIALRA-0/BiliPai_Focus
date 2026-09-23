package com.android.purebilibili.navigation3

import com.android.purebilibili.feature.settings.resolveSettingsNavPopTransition
import com.android.purebilibili.navigation.AppSystemBackAction
import com.android.purebilibili.navigation.shouldInterceptSystemBackForAppAction

internal enum class BiliPaiNavMotionMode {
    CARD_DISABLED,
    CLASSIC_CARD
}

internal enum class BiliPaiNavRouteTransition {
    NO_OP_SHARED_ELEMENT,
    REDUCED_MOTION_FADE,
    CARD_DISABLED_VIDEO_FORWARD_FROM_LEFT,
    CARD_DISABLED_VIDEO_FORWARD_FROM_RIGHT,
    CARD_DISABLED_VIDEO_RETURN_TO_LEFT,
    CARD_DISABLED_VIDEO_RETURN_TO_RIGHT,
    SPACE_FORWARD,
    LIGHT_SIBLING_FORWARD,
    LIGHT_SIBLING_POP,
    BOTTOM_BAR_SIBLING_FORWARD,
    BOTTOM_BAR_SIBLING_POP,
    SETTINGS_IOS_PUSH_FORWARD,
    SETTINGS_IOS_PUSH_POP,
    CLASSIC_CARD,
    FALLBACK
}

internal data class BiliPaiNavMotionDecision(
    val mode: BiliPaiNavMotionMode,
    val routeTransition: BiliPaiNavRouteTransition,
    val interceptSystemBack: Boolean
)

internal data class BiliPaiBackGestureDecision(
    val routeTransition: BiliPaiNavRouteTransition,
    val interceptSystemBack: Boolean
)

internal fun resolveBiliPaiNavMotionMode(
    cardTransitionEnabled: Boolean
): BiliPaiNavMotionMode {
    return if (cardTransitionEnabled) {
        BiliPaiNavMotionMode.CLASSIC_CARD
    } else {
        BiliPaiNavMotionMode.CARD_DISABLED
    }
}

internal fun resolveBiliPaiNavMotionDecision(
    fromKey: BiliPaiNavKey?,
    toKey: BiliPaiNavKey?,
    cardTransitionEnabled: Boolean,
    sharedTransitionReady: Boolean,
    appBackActionRequiresInterception: Boolean = false
): BiliPaiNavMotionDecision {
    val mode = resolveBiliPaiNavMotionMode(cardTransitionEnabled = cardTransitionEnabled)
    val isVideoToCardReturn = fromKey is BiliPaiNavKey.VideoDetail &&
        toKey != null &&
        isCardReturnTargetNavKey(toKey)
    val isCardToVideoForward = fromKey != null &&
        isCardReturnTargetNavKey(fromKey) &&
        toKey is BiliPaiNavKey.VideoDetail
    val routeTransition = when {
        cardTransitionEnabled &&
            sharedTransitionReady &&
            (isVideoToCardReturn || isCardToVideoForward) ->
            BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT
        mode == BiliPaiNavMotionMode.CLASSIC_CARD ->
            BiliPaiNavRouteTransition.CLASSIC_CARD
        else -> BiliPaiNavRouteTransition.FALLBACK
    }

    return BiliPaiNavMotionDecision(
        mode = mode,
        routeTransition = routeTransition,
        interceptSystemBack = shouldInterceptSystemBackForNavigation3(
            mode = mode,
            appBackActionRequiresInterception = appBackActionRequiresInterception
        )
    )
}

internal fun resolveBiliPaiBackGestureDecision(
    cardTransitionEnabled: Boolean,
    systemBackAction: AppSystemBackAction,
    currentKey: BiliPaiNavKey?,
    previousKey: BiliPaiNavKey?,
    sourceMetadata: BiliPaiNavSourceMetadata,
    activeMainHostRoute: String? = null,
): BiliPaiBackGestureDecision {
    val motionMode = resolveBiliPaiNavMotionMode(cardTransitionEnabled = cardTransitionEnabled)
    val routeTransition = resolveBiliPaiNavDisplayPopRouteTransition(
        cardTransitionEnabled = cardTransitionEnabled,
        sourceMetadata = sourceMetadata,
        fromKey = currentKey,
        toKey = previousKey,
        activeMainHostRoute = activeMainHostRoute,
    )
    val isAppAction = systemBackAction == AppSystemBackAction.RETURN_TO_HOME_TAB
    return BiliPaiBackGestureDecision(
        routeTransition = if (isAppAction) {
            BiliPaiNavRouteTransition.FALLBACK
        } else {
            routeTransition
        },
        interceptSystemBack = shouldInterceptSystemBackForAppAction(systemBackAction)
    )
}

internal fun shouldBindVideoDetailBackPreviewPlayer(
    currentKey: BiliPaiNavKey?,
    previewKey: BiliPaiNavKey?,
): Boolean {
    if (previewKey !is BiliPaiNavKey.VideoDetail) return false
    if (currentKey !is BiliPaiNavKey.VideoDetail) return true
    // Keep the exact related parent surface attached but its playback session suspended while the
    // child is on top. This preserves the parent's last decoded frame for return without double
    // playback; activating the session remains a separate committed-return decision below.
    return isRelatedVideoDetailReturn(currentKey, previewKey)
}

internal fun shouldActivateVideoDetailPlaybackSession(
    currentKey: BiliPaiNavKey?,
    detailKey: BiliPaiNavKey.VideoDetail,
    isImmediateBackPreview: Boolean,
    activateBackPreviewPlayback: Boolean = false,
): Boolean {
    return currentKey == detailKey ||
        (
            currentKey is BiliPaiNavKey.AudioMode &&
                currentKey.sourceBvid == detailKey.bvid
        ) ||
        (
            isImmediateBackPreview &&
                currentKey is BiliPaiNavKey.VideoDetail &&
                activateBackPreviewPlayback
        )
}

internal fun shouldRecoverVideoPlayerAfterBackCancellation(
    currentKey: BiliPaiNavKey?,
    targetKey: BiliPaiNavKey?
): Boolean {
    return currentKey is BiliPaiNavKey.VideoDetail && targetKey is BiliPaiNavKey.VideoDetail
}

/**
 * 解析 [BiliPaiNavDisplayHost] 全局 `popTransitionSpec` / `predictivePopTransitionSpec` 使用的过渡。
 *
 * 实际生效场景：
 *   - **预测式返回手势**（Android 13+ swipe-back）：entry metadata 不注入 PREDICTIVE_POP_TRANSITION_SPEC，
 *     所以此函数的输出是唯一来源；
 *   - 普通 pop：entry metadata 会注入 POP_TRANSITION_SPEC 并优先生效（见
 *     [resolveBiliPaiNavEntryPopRouteTransition]），此函数仅作兜底。
 *
 * 两条路径需要保持视觉一致——任何对此函数的逻辑修改都应同步检查
 * [resolveBiliPaiNavEntryPopRouteTransition]，反之亦然。
 */
internal fun resolveBiliPaiNavDisplayPopRouteTransition(
    cardTransitionEnabled: Boolean = true,
    sourceMetadata: BiliPaiNavSourceMetadata,
    fromKey: BiliPaiNavKey?,
    toKey: BiliPaiNavKey?,
    activeMainHostRoute: String? = null,
): BiliPaiNavRouteTransition {
    resolveSettingsNavPopTransition(
        fromKey = fromKey,
        toKey = toKey,
        activeMainHostRoute = activeMainHostRoute,
    )?.let { return it }
    val fromVideoKey = fromKey as? BiliPaiNavKey.VideoDetail
    val toIsCardReturnTarget = toKey != null && isCardReturnTargetNavKey(toKey)
    if (cardTransitionEnabled) {
        if (isRelatedVideoDetailReturn(fromVideoKey, toKey)) {
            return BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT
        }
        val sharedReadyFavoriteCollectionReturn =
            fromKey is BiliPaiNavKey.SeasonSeriesDetail &&
                fromKey.sharedElementTransition &&
                (toKey == BiliPaiNavKey.MainHost || toKey == BiliPaiNavKey.Favorite)
        if (sharedReadyFavoriteCollectionReturn) {
            // 与首页一致：预测返回走 sharedBounds 整卡 morph（NO_OP 路由层 + SharedElement handler）。
            return BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT
        }

        // Story 直达返回：没有 sharedBounds 对端，必须走普通过渡，否则黑底悬浮卡。
        if (fromKey is BiliPaiNavKey.Story) {
            return BiliPaiNavRouteTransition.FALLBACK
        }

        val morphSourceRoute = resolveCardMorphDestinationSourceRoute(fromKey)
        val normalizedMorphRoute = morphSourceRoute?.substringBefore("?")
        // VideoDetail.sourceRoute 在 push 时写入 key，完整观看后仍可靠。
        // 不再依赖 CardPosition / sharedTransitionEntryReady：任一过期都会把 pop 打成
        // CLASSIC_CARD fade，表现为「卡片已在原位、没有落位动画」。
        if (toIsCardReturnTarget && !normalizedMorphRoute.isNullOrBlank()) {
            return BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT
        }
        return BiliPaiNavRouteTransition.CLASSIC_CARD
    }
    // 关闭共享元素时：VideoDetail → 任意 card-return-target 一律走方向化横向过渡，
    // 没有源方向信息（单列、居中、未点击源、卡片已滚出视口等）时兜底向右滑出。
    if (fromVideoKey != null && toIsCardReturnTarget) {
        return resolveCardDisabledReturnTransition(sourceMetadata.cardSourceDirection)
    }
    return BiliPaiNavRouteTransition.FALLBACK
}

internal fun isRelatedVideoDetailEntry(
    key: BiliPaiNavKey,
    sourceMetadata: BiliPaiNavSourceMetadata
): Boolean {
    val videoKey = key as? BiliPaiNavKey.VideoDetail ?: return false
    val sourceRoute = sourceMetadata.sourceRoute?.substringBefore("?") ?: return false
    return sourceRoute.startsWith("video/") &&
        videoKey.sourceRoute?.substringBefore("?") == sourceRoute &&
        sourceMetadata.sourceKey == "$sourceRoute:${videoKey.bvid}"
}

internal fun isRelatedVideoDetailReturn(
    fromKey: BiliPaiNavKey.VideoDetail?,
    toKey: BiliPaiNavKey?
): Boolean {
    val targetKey = toKey as? BiliPaiNavKey.VideoDetail ?: return false
    return fromKey?.sourceRoute?.substringBefore("?") == "video/${targetKey.bvid}"
}

internal fun shouldInterceptSystemBackForNavigation3(
    mode: BiliPaiNavMotionMode,
    appBackActionRequiresInterception: Boolean
): Boolean {
    return appBackActionRequiresInterception
}

internal fun resolveCardDisabledReturnTransition(
    sourceDirection: BiliPaiNavCardSourceDirection
): BiliPaiNavRouteTransition {
    return when (sourceDirection) {
        BiliPaiNavCardSourceDirection.SOURCE_LEFT ->
            BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT
        BiliPaiNavCardSourceDirection.SOURCE_RIGHT ->
            BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT
        // Unknown origin: soft sibling pop instead of always forcing right-half exit.
        BiliPaiNavCardSourceDirection.NONE ->
            BiliPaiNavRouteTransition.LIGHT_SIBLING_POP
    }
}
