package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailDisposePolicyTest {

    @Test
    fun videoSwitchFlagUsesLocalStateAsFallbackWhenManagerFlagIsResetEarly() {
        assertTrue(
            resolveIsNavigatingToVideoDuringDispose(
                localNavigatingToVideo = true,
                managerNavigatingToVideo = false
            )
        )
    }

    @Test
    fun videoSwitchFlagUsesManagerStateWhenAvailable() {
        assertTrue(
            resolveIsNavigatingToVideoDuringDispose(
                localNavigatingToVideo = false,
                managerNavigatingToVideo = true
            )
        )
    }

    @Test
    fun disposeHandledAsNavigationExitWhenNoKeepAliveConditionMatches() {
        assertTrue(
            shouldHandleVideoDetailDisposeAsNavigationExit(
                isNavigatingToAudioMode = false,
                isNavigatingToMiniMode = false,
                isMiniModeActive = false,
                isChangingConfigurations = false,
                isNavigatingToVideo = false
            )
        )
    }

    @Test
    fun disposeSkippedWhenNavigatingToAudioMode() {
        assertFalse(
            shouldHandleVideoDetailDisposeAsNavigationExit(
                isNavigatingToAudioMode = true,
                isNavigatingToMiniMode = false,
                isMiniModeActive = false,
                isChangingConfigurations = false,
                isNavigatingToVideo = false
            )
        )
    }

    @Test
    fun disposeSkippedWhenNavigatingToMiniMode() {
        assertFalse(
            shouldHandleVideoDetailDisposeAsNavigationExit(
                isNavigatingToAudioMode = false,
                isNavigatingToMiniMode = true,
                isMiniModeActive = false,
                isChangingConfigurations = false,
                isNavigatingToVideo = false
            )
        )
    }

    @Test
    fun disposeSkippedWhenMiniModeWasActivatedByNavigationLayer() {
        assertFalse(
            shouldHandleVideoDetailDisposeAsNavigationExit(
                isNavigatingToAudioMode = false,
                isNavigatingToMiniMode = false,
                isMiniModeActive = true,
                isChangingConfigurations = false,
                isNavigatingToVideo = false
            )
        )
    }

    @Test
    fun disposeSkippedWhenConfigurationChanges() {
        assertFalse(
            shouldHandleVideoDetailDisposeAsNavigationExit(
                isNavigatingToAudioMode = false,
                isNavigatingToMiniMode = false,
                isMiniModeActive = false,
                isChangingConfigurations = true,
                isNavigatingToVideo = false
            )
        )
    }

    @Test
    fun disposeSkippedWhenNavigatingToAnotherVideo() {
        assertFalse(
            shouldHandleVideoDetailDisposeAsNavigationExit(
                isNavigatingToAudioMode = false,
                isNavigatingToMiniMode = false,
                isMiniModeActive = false,
                isChangingConfigurations = false,
                isNavigatingToVideo = true
            )
        )
    }

    @Test
    fun returningState_isMarkedOnlyWhenDisposeShouldBeHandledAsNavigationExit() {
        assertTrue(
            shouldMarkReturningStateOnVideoDetailDispose(
                shouldHandleAsNavigationExit = true
            )
        )
        assertFalse(
            shouldMarkReturningStateOnVideoDetailDispose(
                shouldHandleAsNavigationExit = false
            )
        )
    }

    @Test
    fun enteringVideoDetail_clearsStaleReturningStateWhenPresent() {
        assertTrue(
            shouldClearStaleReturningStateOnVideoDetailEnter(
                isReturningFromDetail = true
            )
        )
        assertFalse(
            shouldClearStaleReturningStateOnVideoDetailEnter(
                isReturningFromDetail = false
            )
        )
    }
}
