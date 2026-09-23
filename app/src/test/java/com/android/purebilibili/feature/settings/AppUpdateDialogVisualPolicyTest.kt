package com.android.purebilibili.feature.settings

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppUpdateDialogVisualPolicyTest {

    @Test
    fun `resolveAppUpdateDialogTextColors uses stronger body contrast in dark mode`() {
        val colors = resolveAppUpdateDialogTextColors(isDarkTheme = true)

        assertEquals(colors.titleColor, colors.releaseNotesColor)
        assertEquals(colors.titleColor, colors.currentVersionColor)
        assertTrue(colors.releaseNotesColor.alpha > 0.9f)
    }

    @Test
    fun `resolveAppUpdateDialogTextColors keeps softer supporting text in light mode`() {
        val colors = resolveAppUpdateDialogTextColors(isDarkTheme = false)

        assertEquals(Color.Unspecified, colors.titleColor)
        assertEquals(Color.Unspecified, colors.currentVersionColor)
        assertEquals(Color.Unspecified, colors.releaseNotesColor)
    }

    @Test
    fun `scrollable content height follows the current window and stays bounded`() {
        assertEquals(
            384,
            resolveAppUpdateScrollableContentMaxHeight(
                screenHeightDp = 800,
                heightFraction = 0.48f,
                minHeightDp = 170,
                maxHeightDp = 390,
            ),
        )
        assertEquals(
            170,
            resolveAppUpdateScrollableContentMaxHeight(
                screenHeightDp = 300,
                heightFraction = 0.48f,
                minHeightDp = 170,
                maxHeightDp = 390,
            ),
        )
        assertEquals(
            390,
            resolveAppUpdateScrollableContentMaxHeight(
                screenHeightDp = 1200,
                heightFraction = 0.48f,
                minHeightDp = 170,
                maxHeightDp = 390,
            ),
        )
    }
}
