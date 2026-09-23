package com.android.purebilibili.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomPagerGlassContinuityStructureTest {
    @Test
    fun bottomPager_preloadsEveryOtherVisiblePage() {
        assertEquals(4, BOTTOM_PAGER_MAX_PRELOAD_DISTANCE)
        assertEquals(4, resolveBottomPagerBeyondViewportPageCount(pageCount = 5, contentReady = true))
    }

    @Test
    fun bottomBar_keepsLiquidGlassMountedDuringPageSwitch() {
        val source = sourceFile("navigation/AppNavigation.kt")
        val bottomBarSection = source.substringAfter("if (bottomBarCanMount)")

        assertTrue(bottomBarSection.contains("forceLowBlurBudget = false"))
        assertFalse(
            bottomBarSection.contains(
                "forceLowBlurBudget = bottomPagerRenderBudget.forceLowBlurBudget"
            )
        )
    }

    @Test
    fun bottomBarIndicator_followsPagerPositionDuringUserDrag() {
        val source = sourceFile("navigation/AppNavigation.kt")
        val bottomBarSection = source.substringAfter("if (bottomBarCanMount)")

        assertTrue(
            bottomBarSection.contains(
                "mainBottomPagerState.indicatorPositionProvider"
            )
        )
        assertTrue(
            bottomBarSection.contains(
                "mainBottomPagerState.scrollInProgressProvider"
            )
        )
        assertFalse(bottomBarSection.contains("bottomPagerState.currentPageOffsetFraction"))
    }

    private fun sourceFile(relativePath: String): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/$relativePath"),
            File("src/main/java/com/android/purebilibili/$relativePath"),
        ).first { it.exists() }.readText()
    }
}
