package com.android.purebilibili.core.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppTextTapCopyPolicyTest {

    @Test
    fun plainShortTapCopiesText() {
        assertTrue(
            shouldCopyGlobalTextTap(
                text = "可复制文字",
                globalCopyEnabled = true,
                gestureCanceled = false,
                pressDurationMillis = 120L,
                longPressTimeoutMillis = 500L,
            )
        )
    }

    @Test
    fun blankConsumedMovedOrLongPressDoesNotCopy() {
        assertFalse(shouldCopyGlobalTextTap(" ", true, false, 120L, 500L))
        assertFalse(shouldCopyGlobalTextTap("按钮文字", true, true, 120L, 500L))
        assertFalse(shouldCopyGlobalTextTap("滚动文字", true, true, 120L, 500L))
        assertFalse(shouldCopyGlobalTextTap("长按文字", true, false, 500L, 500L))
    }

    @Test
    fun globalSwitchDefaultsToNoCopyBehavior() {
        assertFalse(shouldCopyGlobalTextTap("普通文字", false, false, 120L, 500L))
    }
}
