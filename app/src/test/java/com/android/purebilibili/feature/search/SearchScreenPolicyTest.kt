package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchScreenPolicyTest {

    @Test
    fun `hot header stays hidden when hot search toggle is off`() {
        assertFalse(shouldShowSearchHotHeader(hotItemCount = 12, hotSearchEnabled = false))
        assertFalse(shouldShowSearchHotSection(hotItemCount = 12, hotSearchEnabled = false))
    }

    @Test
    fun `hot header and body appear only when enabled and data exists`() {
        assertTrue(shouldShowSearchHotHeader(hotItemCount = 1, hotSearchEnabled = true))
        assertTrue(shouldShowSearchHotSection(hotItemCount = 1, hotSearchEnabled = true))
        assertFalse(shouldShowSearchHotHeader(hotItemCount = 0, hotSearchEnabled = true))
    }

    @Test
    fun resetSearchScroll_onlyWhenShowingNonBlankResults() {
        assertTrue(
            shouldResetSearchResultScroll(
                searchSessionId = 1L,
                showResults = true,
                lastResetSessionId = 0L
            )
        )
        assertFalse(
            shouldResetSearchResultScroll(
                searchSessionId = 0L,
                showResults = true,
                lastResetSessionId = 0L
            )
        )
        assertFalse(
            shouldResetSearchResultScroll(
                searchSessionId = 2L,
                showResults = false,
                lastResetSessionId = 1L
            )
        )
    }

    @Test
    fun submitKeyword_prefersTypedQuery_thenFallsBackToSuggestedKeyword() {
        assertEquals(
            "黑神话悟空",
            resolveSearchSubmitKeyword(
                query = "  黑神话悟空 ",
                suggestedKeyword = "睡羊妹妹m"
            )
        )
        assertEquals(
            "睡羊妹妹m",
            resolveSearchSubmitKeyword(
                query = " ",
                suggestedKeyword = " 睡羊妹妹m "
            )
        )
        assertEquals(
            "",
            resolveSearchSubmitKeyword(
                query = "",
                suggestedKeyword = " "
            )
        )
    }
}
