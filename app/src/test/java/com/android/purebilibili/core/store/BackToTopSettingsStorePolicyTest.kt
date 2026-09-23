package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackToTopSettingsStorePolicyTest {
    @Test
    fun backToTopButton_isEnabledByDefault() {
        assertTrue(DEFAULT_BACK_TO_TOP_BUTTON_ENABLED)
    }

    @Test
    fun backToTopButton_customOffset_isZeroByDefault() {
        assertEquals(0f, DEFAULT_BACK_TO_TOP_OFFSET_X_DP)
        assertEquals(0f, DEFAULT_BACK_TO_TOP_OFFSET_Y_DP)
        assertEquals(Pair(0f, 0f), BackToTopSettingsStore.getCachedOffsetDp())
    }
}
