package com.android.purebilibili.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomPagerStatePersistenceStructureTest {

    @Test
    fun `bottom tabs are hosted by main horizontal pager state`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")

        assertTrue(source.contains("BiliPaiNavDisplayHost("))
        assertTrue(source.contains("rememberPagerState("))
        assertTrue(source.contains("rememberMainBottomPagerState("))
        assertTrue(source.contains("HorizontalPager("))
        assertTrue(source.contains("rememberSaveableStateHolder()"))
        assertTrue(source.contains("bottomPagerSaveableStateHolder.SaveableStateProvider("))
        assertTrue(source.contains("resolveBottomPagerSaveableStateKey(slotItem)"))
        assertTrue(source.contains("historyViewModel.loadData("))
        assertTrue(source.contains("isBottomPagerPageActive"))
        assertTrue(source.contains("userScrollEnabled = shouldEnableBottomPagerUserScroll()"))
        assertTrue(source.contains("resolveBottomPagerBeyondViewportPageCount("))
        assertTrue(source.contains("pageCount = visibleBottomBarItems.size"))
        assertTrue(source.contains("contentReady = bottomPagerContentReady"))
        assertTrue(source.contains("resolveBottomPagerRenderBudget(isNavigating = mainBottomPagerState.isNavigating)"))
        assertFalse(source.contains("pendingBottomTabTransitionRoute"))
        assertFalse(source.contains("retainedBottomNavItem"))
        assertFalse(source.contains("resolveBottomTabTransitionTargetRoute"))
        assertFalse(source.contains("VerticalPager("))
    }

    @Test
    fun `bottom tab switch follows user input scroll mutation`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/MainBottomPagerState.kt")
        val sharedMotionSource = loadSource("app/src/main/java/com/android/purebilibili/navigation/PagerSelectionMotion.kt")
        val switchNavigationSource = source
            .substringAfter("fun switchToPage(")
            .substringBefore("fun syncPage(")

        assertTrue(source.contains("navigationStartPage"))
        assertTrue(switchNavigationSource.contains("animatePagerSelection(pagerState, safeTargetIndex)"))
        assertTrue(sharedMotionSource.contains("pagerState.scroll(MutatePriority.UserInput)"))
        assertTrue(sharedMotionSource.contains("scrollBy(value - consumedPx)"))
        assertTrue(sharedMotionSource.contains("easing = EaseInOut"))
        assertTrue(sharedMotionSource.contains("resolveBottomPagerNavigationDurationMillis(pageDistance)"))
        assertTrue(sharedMotionSource.contains("pagerState.scrollToPage(safeTargetPage)"))
        assertFalse(sharedMotionSource.contains("pagerState.animateScrollBy("))
        assertTrue(sharedMotionSource.contains("scrollDistancePx"))
        // No self-invented absolute seek / predictive progress path.
        assertFalse(source.contains("seekPredictiveReturnToPage"))
        assertFalse(source.contains("dispatchRawDelta"))
        assertFalse(source.contains("commitPredictiveReturnToPage"))
        assertFalse(source.contains("cancelPredictiveReturn"))
    }

    @Test
    fun `main bottom pager state coordinates page selection and indicator motion`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/MainBottomPagerState.kt")

        assertTrue(source.contains("@Stable\ninternal class MainBottomPagerState("))
        assertTrue(source.contains("val indicatorPosition: Float"))
        assertTrue(
            source.contains(
                "pagerState.currentPage + pagerState.currentPageOffsetFraction"
            )
        )
        assertTrue(source.contains("val isScrollInProgress: Boolean"))
        assertTrue(source.contains("get() = pagerState.isScrollInProgress"))
        assertTrue(source.contains("val indicatorPositionProvider: () -> Float"))
        assertTrue(source.contains("val scrollInProgressProvider: () -> Boolean"))
    }

    @Test
    fun `main bottom pager reconciles stale switches`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/MainBottomPagerState.kt")
        val switchNavigationSource = source
            .substringAfter("fun switchToPage(")
            .substringBefore("fun syncPage(")

        assertTrue(switchNavigationSource.contains("navJob?.cancel()"))
        assertTrue(switchNavigationSource.contains("if (navJob == myJob)"))
        assertTrue(switchNavigationSource.contains("selectedPage = pagerState.currentPage"))
        assertTrue(switchNavigationSource.contains("navigationStartPage = pagerState.currentPage"))
        assertFalse(switchNavigationSource.contains("withContext(NonCancellable)"))
        assertFalse(switchNavigationSource.contains("settleLatestNavigation"))
    }

    @Test
    fun `tab back handler wires BiliPai onBackCompleted to switchToPage home`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")

        assertTrue(source.contains("MainHostTabBackHandler("))
        assertTrue(source.contains("onReturnToHomeTab = {"))
        assertTrue(source.contains("mainBottomPagerState.switchToPage(homeIndex)"))
        assertFalse(source.contains("seekPredictiveReturnToPage"))
        assertFalse(source.contains("commitPredictiveReturnToPage"))
        assertFalse(source.contains("cancelPredictiveReturn"))
        assertFalse(source.contains("onPredictiveProgress"))
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
