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
        assertTrue(source.contains("dragSelectionEnabled = labels.size > 1"))
        assertTrue(source.contains("liquidGlassEffectsEnabled = true"))
        assertTrue(source.contains("tapPressRefractionEnabled = true"))
        assertTrue(source.contains("miuixBackdrop = popularBackdrop"))
        assertTrue(source.contains("preferInlineContentStyle = true"))
        assertTrue(source.contains("height = AppSpacingTokens.TripleExtraLarge"))
        assertTrue(source.contains("roundMatchedLiquidIndicatorHeightDp("))
        assertTrue(
            source.contains(
                ".padding(horizontal = AppSpacingTokens.Small, vertical = AppSpacingTokens.None)"
            )
        )
        assertFalse(source.contains("PopularSubCategory.entries.forEach { subCategory ->\n                            FilterChip("))
    }

    @Test
    fun `popular subcategory content switches by click without nested pager`() {
        val screenSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt"
        )
        val viewModelSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/HomeViewModel.kt"
        )
        val switchSource = viewModelSource
            .substringAfter("fun switchPopularSubCategory")
            .substringBefore("//  [新增] 添加到稍后再看")

        assertFalse(screenSource.contains("val popularPagerState = rememberPagerState("))
        assertTrue(screenSource.contains(".getPopularCategoryState(popularSubCategory)"))
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
