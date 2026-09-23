package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.store.PortraitPlayerCollapseMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitDetailPresentationPolicyTest {

    @Test
    fun portraitFullscreenPlayback_reusesSharedPlayerForSeamlessSurfaceHandoff() {
        assertTrue(shouldUseSharedPlayerForPortraitFullscreen())
    }

    @Test
    fun officialInlinePortraitMode_enabledForPhoneVerticalVideo() {
        assertTrue(
            shouldUseOfficialInlinePortraitDetailExperience(
                useTabletLayout = false,
                isVerticalVideo = true,
                portraitExperienceEnabled = true
            )
        )
    }

    @Test
    fun officialInlinePortraitMode_disabledForDirectPortraitEntry() {
        assertFalse(
            shouldUseOfficialInlinePortraitDetailExperience(
                useTabletLayout = false,
                isVerticalVideo = true,
                portraitExperienceEnabled = true,
                directPortraitEntry = true
            )
        )
    }

    @Test
    fun officialInlinePortraitMode_disabledForTabletLayout() {
        assertFalse(
            shouldUseOfficialInlinePortraitDetailExperience(
                useTabletLayout = true,
                isVerticalVideo = true,
                portraitExperienceEnabled = true
            )
        )
    }

    @Test
    fun standalonePortraitPager_alwaysEntersDirectlyWithoutCenteredCrossfade() {
        assertFalse(
            shouldAnimateStandalonePortraitPager(
                useSharedPlayer = true,
                directPortraitEntry = true
            )
        )
        assertFalse(
            shouldAnimateStandalonePortraitPager(
                useSharedPlayer = true,
                directPortraitEntry = false
            )
        )
    }

    @Test
    fun directPortraitEntry_suppressesPhoneDetailBodyWhileFullscreen() {
        assertTrue(
            shouldSuppressPhoneDetailBodyForDirectPortraitEntry(
                directPortraitEntry = true,
                isPortraitFullscreen = true
            )
        )
        assertFalse(
            shouldSuppressPhoneDetailBodyForDirectPortraitEntry(
                directPortraitEntry = true,
                isPortraitFullscreen = false
            )
        )
    }

    @Test
    fun standalonePortraitPager_suppressesPhoneDetailBodyToAvoidDualPlayerHosts() {
        assertTrue(
            shouldSuppressPhoneDetailBodyUnderStandalonePortraitPager(
                portraitExperienceEnabled = true,
                isPortraitFullscreen = true,
                hasPlayableState = true,
            )
        )
        assertFalse(
            shouldSuppressPhoneDetailBodyUnderStandalonePortraitPager(
                portraitExperienceEnabled = true,
                isPortraitFullscreen = false,
                hasPlayableState = true,
            )
        )
        assertFalse(
            shouldSuppressPhoneDetailBodyUnderStandalonePortraitPager(
                portraitExperienceEnabled = false,
                isPortraitFullscreen = true,
                hasPlayableState = true,
            )
        )
        assertFalse(
            shouldSuppressPhoneDetailBodyUnderStandalonePortraitPager(
                portraitExperienceEnabled = true,
                isPortraitFullscreen = true,
                hasPlayableState = false,
            )
        )
    }

    @Test
    fun stateHolder_suppressesDetailBodyWhenStandalonePortraitPagerIsShown() {
        val source = java.io.File(
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt"
        ).readText()

        assertTrue(source.contains("shouldSuppressPhoneDetailBodyUnderStandalonePortraitPager"))
        assertTrue(source.contains("if (!suppressPhoneDetailBodyForDirectPortrait && !isPortraitFullscreen)"))
        assertTrue(source.contains("shouldCommitPortraitProgressToDetailState("))
    }

    @Test
    fun inlineHost_exitsCompositionWhenPortraitFullscreenOwnsPlayback() {
        val source = java.io.File(
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailPlayerTransitionHost.kt"
        ).readText()

        assertTrue(source.contains("if (isPortraitFullscreen) {"))
        assertTrue(source.contains("PortraitInlineVideoPlayerHost("))
    }

    @Test
    fun standalonePortraitPager_showsWhenPortraitFullscreenRequestedEvenInInlineMode() {
        assertTrue(
            shouldShowStandalonePortraitPager(
                portraitExperienceEnabled = true,
                isPortraitFullscreen = true,
                useOfficialInlinePortraitDetailExperience = true,
                hasPlayableState = true
            )
        )
    }

    @Test
    fun portraitFullscreenRequest_isAllowedWhenPortraitExperienceEnabled() {
        assertTrue(
            shouldActivatePortraitFullscreenState(
                portraitExperienceEnabled = true
            )
        )
        assertFalse(
            shouldActivatePortraitFullscreenState(
                portraitExperienceEnabled = false
            )
        )
    }

    @Test
    fun inlinePortraitPlayerLayout_usesFullWidthExpandedHeader() {
        val spec = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 412f,
            screenHeightDp = 915f,
            isCollapsed = false
        )

        assertEquals(412f, spec.widthDp)
        assertTrue(spec.heightDp > spec.widthDp)
        assertEquals(594.75f, spec.heightDp)
    }

    @Test
    fun inlinePortraitPlayerLayout_keepsFoldableInnerScreenDetailReachable() {
        val spec = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 768f,
            screenHeightDp = 1024f,
            isCollapsed = false
        )

        assertEquals(768f, spec.widthDp)
        assertEquals(532.48f, spec.heightDp, absoluteTolerance = 0.01f)
        assertTrue(spec.heightDp < spec.widthDp)
        assertTrue(spec.heightDp < 1024f * 0.6f)
    }

    @Test
    fun inlinePortraitPlayerLayout_capsCompactFoldableCoverForScrollableComments() {
        val expanded = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 672f,
            screenHeightDp = 460f,
            isCollapsed = false,
            isFoldableCoverWindow = true,
        )
        val collapsed = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 672f,
            screenHeightDp = 460f,
            isCollapsed = true,
            isFoldableCoverWindow = true,
        )

        assertEquals(230f, expanded.heightDp, absoluteTolerance = 0.01f)
        assertEquals(230f, collapsed.heightDp, absoluteTolerance = 0.01f)
        assertTrue(expanded.heightDp < 460f)

        val portraitWindowExpanded = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 412f,
            screenHeightDp = 915f,
            isCollapsed = false,
            isFoldableCoverWindow = true,
        )
        assertEquals(594.75f, portraitWindowExpanded.heightDp, absoluteTolerance = 0.01f)
    }

    @Test
    fun inlinePortraitPlayerLayout_collapsesToFullWidth16By9Header() {
        val expanded = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 412f,
            screenHeightDp = 915f,
            isCollapsed = false
        )
        val collapsed = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 412f,
            screenHeightDp = 915f,
            isCollapsed = true
        )

        assertEquals(412f, collapsed.widthDp)
        assertTrue(collapsed.heightDp < expanded.heightDp)
        assertEquals(231.75f, collapsed.heightDp)
    }

    @Test
    fun enabledCollapseModes_keepFullWidth16By9PortraitCanvas() {
        assertEquals(
            231.75f,
            resolvePiliPlusCollapsedPlayerViewportHeightDp(
                standardCollapsedHeightDp = 231.75f,
                collapseMode = PortraitPlayerCollapseMode.PAUSED_ONLY,
                isPlaybackPaused = true,
            )
        )
        assertEquals(
            231.75f,
            resolvePiliPlusCollapsedPlayerViewportHeightDp(
                standardCollapsedHeightDp = 231.75f,
                collapseMode = PortraitPlayerCollapseMode.PAUSED_ONLY,
                isPlaybackPaused = false,
            )
        )
    }

    @Test
    fun everyCollapseMode_usesFullWidth16By9PortraitCanvas() {
        listOf(
            PortraitPlayerCollapseMode.OFF,
            PortraitPlayerCollapseMode.INTRO_ONLY,
            PortraitPlayerCollapseMode.COMMENT_ONLY,
            PortraitPlayerCollapseMode.BOTH,
        ).forEach { mode ->
            assertEquals(
                231.75f,
                resolvePiliPlusCollapsedPlayerViewportHeightDp(
                    standardCollapsedHeightDp = 231.75f,
                    collapseMode = mode,
                    isPlaybackPaused = true,
                )
            )
        }
    }

    @Test
    fun switchingToCommentTab_doesNotHidePortraitPlayer() {
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = false,
                collapseMode = PortraitPlayerCollapseMode.BOTH,
                isVerticalVideo = true,
            )
        )
    }

    @Test
    fun landscapeDetailPlayerHeight_matchesActualLayoutWidthFor16By9() {
        // vivo 类窄机：按真实布局宽算 9/16，避免 screenWidthDp 偏差导致左右黑边
        assertEquals(608, resolveLandscapeDetailPlayerContentHeightPx(layoutWidthPx = 1080))
        assertEquals(684, resolveLandscapeDetailPlayerContentHeightPx(layoutWidthPx = 1216))
        assertEquals(1, resolveLandscapeDetailPlayerContentHeightPx(layoutWidthPx = 0))
    }

    @Test
    fun inlinePortraitScrollTransform_respectsSettingEvenForOfficialMode() {
        assertFalse(
            shouldEnableInlinePortraitScrollTransform(
                collapseMode = PortraitPlayerCollapseMode.OFF,
                selectedTabIndex = 0
            )
        )
    }

    @Test
    fun portraitButton_entersPortraitFullscreenInOfficialInlineMode() {
        assertEquals(
            PortraitFullscreenButtonAction.ENTER_PORTRAIT_FULLSCREEN,
            resolvePortraitFullscreenButtonAction(
                useOfficialInlinePortraitDetailExperience = true
            )
        )
    }

    @Test
    fun portraitButton_entersPortraitFullscreenInRegularModeToo() {
        assertEquals(
            PortraitFullscreenButtonAction.ENTER_PORTRAIT_FULLSCREEN,
            resolvePortraitFullscreenButtonAction(
                useOfficialInlinePortraitDetailExperience = false
            )
        )
    }

    @Test
    fun inlinePortraitPlayer_keepsCommentTabPlayerVisible() {
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = false,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = false,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = true,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
    }

    @Test
    fun inlinePortraitPlayer_commentHistoryDoesNotCollapseIntroTab() {
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
    }

    @Test
    fun inlinePortraitPlayer_keepsPlayerWhenCommentThreadDetailIsVisible() {
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                isCommentThreadVisible = true,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = false,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                isCommentThreadVisible = true,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
    }

    @Test
    fun inlinePortraitPlayer_pausedOnlyKeepsCommentTabExpandedWhilePlaying() {
        assertFalse(
            shouldEnableInlinePortraitScrollTransform(
                collapseMode = PortraitPlayerCollapseMode.PAUSED_ONLY,
                selectedTabIndex = 1,
                isVerticalVideo = false,
                isPlaybackPaused = false
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = false,
                collapseMode = PortraitPlayerCollapseMode.PAUSED_ONLY,
                isVerticalVideo = false,
                isPlaybackPaused = false
            )
        )
    }

    @Test
    fun inlinePortraitPlayer_pausedOnlyAllowsCommentScrollCollapseWhenPausedInAnyOrientation() {
        assertTrue(
            shouldEnableInlinePortraitScrollTransform(
                collapseMode = PortraitPlayerCollapseMode.PAUSED_ONLY,
                selectedTabIndex = 1,
                isVerticalVideo = false,
                isPlaybackPaused = true
            )
        )
        assertTrue(
            shouldEnableInlinePortraitScrollTransform(
                collapseMode = PortraitPlayerCollapseMode.PAUSED_ONLY,
                selectedTabIndex = 1,
                isVerticalVideo = true,
                isPlaybackPaused = true
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = false,
                collapseMode = PortraitPlayerCollapseMode.PAUSED_ONLY,
                isVerticalVideo = false,
                isPlaybackPaused = true
            )
        )
    }

    @Test
    fun inlinePortraitPlayer_pausedOnlyKeepsIntroScrollCollapseAvailableWhenPaused() {
        assertTrue(
            shouldUseCompactInlinePortraitPlayerForIntroScroll(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                collapseMode = PortraitPlayerCollapseMode.PAUSED_ONLY,
                isVerticalVideo = false,
                isPlaybackPaused = true
            )
        )
    }

    @Test
    fun inlinePortraitPlayer_compactsWhenIntroHasScrolledDown() {
        assertTrue(
            shouldUseCompactInlinePortraitPlayerForIntroScroll(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 80,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForIntroScroll(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 20,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
    }

    @Test
    fun introScrollCollapseThreshold_ignoresPixelChangesWithinSameSide() {
        assertFalse(isVideoDetailIntroScrollPastCollapseThreshold(0, 20))
        assertFalse(isVideoDetailIntroScrollPastCollapseThreshold(0, 55))
        assertTrue(isVideoDetailIntroScrollPastCollapseThreshold(0, 56))
        assertTrue(isVideoDetailIntroScrollPastCollapseThreshold(1, 0))
    }

    @Test
    fun inlinePortraitPlayerCollapseMode_followsPortraitOrientationStrategy() {
        assertTrue(
            shouldUseCompactInlinePortraitPlayerForIntroScroll(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                collapseMode = PortraitPlayerCollapseMode.INTRO_ONLY,
                isVerticalVideo = true
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = false,
                collapseMode = PortraitPlayerCollapseMode.INTRO_ONLY,
                isVerticalVideo = true
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForIntroScroll(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                collapseMode = PortraitPlayerCollapseMode.INTRO_ONLY,
                isVerticalVideo = false
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = false,
                collapseMode = PortraitPlayerCollapseMode.INTRO_ONLY,
                isVerticalVideo = false
            )
        )
        assertTrue(
            shouldUseCompactInlinePortraitPlayerForIntroScroll(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                collapseMode = PortraitPlayerCollapseMode.COMMENT_ONLY,
                isVerticalVideo = false
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = false,
                collapseMode = PortraitPlayerCollapseMode.COMMENT_ONLY,
                isVerticalVideo = false
            )
        )
    }

    @Test
    fun inlinePortraitPlayer_scrollProgressFollowsManualOffsetForRestore() {
        assertEquals(
            0f,
            resolveInlinePortraitPlayerCollapseProgress(
                manualCollapseProgress = 0f,
                compactForCommentTabProgress = 1f
            )
        )
        assertEquals(
            0.4f,
            resolveInlinePortraitPlayerCollapseProgress(
                manualCollapseProgress = 0.4f,
                compactForCommentTabProgress = 0f
            )
        )
        assertEquals(
            0.2f,
            resolveInlinePortraitPlayerCollapseProgress(
                manualCollapseProgress = 0.2f,
                compactForCommentTabProgress = 0.6f
            )
        )
        assertEquals(
            0f,
            resolveInlinePortraitPlayerCollapseProgress(
                manualCollapseProgress = 1f,
                compactForCommentTabProgress = 1f,
                restoreRequested = true
            )
        )
    }

    @Test
    fun inlinePortraitPlayer_commentCollapseMotionUsesTabSwitchDuration() {
        val spec = VideoContentTabSwitchAnimationSpec(durationMs = 360)

        assertEquals(
            spec.durationMs,
            resolveInlinePortraitPlayerCommentCollapseDurationMillis(spec)
        )
    }

    @Test
    fun inlinePortraitPlayer_restoreUsesDedicatedAnimatedProgress() {
        val source = java.io.File(
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt"
        ).readText()

        assertTrue(source.contains("label = \"inline_portrait_player_restore\""))
        assertTrue(source.contains("manualOrCompactCollapseProgress = animatedCollapseProgress"))
        assertTrue(source.contains("if (inlinePlayerCollapseState.restoreRequested)"))
    }

    @Test
    fun standalonePortraitPagerMotionSpec_keepsExitTransitionShortAndTight() {
        val spec = resolveStandalonePortraitPagerMotionSpec()

        assertEquals(0, spec.enterDurationMillis)
        assertEquals(220, spec.exitDurationMillis)
        assertEquals(0.96f, spec.exitScaleTarget)
        assertEquals(0.08f, spec.exitTranslateUpFraction)
        assertEquals(240, spec.inlineReturnDurationMillis)
        assertEquals(0.985f, spec.inlineReturnInitialScale)
    }

    @Test
    fun continuousPlayerInlineHeight_prefersLayoutWidthWhenExpandedButHonorsCollapse() {
        val layoutWidth = 1080
        val layoutExpanded =
            resolveLandscapeDetailPlayerContentHeightPx(layoutWidthPx = layoutWidth) + 80
        // screenWidthDp 估高：caller 展开高度大于真布局宽 → 取 layout 消黑边
        assertEquals(
            layoutExpanded,
            resolveContinuousPlayerInlineHeightPx(
                layoutWidthPx = layoutWidth,
                preferLayoutWidth16x9Inline = true,
                callerInlineHeightPx = layoutExpanded + 40,
                inlineTopInsetPx = 80,
            ),
        )
        // 评论上滑折叠：caller 更小 → 必须跟着缩小，不能卡死在 16:9
        assertEquals(
            120,
            resolveContinuousPlayerInlineHeightPx(
                layoutWidthPx = layoutWidth,
                preferLayoutWidth16x9Inline = true,
                callerInlineHeightPx = 120,
                inlineTopInsetPx = 80,
            ),
        )
        // 未启用 layout-width 时原样使用 caller
        assertEquals(
            200,
            resolveContinuousPlayerInlineHeightPx(
                layoutWidthPx = layoutWidth,
                preferLayoutWidth16x9Inline = false,
                callerInlineHeightPx = 200,
                inlineTopInsetPx = 80,
            ),
        )
    }

    @Test
    fun standalonePortraitEntry_isDirectForSharedAndDedicatedPlayers() {
        assertFalse(shouldAnimateStandalonePortraitPager(useSharedPlayer = true))
        assertFalse(shouldAnimateStandalonePortraitPager(useSharedPlayer = false))
    }

    @Test
    fun portraitEntryCoverPlaceholder_isSuppressedOnEntry() {
        assertFalse(
            shouldShowPortraitEntryCoverPlaceholder(
                showPortraitFullscreen = true,
                hasPlayableSuccess = false,
                entryCoverUrl = "https://example.com/cover.jpg",
            )
        )
        assertFalse(
            shouldShowPortraitEntryCoverPlaceholder(
                showPortraitFullscreen = true,
                hasPlayableSuccess = true,
                entryCoverUrl = "https://example.com/cover.jpg",
            )
        )
        assertFalse(
            shouldShowPortraitEntryCoverPlaceholder(
                showPortraitFullscreen = true,
                hasPlayableSuccess = false,
                entryCoverUrl = "",
            )
        )
    }
}
