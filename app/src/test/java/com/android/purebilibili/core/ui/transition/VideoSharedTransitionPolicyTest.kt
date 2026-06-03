package com.android.purebilibili.core.ui.transition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoSharedTransitionPolicyTest {

    @Test
    fun coverSharedTransition_enabled_whenTransitionAndScopesAreReady() {
        assertTrue(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = false,
                hasAnimatedVisibilityScope = true
            )
        )
    }

    @Test
    fun metadataSharedTransition_keepsDefaultForNonHomeCallers() {
        assertEquals(VideoSharedTransitionProfile.COVER_AND_METADATA, resolveVideoSharedTransitionProfile())
        assertTrue(
            shouldEnableVideoMetadataSharedTransition(
                coverSharedEnabled = true,
                isQuickReturnLimited = false
            )
        )
    }

    @Test
    fun metadataSharedTransition_staysEnabledWhenQuickReturnLimitedForNonHomeCallers() {
        assertTrue(
            shouldEnableVideoMetadataSharedTransition(
                coverSharedEnabled = true,
                isQuickReturnLimited = true
            )
        )
    }

    @Test
    fun metadataSharedTransition_disabledWhenCardContainerOwnsSharedBounds() {
        assertFalse(
            shouldEnableVideoMetadataSharedTransition(
                coverSharedEnabled = true,
                isQuickReturnLimited = false,
                useCardContainerSharedBounds = true
            )
        )
    }

    @Test
    fun homeVideoTransition_usesCoverAsPrimaryAnchor() {
        val policy = resolveVideoSharedTransitionOwnership(
            sourceRoute = "home",
            coverSharedEnabled = true,
            isQuickReturnLimited = false
        )

        assertTrue(policy.useCoverSharedBounds)
        // Home 源也启用 metadata sharedBounds，标题、UP主等独立过渡
        assertTrue(policy.useMetadataSharedBounds)
    }

    @Test
    fun videoCardShellKey_keepsSourceRouteDistinctFromCoverKey() {
        val shellKey = videoCardShellSharedElementKey(
            bvid = "BV1",
            sourceRoute = "history"
        )
        val coverKey = videoCoverSharedElementKey(
            bvid = "BV1",
            sourceRoute = "history"
        )

        assertEquals(VideoSharedElement.CARD_SHELL, shellKey.element)
        assertEquals("history", shellKey.sourceRoute)
        assertFalse(shellKey == coverKey)
    }

    @Test
    fun nonHomeVideoTransition_keepsMetadataSharedBoundsWhenAvailable() {
        val policy = resolveVideoSharedTransitionOwnership(
            sourceRoute = "search",
            coverSharedEnabled = true,
            isQuickReturnLimited = false
        )

        assertTrue(policy.useCoverSharedBounds)
        assertTrue(policy.useMetadataSharedBounds)
    }

    @Test
    fun videoCardSharedTransitionMotion_usesShortCoverPrimaryTimeline() {
        val motion = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true
        )

        assertTrue(motion.enabled)
        assertEquals(360, motion.durationMillis)
        assertEquals(40, motion.contentDelayMillis)
        assertEquals(220, motion.contentDurationMillis)
        assertEquals(14, motion.contentSlideOffsetDp)
        assertEquals(0.985f, motion.contentInitialScale, 0.0001f)
        assertTrue(motion.easing.transform(0.35f) > 0.7f)
        assertTrue(motion.easing.transform(0.75f) > 0.96f)
    }

    @Test
    fun videoCardSharedTransitionMotion_keepsTimelineForNonHomeSources() {
        val motion = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "search",
            transitionEnabled = true
        )

        assertTrue(motion.enabled)
        assertEquals(360, motion.durationMillis)
    }

    @Test
    fun homeSharedTransitionCornerSpec_softlyConvergesFromCardToPlayer() {
        val corner = resolveHomeVideoSharedTransitionCornerSpec(
            sourceRoute = "home",
            transitionEnabled = true
        )

        assertTrue(corner.enabled)
        assertEquals(16, corner.startCornerDp)
        assertEquals(12, corner.endCornerDp)
    }

    @Test
    fun sharedCoverAspectRatio_defaultsToHomeCardSixteenByTen() {
        assertEquals(1.6f, VIDEO_SHARED_COVER_ASPECT_RATIO, 0.0001f)
    }

    @Test
    fun sharedTransitionVisualSpec_coverFirst_anchorsToInlineCover() {
        val spec = resolveVideoSharedTransitionVisualSpec(
            sourceRoute = "home",
            sourceCornerDp = 12,
            playbackIntent = VideoSharedTransitionPlaybackIntent.CoverFirst,
            fullscreen = false,
            autoPortrait = false,
            initialVertical = false,
            isVerticalVideo = false,
            isReturning = false
        )

        assertEquals(VideoSharedTransitionTargetMode.InlineCover, spec.targetMode)
        assertEquals(12, spec.sourceCornerDp)
        assertEquals(12, spec.targetCornerDp)
        assertFalse(spec.fillTargetViewport)
        assertTrue(spec.useCoverSharedBounds)
        assertFalse(spec.suppressCoverFade)
    }

    @Test
    fun sharedTransitionVisualSpec_coverFirstVertical_usesPortraitViewport() {
        val spec = resolveVideoSharedTransitionVisualSpec(
            sourceRoute = "home",
            sourceCornerDp = 12,
            playbackIntent = VideoSharedTransitionPlaybackIntent.CoverFirst,
            fullscreen = false,
            autoPortrait = true,
            initialVertical = true,
            isVerticalVideo = true,
            isReturning = false
        )

        assertEquals(VideoSharedTransitionTargetMode.PortraitFullscreen, spec.targetMode)
        assertEquals(0, spec.targetCornerDp)
        assertTrue(spec.fillTargetViewport)
        assertTrue(spec.useCoverSharedBounds)
    }

    @Test
    fun sharedTransitionVisualSpec_immediateLandscapeFullscreen_usesSquareViewport() {
        val spec = resolveVideoSharedTransitionVisualSpec(
            sourceRoute = "partition",
            sourceCornerDp = 10,
            playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
            fullscreen = true,
            autoPortrait = false,
            initialVertical = false,
            isVerticalVideo = false,
            isReturning = false
        )

        assertEquals(VideoSharedTransitionTargetMode.LandscapeFullscreen, spec.targetMode)
        assertEquals(10, spec.sourceCornerDp)
        assertEquals(0, spec.targetCornerDp)
        assertTrue(spec.fillTargetViewport)
    }

    @Test
    fun sharedTransitionVisualSpec_portraitRoute_usesPortraitViewport() {
        val spec = resolveVideoSharedTransitionVisualSpec(
            sourceRoute = "home",
            sourceCornerDp = 12,
            playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
            fullscreen = false,
            autoPortrait = true,
            initialVertical = true,
            isVerticalVideo = true,
            isReturning = false
        )

        assertEquals(VideoSharedTransitionTargetMode.PortraitFullscreen, spec.targetMode)
        assertEquals(0, spec.targetCornerDp)
        assertTrue(spec.fillTargetViewport)
    }

    @Test
    fun sharedTransitionVisualSpec_returnConvergesToRecordedCardCorner() {
        val spec = resolveVideoSharedTransitionVisualSpec(
            sourceRoute = "watch_later",
            sourceCornerDp = 8,
            playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
            fullscreen = true,
            autoPortrait = true,
            initialVertical = true,
            isVerticalVideo = true,
            isReturning = true
        )

        assertEquals(VideoSharedTransitionTargetMode.InlineCover, spec.targetMode)
        assertEquals(8, spec.targetCornerDp)
        assertFalse(spec.fillTargetViewport)
        assertTrue(spec.suppressCoverFade)
    }

    @Test
    fun sharedTransitionSourceCorner_mapsKnownNonHomeSources() {
        assertEquals(10, resolveVideoSharedTransitionSourceCornerDp("dynamic", fallbackCornerDp = 12))
        assertEquals(8, resolveVideoSharedTransitionSourceCornerDp("watch_later", fallbackCornerDp = 12))
        assertEquals(12, resolveVideoSharedTransitionSourceCornerDp("history", fallbackCornerDp = 12))
        assertEquals(12, resolveVideoSharedTransitionSourceCornerDp("partition?from=tab", fallbackCornerDp = 12))
    }
}
