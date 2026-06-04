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
    fun `search discovery section uses original style when trending action is absent`() {
        assertTrue(shouldUseOriginalSearchDiscoverStyle(showTrendingAction = false))
        assertFalse(shouldUseOriginalSearchDiscoverStyle(showTrendingAction = true))
    }

    @Test
    fun `search discovery section keeps two columns to match original layout`() {
        assertEquals(2, resolveSearchKeywordSectionColumns(requestedColumns = 1, showTrendingAction = false))
        assertEquals(2, resolveSearchKeywordSectionColumns(requestedColumns = 4, showTrendingAction = false))
        assertEquals(3, resolveSearchKeywordSectionColumns(requestedColumns = 3, showTrendingAction = true))
    }

    @Test
    fun `search discovery original cell colors use themed primary tint`() {
        val light = resolveSearchDiscoverOriginalCellColors(lightColorScheme())
        val dark = resolveSearchDiscoverOriginalCellColors(darkColorScheme())

        assertTrue(light.containerColor.alpha > 0f)
        assertTrue(light.borderColor.alpha > 0f)
        assertTrue(dark.containerColor.alpha > light.containerColor.alpha)
    }

    @Test
    fun `search discovery original subtitle keeps update metadata but hides generic reasons`() {
        assertEquals("15小时前更新", resolveSearchDiscoverOriginalSubtitle("15小时前更新"))
        assertEquals("47分钟前更新", resolveSearchDiscoverOriginalSubtitle("47分钟前更新"))
        assertEquals(null, resolveSearchDiscoverOriginalSubtitle("关注的 UP 主"))
        assertEquals(null, resolveSearchDiscoverOriginalSubtitle("与最近搜索相关"))
    }

    @Test
    fun `landing sections are fully guarded by focus visibility switches`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/search/SearchLandingUi.kt")
            .replace(Regex("\\s+"), " ")

        assertTrue(source.contains("if (hotSearchEnabled) { item { SearchKeywordSection( title = \"大家都在搜\""))
        assertTrue(source.contains("if (discoverSectionEnabled) { item { SearchKeywordSection( title = discoverTitle"))
        assertTrue(source.contains("if (historySectionEnabled) { item { SearchHistorySectionModern("))
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
