package com.android.purebilibili.feature.video.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuSendDialogLayoutPolicyTest {

    @Test
    fun `danmaku dialog should use bottom sheet style to avoid blocking video center`() {
        val policy = resolveDanmakuSendDialogLayoutPolicy()

        assertTrue(policy.bottomAligned)
        assertEquals(1f, policy.fillMaxWidthFraction)
        assertEquals(14, policy.bottomLiftDp)
    }

    @Test
    fun `ime visible should remove extra bottom lift to avoid gap`() {
        val liftWhenImeVisible = resolveDanmakuDialogBottomLiftDp(
            defaultBottomLiftDp = 14,
            imeBottomPx = 320
        )
        val liftWhenImeHidden = resolveDanmakuDialogBottomLiftDp(
            defaultBottomLiftDp = 14,
            imeBottomPx = 0
        )

        assertEquals(0, liftWhenImeVisible)
        assertEquals(14, liftWhenImeHidden)
    }

    @Test
    fun `invalid remembered send options should fall back to safe defaults`() {
        val selection = resolveDanmakuSendSelectionState(
            initialColor = 123,
            initialMode = 99,
            initialFontSize = 77,
            colorOptions = listOf(16777215, 16646914),
            modeOptions = listOf(1, 4, 5),
            fontSizeOptions = listOf(18, 25, 36)
        )

        assertEquals(16777215, selection.color)
        assertEquals(1, selection.mode)
        assertEquals(25, selection.fontSize)
    }
}
