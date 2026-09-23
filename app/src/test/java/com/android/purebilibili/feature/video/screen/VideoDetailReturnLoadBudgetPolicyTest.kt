package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.ui.transition.VideoCardReturnCoverOwnership
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoDetailReturnLoadBudgetPolicyTest {

    @Test
    fun playerMode_forcesLiveMorphWhenRenderableFrameAvailable() {
        assertEquals(
            VideoDetailReturnPlayerMode.LiveMorph,
            resolveVideoDetailReturnPlayerMode(hasRenderableLiveFrame = true),
        )
        assertEquals(
            VideoDetailReturnPlayerMode.ResidentCover,
            resolveVideoDetailReturnPlayerMode(hasRenderableLiveFrame = false),
        )
    }

    @Test
    fun settledPlayingMorphBudget_neverOffersSnapshotOrForceCover() {
        val budget = resolveVideoDetailReturnVisualBudget(
            phase = VideoDetailReturnSessionPhase.Morph,
            hasRenderableLiveFrame = true,
            secondaryContentAlpha = 0.5f,
        )
        assertEquals(VideoDetailReturnPlayerMode.LiveMorph, budget.playerMode)
        assertEquals(
            VideoCardReturnCoverOwnership.LIVE_SURFACE,
            mapReturnPlayerModeToCoverOwnershipHint(budget.playerMode),
        )
        assertTrue(shouldExpectLiveSurfaceOwnershipForReturnBudget(budget))
        assertFalse(budget.allowPlaybackStopIntent)
        assertEquals(VideoDetailReturnDanmakuMode.Keep, budget.danmakuMode)
        assertEquals(VideoDetailReturnOverlayControlsMode.Keep, budget.overlayControlsMode)
        assertEquals(VideoDetailReturnSecondaryContentMode.Keep, budget.secondaryContentMode)
        assertEquals(VideoDetailReturnDepthBlurMode.QuantizedLite, budget.depthBlurMode)
        assertEquals(VideoDetailReturnHomeHeavyWorkMode.DeferToSettle, budget.homeHeavyWorkMode)
    }

    @Test
    fun liveMorph_blocksPlaybackStopUntilHandoff() {
        assertFalse(
            shouldAllowPlaybackStopIntentForReturnBudget(
                phase = VideoDetailReturnSessionPhase.Commit,
                playerMode = VideoDetailReturnPlayerMode.LiveMorph,
            ),
        )
        assertFalse(
            shouldAllowPlaybackStopIntentForReturnBudget(
                phase = VideoDetailReturnSessionPhase.Morph,
                playerMode = VideoDetailReturnPlayerMode.LiveMorph,
            ),
        )
        assertTrue(
            shouldAllowPlaybackStopIntentForReturnBudget(
                phase = VideoDetailReturnSessionPhase.Handoff,
                playerMode = VideoDetailReturnPlayerMode.LiveMorph,
            ),
        )
        assertTrue(
            shouldAllowPlaybackStopIntentForReturnBudget(
                phase = VideoDetailReturnSessionPhase.Settle,
                playerMode = VideoDetailReturnPlayerMode.LiveMorph,
            ),
        )
    }

    @Test
    fun sessionPhase_mapsCommitMorphHandoffSettle() {
        assertEquals(
            VideoDetailReturnSessionPhase.Idle,
            resolveVideoDetailReturnSessionPhase(
                isCommittedCardReturn = false,
                isExitTransitionInProgress = true,
                settleProgress = 0.2f,
            ),
        )
        assertEquals(
            VideoDetailReturnSessionPhase.Commit,
            resolveVideoDetailReturnSessionPhase(
                isCommittedCardReturn = true,
                isExitTransitionInProgress = true,
                settleProgress = 0f,
            ),
        )
        assertEquals(
            VideoDetailReturnSessionPhase.Morph,
            resolveVideoDetailReturnSessionPhase(
                isCommittedCardReturn = true,
                isExitTransitionInProgress = true,
                settleProgress = 0.4f,
            ),
        )
        assertEquals(
            VideoDetailReturnSessionPhase.Handoff,
            resolveVideoDetailReturnSessionPhase(
                isCommittedCardReturn = true,
                isExitTransitionInProgress = true,
                settleProgress = 0.99f,
            ),
        )
        assertEquals(
            VideoDetailReturnSessionPhase.Settle,
            resolveVideoDetailReturnSessionPhase(
                isCommittedCardReturn = true,
                isExitTransitionInProgress = false,
                settleProgress = 1f,
            ),
        )
    }

    @Test
    fun secondaryContent_detachesOnlyWhenAlphaNearZero() {
        assertEquals(
            VideoDetailReturnSecondaryContentMode.Freeze,
            resolveVideoDetailReturnSecondaryContentMode(
                phase = VideoDetailReturnSessionPhase.Morph,
                secondaryContentAlpha = 0.3f,
            ),
        )
        assertEquals(
            VideoDetailReturnSecondaryContentMode.Detach,
            resolveVideoDetailReturnSecondaryContentMode(
                phase = VideoDetailReturnSessionPhase.Morph,
                secondaryContentAlpha = 0.01f,
            ),
        )
        assertFalse(
            shouldDetachSecondaryContentForReturnBudget(
                resolveVideoDetailReturnVisualBudget(
                    phase = VideoDetailReturnSessionPhase.Handoff,
                    hasRenderableLiveFrame = true,
                    secondaryContentAlpha = 0f,
                ),
            ),
        )
    }

    @Test
    fun committedLiveReturnKeepsBothContentTreesForFlyingCardTransform() {
        val secondaryContentAlpha = resolveVideoDetailReturnSecondaryContentAlphaPreview(
            isCommittedCardReturn = true,
            hasRenderableLiveFrame = true,
        )

        assertEquals(1f, secondaryContentAlpha, 0.0001f)
        assertFalse(
            shouldDetachSecondaryContentForReturnBudget(
                resolveVideoDetailReturnVisualBudget(
                    phase = VideoDetailReturnSessionPhase.Commit,
                    hasRenderableLiveFrame = true,
                    secondaryContentAlpha = secondaryContentAlpha,
                ),
            ),
        )
        assertEquals(
            VideoDetailReturnPlayerMode.LiveMorph,
            resolveVideoDetailReturnVisualBudget(
                phase = VideoDetailReturnSessionPhase.Commit,
                hasRenderableLiveFrame = true,
                secondaryContentAlpha = secondaryContentAlpha,
            ).playerMode,
        )
    }

    @Test
    fun committedResidentReturnMayDetachDetailBodyAfterCoverOwnsTheFrame() {
        val secondaryContentAlpha = resolveVideoDetailReturnSecondaryContentAlphaPreview(
            isCommittedCardReturn = true,
            hasRenderableLiveFrame = false,
        )
        val budget = resolveVideoDetailReturnVisualBudget(
            phase = VideoDetailReturnSessionPhase.Morph,
            hasRenderableLiveFrame = false,
            secondaryContentAlpha = secondaryContentAlpha,
        )

        assertEquals(0f, secondaryContentAlpha, 0.0001f)
        assertEquals(VideoDetailReturnPlayerMode.ResidentCover, budget.playerMode)
        assertTrue(shouldDetachSecondaryContentForReturnBudget(budget))
        assertEquals(VideoDetailReturnDanmakuMode.PauseHide, budget.danmakuMode)
        assertEquals(
            VideoDetailReturnOverlayControlsMode.Suppress,
            budget.overlayControlsMode,
        )
    }

    @Test
    fun uncommittedReturn_keepsDetailContentForPredictiveBackCancellation() {
        assertEquals(
            1f,
            resolveVideoDetailReturnSecondaryContentAlphaPreview(
                isCommittedCardReturn = false,
            ),
            0.0001f,
        )
    }

    @Test
    fun depthBlurQuantum_isCoarserInLiteMode() {
        val full = resolveVideoDetailReturnDepthBlurQuantumPx(
            VideoDetailReturnDepthBlurMode.Full,
        )
        val lite = resolveVideoDetailReturnDepthBlurQuantumPx(
            VideoDetailReturnDepthBlurMode.QuantizedLite,
        )
        assertTrue(lite > full)
        assertEquals(1f, full, 0.001f)
        assertEquals(4f, lite, 0.001f)
    }

    @Test
    fun settleProgress_fromMorphDepth() {
        assertEquals(
            0f,
            resolveVideoDetailReturnSettleProgressFromMorphDepth(1f),
            0.0001f,
        )
        assertEquals(
            1f,
            resolveVideoDetailReturnSettleProgressFromMorphDepth(0f),
            0.0001f,
        )
        assertEquals(
            0.25f,
            resolveVideoDetailReturnSettleProgressFromMorphDepth(0.75f),
            0.0001f,
        )
    }

    @Test
    fun reduceMotion_usesScrimOnlyDepth() {
        val budget = resolveVideoDetailReturnVisualBudget(
            phase = VideoDetailReturnSessionPhase.Morph,
            hasRenderableLiveFrame = true,
            reduceMotion = true,
        )
        assertEquals(VideoDetailReturnDepthBlurMode.ScrimOnly, budget.depthBlurMode)
        assertEquals(VideoDetailReturnPlayerMode.LiveMorph, budget.playerMode)
    }

    @Test
    fun idleBudget_keepsHeavyWorkAndDanmaku() {
        val budget = resolveVideoDetailReturnVisualBudget(
            phase = VideoDetailReturnSessionPhase.Idle,
            hasRenderableLiveFrame = true,
        )
        assertEquals(VideoDetailReturnDanmakuMode.Keep, budget.danmakuMode)
        assertEquals(VideoDetailReturnOverlayControlsMode.Keep, budget.overlayControlsMode)
        assertEquals(VideoDetailReturnHomeHeavyWorkMode.Allow, budget.homeHeavyWorkMode)
        assertFalse(shouldPauseHideDanmakuForReturnBudget(budget))
        assertFalse(shouldSuppressOverlayControlsForReturnBudget(budget))
    }

    @Test
    fun deferPlaybackStop_onlyWhenSharedCardReturn() {
        assertTrue(
            shouldDeferPlaybackStopForSharedLiveReturn(
                cardTransitionEnabled = true,
                hasSourceRoute = true,
            ),
        )
        assertFalse(
            shouldDeferPlaybackStopForSharedLiveReturn(
                cardTransitionEnabled = false,
                hasSourceRoute = true,
            ),
        )
        assertFalse(
            shouldDeferPlaybackStopForSharedLiveReturn(
                cardTransitionEnabled = true,
                hasSourceRoute = false,
            ),
        )
    }
}
