package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchHotVisibilityPolicyTest {

    @Test
    fun hotHeaderVisibleWhenDisabledSoUserCanRestoreIt() {
        assertTrue(
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

    @Test
    fun keywordSectionHiddenCopy_mentionsSectionTitle() {
        assertEquals("已隐藏大家都在搜", resolveSearchKeywordSectionHiddenText("大家都在搜"))
        assertEquals("已隐藏搜索发现", resolveSearchKeywordSectionHiddenText("搜索发现"))
    }
}
