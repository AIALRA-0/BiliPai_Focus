package com.android.purebilibili.feature.settings.screen

import com.android.purebilibili.core.plugin.skin.SkinCatalog
import com.android.purebilibili.core.plugin.skin.SkinCatalogEntry
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkinCatalogSearchPolicyTest {

    @Test
    fun searchMatchesDisplayNameAndIdIgnoringCase() {
        val themes = listOf(
            catalogEntry(id = "winter-cloud", name = "冬日云朵"),
            catalogEntry(id = "summer-sea", name = "夏日海岸"),
        )

        assertEquals(
            listOf("winter-cloud"),
            SkinCatalogUiState(catalog = catalog(themes), searchQuery = "云朵")
                .filteredThemes.map { it.id },
        )
        assertEquals(
            listOf("summer-sea"),
            SkinCatalogUiState(catalog = catalog(themes), searchQuery = "SUMMER")
                .filteredThemes.map { it.id },
        )
    }

    @Test
    fun catalogScreenExposesSearchFieldAndEmptyResultFeedback() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/settings/screen/SkinCatalogScreen.kt"
        ).readText()

        assertTrue(source.contains("AppLiquidAwareSearchField("))
        assertTrue(source.contains("onQueryChange = stateHolder::setSearchQuery"))
        assertTrue(source.contains("没有找到相关装扮"))
    }

    private fun catalog(themes: List<SkinCatalogEntry>) = SkinCatalog(
        catalogVersion = 1,
        sourceRepo = "test/repo",
        sourceBranch = "main",
        themes = themes,
    )

    private fun catalogEntry(id: String, name: String) = SkinCatalogEntry(
        id = id,
        name = name,
        previewUrl = "https://example.com/$id.jpg",
        packageZipUrl = "https://example.com/$id.zip",
    )
}
