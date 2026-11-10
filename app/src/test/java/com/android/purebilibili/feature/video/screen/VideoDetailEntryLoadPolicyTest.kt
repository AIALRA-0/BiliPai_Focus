package com.android.purebilibili.feature.video.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoDetailEntryLoadPolicyTest {
    @Test
    fun ordinaryNavigationStaggersEntryWorkEvenWhenCardAnimationIsOff() {
        val defer = shouldDeferVideoDetailLoadUntilEntryTransitionFinished(
            transitionEnabled = false,
            detailShellSharedBoundsEnabled = false,
            reuseFromMiniPlayerAtEntry = false,
            hasNavigationTransitionScope = true,
        )
        assertTrue(defer)
        assertTrue(shouldStaggerVideoDetailEntryWork(defer, false))
        // 已有播放器复用不新增等待；无导航许可的直接入口仍直接放行。
        assertFalse(shouldStaggerVideoDetailEntryWork(defer, true))
        assertFalse(shouldStaggerVideoDetailEntryWork(false, false))
    }

    @Test
    fun playerHostWaitsOnlyForFreshCoveredCardEntry() {
        assertFalse(shouldMountVideoDetailPlayerHost(
            alreadyMounted = false, deferEntryLoad = true, entryPlaybackReady = false,
            hasResidentCover = true, preserveLivePlayer = false,
        ))
        assertTrue(shouldMountVideoDetailPlayerHost(
            alreadyMounted = false, deferEntryLoad = true, entryPlaybackReady = true,
            hasResidentCover = true, preserveLivePlayer = false,
        ))
        // A return preview or a new episode must not detach the previously mounted host.
        assertTrue(shouldMountVideoDetailPlayerHost(
            alreadyMounted = true, deferEntryLoad = true, entryPlaybackReady = false,
            hasResidentCover = true, preserveLivePlayer = false,
        ))
    }

    @Test
    fun playerHostDoesNotWaitWithoutCoverOrForExistingPlayback() {
        assertTrue(shouldMountVideoDetailPlayerHost(
            alreadyMounted = false, deferEntryLoad = true, entryPlaybackReady = false,
            hasResidentCover = false, preserveLivePlayer = false,
        ))
        assertTrue(shouldMountVideoDetailPlayerHost(
            alreadyMounted = false, deferEntryLoad = true, entryPlaybackReady = false,
            hasResidentCover = true, preserveLivePlayer = true,
        ))
        assertTrue(shouldMountVideoDetailPlayerHost(
            alreadyMounted = false, deferEntryLoad = false, entryPlaybackReady = false,
            hasResidentCover = true, preserveLivePlayer = false,
        ))
    }

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
    fun ordinaryNavigation_defersPreparationEvenWithoutSharedCardAnimation() {
        assertTrue(
            shouldDeferVideoDetailLoadUntilEntryTransitionFinished(
                transitionEnabled = false,
                detailShellSharedBoundsEnabled = false,
                reuseFromMiniPlayerAtEntry = false,
                hasNavigationTransitionScope = true,
            )
        )
        assertFalse(
            shouldDeferVideoDetailLoadUntilEntryTransitionFinished(
                transitionEnabled = false,
                detailShellSharedBoundsEnabled = false,
                reuseFromMiniPlayerAtEntry = true,
                hasNavigationTransitionScope = true,
            )
        )
        // 快速返回时不能借全局返回标志提前准备，已完成父详情由 observer 的锁存放行。
        assertTrue(
            shouldDeferVideoDetailLoadUntilEntryTransitionFinished(
                transitionEnabled = false,
                detailShellSharedBoundsEnabled = false,
                reuseFromMiniPlayerAtEntry = false,
                isReturningFromDetail = true,
                hasNavigationTransitionScope = true,
            )
        )
    }

    @Test
    fun navigationEntry_onlyFinishesAtTheIdleTargetWithSettledVisuals() {
        fun canFinish(
            running: Boolean = false,
            top: Boolean = true,
            sharedActive: Boolean = false,
            legacyRunning: Boolean = false,
            heroSettled: Boolean = true,
        ) = shouldFinishVideoDetailNavigationEntry(
            isNavigationRunning = running,
            isEntryTop = top,
            isSharedTransitionActive = sharedActive,
            isLegacyNavRunning = legacyRunning,
            isHeroSettled = heroSettled,
        )

        assertTrue(canFinish())
        assertFalse(canFinish(running = true))
        assertFalse(canFinish(top = false))
        assertFalse(canFinish(sharedActive = true))
        assertFalse(canFinish(legacyRunning = true))
        assertFalse(canFinish(heroSettled = false))
    }

    @Test
    fun preparationUsesCompletionSignalInsteadOfTimedMidAnimationPreload() {
        val source = listOf(
            java.io.File("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt"),
            java.io.File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt"),
        ).first { it.exists() }.readText()
        assertTrue(source.contains("val entryPlaybackReady = entryTransitionFinished"))
        assertTrue(source.contains("navigationTransitionScope = entryNavigationTransitionScope"))
        assertTrue(source.contains("entryTransitionFinished = entryPlaybackReady"))
        assertFalse(source.contains("rememberVideoDetailEntryPlaybackReady("))
    }
}
