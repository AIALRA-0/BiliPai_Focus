package com.android.purebilibili.feature.bangumi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BangumiChromeCollapsePolicyTest {

    @Test
    fun `index chrome stays expanded near top`() {
        assertFalse(
            shouldCollapseBangumiIndexChrome(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 48
            )
        )
    }

    @Test
    fun `index chrome collapses after scrolling past threshold`() {
        assertTrue(
            shouldCollapseBangumiIndexChrome(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 180
            )
        )
    }

    @Test
    fun `index chrome collapses once list leaves first item`() {
        assertTrue(
            shouldCollapseBangumiIndexChrome(
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0
            )
        )
    }

    @Test
    fun `back to top button stays hidden near top`() {
        assertFalse(
            shouldShowBangumiIndexBackToTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 240
            )
        )
    }

    @Test
    fun `back to top button appears after meaningful scroll`() {
        assertTrue(
            shouldShowBangumiIndexBackToTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 720
            )
        )
        assertTrue(
            shouldShowBangumiIndexBackToTop(
                firstVisibleItemIndex = 2,
                firstVisibleItemScrollOffset = 0
            )
        )
    }
}
