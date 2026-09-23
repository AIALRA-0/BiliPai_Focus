package com.android.purebilibili.feature.dynamic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicBackToTopPolicyTest {

    @Test
    fun `back to top button stays hidden near top`() {
        assertFalse(
            shouldShowDynamicBackToTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0
            )
        )
        assertFalse(
            shouldShowDynamicBackToTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 220
            )
        )
    }

    @Test
    fun `back to top button appears after meaningful scroll`() {
        assertTrue(
            shouldShowDynamicBackToTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 720
            )
        )
        assertTrue(
            shouldShowDynamicBackToTop(
                firstVisibleItemIndex = 2,
                firstVisibleItemScrollOffset = 0
            )
        )
    }

    @Test
    fun `scrollToTop request only scrolls when not at top and never refreshes`() {
        val notAtTopPlan = resolveDynamicScrollActionPlan(
            request = DynamicScrollRequest.SCROLL_TO_TOP,
            isAtTop = false
        )
        assertTrue(notAtTopPlan.shouldScrollToTop)
        assertFalse(notAtTopPlan.shouldRefresh)

        val atTopPlan = resolveDynamicScrollActionPlan(
            request = DynamicScrollRequest.SCROLL_TO_TOP,
            isAtTop = true
        )
        assertFalse(atTopPlan.shouldScrollToTop)
        assertFalse(atTopPlan.shouldRefresh)
    }

    @Test
    fun `single click reselect scrolls to top when scrolled and refreshes when at top`() {
        val notAtTopPlan = resolveDynamicScrollActionPlan(
            request = DynamicScrollRequest.SCROLL_TO_TOP_OR_REFRESH,
            isAtTop = false
        )
        assertTrue(notAtTopPlan.shouldScrollToTop)
        assertFalse(notAtTopPlan.shouldRefresh)

        val atTopPlan = resolveDynamicScrollActionPlan(
            request = DynamicScrollRequest.SCROLL_TO_TOP_OR_REFRESH,
            isAtTop = true
        )
        assertFalse(atTopPlan.shouldScrollToTop)
        assertTrue(atTopPlan.shouldRefresh)
    }

    @Test
    fun `double click request always refreshes and scrolls when not at top`() {
        val notAtTopPlan = resolveDynamicScrollActionPlan(
            request = DynamicScrollRequest.SCROLL_TO_TOP_AND_REFRESH,
            isAtTop = false
        )
        assertTrue(notAtTopPlan.shouldScrollToTop)
        assertTrue(notAtTopPlan.shouldRefresh)

        val atTopPlan = resolveDynamicScrollActionPlan(
            request = DynamicScrollRequest.SCROLL_TO_TOP_AND_REFRESH,
            isAtTop = true
        )
        assertFalse(atTopPlan.shouldScrollToTop)
        assertTrue(atTopPlan.shouldRefresh)
    }
}

