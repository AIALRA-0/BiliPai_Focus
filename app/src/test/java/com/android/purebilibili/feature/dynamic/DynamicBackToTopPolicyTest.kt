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
}
