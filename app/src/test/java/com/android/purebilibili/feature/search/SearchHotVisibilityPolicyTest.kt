package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchHotVisibilityPolicyTest {

    @Test
    fun hotHeaderHiddenWhenDisabled() {
        assertFalse(
            shouldShowSearchHotHeader(
                hotItemCount = 10,
                hotSearchEnabled = false
            )
        )
    }

    @Test
    fun hotSectionHiddenWhenUserDisabledIt() {
        assertFalse(
            shouldShowSearchHotSection(
                hotItemCount = 10,
                hotSearchEnabled = false
            )
        )
    }

    @Test
    fun hotSectionHiddenWhenNoHotItemsExist() {
        assertFalse(
            shouldShowSearchHotSection(
                hotItemCount = 0,
                hotSearchEnabled = true
            )
        )
    }

    @Test
    fun hotSectionShownWhenEnabledAndDataExists() {
        assertTrue(
            shouldShowSearchHotSection(
                hotItemCount = 6,
                hotSearchEnabled = true
            )
        )
    }

    @Test
    fun hotHeaderHiddenWhenDisabledAndNoDataExists() {
        assertFalse(
            shouldShowSearchHotHeader(
                hotItemCount = 0,
                hotSearchEnabled = false
            )
        )
    }

    @Test
    fun keywordSectionToggleLabel_matchesVisibilityState() {
        assertEquals("隐藏", resolveSearchKeywordSectionToggleLabel(enabled = true))
        assertEquals("显示", resolveSearchKeywordSectionToggleLabel(enabled = false))
    }
}
