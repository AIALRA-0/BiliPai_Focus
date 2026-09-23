package com.android.purebilibili.navigation3.predictiveback

import com.android.purebilibili.navigation3.BiliPaiNavCardSourceDirection
import com.android.purebilibili.navigation3.BiliPaiNavRouteTransition

internal fun resolveBiliPaiAutoPredictiveBackExitDirection(
    popRouteTransition: BiliPaiNavRouteTransition,
    cardSourceDirection: BiliPaiNavCardSourceDirection,
): BiliPaiPredictiveBackExitDirection {
    if (popRouteTransition == BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT) {
        return BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE
    }
    // 关闭「过渡动画」后，提交阶段已是无动画切换；预测返回仍按安卓原生返回
    // 方向向右跟手，避免预览方向受历史卡片列位置影响。
    if (
        popRouteTransition == BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT ||
        popRouteTransition == BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT
    ) {
        return BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT
    }
    return when (cardSourceDirection) {
        BiliPaiNavCardSourceDirection.SOURCE_LEFT -> BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT
        BiliPaiNavCardSourceDirection.SOURCE_RIGHT -> BiliPaiPredictiveBackExitDirection.ALWAYS_LEFT
        BiliPaiNavCardSourceDirection.NONE -> BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE
    }
}

internal fun resolveBiliPaiPredictiveBackExitDirection(
    storageValue: String?,
    autoDerived: BiliPaiPredictiveBackExitDirection,
): BiliPaiPredictiveBackExitDirection {
    return when (storageValue) {
        "follow_gesture" -> BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE
        "always_right" -> BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT
        "always_left" -> BiliPaiPredictiveBackExitDirection.ALWAYS_LEFT
        else -> autoDerived
    }
}
