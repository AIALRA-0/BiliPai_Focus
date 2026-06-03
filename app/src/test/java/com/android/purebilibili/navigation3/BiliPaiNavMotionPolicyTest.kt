package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation.AppSystemBackAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BiliPaiNavMotionPolicyTest {

    @Test
    fun cardTransitionEnabled_usesClassicCardMode() {
        assertEquals(
            BiliPaiNavMotionMode.CLASSIC_CARD,
            resolveBiliPaiNavMotionMode(cardTransitionEnabled = true)
        )
    }

    @Test
    fun cardTransitionDisabled_usesCardDisabledMode() {
        assertEquals(
            BiliPaiNavMotionMode.CARD_DISABLED,
            resolveBiliPaiNavMotionMode(cardTransitionEnabled = false)
        )
    }

    @Test
    fun sharedElementReady_videoReturn_prefersNoOpRouteLayer() {
        val decision = resolveBiliPaiNavMotionDecision(
            fromKey = BiliPaiNavKey.VideoDetail("BV1"),
            toKey = BiliPaiNavKey.Home,
            cardTransitionEnabled = true,
            sharedTransitionReady = true
        )

        assertEquals(BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT, decision.routeTransition)
        assertTrue(decision.interceptSystemBack)
    }

    @Test
    fun disabledCardTransitionWithSharedReadyVideoReturnDoesNotUseNoOpRouteLayer() {
        val decision = resolveBiliPaiNavMotionDecision(
            fromKey = BiliPaiNavKey.VideoDetail("BV1"),
            toKey = BiliPaiNavKey.Home,
            cardTransitionEnabled = false,
            sharedTransitionReady = true
        )

        assertEquals(BiliPaiNavMotionMode.CARD_DISABLED, decision.mode)
        assertEquals(BiliPaiNavRouteTransition.FALLBACK, decision.routeTransition)
        assertFalse(decision.interceptSystemBack)
    }

    @Test
    fun videoSharedReturnUsesClassicAppBack() {
        val decision = resolveBiliPaiBackGestureDecision(
            cardTransitionEnabled = true,
            systemBackAction = AppSystemBackAction.NAVIGATE_UP,
            currentKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "home"),
            previousKey = BiliPaiNavKey.Home,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = true
            )
        )

        assertEquals(BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT, decision.routeTransition)
        assertTrue(decision.interceptSystemBack)
    }

    @Test
    fun staleVideoReturnUsesClassicCardRouteLayer() {
        val decision = resolveBiliPaiBackGestureDecision(
            cardTransitionEnabled = true,
            systemBackAction = AppSystemBackAction.NAVIGATE_UP,
            currentKey = BiliPaiNavKey.VideoDetail("BV2", sourceRoute = "home"),
            previousKey = BiliPaiNavKey.Home,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = true
            )
        )

        assertEquals(BiliPaiNavRouteTransition.CLASSIC_CARD, decision.routeTransition)
        assertTrue(decision.interceptSystemBack)
    }

    @Test
    fun returnToHomeTabAlwaysUsesAppActionBack() {
        val decision = resolveBiliPaiBackGestureDecision(
            cardTransitionEnabled = true,
            systemBackAction = AppSystemBackAction.RETURN_TO_HOME_TAB,
            currentKey = BiliPaiNavKey.MainHost,
            previousKey = null,
            sourceMetadata = BiliPaiNavSourceMetadata()
        )

        assertEquals(BiliPaiNavRouteTransition.FALLBACK, decision.routeTransition)
        assertTrue(decision.interceptSystemBack)
    }

    @Test
    fun navDisplayPop_sharedReadyVideoReturn_keepsRouteLayerNoOp() {
        val transition = resolveBiliPaiNavDisplayPopRouteTransition(
            cardTransitionEnabled = true,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "history:BV1",
                sourceRoute = "history",
                clickedBoundsRecorded = true,
                cardFullyVisible = true
            ),
            fromKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "history"),
            toKey = BiliPaiNavKey.History
        )

        assertEquals(BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT, transition)
    }

    @Test
    fun navDisplayPop_disabledSharedTransition_usesDirectionalReturnFallback() {
        val transition = resolveBiliPaiNavDisplayPopRouteTransition(
            cardTransitionEnabled = false,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = true,
                cardSourceDirection = BiliPaiNavCardSourceDirection.SOURCE_LEFT
            ),
            fromKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "home"),
            toKey = BiliPaiNavKey.Home
        )

        assertEquals(BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT, transition)
    }

    @Test
    fun navDisplayPop_disabledSharedTransition_noDirectionFallsBackToRight() {
        val transition = resolveBiliPaiNavDisplayPopRouteTransition(
            cardTransitionEnabled = false,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = true,
                cardSourceDirection = BiliPaiNavCardSourceDirection.NONE
            ),
            fromKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "home"),
            toKey = BiliPaiNavKey.Home
        )

        assertEquals(BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT, transition)
    }

    @Test
    fun navDisplayPop_disabledSharedTransition_scrolledOutCardStillSlidesHorizontally() {
        // 详情中分 P 切换导致卡片滚出视口 → cardFullyVisible=false，没有源方向。
        // 期望仍走方向化退出（兜底右侧），而不是退化为 fade。
        val transition = resolveBiliPaiNavDisplayPopRouteTransition(
            cardTransitionEnabled = false,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = false,
                cardSourceDirection = BiliPaiNavCardSourceDirection.NONE
            ),
            fromKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "home"),
            toKey = BiliPaiNavKey.Home
        )

        assertEquals(BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT, transition)
    }

    @Test
    fun navDisplayPop_disabledSharedTransition_deepLinkEntryStillSlidesHorizontally() {
        // 深链 / 通知 / 桌面快捷方式进入详情 → clickedBoundsRecorded=false。
        val transition = resolveBiliPaiNavDisplayPopRouteTransition(
            cardTransitionEnabled = false,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = null,
                sourceRoute = null,
                clickedBoundsRecorded = false,
                cardFullyVisible = false,
                cardSourceDirection = BiliPaiNavCardSourceDirection.NONE
            ),
            fromKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "home"),
            toKey = BiliPaiNavKey.Home
        )

        assertEquals(BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT, transition)
    }

    @Test
    fun navDisplayPop_disabledSharedTransition_videoToNonCardTargetUsesFallback() {
        // 详情套详情等非 card-return-target 的 pop 不应被横向覆盖。
        val transition = resolveBiliPaiNavDisplayPopRouteTransition(
            cardTransitionEnabled = false,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = true,
                cardSourceDirection = BiliPaiNavCardSourceDirection.SOURCE_LEFT
            ),
            fromKey = BiliPaiNavKey.VideoDetail("BV2", sourceRoute = "video"),
            toKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "home")
        )

        assertEquals(BiliPaiNavRouteTransition.FALLBACK, transition)
    }

    @Test
    fun plainPopOverridesOnlySharedOrDirectionalVideoReturnTransitions() {
        assertNotNull(
            resolveBiliPaiNavPopContentTransform(BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT)
        )
        assertNotNull(
            resolveBiliPaiNavPopContentTransform(
                BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT
            )
        )
        assertNotNull(
            resolveBiliPaiNavPopContentTransform(
                BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT
            )
        )
        assertNull(
            resolveBiliPaiNavPopContentTransform(BiliPaiNavRouteTransition.FALLBACK)
        )
    }

    @Test
    fun entryPop_videoReturnToRecordedSource_keepsRouteLayerNoOp() {
        val transition = resolveBiliPaiNavEntryPopRouteTransition(
            defaultTransition = BiliPaiNavRouteTransition.FALLBACK,
            fromRoute = "video",
            toRoute = "home",
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = true
            )
        )

        assertEquals(BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT, transition)
    }

    @Test
    fun entryPop_videoReturnToDifferentSource_usesFallbackRouteLayer() {
        val transition = resolveBiliPaiNavEntryPopRouteTransition(
            defaultTransition = BiliPaiNavRouteTransition.FALLBACK,
            fromRoute = "video",
            toRoute = "dynamic",
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = true
            )
        )

        assertEquals(BiliPaiNavRouteTransition.FALLBACK, transition)
    }

    @Test
    fun sharedElementReady_homeVideoForward_prefersNoOpRouteLayer() {
        val decision = resolveBiliPaiNavMotionDecision(
            fromKey = BiliPaiNavKey.Home,
            toKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "home"),
            cardTransitionEnabled = true,
            sharedTransitionReady = true
        )

        assertEquals(BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT, decision.routeTransition)
        assertTrue(decision.interceptSystemBack)
    }

    @Test
    fun classicCardMode_interceptsSystemBackSoAppOwnsBackAction() {
        val decision = resolveBiliPaiNavMotionDecision(
            fromKey = BiliPaiNavKey.VideoDetail("BV1"),
            toKey = BiliPaiNavKey.Home,
            cardTransitionEnabled = true,
            sharedTransitionReady = false
        )

        assertEquals(BiliPaiNavMotionMode.CLASSIC_CARD, decision.mode)
        assertEquals(BiliPaiNavRouteTransition.CLASSIC_CARD, decision.routeTransition)
        assertTrue(decision.interceptSystemBack)
    }
}
