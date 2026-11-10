package com.android.purebilibili.feature.list

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FavoriteFolderSwitchMotionPolicyTest {

    @Test
    fun `near folder hops keep full intermediate traversal`() {
        assertNull(resolveFavoriteFolderSwitchPreJumpPage(currentPage = 0, targetPage = 1))
        assertNull(resolveFavoriteFolderSwitchPreJumpPage(currentPage = 2, targetPage = 6))
        assertNull(resolveFavoriteFolderSwitchPreJumpPage(currentPage = 10, targetPage = 7))
    }

    @Test
    fun `distant folder hops pre-jump and animate only the final window`() {
        assertEquals(
            6,
            resolveFavoriteFolderSwitchPreJumpPage(currentPage = 0, targetPage = 10)
        )
        assertEquals(
            36,
            resolveFavoriteFolderSwitchPreJumpPage(currentPage = 11, targetPage = 40)
        )
        assertEquals(
            4,
            resolveFavoriteFolderSwitchPreJumpPage(currentPage = 30, targetPage = 0)
        )
    }

    @Test
    fun `traversal window honors custom max and never drops below one page`() {
        assertEquals(
            8,
            resolveFavoriteFolderSwitchPreJumpPage(
                currentPage = 0,
                targetPage = 10,
                maxTraversedPages = 2
            )
        )
        assertEquals(
            9,
            resolveFavoriteFolderSwitchPreJumpPage(
                currentPage = 0,
                targetPage = 10,
                maxTraversedPages = 0
            )
        )
    }

    @Test
    fun `default traversal window matches favorite folder switch cap`() {
        assertEquals(4, FAVORITE_FOLDER_SWITCH_MAX_TRAVERSED_PAGES)
    }
}
