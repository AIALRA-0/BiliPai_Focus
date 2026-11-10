package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DynamicDeferredStartupPolicyTest {
    @Test
    fun coldStartShowsLoadingInsteadOfEmptyFeedBeforeActivation() {
        val state = createDynamicStartupUiState()
        val presentation = resolveDynamicPagePresentation(state, 0, null)

        assertEquals("all", state.timelineRequestType)
        assertTrue(state.isLoading)
        assertTrue(presentation.items.isEmpty())
        assertTrue(presentation.isLoading)
        assertNull(presentation.error)
    }

    @Test
    fun restoredTimelineTabStartsLoadingBeforeActivation() {
        for (selectedTab in 1..3) {
            val state = createDynamicStartupUiState(selectedTab)
            val presentation = resolveDynamicPagePresentation(state, selectedTab, null)

            assertEquals(resolveDynamicFeedRequestType(selectedTab), state.timelineRequestType)
            assertTrue(state.isLoading)
            assertTrue(presentation.items.isEmpty())
            assertTrue(presentation.isLoading)
            assertNull(presentation.error)
        }
    }

    @Test
    fun restoredUpTabStillShowsUserSelectionInsteadOfTimelineLoading() {
        val state = createDynamicStartupUiState(4)
        val presentation = resolveDynamicPagePresentation(state, 4, null)

        assertFalse(presentation.isSelectedUserFeed)
        assertFalse(presentation.isLoading)
    }

    @Test
    fun successfulEmptyResponseEndsStartupLoading() {
        val initialState = createDynamicStartupUiState()
        val state = updateDynamicTimelinePage(initialState, "all") { page ->
            resolveDynamicTimelinePageAfterSuccess(
                currentPage = page,
                incomingItems = emptyList(),
                isRefresh = true,
                incrementalRefreshEnabled = false,
                hasMore = false,
            )
        }
        val presentation = resolveDynamicPagePresentation(state, 0, null)

        assertTrue(presentation.items.isEmpty())
        assertFalse(presentation.isLoading)
        assertFalse(presentation.hasMore)
        assertNull(presentation.error)
    }

    @Test
    fun failedInitialRequestEndsStartupLoadingAndShowsError() {
        val initialState = createDynamicStartupUiState(1)
        val state = updateDynamicTimelinePage(initialState, "video") { page ->
            resolveDynamicTimelinePageAfterFailure(
                currentPage = page,
                errorMessage = "network unavailable",
                refresh = true,
            )
        }
        val presentation = resolveDynamicPagePresentation(state, 1, null)

        assertFalse(presentation.isLoading)
        assertEquals("network unavailable", presentation.error)
        assertEquals(DynamicFeedErrorSource.INITIAL_LOAD, state.errorSource)
    }

    @Test
    fun sameAccountWithNoRequestCanUseCachePlaceholder() {
        assertTrue(shouldApplyDeferredDynamicCache(true, null, null, false))
    }

    @Test
    fun switchingAccountsDuringCacheReadRejectsOldCache() {
        assertFalse(shouldApplyDeferredDynamicCache(false, null, null, false))
    }

    @Test
    fun newerTimelineRequestRejectsCacheEvenBeforeItReturnsItems() {
        assertFalse(shouldApplyDeferredDynamicCache(true, null, 1L, false))
        assertFalse(shouldApplyDeferredDynamicCache(true, 1L, 2L, false))
    }

    @Test
    fun requestAlreadyStartedBeforeCacheReadKeepsItsLoadingState() {
        assertFalse(shouldApplyDeferredDynamicCache(true, 1L, 1L, false))
    }

    @Test
    fun existingTimelineItemsCannotBeOverwrittenByStartupCache() {
        assertFalse(shouldApplyDeferredDynamicCache(true, null, null, true))
    }
}
