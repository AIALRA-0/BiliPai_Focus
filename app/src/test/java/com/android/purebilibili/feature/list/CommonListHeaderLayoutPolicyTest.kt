package com.android.purebilibili.feature.list

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonListHeaderLayoutPolicyTest {

    @Test
    fun `history content scrolls under chrome when header is always visible`() {
        assertTrue(
            shouldScrollCommonListUnderHeader(
                isHistoryPage = true,
                headerCollapseEnabled = false,
            )
        )
    }

    @Test
    fun `collapsible common list content scrolls under chrome`() {
        assertTrue(
            shouldScrollCommonListUnderHeader(
                isHistoryPage = false,
                headerCollapseEnabled = true,
            )
        )
    }

    @Test
    fun `fixed non history header keeps its layout inset`() {
        assertFalse(
            shouldScrollCommonListUnderHeader(
                isHistoryPage = false,
                headerCollapseEnabled = false,
            )
        )
    }

    @Test
    fun `progressive blur captures likes and favorites under a fixed header`() {
        assertTrue(
            shouldScrollCommonListUnderHeader(
                isHistoryPage = false,
                headerCollapseEnabled = false,
                captureScrollableContent = true,
            )
        )
    }
}
