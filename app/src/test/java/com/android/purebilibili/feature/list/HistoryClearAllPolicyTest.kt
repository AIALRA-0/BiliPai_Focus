package com.android.purebilibili.feature.list

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryClearAllPolicyTest {

    @Test
    fun `clear all action shows only for history list with items and toggle enabled`() {
        assertTrue(
            shouldShowHistoryClearAllAction(
                hasHistoryViewModel = true,
                hasItems = true,
                settingEnabled = true,
                isBatchMode = false
            )
        )
    }

    @Test
    fun `clear all action hides during batch mode or when setting is disabled`() {
        assertFalse(
            shouldShowHistoryClearAllAction(
                hasHistoryViewModel = true,
                hasItems = true,
                settingEnabled = false,
                isBatchMode = false
            )
        )
        assertFalse(
            shouldShowHistoryClearAllAction(
                hasHistoryViewModel = true,
                hasItems = true,
                settingEnabled = true,
                isBatchMode = true
            )
        )
        assertFalse(
            shouldShowHistoryClearAllAction(
                hasHistoryViewModel = false,
                hasItems = true,
                settingEnabled = true,
                isBatchMode = false
            )
        )
    }
}
