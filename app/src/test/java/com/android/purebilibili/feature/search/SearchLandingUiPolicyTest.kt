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
    fun `visibility toggle requires a handler`() {
        assertTrue(shouldShowSearchKeywordSectionVisibilityToggle(hasToggleHandler = true))
        assertFalse(shouldShowSearchKeywordSectionVisibilityToggle(hasToggleHandler = false))
    }

    @Test
    fun `normal phone keeps labeled section actions`() {
        assertFalse(shouldUseCompactSearchSectionActions(widthDp = 412, fontScale = 1f))
    }

    @Test
    fun `narrow phone keeps section actions reachable as icons`() {
        assertTrue(shouldUseCompactSearchSectionActions(widthDp = 320, fontScale = 1f))
    }

    @Test
    fun `large font avoids wrapping section action labels`() {
        assertTrue(shouldUseCompactSearchSectionActions(widthDp = 412, fontScale = 2f))
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
        val order = resolveSearchLandingSectionOrder()
        assertEquals(
            emptyList(),
            resolveVisibleSearchLandingSectionOrder(
                sectionOrder = order,
                showHotSection = false,
                showHistorySection = false,
                showDiscoverSection = false,
            ),
        )
        assertEquals(
            listOf(SearchLandingSection.TRENDING),
            resolveVisibleSearchLandingSectionOrder(
                sectionOrder = order,
                showHotSection = true,
                showHistorySection = false,
                showDiscoverSection = false,
            ),
        )
        assertEquals(
            listOf(SearchLandingSection.TRENDING, SearchLandingSection.DISCOVER),
            resolveVisibleSearchLandingSectionOrder(
                sectionOrder = order,
                showHotSection = true,
                showHistorySection = false,
                showDiscoverSection = true,
            ),
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
        val policySource = loadSource("app/src/main/java/com/android/purebilibili/feature/search/SearchVideoFilterPolicy.kt")
            .replace(Regex("\\s+"), " ")

        assertTrue(source.contains("resolveVisibleSearchLandingSectionOrder("))
        assertTrue(source.contains("visibleSectionOrder.forEach { section ->"))
        assertTrue(policySource.contains("internal fun resolveVisibleSearchLandingSectionOrder("))
        assertTrue(policySource.contains("SearchLandingSection.TRENDING -> showHotSection"))
        assertTrue(policySource.contains("SearchLandingSection.HISTORY -> showHistorySection"))
        assertTrue(policySource.contains("SearchLandingSection.DISCOVER -> showDiscoverSection"))
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
