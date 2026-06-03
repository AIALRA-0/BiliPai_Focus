package com.android.purebilibili.feature.home.components.cards

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCardHistoryProgressPolicyTest {

    @Test
    fun `history card should show progress bar when resolved progress is non zero`() {
        assertTrue(shouldShowVideoCardHistoryProgressBar(viewAt = 1700000000L, durationSec = 120, progressSec = 30))
    }

    @Test
    fun `history card should hide progress bar for zero progress`() {
        assertFalse(shouldShowVideoCardHistoryProgressBar(viewAt = 1700000000L, durationSec = 120, progressSec = 0))
    }

    @Test
    fun `non history card should show progress bar when progress exists`() {
        assertTrue(shouldShowVideoCardHistoryProgressBar(viewAt = 0L, durationSec = 120, progressSec = 20))
    }

    @Test
    fun `non history card should hide progress bar for default completed sentinel without watch record`() {
        assertFalse(shouldShowVideoCardHistoryProgressBar(viewAt = 0L, durationSec = 120, progressSec = -1))
    }

    @Test
    fun `finished progress should render full width`() {
        assertEquals(1f, resolveVideoCardHistoryProgressFraction(progressSec = -1, durationSec = 120), 0.0001f)
    }

    @Test
    fun `normal progress should map to ratio and clamp`() {
        assertEquals(0.25f, resolveVideoCardHistoryProgressFraction(progressSec = 30, durationSec = 120), 0.0001f)
        assertEquals(1f, resolveVideoCardHistoryProgressFraction(progressSec = 999, durationSec = 120), 0.0001f)
        assertEquals(0f, resolveVideoCardHistoryProgressFraction(progressSec = -10, durationSec = 120), 0.0001f)
    }

    @Test
    fun `invalid duration should fallback to zero fraction`() {
        assertEquals(0f, resolveVideoCardHistoryProgressFraction(progressSec = 30, durationSec = 0), 0.0001f)
    }

    @Test
    fun `history progress bar uses current theme primary color`() {
        val themePrimary = Color(0xFF34C759)

        assertEquals(themePrimary, resolveVideoCardHistoryProgressBarColor(themePrimary))
    }
}
