package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchHotVisibilityPolicyTest {

    @Test
    fun hotHeaderHiddenWhenUserDisabledIt() {
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
    fun hotHeaderHiddenWhenNoDataExists() {
        assertFalse(
            shouldShowSearchHotHeader(
                hotItemCount = 0,
                hotSearchEnabled = true
            )
        )
    }
}
