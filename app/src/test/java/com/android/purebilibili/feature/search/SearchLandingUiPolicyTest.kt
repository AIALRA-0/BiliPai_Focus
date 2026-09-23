package com.android.purebilibili.feature.search

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchLandingUiPolicyTest {

    @Test
    fun `hot ranking header keeps a persistable visibility toggle`() {
        assertTrue(shouldShowSearchKeywordSectionVisibilityToggle(hasToggleHandler = true))
        assertFalse(shouldShowSearchKeywordSectionVisibilityToggle(hasToggleHandler = false))
        assertEquals("隐藏大家都在搜", resolveSearchKeywordSectionToggleContentDescription(true, "大家都在搜"))
        assertEquals("显示大家都在搜", resolveSearchKeywordSectionToggleContentDescription(false, "大家都在搜"))
        val source = java.io.File(
            "app/src/main/java/com/android/purebilibili/feature/search/SearchLandingUi.kt"
        ).takeIf { it.exists() } ?: java.io.File(
            "src/main/java/com/android/purebilibili/feature/search/SearchLandingUi.kt"
        )
        val header = source.readText()
            .substringAfter("private fun SearchKeywordSectionHeader(")
            .substringBefore("private fun SearchDiscoverOriginalCell(")
        assertTrue(header.contains("shouldShowSearchKeywordSectionVisibilityToggle("))
        assertFalse(header.contains("onToggleEnabled != null && useOriginalDiscoverStyle"))
    }

    @Test
    fun `search discovery section uses original style when trending action is absent`() {
        assertTrue(shouldUseOriginalSearchDiscoverStyle(showTrendingAction = false))
        assertFalse(shouldUseOriginalSearchDiscoverStyle(showTrendingAction = true))
    }

    @Test
    fun `search discovery section keeps two columns to match original layout`() {
        assertEquals(2, resolveSearchKeywordSectionColumns(requestedColumns = 1, showTrendingAction = false))
        assertEquals(2, resolveSearchKeywordSectionColumns(requestedColumns = 4, showTrendingAction = false))
        assertEquals(2, resolveSearchKeywordSectionColumns(requestedColumns = 3, showTrendingAction = true))
    }

    @Test
    fun `search landing section order matches bilipai portrait layout`() {
        assertEquals(
            listOf(
                SearchLandingSection.TRENDING,
                SearchLandingSection.HISTORY,
                SearchLandingSection.DISCOVER
            ),
            resolveSearchLandingSectionOrder()
        )
    }

    @Test
    fun `focus visibility preferences omit complete landing sections`() {
        val disabledSections = SearchLandingSection.entries.filterNot { section ->
            shouldShowSearchLandingSection(
                section = section,
                hotSearchEnabled = false,
                historySectionEnabled = false,
                discoverSectionEnabled = false,
            )
        }
        assertEquals(SearchLandingSection.entries.toSet(), disabledSections.toSet())

        assertTrue(
            shouldShowSearchLandingSection(
                section = SearchLandingSection.TRENDING,
                hotSearchEnabled = true,
                historySectionEnabled = false,
                discoverSectionEnabled = false,
            )
        )
        assertFalse(
            shouldShowSearchLandingSection(
                section = SearchLandingSection.HISTORY,
                hotSearchEnabled = true,
                historySectionEnabled = false,
                discoverSectionEnabled = false,
            )
        )
        assertTrue(
            shouldShowSearchLandingSection(
                section = SearchLandingSection.DISCOVER,
                hotSearchEnabled = false,
                historySectionEnabled = false,
                discoverSectionEnabled = true,
            )
        )
    }

    @Test
    fun `search discovery original cell colors stay neutral without theme primary`() {
        val lightScheme = lightColorScheme()
        val darkScheme = darkColorScheme()
        val light = resolveSearchDiscoverOriginalCellColors(lightScheme)
        val dark = resolveSearchDiscoverOriginalCellColors(darkScheme)

        assertTrue(light.containerColor.alpha > 0f)
        assertTrue(light.borderColor.alpha > 0f)
        // Must not tint with brand primary (搜索发现 stays neutral).
        assertTrue(light.containerColor.red != lightScheme.primary.red || light.containerColor.alpha < 0.3f)
        assertEquals(lightScheme.onSurface, light.titleColor)
        assertEquals(darkScheme.onSurface, dark.titleColor)
    }

    @Test
    fun `search discovery original subtitle keeps all non blank metadata`() {
        assertEquals("15小时前更新", resolveSearchDiscoverOriginalSubtitle("15小时前更新"))
        assertEquals("47分钟前更新", resolveSearchDiscoverOriginalSubtitle("47分钟前更新"))
        assertEquals("关注的 UP 主", resolveSearchDiscoverOriginalSubtitle("关注的 UP 主"))
        assertEquals("与最近搜索相关", resolveSearchDiscoverOriginalSubtitle("与最近搜索相关"))
        assertEquals(null, resolveSearchDiscoverOriginalSubtitle("   "))
    }

    @Test
    fun `landing sections are fully guarded by focus visibility switches`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/search/SearchLandingUi.kt")
            .replace(Regex("\\s+"), " ")

        assertTrue(source.contains("if (hotSearchEnabled) item { TrendingSection() }"))
        assertTrue(source.contains("if (discoverSectionEnabled) item { DiscoverSection() }"))
        assertTrue(source.contains("if (historySectionEnabled) item { HistorySection() }"))
        assertTrue(source.contains("sectionOrder.filter { section ->"))
        assertTrue(source.contains("shouldShowSearchLandingSection("))
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
