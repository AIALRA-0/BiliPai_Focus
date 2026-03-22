package com.android.purebilibili.feature.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchScreenPolicyTest {

    @Test
    fun `hot header stays hidden when hot search toggle is off`() {
        assertFalse(shouldShowSearchHotHeader(hotItemCount = 12, hotSearchEnabled = false))
        assertFalse(shouldShowSearchHotSection(hotItemCount = 12, hotSearchEnabled = false))
    }

    @Test
    fun `hot header and body appear only when enabled and data exists`() {
        assertTrue(shouldShowSearchHotHeader(hotItemCount = 1, hotSearchEnabled = true))
        assertTrue(shouldShowSearchHotSection(hotItemCount = 1, hotSearchEnabled = true))
        assertFalse(shouldShowSearchHotHeader(hotItemCount = 0, hotSearchEnabled = true))
    }
}
