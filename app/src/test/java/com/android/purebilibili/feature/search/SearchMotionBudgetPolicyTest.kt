package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchMotionBudgetPolicyTest {

    @Test
    fun activeSearchInteraction_reducesBudget() {
        assertEquals(
            SearchMotionBudget.REDUCED,
            resolveSearchMotionBudget(
                hasQuery = true,
                isSearching = true,
                isScrolling = false
            )
        )
        assertEquals(
            SearchMotionBudget.REDUCED,
            resolveSearchMotionBudget(
                hasQuery = true,
                isSearching = false,
                isScrolling = true
            )
        )
    }

    @Test
    fun scrollingResults_keepsHazeEnabled() {
        val budget = resolveSearchMotionBudget(
            hasQuery = true,
            isSearching = false,
            isScrolling = true
        )

        assertEquals(SearchMotionBudget.REDUCED, budget)
        assertTrue(
            shouldEnableSearchHazeSource(
                isSearching = false
            )
        )
    }

    @Test
    fun idleSearchState_keepsFullBudgetAndHaze() {
        val budget = resolveSearchMotionBudget(
            hasQuery = false,
            isSearching = false,
            isScrolling = false
        )

        assertEquals(SearchMotionBudget.FULL, budget)
        assertTrue(
            shouldEnableSearchHazeSource(
                isSearching = false
            )
        )
    }

    @Test
    fun activeSearchRequest_disablesHazeSource() {
        assertFalse(
            shouldEnableSearchHazeSource(
                isSearching = true
            )
        )
    }
}
