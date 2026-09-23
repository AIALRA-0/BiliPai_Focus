package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackExitDirection
import com.android.purebilibili.navigation3.predictiveback.resolveBiliPaiAutoPredictiveBackExitDirection
import com.android.purebilibili.navigation3.predictiveback.resolveBiliPaiPredictiveBackExitDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class BiliPaiPredictiveBackExitDirectionPolicyTest {

    @Test
    fun autoDerived_sharedElementRoute_followsGesture() {
        val direction = resolveBiliPaiAutoPredictiveBackExitDirection(
            popRouteTransition = BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT,
            cardSourceDirection = BiliPaiNavCardSourceDirection.SOURCE_LEFT,
        )
        assertEquals(BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE, direction)
    }

    @Test
    fun autoDerived_cardFromLeft_exitsRight() {
        val direction = resolveBiliPaiAutoPredictiveBackExitDirection(
            popRouteTransition = BiliPaiNavRouteTransition.CLASSIC_CARD,
            cardSourceDirection = BiliPaiNavCardSourceDirection.SOURCE_LEFT,
        )
        assertEquals(BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT, direction)
    }

    @Test
    fun autoDerived_cardFromRight_exitsLeft() {
        val direction = resolveBiliPaiAutoPredictiveBackExitDirection(
            popRouteTransition = BiliPaiNavRouteTransition.CLASSIC_CARD,
            cardSourceDirection = BiliPaiNavCardSourceDirection.SOURCE_RIGHT,
        )
        assertEquals(BiliPaiPredictiveBackExitDirection.ALWAYS_LEFT, direction)
    }

    @Test
    fun autoDerived_cardDisabled_alwaysExitsRightLikeNativeBack() {
        // 关闭过渡动画后提交 pop 恒向右全宽滑出（安卓原生），预测预览必须同向，
        // 避免「预览右滑、松手提交左滑」的换向撕裂（与卡片列方向无关）。
        assertEquals(
            BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT,
            resolveBiliPaiAutoPredictiveBackExitDirection(
                popRouteTransition = BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT,
                cardSourceDirection = BiliPaiNavCardSourceDirection.SOURCE_LEFT,
            ),
        )
        assertEquals(
            BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT,
            resolveBiliPaiAutoPredictiveBackExitDirection(
                popRouteTransition = BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT,
                cardSourceDirection = BiliPaiNavCardSourceDirection.SOURCE_RIGHT,
            ),
        )
    }

    @Test
    fun storageAuto_usesAutoDerived() {
        val autoDerived = BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT
        assertEquals(
            autoDerived,
            resolveBiliPaiPredictiveBackExitDirection("auto", autoDerived),
        )
    }

    @Test
    fun storageOverride_takesPrecedence() {
        val autoDerived = BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT
        assertEquals(
            BiliPaiPredictiveBackExitDirection.ALWAYS_LEFT,
            resolveBiliPaiPredictiveBackExitDirection("always_left", autoDerived),
        )
        assertEquals(
            BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE,
            resolveBiliPaiPredictiveBackExitDirection("follow_gesture", autoDerived),
        )
    }
}