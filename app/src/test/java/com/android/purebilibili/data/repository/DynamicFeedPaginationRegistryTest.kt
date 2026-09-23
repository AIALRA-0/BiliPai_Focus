package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicFeedPaginationRegistryTest {

    @Test
    fun states_areIsolatedBetweenScopes() {
        val registry = DynamicFeedPaginationRegistry()

        registry.update(
            scope = DynamicFeedScope.HOME_FOLLOW,
            type = "all",
            offset = "home_offset",
            updateBaseline = "home_baseline",
            hasMore = false
        )
        registry.update(
            scope = DynamicFeedScope.DYNAMIC_SCREEN,
            type = "all",
            offset = "dynamic_offset",
            updateBaseline = "dynamic_baseline",
            hasMore = true
        )

        assertEquals("home_offset", registry.offset(DynamicFeedScope.HOME_FOLLOW))
        assertEquals("dynamic_offset", registry.offset(DynamicFeedScope.DYNAMIC_SCREEN))
        assertEquals("home_baseline", registry.updateBaseline(DynamicFeedScope.HOME_FOLLOW))
        assertEquals("dynamic_baseline", registry.updateBaseline(DynamicFeedScope.DYNAMIC_SCREEN))
        assertFalse(registry.hasMore(DynamicFeedScope.HOME_FOLLOW))
        assertTrue(registry.hasMore(DynamicFeedScope.DYNAMIC_SCREEN))
    }

    @Test
    fun reset_onlyAffectsTargetScope() {
        val registry = DynamicFeedPaginationRegistry()
        registry.update(
            DynamicFeedScope.HOME_FOLLOW,
            type = "all",
            offset = "home_offset",
            updateBaseline = "home_baseline",
            hasMore = false
        )
        registry.update(
            DynamicFeedScope.DYNAMIC_SCREEN,
            type = "all",
            offset = "dynamic_offset",
            updateBaseline = "dynamic_baseline",
            hasMore = false
        )

        registry.reset(DynamicFeedScope.HOME_FOLLOW)

        assertEquals("", registry.offset(DynamicFeedScope.HOME_FOLLOW))
        assertEquals("", registry.updateBaseline(DynamicFeedScope.HOME_FOLLOW))
        assertTrue(registry.hasMore(DynamicFeedScope.HOME_FOLLOW))
        assertEquals("dynamic_offset", registry.offset(DynamicFeedScope.DYNAMIC_SCREEN))
        assertEquals("dynamic_baseline", registry.updateBaseline(DynamicFeedScope.DYNAMIC_SCREEN))
        assertFalse(registry.hasMore(DynamicFeedScope.DYNAMIC_SCREEN))
    }

    @Test
    fun states_areIsolatedBetweenDynamicTypes() {
        val registry = DynamicFeedPaginationRegistry()

        registry.update(
            scope = DynamicFeedScope.DYNAMIC_SCREEN,
            type = "pgc",
            offset = "pgc_offset",
            updateBaseline = "pgc_baseline",
            hasMore = false
        )
        registry.update(
            scope = DynamicFeedScope.DYNAMIC_SCREEN,
            type = "all",
            offset = "all_offset",
            updateBaseline = "all_baseline",
            hasMore = true
        )

        assertEquals("pgc_offset", registry.offset(DynamicFeedScope.DYNAMIC_SCREEN, type = "pgc"))
        assertEquals("pgc_baseline", registry.updateBaseline(DynamicFeedScope.DYNAMIC_SCREEN, type = "pgc"))
        assertFalse(registry.hasMore(DynamicFeedScope.DYNAMIC_SCREEN, type = "pgc"))
        assertEquals("all_offset", registry.offset(DynamicFeedScope.DYNAMIC_SCREEN, type = "all"))
        assertEquals("all_baseline", registry.updateBaseline(DynamicFeedScope.DYNAMIC_SCREEN, type = "all"))
        assertTrue(registry.hasMore(DynamicFeedScope.DYNAMIC_SCREEN, type = "all"))
    }

    @Test
    fun updateBaseline_preservesPaginationState() {
        val registry = DynamicFeedPaginationRegistry()
        registry.update(
            scope = DynamicFeedScope.DYNAMIC_SCREEN,
            type = "all",
            offset = "offset",
            updateBaseline = "old_baseline",
            hasMore = false
        )

        registry.updateBaseline(
            scope = DynamicFeedScope.DYNAMIC_SCREEN,
            type = "all",
            updateBaseline = "new_baseline"
        )

        assertEquals("offset", registry.offset(DynamicFeedScope.DYNAMIC_SCREEN, type = "all"))
        assertEquals("new_baseline", registry.updateBaseline(DynamicFeedScope.DYNAMIC_SCREEN, type = "all"))
        assertFalse(registry.hasMore(DynamicFeedScope.DYNAMIC_SCREEN, type = "all"))
    }

    @Test
    fun fullRefresh_usesResponsePaginationInsteadOfOldCursor() {
        val result = resolveDynamicPaginationStateAfterPage(
            paginationBeforeRefresh = DynamicPaginationState(),
            responseOffset = "fresh_offset",
            responseUpdateBaseline = "fresh_baseline",
            responseHasMore = true,
            preserveExistingPagination = false
        )

        assertEquals("fresh_offset", result.offset)
        assertEquals("fresh_baseline", result.updateBaseline)
        assertTrue(result.hasMore)
    }

    @Test
    fun incrementalRefresh_preservesOlderTimelinePaginationAndAdvancesBaseline() {
        val result = resolveDynamicPaginationStateAfterPage(
            paginationBeforeRefresh = DynamicPaginationState(
                offset = "older_page_offset",
                updateBaseline = "old_baseline",
                hasMore = true
            ),
            responseOffset = "incremental_window_offset",
            responseUpdateBaseline = "new_baseline",
            responseHasMore = false,
            preserveExistingPagination = true
        )

        assertEquals("older_page_offset", result.offset)
        assertEquals("new_baseline", result.updateBaseline)
        assertTrue(result.hasMore)
    }

    @Test
    fun incrementalRefresh_withBlankOldOffset_usesResponseOffset() {
        val result = resolveDynamicPaginationStateAfterPage(
            paginationBeforeRefresh = DynamicPaginationState(
                offset = "",
                updateBaseline = "old_baseline",
                hasMore = true
            ),
            responseOffset = "fresh_response_offset",
            responseUpdateBaseline = "new_baseline",
            responseHasMore = true,
            preserveExistingPagination = true
        )

        assertEquals("fresh_response_offset", result.offset)
        assertEquals("new_baseline", result.updateBaseline)
        assertTrue(result.hasMore)
    }

    @Test
    fun incrementalRefresh_withZeroUpdateNum_usesResponseOffset() {
        val result = resolveDynamicPaginationStateAfterPage(
            paginationBeforeRefresh = DynamicPaginationState(
                offset = "stale_offset",
                updateBaseline = "old_baseline",
                hasMore = true
            ),
            responseOffset = "fresh_response_offset",
            responseUpdateBaseline = "new_baseline",
            responseHasMore = true,
            preserveExistingPagination = true,
            reportedUpdateNum = 0
        )

        assertEquals("fresh_response_offset", result.offset)
        assertEquals("new_baseline", result.updateBaseline)
        assertTrue(result.hasMore)
    }

    @Test
    fun incrementalRefresh_requiresEnabledSettingAndEstablishedBaseline() {
        assertFalse(
            shouldUseDynamicIncrementalRefresh(
                refresh = true,
                incrementalRefreshEnabled = false,
                updateBaseline = "baseline"
            )
        )
        assertFalse(
            shouldUseDynamicIncrementalRefresh(
                refresh = true,
                incrementalRefreshEnabled = true,
                updateBaseline = ""
            )
        )
        assertTrue(
            shouldUseDynamicIncrementalRefresh(
                refresh = true,
                incrementalRefreshEnabled = true,
                updateBaseline = "baseline"
            )
        )
    }

    @Test
    fun update_count_polling_doesNotAdvanceExistingBaselineUntilFeedIsRead() {
        assertEquals(
            "old_baseline",
            resolveDynamicUpdateCountBaseline(
                currentBaseline = "old_baseline",
                responseBaseline = "new_baseline",
                advanceBaseline = false
            )
        )
        assertEquals(
            "",
            resolveDynamicUpdateCountBaseline(
                currentBaseline = "",
                responseBaseline = "new_baseline",
                advanceBaseline = false
            )
        )
        assertEquals(
            "new_baseline",
            resolveDynamicUpdateCountBaseline(
                currentBaseline = "old_baseline",
                responseBaseline = "new_baseline",
                advanceBaseline = true
            )
        )
    }
}
