package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomePopularSubCategorySegmentedControlStructureTest {

    @Test
    fun `popular subcategory row delegates to bottom bar liquid segmented control`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/HomeCategoryPage.kt"
        )

        assertTrue(source.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(source.contains("PopularSubCategorySegmentedControl("))
        assertTrue(source.contains("dragSelectionEnabled = true"))
        assertTrue(source.contains("liquidGlassEffectsEnabled = true"))
        assertTrue(source.contains("preferInlineContentStyle = true"))
        assertFalse(source.contains("PopularSubCategory.entries.forEach { subCategory ->\n                            FilterChip("))
    }

    @Test
    fun `popular subcategory content switches through pager instead of clearing list`() {
        val screenSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt"
        )
        val viewModelSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/HomeViewModel.kt"
        )
        val switchSource = viewModelSource
            .substringAfter("fun switchPopularSubCategory")
            .substringBefore("//  [新增] 添加到稍后再看")

        assertTrue(screenSource.contains("val popularPagerState = rememberPagerState("))
        assertTrue(screenSource.contains("HorizontalPager(\n                                         state = popularPagerState"))
        assertTrue(screenSource.contains(".getPopularCategoryState(subCategory)"))
        assertTrue(viewModelSource.contains("popularCategoryStates = PopularSubCategory.entries.associateWith"))
        assertTrue(viewModelSource.contains("private fun updatePopularCategoryState("))
        assertTrue(viewModelSource.contains("fun getPopularCategoryState(subCategory: PopularSubCategory)"))
        assertFalse(switchSource.contains("videos = emptyList()"))
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
