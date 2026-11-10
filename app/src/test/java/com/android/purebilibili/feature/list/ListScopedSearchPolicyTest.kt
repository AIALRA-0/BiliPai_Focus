package com.android.purebilibili.feature.list

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListScopedSearchPolicyTest {

    @Test
    fun listScopedSearch_requiresBottomBarSearchEnabled() {
        assertTrue(
            isListScopedSearchActive(
                bottomBarSearchEnabled = true,
                listScopedSearchEnabled = true,
            )
        )
        assertFalse(
            isListScopedSearchActive(
                bottomBarSearchEnabled = false,
                listScopedSearchEnabled = true,
            )
        )
        assertFalse(
            isListScopedSearchActive(
                bottomBarSearchEnabled = true,
                listScopedSearchEnabled = false,
            )
        )
        assertFalse(
            isListScopedSearchActive(
                bottomBarSearchEnabled = false,
                listScopedSearchEnabled = false,
            )
        )
    }

    @Test
    fun hideTopSearchBar_keepsSearchDestinationChrome() {
        assertTrue(
            shouldHideListTopSearchBar(
                bottomBarSearchEnabled = true,
                listScopedSearchEnabled = true,
                isSearchDestination = false,
            )
        )
        assertFalse(
            shouldHideListTopSearchBar(
                bottomBarSearchEnabled = true,
                listScopedSearchEnabled = true,
                isSearchDestination = true,
            )
        )
        assertFalse(
            shouldHideListTopSearchBar(
                bottomBarSearchEnabled = false,
                listScopedSearchEnabled = true,
                isSearchDestination = false,
            )
        )
    }

    @Test
    fun activeBar_requiresNonBlankQueryAndEnabledFlags() {
        assertTrue(
            shouldShowListScopedSearchActiveBar(
                bottomBarSearchEnabled = true,
                listScopedSearchEnabled = true,
                searchQuery = "  靖  ",
            )
        )
        assertFalse(
            shouldShowListScopedSearchActiveBar(
                bottomBarSearchEnabled = true,
                listScopedSearchEnabled = true,
                searchQuery = "   ",
            )
        )
        assertFalse(
            shouldShowListScopedSearchActiveBar(
                bottomBarSearchEnabled = true,
                listScopedSearchEnabled = false,
                searchQuery = "靖",
            )
        )
    }

    @Test
    fun activeBarLabel_usesTrimmedQuery() {
        assertEquals("搜索中：靖", resolveListScopedSearchActiveBarLabel("  靖  "))
        assertEquals("搜索中", resolveListScopedSearchActiveBarLabel("   "))
    }
}
