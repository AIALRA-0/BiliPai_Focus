package com.android.purebilibili.core.util

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowSizeUtilsTest {

    @Test
    fun `responsive text scaling keeps unspecified units`() {
        val scaled = TextUnit.Unspecified.scaledIfSpecified(1.2f)

        assertTrue(scaled.isUnspecified)
    }

    @Test
    fun `responsive text scaling scales specified units`() {
        val scaled = 14.sp.scaledIfSpecified(1.2f)

        assertEquals(16.8f, scaled.value, 0.0001f)
    }

    @Test
    fun `stable device width ignores UI density multiplier`() {
        val scaledWindowWidth = (600 / 1.08f).dp

        assertEquals(
            WindowWidthSizeClass.Compact,
            resolveWindowWidthSizeClass(scaledWindowWidth)
        )
        assertEquals(
            WindowWidthSizeClass.Medium,
            resolveStableDeviceWidthSizeClass(600)
        )
    }

    @Test
    fun `window size class separates current window from device shape`() {
        val windowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Compact,
            heightSizeClass = WindowHeightSizeClass.Medium,
            widthDp = 555.dp,
            heightDp = 800.dp,
            deviceWidthSizeClass = WindowWidthSizeClass.Medium
        )

        assertFalse(windowSizeClass.isTablet)
        assertFalse(windowSizeClass.isCompactDevice)
        assertTrue(windowSizeClass.isTabletDevice)
    }
}
