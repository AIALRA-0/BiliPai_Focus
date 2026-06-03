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
        assertTrue(source.contains("userScrollEnabled = shouldEnableBottomPagerUserScroll()"))
        assertTrue(source.contains("resolveBottomPagerRenderBudget(isNavigating = mainBottomPagerState.isNavigating)"))
        assertFalse(source.contains("pendingBottomTabTransitionRoute"))
        assertFalse(source.contains("retainedBottomNavItem"))
        assertFalse(source.contains("resolveBottomTabTransitionTargetRoute"))
        assertFalse(source.contains("VerticalPager("))
    }

    @Test
    fun `main bottom pager keeps continuous scroll and tracks transition start page`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/MainBottomPagerState.kt")

        assertTrue(source.contains("navigationStartPage"))
        assertTrue(source.contains("pagerState.animateScrollBy("))
        assertFalse(source.contains("shouldUseDirectBottomPagerJump("))
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
