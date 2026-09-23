package com.android.purebilibili.navigation3

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BiliPaiNavDisplayHostNativeTransitionStructureTest {

    @Test
    fun videoCardMorphOwnsCornersWithoutHostLeadingClip() {
        val source = loadSource()

        assertTrue(source.contains("val videoCardMorphOwnsCorners = cardMorphAvailable"))
        assertTrue(source.contains("isCardMorphDestinationNavKey(currentKey)"))
        assertTrue(source.contains("VideoCardTransitionExposure.Returning"))
        assertTrue(source.contains("val enableHostCornerClip = !videoCardMorphOwnsCorners"))
        assertTrue(source.contains("enableCornerClip = enableHostCornerClip"))
        assertTrue(source.contains("val hostDimAmount = if (videoCardMorphOwnsCorners) 0f else 0.5f"))
        assertTrue(source.contains("dimAmount = hostDimAmount"))
    }

    @Test
    fun hostRoutesOnlyCancelledVideoBackGesturesToPlayerRecovery() {
        val source = loadSource()
        val appNavigation = loadAppNavigationSource()

        assertTrue(source.contains("videoCardTransitionProgress.settleStateOrNull()"))
        assertTrue(source.contains("VideoCardTransitionSettleState.CancelRestore"))
        assertTrue(source.contains("previousSettleState != VideoCardTransitionSettleState.CancelRestore"))
        assertTrue(source.contains("shouldRecoverVideoPlayerAfterBackCancellation("))
        assertTrue(source.contains("latestPredictiveBackCancelled(currentKey, currentBackTarget)"))
        assertTrue(appNavigation.contains("onPredictiveBackCancelled = { _, _ ->"))
        assertTrue(appNavigation.contains("predictiveBackCancelRecoveryGeneration += 1"))
        assertTrue(
            Regex(
                """predictiveBackCancelRecoveryGeneration\.takeIf\s*\{\s*navigation3BackStack\.lastOrNull\(\)\s*==\s*videoKey""",
            ).containsMatchIn(appNavigation),
        )
    }

    @Test
    fun hostSharesOneMiuixProgressDriverAcrossVideoTransitions() {
        val source = loadSource()

        assertTrue(source.contains("remember(sourceMetadata.sourceKey) { MiuixVideoCardTransitionProgress() }"))
        assertTrue(source.contains("videoCardTransitionProgress.observe("))
        assertTrue(source.contains("videoCardTransitionProgress.clear()"))
        assertTrue(source.contains("videoCardClock.bindNavigationDriver("))
    }

    @Test
    fun completedBackPreparesSharedReturnBeforePoppingNavigation() {
        val performBackSource = loadSource()
            .substringAfter("val performBack = remember(")
            .substringBefore("DisposableEffect(programmaticBackDispatcher, performBack)")
        val prepareIndex = performBackSource.indexOf("latestPrepareReturn()")
        val popIndex = performBackSource.indexOf("latestOnBack()")

        assertTrue(prepareIndex >= 0)
        assertTrue(popIndex >= 0)
        assertTrue(prepareIndex < popIndex)
        assertTrue(performBackSource.countOccurrences("latestPrepareReturn()") == 1)
    }

    @Test
    fun predictiveCancelRecoveryIsSeparateFromCommittedReturn() {
        val source = loadSource()
        val cancelEdge = source
            .substringAfter("if (\n                settle == VideoCardTransitionSettleState.CancelRestore")
            .substringBefore("previousSettleState = settle")

        assertTrue(cancelEdge.contains("previousSettleState != VideoCardTransitionSettleState.CancelRestore"))
        assertTrue(cancelEdge.contains("shouldRecoverVideoPlayerAfterBackCancellation("))
        assertTrue(cancelEdge.contains("latestPredictiveBackCancelled(currentKey, currentBackTarget)"))
        assertTrue(source.contains("VideoCardTransitionSettleState.AutoReturn"))
    }

    @Test
    fun settledOrUnavailableCardMorphReleasesNativeCardLayers() {
        val source = loadSource()

        assertTrue(source.contains("if (!cardMorphAvailable)"))
        assertTrue(source.contains("state == VideoCardTransitionSettleState.Idle"))
        assertTrue(source.countOccurrences("CardPositionManager.clearNativeVideoCardLayers()") == 2)
    }

    private fun String.countOccurrences(needle: String): Int =
        windowed(size = needle.length, step = 1).count { it == needle }

    private fun loadSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt")
        ).first { it.exists() }.readText()
    }

    private fun loadAppNavigationSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"),
            File("src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        ).first { it.exists() }.readText()
    }
}
