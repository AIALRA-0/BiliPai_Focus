package com.android.purebilibili.navigation

import com.android.purebilibili.core.ui.transition.VideoSharedTransitionProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigationTransitionPolicyTest {

    private fun modeFlags(mode: BackRouteMotionMode): Pair<Boolean, Boolean> {
        val predictiveBackAnimationEnabled = mode == BackRouteMotionMode.PREDICTIVE_STABLE
        val cardTransitionEnabled = mode != BackRouteMotionMode.CARD_DISABLED
        return predictiveBackAnimationEnabled to cardTransitionEnabled
    }

    @Test
    fun tabletBackToHomeFromVideo_usesSeamlessTransition() {
        assertTrue(
            shouldUseTabletSeamlessBackTransition(
                isTabletLayout = true,
                cardTransitionEnabled = true,
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun tabletBackToHistoryFromVideo_usesSeamlessTransition() {
        assertTrue(
            shouldUseTabletSeamlessBackTransition(
                isTabletLayout = true,
                cardTransitionEnabled = true,
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.History.route
            )
        )
    }

    @Test
    fun phoneBackToHomeFromVideo_keepsDefaultTransition() {
        assertFalse(
            shouldUseTabletSeamlessBackTransition(
                isTabletLayout = false,
                cardTransitionEnabled = true,
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun tabletBackToHomeWithoutCardTransition_keepsDefaultTransition() {
        assertFalse(
            shouldUseTabletSeamlessBackTransition(
                isTabletLayout = true,
                cardTransitionEnabled = false,
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun tabletBackToHomeFromNonVideoRoute_keepsDefaultTransition() {
        assertFalse(
            shouldUseTabletSeamlessBackTransition(
                isTabletLayout = true,
                cardTransitionEnabled = true,
                fromRoute = ScreenRoutes.Search.route,
                toRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun leavingVideoToHome_shouldStopPlaybackEagerly() {
        assertTrue(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun leavingVideoToAudioMode_shouldNotStopPlaybackEagerly() {
        assertFalse(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.AudioMode.route
            )
        )
    }

    @Test
    fun switchingBetweenVideoRoutes_shouldNotStopPlaybackEagerly() {
        assertFalse(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = VideoRoute.route
            )
        )
    }

    @Test
    fun leavingVideoWithUnknownTargetRoute_shouldNotStopPlaybackEagerly() {
        assertFalse(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = null
            )
        )
    }

    @Test
    fun classicBackMotion_interceptsSystemBackWhenPreviousEntryExists() {
        assertTrue(
            shouldInterceptSystemBackForClassicMotion(
                predictiveBackAnimationEnabled = false,
                hasPreviousBackStackEntry = true
            )
        )
        assertFalse(
            shouldInterceptSystemBackForClassicMotion(
                predictiveBackAnimationEnabled = true,
                hasPreviousBackStackEntry = true
            )
        )
        assertFalse(
            shouldInterceptSystemBackForClassicMotion(
                predictiveBackAnimationEnabled = false,
                hasPreviousBackStackEntry = false
            )
        )
    }

    @Test
    fun nonSharedReturnToHome_leftCard_slidesRightToLeft() {
        assertEquals(
            VideoPopExitDirection.LEFT,
            resolveVideoPopExitDirection(
                targetRoute = ScreenRoutes.Home.route,
                isSingleColumnCard = false,
                lastClickedCardCenterX = 0.2f
            )
        )
    }

    @Test
    fun nonSharedReturnToHome_rightCard_slidesLeftToRight() {
        assertEquals(
            VideoPopExitDirection.RIGHT,
            resolveVideoPopExitDirection(
                targetRoute = ScreenRoutes.Home.route,
                isSingleColumnCard = false,
                lastClickedCardCenterX = 0.8f
            )
        )
    }

    @Test
    fun nonSharedReturnToNonCardRoute_singleColumn_slidesDown() {
        assertEquals(
            VideoPopExitDirection.DOWN,
            resolveVideoPopExitDirection(
                targetRoute = ScreenRoutes.Settings.route,
                isSingleColumnCard = true,
                lastClickedCardCenterX = 0.2f
            )
        )
    }

    @Test
    fun nonSharedReturnToCardList_singleColumn_keepsHorizontalDirectionLikeHome() {
        assertEquals(
            VideoPopExitDirection.LEFT,
            resolveVideoPopExitDirection(
                targetRoute = ScreenRoutes.History.route,
                isSingleColumnCard = true,
                lastClickedCardCenterX = 0.2f
            )
        )
    }

    @Test
    fun cardReturnTargetPolicy_matchesExpectedRoutes() {
        assertTrue(isVideoCardReturnTargetRoute(ScreenRoutes.Home.route))
        assertTrue(isVideoCardReturnTargetRoute(ScreenRoutes.History.route))
        assertTrue(isVideoCardReturnTargetRoute(ScreenRoutes.Favorite.route))
        assertTrue(isVideoCardReturnTargetRoute(ScreenRoutes.WatchLater.route))
        assertTrue(isVideoCardReturnTargetRoute(ScreenRoutes.Search.route))
        assertTrue(isVideoCardReturnTargetRoute(ScreenRoutes.Dynamic.route))
        assertTrue(isVideoCardReturnTargetRoute(ScreenRoutes.DynamicDetail.route))
        assertTrue(isVideoCardReturnTargetRoute(ScreenRoutes.Partition.route))
        assertTrue(isVideoCardReturnTargetRoute(ScreenRoutes.Space.route))
        assertTrue(isVideoCardReturnTargetRoute(ScreenRoutes.SeasonSeriesDetail.route))
        assertTrue(isVideoCardReturnTargetRoute(ScreenRoutes.Category.route))
        assertFalse(isVideoCardReturnTargetRoute(ScreenRoutes.Settings.route))
    }

    @Test
    fun videoToVideoRouteTransition_usesNoOpWhenCardTransitionEnabled() {
        assertTrue(
            shouldUseNoOpRouteTransitionBetweenVideoDetails(
                cardTransitionEnabled = true,
                fromRoute = VideoRoute.route,
                toRoute = VideoRoute.route
            )
        )
    }

    @Test
    fun videoToVideoRouteTransition_disabledWhenCardTransitionDisabledOrRouteMismatch() {
        assertFalse(
            shouldUseNoOpRouteTransitionBetweenVideoDetails(
                cardTransitionEnabled = false,
                fromRoute = VideoRoute.route,
                toRoute = VideoRoute.route
            )
        )
        assertFalse(
            shouldUseNoOpRouteTransitionBetweenVideoDetails(
                cardTransitionEnabled = true,
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun videoPushEnterAction_videoToVideoRoute_usesNoOp() {
        assertEquals(
            VideoPushEnterAction.NO_OP,
            resolveVideoPushEnterAction(
                cardTransitionEnabled = true,
                predictiveBackAnimationEnabled = false,
                fromRoute = VideoRoute.route,
                toRoute = VideoRoute.route,
                sharedTransitionReady = true
            )
        )
    }

    @Test
    fun videoPushEnterAction_classicBackWithSharedReady_usesHeroExpandFade() {
        assertEquals(
            VideoPushEnterAction.HERO_EXPAND_FADE,
            resolveVideoPushEnterAction(
                cardTransitionEnabled = true,
                predictiveBackAnimationEnabled = false,
                fromRoute = ScreenRoutes.Home.route,
                toRoute = VideoRoute.route,
                sharedTransitionReady = true
            )
        )
    }

    @Test
    fun videoPushEnterAction_classicBackWithoutSharedReady_usesSoftFade() {
        assertEquals(
            VideoPushEnterAction.SOFT_FADE,
            resolveVideoPushEnterAction(
                cardTransitionEnabled = true,
                predictiveBackAnimationEnabled = false,
                fromRoute = ScreenRoutes.Home.route,
                toRoute = VideoRoute.route,
                sharedTransitionReady = false
            )
        )
    }

    @Test
    fun videoPushEnterAction_predictiveBack_usesLeftSlide() {
        assertEquals(
            VideoPushEnterAction.LEFT_SLIDE,
            resolveVideoPushEnterAction(
                cardTransitionEnabled = true,
                predictiveBackAnimationEnabled = true,
                fromRoute = ScreenRoutes.Home.route,
                toRoute = VideoRoute.route,
                sharedTransitionReady = true
            )
        )
    }

    @Test
    fun videoPushEnterAction_cardTransitionDisabled_usesLeftSlide() {
        assertEquals(
            VideoPushEnterAction.LEFT_SLIDE,
            resolveVideoPushEnterAction(
                cardTransitionEnabled = false,
                predictiveBackAnimationEnabled = false,
                fromRoute = ScreenRoutes.Home.route,
                toRoute = VideoRoute.route,
                sharedTransitionReady = false
            )
        )
    }

    @Test
    fun disposingVideoDestination_whileStillInVideoRoute_shouldClearReturningState() {
        assertTrue(
            shouldClearReturningStateWhenDisposingVideoDestination(
                stillInVideoRoute = true
            )
        )
    }

    @Test
    fun disposingVideoDestination_toNonVideoRoute_shouldNotForceClearReturningState() {
        assertFalse(
            shouldClearReturningStateWhenDisposingVideoDestination(
                stillInVideoRoute = false
            )
        )
    }

    @Test
    fun quickReturn_nonHomeCardRoute_alignsWithHomePolicy_andDoesNotForceNoOp() {
        assertFalse(
            shouldUseNoOpQuickReturnForNonHomeCardRoute(
                targetRoute = ScreenRoutes.History.route,
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true,
                sharedTransitionReady = true,
                profile = VideoSharedTransitionProfile.COVER_ONLY
            )
        )
    }

    @Test
    fun quickReturn_homeRoute_doesNotForceNoOpViaNonHomePolicy() {
        assertFalse(
            shouldUseNoOpQuickReturnForNonHomeCardRoute(
                targetRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true,
                sharedTransitionReady = true,
                profile = VideoSharedTransitionProfile.COVER_ONLY
            )
        )
    }

    @Test
    fun quickReturn_coverOnlyProfile_usesNoOpWhenSharedTransitionReady() {
        assertTrue(
            shouldUseNoOpRouteTransitionOnQuickReturn(
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true,
                sharedTransitionReady = true,
                profile = VideoSharedTransitionProfile.COVER_ONLY
            )
        )
    }

    @Test
    fun quickReturn_coverOnlyProfile_usesFallbackWhenSharedTransitionNotReady() {
        assertFalse(
            shouldUseNoOpRouteTransitionOnQuickReturn(
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true,
                sharedTransitionReady = false,
                profile = VideoSharedTransitionProfile.COVER_ONLY
            )
        )
    }

    @Test
    fun quickReturn_coverAndMetadataProfile_allowsNoOpRouteTransition() {
        assertTrue(
            shouldUseNoOpRouteTransitionOnQuickReturn(
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true,
                sharedTransitionReady = false,
                profile = VideoSharedTransitionProfile.COVER_AND_METADATA
            )
        )
    }

    @Test
    fun predictiveBack_enabled_withCardTransitionAndSharedReady_disablesOneTakeReturnForStability() {
        assertFalse(
            shouldPreferOneTakeVideoToHomeReturn(
                predictiveBackAnimationEnabled = true,
                cardTransitionEnabled = true,
                sharedTransitionReady = true
            )
        )
    }

    @Test
    fun predictiveBack_disabled_neverPrefersOneTakeReturn() {
        assertFalse(
            shouldPreferOneTakeVideoToHomeReturn(
                predictiveBackAnimationEnabled = false,
                cardTransitionEnabled = true,
                sharedTransitionReady = true
            )
        )
    }

    @Test
    fun resolveBackRouteMotionMode_cardDisabled_returnsCardDisabled() {
        assertEquals(
            BackRouteMotionMode.CARD_DISABLED,
            resolveBackRouteMotionMode(
                predictiveBackAnimationEnabled = false,
                cardTransitionEnabled = false
            )
        )
        assertEquals(
            BackRouteMotionMode.CARD_DISABLED,
            resolveBackRouteMotionMode(
                predictiveBackAnimationEnabled = true,
                cardTransitionEnabled = false
            )
        )
    }

    @Test
    fun resolveBackRouteMotionMode_predictiveEnabledWithCard_returnsPredictiveStable() {
        assertEquals(
            BackRouteMotionMode.PREDICTIVE_STABLE,
            resolveBackRouteMotionMode(
                predictiveBackAnimationEnabled = true,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun resolveBackRouteMotionMode_predictiveDisabledWithCard_returnsClassicCard() {
        assertEquals(
            BackRouteMotionMode.CLASSIC_CARD,
            resolveBackRouteMotionMode(
                predictiveBackAnimationEnabled = false,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun predictiveBack_disabled_withCardTransition_usesClassicBackRouteMotion() {
        assertTrue(
            shouldUseClassicBackRouteMotion(
                resolveBackRouteMotionMode(
                    predictiveBackAnimationEnabled = false,
                    cardTransitionEnabled = true
                )
            )
        )
        assertFalse(
            shouldUseClassicBackRouteMotion(
                resolveBackRouteMotionMode(
                    predictiveBackAnimationEnabled = true,
                    cardTransitionEnabled = true
                )
            )
        )
    }

    @Test
    fun predictiveBack_enabled_withCardTransition_usesPredictiveStableBackRouteMotion() {
        assertTrue(
            shouldUsePredictiveStableBackRouteMotion(
                resolveBackRouteMotionMode(
                    predictiveBackAnimationEnabled = true,
                    cardTransitionEnabled = true
                )
            )
        )
    }

    @Test
    fun predictiveBack_disabled_orCardTransitionDisabled_doesNotUsePredictiveStableBackRouteMotion() {
        assertFalse(
            shouldUsePredictiveStableBackRouteMotion(
                resolveBackRouteMotionMode(
                    predictiveBackAnimationEnabled = false,
                    cardTransitionEnabled = true
                )
            )
        )
        assertFalse(
            shouldUsePredictiveStableBackRouteMotion(
                resolveBackRouteMotionMode(
                    predictiveBackAnimationEnabled = true,
                    cardTransitionEnabled = false
                )
            )
        )
    }

    @Test
    fun predictiveBack_enabled_withCardTransition_usesLinkedSettingsBackMotion() {
        assertTrue(
            shouldUseLinkedSettingsBackMotion(
                resolveBackRouteMotionMode(
                    predictiveBackAnimationEnabled = true,
                    cardTransitionEnabled = true
                )
            )
        )
    }

    @Test
    fun predictiveBack_disabled_forSettingsBackMotion_fallsBackToClassic() {
        assertFalse(
            shouldUseLinkedSettingsBackMotion(
                resolveBackRouteMotionMode(
                    predictiveBackAnimationEnabled = false,
                    cardTransitionEnabled = true
                )
            )
        )
    }

    @Test
    fun cardTransition_disabled_forSettingsBackMotion_staysClassic() {
        assertFalse(
            shouldUseLinkedSettingsBackMotion(
                resolveBackRouteMotionMode(
                    predictiveBackAnimationEnabled = true,
                    cardTransitionEnabled = false
                )
            )
        )
    }

    @Test
    fun returningFromDetailToHome_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                currentRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun notReturningFromDetail_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = false,
                currentRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun returningFromDetailOnNonHomeRoute_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                currentRoute = ScreenRoutes.History.route
            )
        )
    }

    @Test
    fun noOpSharedElement_enabled_whenAllConditionsMet() {
        assertTrue(
            shouldUseNoOpSharedElementRouteTransition(
                cardTransitionEnabled = true,
                sharedTransitionReady = true,
                predictiveBackAnimationEnabled = false
            )
        )
    }

    @Test
    fun noOpSharedElement_disabled_whenCardTransitionDisabled() {
        assertFalse(
            shouldUseNoOpSharedElementRouteTransition(
                cardTransitionEnabled = false,
                sharedTransitionReady = true,
                predictiveBackAnimationEnabled = false
            )
        )
    }

    @Test
    fun noOpSharedElement_disabled_whenSharedTransitionNotReady() {
        assertFalse(
            shouldUseNoOpSharedElementRouteTransition(
                cardTransitionEnabled = true,
                sharedTransitionReady = false,
                predictiveBackAnimationEnabled = false
            )
        )
    }

    @Test
    fun noOpSharedElement_disabled_whenPredictiveBackEnabled() {
        assertFalse(
            shouldUseNoOpSharedElementRouteTransition(
                cardTransitionEnabled = true,
                sharedTransitionReady = true,
                predictiveBackAnimationEnabled = true
            )
        )
    }

    @Test
    fun videoCardReturnEnterAction_nonVideoSource_returnsNull() {
        assertEquals(
            null,
            resolveVideoCardReturnEnterAction(
                fromRoute = ScreenRoutes.Search.route,
                targetRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = true,
                predictiveBackAnimationEnabled = false,
                isQuickReturnFromDetail = false,
                sharedTransitionReady = false,
                isTabletLayout = false,
                allowNoOpSharedElement = false
            )
        )
    }

    @Test
    fun videoCardReturnEnterAction_sharedElementReadyAndAllowed_returnsNoOp() {
        assertEquals(
            VideoCardReturnEnterAction.NO_OP,
            resolveVideoCardReturnEnterAction(
                fromRoute = VideoRoute.route,
                targetRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = true,
                predictiveBackAnimationEnabled = false,
                isQuickReturnFromDetail = false,
                sharedTransitionReady = true,
                isTabletLayout = false,
                allowNoOpSharedElement = true
            )
        )
    }

    @Test
    fun videoCardReturnEnterAction_classicBackMotion_returnsRightSlide() {
        assertEquals(
            VideoCardReturnEnterAction.RIGHT_SLIDE,
            resolveVideoCardReturnEnterAction(
                fromRoute = VideoRoute.route,
                targetRoute = ScreenRoutes.History.route,
                cardTransitionEnabled = true,
                predictiveBackAnimationEnabled = false,
                isQuickReturnFromDetail = false,
                sharedTransitionReady = false,
                isTabletLayout = false,
                allowNoOpSharedElement = false
            )
        )
    }

    @Test
    fun videoCardReturnEnterAction_tabletClassicBack_usesSeamlessFade() {
        assertEquals(
            VideoCardReturnEnterAction.SEAMLESS_FADE,
            resolveVideoCardReturnEnterAction(
                fromRoute = VideoRoute.route,
                targetRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = true,
                predictiveBackAnimationEnabled = false,
                isQuickReturnFromDetail = false,
                sharedTransitionReady = false,
                isTabletLayout = true,
                allowNoOpSharedElement = false
            )
        )
    }

    @Test
    fun videoCardReturnEnterAction_quickReturnCardTargetSharedReady_returnsNoOp() {
        assertEquals(
            VideoCardReturnEnterAction.NO_OP,
            resolveVideoCardReturnEnterAction(
                fromRoute = VideoRoute.route,
                targetRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = true,
                predictiveBackAnimationEnabled = false,
                isQuickReturnFromDetail = true,
                sharedTransitionReady = true,
                isTabletLayout = false,
                allowNoOpSharedElement = false
            )
        )
    }

    @Test
    fun videoCardReturnEnterAction_quickReturnCardTargetSharedNotReady_avoidsClassicRightSlide() {
        val action = resolveVideoCardReturnEnterAction(
            fromRoute = VideoRoute.route,
            targetRoute = ScreenRoutes.Home.route,
            cardTransitionEnabled = true,
            predictiveBackAnimationEnabled = false,
            isQuickReturnFromDetail = true,
            sharedTransitionReady = false,
            isTabletLayout = false,
            allowNoOpSharedElement = false
        )
        assertTrue(
            action == VideoCardReturnEnterAction.SOFT_FADE ||
                action == VideoCardReturnEnterAction.NO_OP
        )
    }

    @Test
    fun videoCardReturnEnterAction_quickReturnWithoutSharedReady_keepsClassicRightSlide() {
        assertEquals(
            VideoCardReturnEnterAction.RIGHT_SLIDE,
            resolveVideoCardReturnEnterAction(
                fromRoute = VideoRoute.route,
                targetRoute = ScreenRoutes.Settings.route,
                cardTransitionEnabled = true,
                predictiveBackAnimationEnabled = false,
                isQuickReturnFromDetail = true,
                sharedTransitionReady = false,
                isTabletLayout = false,
                allowNoOpSharedElement = false
            )
        )
    }

    @Test
    fun videoCardReturnEnterAction_predictiveBackToCardTarget_usesNoOp() {
        assertEquals(
            VideoCardReturnEnterAction.NO_OP,
            resolveVideoCardReturnEnterAction(
                fromRoute = VideoRoute.route,
                targetRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = true,
                predictiveBackAnimationEnabled = true,
                isQuickReturnFromDetail = false,
                sharedTransitionReady = true,
                isTabletLayout = false,
                allowNoOpSharedElement = false
            )
        )
    }

    @Test
    fun videoCardReturnEnterAction_cardDisabled_respectsFallbackAction() {
        assertEquals(
            VideoCardReturnEnterAction.SOFT_FADE,
            resolveVideoCardReturnEnterAction(
                fromRoute = VideoRoute.route,
                targetRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = false,
                predictiveBackAnimationEnabled = false,
                isQuickReturnFromDetail = false,
                sharedTransitionReady = false,
                isTabletLayout = false,
                allowNoOpSharedElement = false,
                noCardTransitionAction = VideoCardReturnEnterAction.SOFT_FADE
            )
        )
    }

    @Test
    fun videoPopExitAction_videoToVideo_returnsNoOp() {
        assertEquals(
            VideoPopExitAction.NO_OP,
            resolveVideoPopExitAction(
                cardTransitionEnabled = true,
                predictiveBackAnimationEnabled = false,
                isTabletLayout = false,
                fromRoute = VideoRoute.route,
                targetRoute = VideoRoute.route,
                isQuickReturnFromDetail = false,
                sharedTransitionReady = false,
                isSingleColumnCard = false,
                lastClickedCardCenterX = 0.6f
            ).action
        )
    }

    @Test
    fun videoPopExitAction_predictiveBackToCardTarget_usesNoOp() {
        assertEquals(
            VideoPopExitAction.NO_OP,
            resolveVideoPopExitAction(
                cardTransitionEnabled = true,
                predictiveBackAnimationEnabled = true,
                isTabletLayout = false,
                fromRoute = VideoRoute.route,
                targetRoute = ScreenRoutes.Home.route,
                isQuickReturnFromDetail = false,
                sharedTransitionReady = true,
                isSingleColumnCard = false,
                lastClickedCardCenterX = 0.4f
            ).action
        )
    }

    @Test
    fun videoPopExitAction_cardTarget_routeByMode_matrix() {
        val expected = mapOf(
            BackRouteMotionMode.CARD_DISABLED to VideoPopExitAction.DIRECTIONAL_SLIDE,
            BackRouteMotionMode.CLASSIC_CARD to VideoPopExitAction.DIRECTIONAL_SLIDE,
            BackRouteMotionMode.PREDICTIVE_STABLE to VideoPopExitAction.NO_OP
        )

        expected.forEach { (mode, expectedAction) ->
            val (predictiveBackAnimationEnabled, cardTransitionEnabled) = modeFlags(mode)
            val decision = resolveVideoPopExitAction(
                cardTransitionEnabled = cardTransitionEnabled,
                predictiveBackAnimationEnabled = predictiveBackAnimationEnabled,
                isTabletLayout = false,
                fromRoute = VideoRoute.route,
                targetRoute = ScreenRoutes.Home.route,
                isQuickReturnFromDetail = false,
                sharedTransitionReady = false,
                isSingleColumnCard = false,
                lastClickedCardCenterX = 0.2f
            )
            assertEquals(expectedAction, decision.action, "mode=$mode")
            if (expectedAction == VideoPopExitAction.DIRECTIONAL_SLIDE) {
                assertEquals(VideoPopExitDirection.LEFT, decision.direction, "mode=$mode")
            }
        }
    }

    @Test
    fun videoPopExitAction_nonCardQuickReturn_routeByMode_matrix() {
        val expected = mapOf(
            BackRouteMotionMode.CARD_DISABLED to VideoPopExitAction.DIRECTIONAL_SLIDE,
            BackRouteMotionMode.CLASSIC_CARD to VideoPopExitAction.SOFT_FADE,
            BackRouteMotionMode.PREDICTIVE_STABLE to VideoPopExitAction.NO_OP
        )

        expected.forEach { (mode, expectedAction) ->
            val (predictiveBackAnimationEnabled, cardTransitionEnabled) = modeFlags(mode)
            val decision = resolveVideoPopExitAction(
                cardTransitionEnabled = cardTransitionEnabled,
                predictiveBackAnimationEnabled = predictiveBackAnimationEnabled,
                isTabletLayout = false,
                fromRoute = VideoRoute.route,
                targetRoute = ScreenRoutes.Settings.route,
                isQuickReturnFromDetail = true,
                sharedTransitionReady = false,
                isSingleColumnCard = false,
                lastClickedCardCenterX = 0.2f,
                profile = VideoSharedTransitionProfile.COVER_ONLY
            )
            assertEquals(expectedAction, decision.action, "mode=$mode")
            if (expectedAction == VideoPopExitAction.DIRECTIONAL_SLIDE) {
                assertEquals(VideoPopExitDirection.LEFT, decision.direction, "mode=$mode")
            }
        }
    }

    @Test
    fun videoPopExitAction_classicBackToCardTarget_usesDirectionalSlide() {
        val decision = resolveVideoPopExitAction(
            cardTransitionEnabled = true,
            predictiveBackAnimationEnabled = false,
            isTabletLayout = false,
            fromRoute = VideoRoute.route,
            targetRoute = ScreenRoutes.Home.route,
            isQuickReturnFromDetail = false,
            sharedTransitionReady = false,
            isSingleColumnCard = false,
            lastClickedCardCenterX = 0.2f
        )
        assertEquals(VideoPopExitAction.DIRECTIONAL_SLIDE, decision.action)
        assertEquals(VideoPopExitDirection.LEFT, decision.direction)
    }

    @Test
    fun videoPopExitAction_quickReturnWithoutSharedReady_usesSoftFade() {
        val decision = resolveVideoPopExitAction(
            cardTransitionEnabled = true,
            predictiveBackAnimationEnabled = false,
            isTabletLayout = false,
            fromRoute = VideoRoute.route,
            targetRoute = ScreenRoutes.Settings.route,
            isQuickReturnFromDetail = true,
            sharedTransitionReady = false,
            isSingleColumnCard = false,
            lastClickedCardCenterX = 0.2f,
            profile = VideoSharedTransitionProfile.COVER_ONLY
        )
        assertEquals(VideoPopExitAction.SOFT_FADE, decision.action)
    }

    @Test
    fun videoPopExitAction_cardDisabled_nonCardTarget_singleColumn_usesDirectionalDown() {
        val decision = resolveVideoPopExitAction(
            cardTransitionEnabled = false,
            predictiveBackAnimationEnabled = false,
            isTabletLayout = false,
            fromRoute = VideoRoute.route,
            targetRoute = ScreenRoutes.Settings.route,
            isQuickReturnFromDetail = false,
            sharedTransitionReady = false,
            isSingleColumnCard = true,
            lastClickedCardCenterX = 0.2f
        )
        assertEquals(VideoPopExitAction.DIRECTIONAL_SLIDE, decision.action)
        assertEquals(VideoPopExitDirection.DOWN, decision.direction)
    }
}
