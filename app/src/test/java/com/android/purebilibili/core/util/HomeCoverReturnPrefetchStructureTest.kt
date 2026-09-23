package com.android.purebilibili.core.util

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeCoverReturnPrefetchStructureTest {

    @Test
    fun videoCardRegistersVisibleCoverOnComposition() {
        val source = videoCardSource()

        assertTrue(source.contains("HomeCoverReturnPrefetchRegistry.onCardVisible("))
        assertTrue(source.contains("HomeCoverReturnPrefetchEntry("))
        // 使用与 AsyncImage 相同的 cacheKey 来源，保证命中同一缓存条目
        assertTrue(source.contains("cacheKey = coverCacheKey"))
    }

    @Test
    fun appNavigationTriggersPrefetchOnGestureAndCommit() {
        val source = appNavigationSource()

        assertTrue(source.contains("onNativeVideoBackProgress = { _, _, progress ->"))
        assertTrue(source.contains("if (progress > 0f) {"))
        assertTrue(source.contains("maybePrefetchHomeCoversForVideoReturn()"))
        assertTrue(source.contains("if (homeCoverPrefetchTriggered.value) return"))
        assertTrue(source.contains("homeCoverPrefetchTriggered.value = true"))
        // 返回提交兜底：普通返回(顶部按钮)也预热
        assertTrue(source.contains("onPrepareVideoCardSharedReturn = {"))
        assertFalse(source.contains("shouldUseClassicBackForVideoSharedElementReturn("))
    }

    @Test
    fun miuixVideoGestureProgressFeedsThePrefetchCallbackOnlyDuringDirectGesture() {
        val source = navHostSource()

        assertTrue(source.contains("videoCardTransitionProgress.isGestureInProgress()"))
        assertTrue(source.contains("videoCardTransitionProgress.gestureBackProgress()"))
        assertTrue(source.contains("latestNativeVideoBackProgress(currentKey, currentBackTarget, progress)"))
    }

    private fun videoCardSource(): String = listOf(
        File("app/src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt"),
        File("src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt")
    ).first { it.exists() }.readText()

    private fun appNavigationSource(): String = listOf(
        File("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"),
        File("src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
    ).first { it.exists() }.readText()

    private fun navHostSource(): String = listOf(
        File("app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt"),
        File("src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt")
    ).first { it.exists() }.readText()
}
