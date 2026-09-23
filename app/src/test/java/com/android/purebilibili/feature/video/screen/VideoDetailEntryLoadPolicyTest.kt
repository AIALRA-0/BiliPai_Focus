package com.android.purebilibili.feature.video.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoDetailEntryLoadPolicyTest {
    @Test
    fun heroEntryGateObservesDriverBeforeLegacyTimeoutIsCreated() {
        val source = listOf(
            java.io.File("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailEntryTransitionObserver.kt"),
            java.io.File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailEntryTransitionObserver.kt"),
        ).first { it.exists() }.readText()
        val hero = source.substringAfter("if (heroDriver != null)").substringBefore("var hasObservedActiveTransition")
        assertTrue(hero.contains("heroDriver.progressProvider()"))
        assertTrue(hero.contains(".first { it }"))
        assertTrue(hero.contains("return@LaunchedEffect"))
        assertFalse(hero.contains("delay("))
    }

    @Test
    fun deferLoad_requiresCardShellTransitionWithoutMiniPlayerReuse() {
        assertTrue(
            shouldDeferVideoDetailLoadUntilEntryTransitionFinished(
                transitionEnabled = true,
                detailShellSharedBoundsEnabled = true,
                reuseFromMiniPlayerAtEntry = false,
            )
        )
    }

    @Test
    fun deferLoad_disabledWhenReturningFromRelatedVideoDetail() {
        assertFalse(
            shouldDeferVideoDetailLoadUntilEntryTransitionFinished(
                transitionEnabled = true,
                detailShellSharedBoundsEnabled = true,
                reuseFromMiniPlayerAtEntry = false,
                isReturningFromDetail = true,
            )
        )
    }

    @Test
    fun deferLoad_disabledWhenTransitionOffOrNoShellOrMiniPlayerReuse() {
        assertFalse(
            shouldDeferVideoDetailLoadUntilEntryTransitionFinished(
                transitionEnabled = false,
                detailShellSharedBoundsEnabled = true,
                reuseFromMiniPlayerAtEntry = false,
            )
        )
        assertFalse(
            shouldDeferVideoDetailLoadUntilEntryTransitionFinished(
                transitionEnabled = true,
                detailShellSharedBoundsEnabled = false,
                reuseFromMiniPlayerAtEntry = false,
            )
        )
        assertFalse(
            shouldDeferVideoDetailLoadUntilEntryTransitionFinished(
                transitionEnabled = true,
                detailShellSharedBoundsEnabled = true,
                reuseFromMiniPlayerAtEntry = true,
            )
        )
    }

    @Test
    fun entryTransitionFinished_whenNotDeferring() {
        assertTrue(
            isVideoDetailEntryTransitionFinished(
                deferLoad = false,
                isSharedTransitionActive = true,
                isNavEnterTransitionRunning = true,
            )
        )
    }

    @Test
    fun entryTransitionFinished_whenBothSignalsIdle() {
        assertTrue(
            isVideoDetailEntryTransitionFinished(
                deferLoad = true,
                isSharedTransitionActive = false,
                isNavEnterTransitionRunning = false,
            )
        )
        assertFalse(
            isVideoDetailEntryTransitionFinished(
                deferLoad = true,
                isSharedTransitionActive = true,
                isNavEnterTransitionRunning = false,
            )
        )
        assertFalse(
            isVideoDetailEntryTransitionFinished(
                deferLoad = true,
                isSharedTransitionActive = false,
                isNavEnterTransitionRunning = true,
            )
        )
    }

    @Test
    fun markFinished_requiresObservedActiveTransitionBeforeIdle() {
        assertFalse(
            shouldMarkVideoDetailEntryTransitionFinished(
                hasObservedActiveTransition = false,
                isSharedTransitionActive = false,
                isNavEnterTransitionRunning = false,
            )
        )
        assertFalse(
            shouldMarkVideoDetailEntryTransitionFinished(
                hasObservedActiveTransition = true,
                isSharedTransitionActive = true,
                isNavEnterTransitionRunning = false,
            )
        )
        assertTrue(
            shouldMarkVideoDetailEntryTransitionFinished(
                hasObservedActiveTransition = true,
                isSharedTransitionActive = false,
                isNavEnterTransitionRunning = false,
            )
        )
    }

    @Test
    fun fallbackTimeout_addsBufferToMorphDuration() {
        assertEquals(
            540,
            resolveVideoDetailEntryTransitionFallbackTimeoutMillis(
                morphDurationMillis = 460,
                bufferMillis = 80,
            )
        )
    }

    @Test
    fun playbackPreload_startsDuringTheLaterPartOfSharedMorph() {
        assertEquals(
            147,
            resolveVideoDetailEntryPlaybackPreloadDelayMillis(morphDurationMillis = 460),
        )
        assertEquals(
            0,
            resolveVideoDetailEntryPlaybackPreloadDelayMillis(morphDurationMillis = 0),
        )
    }
}
