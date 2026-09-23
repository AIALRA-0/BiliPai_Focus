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
    fun hostForwardsPredictiveBackProgressAndCancellationToNativeVideoTransition() {
        val source = loadSource()

        assertTrue(source.contains("onNativeVideoBackProgress:"))
        assertTrue(source.contains("NavigationEventTransitionState.InProgress"))
        assertTrue(source.contains("latestEvent?.progress"))
        assertTrue(source.contains("onNativeVideoBackCancelled("))
    }

    @Test
    fun scopedEntryContentRefreshesItsExposureProviderBeforePredictiveBack() {
        val source = loadSource()
        val scopedContentRememberKeys = source
            .substringAfter("val scopedContent:")
            .substringAfter("remember(")
            .substringBefore(") {")

        assertTrue(scopedContentRememberKeys.contains("videoCardExposureProvider"))
        // sourceMetadata 经 rememberUpdatedState 保持最新且不触发 scopedContent 重建，
        // 不再作为 remember key（避免预测返回期间 entry lambda 重建打断 seek）。
        assertTrue(source.contains("rememberUpdatedState(sourceMetadata)"))
    }

    @Test
    fun predictiveCommitMarksCardReturnBeforeWaitingForNativePop() {
        val performBackSource = loadSource()
            .substringAfter("val performBack:")
            .substringBefore("val latestProgrammaticBackAction")
        val prepareIndex = performBackSource.indexOf("onPrepareVideoCardSharedReturn()")
        val nativePopWaitIndex = performBackSource.indexOf("predictiveBlurFadeJob?.join()")

        assertTrue(prepareIndex >= 0)
        assertTrue(nativePopWaitIndex >= 0)
        assertTrue(prepareIndex < nativePopWaitIndex)
        assertTrue(performBackSource.countOccurrences("onPrepareVideoCardSharedReturn()") == 1)
    }

    @Test
    fun predictiveCancelRestoresLivePlayerBeforeDepthAnimation() {
        val cancelSource = loadSource()
            .substringAfter("onBackCancelled = {")
            .substringBefore("commitTransition()")
        val playerRecoveryIndex = cancelSource.indexOf(
            "onNativeVideoBackCancelled(currentBackKey, targetBackKey)"
        )
        val depthRestoreIndex = cancelSource.indexOf("videoCardClock.animateFallbackTo(")
        val restoreFlagIndex = cancelSource.indexOf("videoCardClock.beginGestureRestore()")
        val restoreLaunchIndex = cancelSource.indexOf(
            "navigationScope.launch",
            startIndex = restoreFlagIndex,
        )

        assertTrue(playerRecoveryIndex >= 0)
        assertTrue(depthRestoreIndex >= 0)
        assertTrue(playerRecoveryIndex < depthRestoreIndex)
        assertTrue(restoreFlagIndex >= 0)
        assertTrue(restoreLaunchIndex >= 0)
        assertTrue(restoreFlagIndex < playerRecoveryIndex)
        assertTrue(restoreFlagIndex < restoreLaunchIndex)
        assertTrue(
            cancelSource.countOccurrences(
                "onNativeVideoBackCancelled(currentBackKey, targetBackKey)"
            ) == 1
        )
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
}
