package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeHeroFlyoutStructureTest {

    @Test
    fun homeScreenNavigatesImmediatelyAndDoesNotRunSourceFlyout() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val clickWrapperSource = source
            .substringAfter("val wrappedOnVideoClick")
            .substringBefore("val onTodayWatchVideoClick")

        assertFalse(source.contains("pendingHeroFlyoutRequest"))
        assertFalse(source.contains("shouldRunHomeHeroFlyoutBeforeNavigation(request)"))
        assertFalse(source.contains("resolveHomeHeroFlyoutNavigationDelayMillis()"))
        assertTrue(clickWrapperSource.contains("hideTopTabsForForwardDetailNav = true"))
        assertTrue(clickWrapperSource.contains("setBottomBarVisible(false)"))
        assertTrue(clickWrapperSource.contains("isVideoNavigating = true"))
        assertTrue(clickWrapperSource.contains("onVideoClick(request)"))
    }

    @Test
    fun homeTopTabsReturnRecoveryFollowsNavigationStateInsteadOfLifecycleStart() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val navigationReturnEffect = source
            .substringAfter("Navigation 返回不一定触发首页 Lifecycle.ON_START")
            .substringBefore("从详情页返回时延后清理")
        val lifecycleObserverSource = source
            .substringAfter("DisposableEffect(lifecycleOwner, useSideNavigation)")
            .substringBefore("lifecycleOwner.lifecycle.addObserver(observer)")

        assertTrue(navigationReturnEffect.contains("LaunchedEffect(isReturningFromVideoDetail"))
        assertTrue(navigationReturnEffect.contains("hideTopTabsForForwardDetailNav = false"))
        assertTrue(navigationReturnEffect.contains("resolveHomeTopTabsRevealDelayMs("))
        assertFalse(lifecycleObserverSource.contains("val returningFromDetail = isReturningFromVideoDetail"))
        assertFalse(lifecycleObserverSource.contains("resolveHomeTopTabsRevealDelayMs("))
    }

    @Test
    fun ordinaryHomeVideoCardDoesNotRunSourceFlyout() {
        val pageSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeCategoryPage.kt")
        val cardSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt")

        assertFalse(pageSource.contains("heroFlyoutBvid"))
        assertFalse(pageSource.contains("heroFlyoutActive"))
        assertFalse(cardSource.contains("heroFlyoutActive"))
        assertFalse(cardSource.contains("resolveHomeHeroFlyoutFrame("))
    }

    @Test
    fun ordinaryHomeVideoCardUsesCoverFirstSharedTransitionPolicy() {
        val cardSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt")

        assertTrue(cardSource.contains("resolveVideoCardSharedTransitionMotionSpec("))
        assertTrue(cardSource.contains("resolveVideoSharedTransitionVisualSpec("))
        assertTrue(cardSource.contains("durationMillis = homeSharedTransitionMotionSpec.durationMillis"))
        assertTrue(cardSource.contains("videoCoverSharedElementKey("))
        assertTrue(cardSource.contains("sharedElementSourceRoute"))
        assertFalse(cardSource.contains("使用 renderInSharedTransitionScopeOverlayOption 控制可见性"))
    }

    @Test
    fun inlinePartitionPageKeepsPartitionVideoSourceInsteadOfHomeFeed() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val partitionPageSource = source
            .substringAfter("when (val entry = resolveHomeTopTabEntryOrNull(topTabEntries, page))")
            .substringAfter("HomeTopTabEntry.Partition ->")
            .substringBefore("is HomeTopTabEntry.Category ->")

        assertTrue(partitionPageSource.contains("LocalVideoCardSharedElementSourceRoute provides partitionVideoSourceRoute"))
        assertTrue(partitionPageSource.contains("onVideoClick = onPartitionVideoClick"))
        assertFalse(partitionPageSource.contains("wrappedOnVideoClick("))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
