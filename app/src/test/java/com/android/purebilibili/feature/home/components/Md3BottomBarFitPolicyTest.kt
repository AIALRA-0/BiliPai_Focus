package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Md3BottomBarFitPolicyTest {
    @Test
    fun `five destinations beside search fit without cutting the last icon`() {
        val fit = resolveMd3BottomBarFit(availableWidthDp = 272f, itemCount = 5, selectedLabelWidthDp = 32f)

        assertFalse(fit.showSelectedLabel)
        assertTrue(5 * 48f + fit.horizontalPaddingDp * 2 + fit.spacingDp * 4 <= 272f)
    }

    @Test
    fun `remove excess spacing before hiding a selected label`() {
        val fit = resolveMd3BottomBarFit(availableWidthDp = 312f, itemCount = 5, selectedLabelWidthDp = 32f)

        assertTrue(fit.showSelectedLabel)
        assertTrue(5 * 48f + 48f + fit.horizontalPaddingDp * 2 + fit.spacingDp * 4 <= 312f)
    }

    @Test
    fun `large windows retain the original toolbar spacing`() {
        assertEquals(Md3BottomBarFit(true, 12f, 8f), resolveMd3BottomBarFit(500f, 6, 48f))
    }

    @Test
    fun `font scaling and long labels fall back to complete icons`() {
        val fit = resolveMd3BottomBarFit(320f, 5, 160f)

        assertFalse(fit.showSelectedLabel)
        assertTrue(5 * 48f + fit.horizontalPaddingDp * 2 + fit.spacingDp * 4 <= 320f)
    }

    @Test
    fun `very narrow split windows leave scrolling room without negative spacing`() {
        assertEquals(Md3BottomBarFit(false, 0f, 0f), resolveMd3BottomBarFit(180f, 5, 32f))
    }
}
