package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.store.PortraitPlayerCollapseMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerCollapseAutoPausePolicyTest {

    @Test
    fun autoPauseKeepsCommentCollapseGeometryAndGestureConsumptionStable() {
        val expandedHeight = 540f
        val compactHeight = 230f
        for (compactForComments in listOf(false, true)) {
            for (offset in listOf(-300f, -310f)) {
                val heights = mutableListOf<Float>()
                val gestureSkips = mutableListOf<Boolean>()
                for (playbackPaused in listOf(false, true, true)) {
                    val layoutPaused = resolveIsPlaybackPausedForInlineLayout(
                        isPlaybackPaused = playbackPaused,
                        wasAutoPausedByCollapse = playbackPaused,
                    )
                    val hideForComments = shouldHidePausedInlinePlayerForComments(1, layoutPaused)
                    val collapsedHeight = resolveInlinePlayerCollapsedViewportHeightDp(
                        standardCollapsedHeightDp = compactHeight,
                        hidePausedPlayerForComments = hideForComments,
                    )
                    val collapsedForAutoPause = resolvePlayerCollapsedForAutoPause(
                        swipeHidePlayerEnabled = true,
                        playerHeightOffsetPx = offset,
                        videoHeightPx = expandedHeight - compactHeight,
                        compactForCommentTab = compactForComments,
                    )
                    assertTrue(collapsedForAutoPause)
                    assertFalse(shouldAutoResumeOnPlayerExpand(
                        autoPauseEnabled = true,
                        isPlayerCollapsed = collapsedForAutoPause,
                        wasAutoPausedByCollapse = playbackPaused,
                    ))
                    val progress = resolveInlinePortraitPlayerCollapseProgress(
                        manualCollapseProgress = -offset / (expandedHeight - collapsedHeight),
                        compactForCommentTabProgress = if (compactForComments) 1f else 0f,
                        hidePausedPlayerForComments = hideForComments,
                    )
                    heights += expandedHeight + (collapsedHeight - expandedHeight) * progress
                    gestureSkips += com.android.purebilibili.feature.video.policy
                        .shouldSkipGesturePlayerCollapseForLayout(
                            compactForIntroScroll = false,
                            compactForCommentTab = compactForComments,
                            hidePausedPlayerForComments = hideForComments,
                        )
                }
                assertEquals(1, heights.distinct().size)
                assertEquals(1, gestureSkips.distinct().size)
            }
        }
    }

    @Test
    fun manualPauseStillEnablesCommentHidingAfterPlaybackResumes() {
        assertFalse(resolveIsPlaybackPausedForInlineLayout(false, false))
        assertFalse(resolveIsPlaybackPausedForInlineLayout(true, true))
        // Resuming releases auto-pause ownership; the next manual pause uses paused rules.
        assertFalse(resolveIsPlaybackPausedForInlineLayout(false, false))
        val manuallyPaused = resolveIsPlaybackPausedForInlineLayout(true, false)
        assertTrue(shouldHidePausedInlinePlayerForComments(1, manuallyPaused))
        assertTrue(shouldEnableInlinePortraitScrollTransform(
            collapseMode = PortraitPlayerCollapseMode.PAUSED_ONLY,
            selectedTabIndex = 0,
            isPlaybackPaused = manuallyPaused,
        ))
    }

    @Test
    fun switchingToCommentsKeepsExpandedPlayerUntilScrollWhileAutoPauseIsDisabled() {
        val expanded = resolvePortraitInlinePlayerLayoutSpec(412f, 760f, isCollapsed = false)
        val compact = resolvePortraitInlinePlayerLayoutSpec(412f, 760f, isCollapsed = true)
        val needsCompact = shouldUseCompactInlinePortraitPlayerForCommentTab(
            useOfficialInlinePortraitDetailExperience = true,
            selectedTabIndex = 1,
            isPortraitFullscreen = false,
            collapseMode = PortraitPlayerCollapseMode.INTRO_ONLY,
            isVerticalVideo = true,
            isPlaybackPaused = false,
        )
        assertFalse(needsCompact)
        val progress = resolveInlinePortraitPlayerCollapseProgress(
            manualCollapseProgress = 0f,
            compactForCommentTabProgress = if (needsCompact) 1f else 0f,
        )
        val height = expanded.heightDp + (compact.heightDp - expanded.heightDp) * progress
        assertEquals(expanded.heightDp, height, 0.001f)
        assertEquals(0f, progress, 0.001f)
        // 暂停把隐藏终点改为 0 高度，已同步的位移仍应保留紧凑画面，继续上滑才隐藏。
        val compactOffset = expanded.heightDp - compact.heightDp
        val pausedProgress = resolveInlinePortraitPlayerCollapseProgress(
            manualCollapseProgress = compactOffset / expanded.heightDp,
            compactForCommentTabProgress = 1f,
            hidePausedPlayerForComments = true,
        )
        assertEquals(compact.heightDp, expanded.heightDp * (1f - pausedProgress), 0.001f)
        val collapsed = resolvePlayerCollapsedForAutoPause(
            swipeHidePlayerEnabled = true,
            playerHeightOffsetPx = 0f,
            videoHeightPx = expanded.heightDp - compact.heightDp,
            compactForCommentTab = needsCompact,
        )
        assertFalse(shouldAutoPauseOnPlayerCollapse(false, collapsed, isPlaying = true))
    }

    @Test
    fun restoredCommentPlayerDoesNotImmediatelyAutoPauseAgain() {
        val collapsed = resolvePlayerCollapsedForAutoPause(
            swipeHidePlayerEnabled = true,
            playerHeightOffsetPx = 0f,
            videoHeightPx = 320f,
            compactForCommentTab = true,
            restoreRequested = true,
        )
        assertFalse(collapsed)
        assertFalse(shouldAutoPauseOnPlayerCollapse(true, collapsed, isPlaying = true))
        assertTrue(shouldAutoResumeOnPlayerExpand(true, collapsed, wasAutoPausedByCollapse = true))
    }

    @Test
    fun pausesOnlyWhenEnabledCollapsedAndPlaying() {
        assertTrue(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                isPlaying = true,
            )
        )
        assertFalse(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = false,
                isPlayerCollapsed = true,
                isPlaying = true,
            )
        )
        assertFalse(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = true,
                isPlayerCollapsed = false,
                isPlaying = true,
            )
        )
        assertFalse(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                isPlaying = false,
            )
        )
    }

    @Test
    fun resumesOnlyWhenExpandingAfterAutoPause() {
        assertTrue(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = true,
                isPlayerCollapsed = false,
                wasAutoPausedByCollapse = true,
            )
        )
        assertFalse(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = true,
                isPlayerCollapsed = false,
                wasAutoPausedByCollapse = false,
            )
        )
        assertFalse(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                wasAutoPausedByCollapse = true,
            )
        )
        assertFalse(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = false,
                isPlayerCollapsed = false,
                wasAutoPausedByCollapse = true,
            )
        )
    }

    @Test
    fun collapseOffDoesNotAutoPauseWhenSwitchingToShortCommentPage() {
        var playing = true
        var autoPaused = false
        val actions = mutableListOf<String>()
        val expanded = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 412f,
            screenHeightDp = 760f,
            isCollapsed = false,
        )
        val compact = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 412f,
            screenHeightDp = 760f,
            isCollapsed = true,
        )
        val autoPauseRange = expanded.heightDp - compact.heightDp

        // Tab changes must not create a collapse or pause, even when comments have little room.
        for (tab in listOf(1, 1, 1, 0)) {
            val inlineScrollEnabled = shouldEnableInlinePortraitScrollTransform(
                collapseMode = PortraitPlayerCollapseMode.OFF,
                selectedTabIndex = tab,
                isPlaybackPaused = !playing,
            )
            val compactForComments = shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = tab,
                isPortraitFullscreen = false,
                isPlaybackPaused = !playing,
                collapseMode = PortraitPlayerCollapseMode.OFF,
            )
            val collapsed = resolvePlayerCollapsedForAutoPause(
                swipeHidePlayerEnabled = inlineScrollEnabled,
                playerHeightOffsetPx = 0f,
                videoHeightPx = autoPauseRange,
                compactForCommentTab = compactForComments,
            )
            if (shouldAutoPauseOnPlayerCollapse(true, collapsed, playing)) {
                playing = false
                autoPaused = true
                actions += "pause"
            } else if (shouldAutoResumeOnPlayerExpand(true, collapsed, autoPaused)) {
                playing = true
                autoPaused = false
                actions += "resume"
            }
            assertFalse(compactForComments)
            assertTrue(playing)
        }

        assertEquals(emptyList<String>(), actions)
        assertTrue(playing)
    }

    @Test
    fun nonCompactPlayerResumesOnlyAfterSwipeExpansion() {
        val collapsed = resolvePlayerCollapsedForAutoPause(
            swipeHidePlayerEnabled = true,
            playerHeightOffsetPx = -300f,
            videoHeightPx = 300f,
            compactForCommentTab = false,
        )
        assertTrue(shouldAutoPauseOnPlayerCollapse(true, collapsed, true))
        assertFalse(shouldAutoResumeOnPlayerExpand(true, collapsed, true))

        val expanded = resolvePlayerCollapsedForAutoPause(
            swipeHidePlayerEnabled = true,
            playerHeightOffsetPx = 0f,
            videoHeightPx = 300f,
            compactForCommentTab = false,
        )
        assertFalse(shouldAutoPauseOnPlayerCollapse(true, expanded, true))
        assertTrue(shouldAutoResumeOnPlayerExpand(true, expanded, true))
    }

    @Test
    fun portraitFullscreenDoesNotTreatHiddenInlinePlayerAsCollapsed() {
        assertFalse(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                isPlaying = true,
                isPortraitFullscreen = true,
            )
        )
        assertTrue(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                wasAutoPausedByCollapse = true,
                isPortraitFullscreen = true,
            )
        )
    }
}
