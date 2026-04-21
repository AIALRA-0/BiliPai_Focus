package com.android.purebilibili.feature.list

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonListBackToTopPolicyTest {

    @Test
    fun farFromTop_showsBackToTopButton() {
        assertTrue(
            shouldShowCommonListBackToTop(
                firstVisibleItemIndex = 3,
                firstVisibleItemScrollOffset = 0
            )
        )
    }

    @Test
    fun topWithSmallOffset_hidesBackToTopButton() {
        assertFalse(
            shouldShowCommonListBackToTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 120
            )
        )
    }
}
