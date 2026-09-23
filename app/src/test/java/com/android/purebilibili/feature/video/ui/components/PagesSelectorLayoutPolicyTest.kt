package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.data.model.response.Page
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PagesSelectorLayoutPolicyTest {

    @Test
    fun `page cards reserve two title lines so items stay equal height`() {
        val source = listOf(
            java.io.File("app/src/main/java/com/android/purebilibili/feature/video/ui/components/PagesSelector.kt"),
            java.io.File("src/main/java/com/android/purebilibili/feature/video/ui/components/PagesSelector.kt"),
        ).first { it.exists() }.readText()
        val itemBlock = source
            .substringAfter("private fun PageSelectorItem(")

        assertTrue(itemBlock.contains("minLines = 2"))
        assertTrue(itemBlock.contains("maxLines = 2"))
    }

    @Test
    fun compactPortraitWithManyPages_prefersPreviewWithExpandEntry() {
        val policy = resolvePagesSelectorLayoutPolicy(
            widthDp = 393,
            isLandscape = false,
            pagesCount = 110,
            forceGridMode = false
        )

        assertEquals(PagesSelectorPresentation.HorizontalPreview, policy.presentation)
        assertEquals(2, policy.gridColumns)
        assertTrue(shouldShowPagesExpandAction(policy = policy, pagesCount = 110))
    }

    @Test
    fun compactLandscape_prefersInlineGridForFastSelection() {
        val policy = resolvePagesSelectorLayoutPolicy(
            widthDp = 760,
            isLandscape = true,
            pagesCount = 110,
            forceGridMode = false
        )

        assertEquals(PagesSelectorPresentation.InlineGrid, policy.presentation)
        assertEquals(3, policy.gridColumns)
        assertFalse(shouldShowPagesExpandAction(policy = policy, pagesCount = 110))
    }

    @Test
    fun expandedWidth_usesDenserGridColumns() {
        val policy = resolvePagesSelectorLayoutPolicy(
            widthDp = 1280,
            isLandscape = true,
            pagesCount = 110,
            forceGridMode = false
        )

        assertEquals(PagesSelectorPresentation.InlineGrid, policy.presentation)
        assertEquals(5, policy.gridColumns)
        assertEquals(520, policy.maxGridHeightDp)
    }

    @Test
    fun forceGridMode_alwaysUsesInlineGrid() {
        val policy = resolvePagesSelectorLayoutPolicy(
            widthDp = 393,
            isLandscape = false,
            pagesCount = 110,
            forceGridMode = true
        )

        assertEquals(PagesSelectorPresentation.InlineGrid, policy.presentation)
        assertFalse(shouldShowPagesExpandAction(policy = policy, pagesCount = 110))
    }

    @Test
    fun chapterGroups_areResolvedFromNumericPrefix() {
        val pages = listOf(
            Page(page = 1, part = "1.1-简介"),
            Page(page = 2, part = "1.2-安装 Rust"),
            Page(page = 3, part = "2.1-猜数游戏"),
            Page(page = 4, part = "附录")
        )

        val groups = resolvePageSelectorGroups(pages)

        assertEquals(listOf("1", "2", "other"), groups.map { it.key })
        assertEquals(2, groups.first { it.key == "1" }.count)
        assertEquals(1, groups.first { it.key == "other" }.count)
    }

    @Test
    fun filterIndices_supportsGroupAndSearchTogether() {
        val pages = listOf(
            Page(page = 1, part = "1.1-简介"),
            Page(page = 2, part = "1.2-Hello Rust"),
            Page(page = 3, part = "2.1-Hello Cargo")
        )

        val indices = filterPageIndicesForSelector(
            pages = pages,
            selectedGroupKey = "1",
            query = "hello"
        )

        assertEquals(listOf(1), indices)
    }

    @Test
    fun bottomContentPadding_includesNavigationBarInset() {
        assertEquals(24, resolvePagesSelectorBottomContentPaddingDp(navigationBarBottomDp = 0))
        assertEquals(58, resolvePagesSelectorBottomContentPaddingDp(navigationBarBottomDp = 34))
    }
}
