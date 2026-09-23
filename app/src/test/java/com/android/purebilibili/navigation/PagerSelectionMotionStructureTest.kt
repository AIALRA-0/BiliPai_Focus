package com.android.purebilibili.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PagerSelectionMotionStructureTest {

    @Test
    fun `tab selection motion scrolls through intermediate pages`() {
        val source = loadSource("navigation/PagerSelectionMotion.kt")

        assertTrue(source.contains("pagerState.scroll(MutatePriority.UserInput)"))
        assertTrue(source.contains("scrollBy(value - consumedPx)"))
        assertTrue(source.contains("resolveBottomPagerNavigationDurationMillis(pageDistance)"))
        assertFalse(source.contains("pagerState.animateScrollToPage("))
    }

    @Test
    fun `primary tab pagers share continuous selection motion`() {
        val bottom = loadSource("navigation/MainBottomPagerState.kt")
        val home = loadSource("feature/home/HomeScreen.kt")
        val dynamic = loadSource("feature/dynamic/DynamicScreen.kt")
        val search = loadSource("feature/search/SearchScreen.kt")
        val commonList = loadSource("feature/list/CommonListScreen.kt")

        assertTrue(bottom.contains("animatePagerSelection(pagerState, safeTargetIndex)"))
        assertTrue(home.contains("animatePagerSelection(pagerState, index)"))
        assertTrue(dynamic.contains("animatePagerSelection(pagerState, visibleIndex)"))
        assertTrue(search.contains("animatePagerSelection(searchPagerState, page)"))
        assertTrue(commonList.contains("animatePagerSelection(pagerState, targetPage)"))
    }

    private fun loadSource(relativePath: String): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/$relativePath"),
            File("src/main/java/com/android/purebilibili/$relativePath")
        ).first { it.exists() }.readText()
    }
}
