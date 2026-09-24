package com.android.purebilibili.core.ui.performance

import kotlin.test.Test
import kotlin.test.assertEquals

class AppDisplayModePreferenceTest {

    private val modes = listOf(
        AppDisplayMode(modeId = 1, refreshRate = 60f, width = 1080, height = 2400),
        AppDisplayMode(modeId = 2, refreshRate = 120f, width = 1080, height = 2400),
    )

    @Test
    fun autoPreference_releasesDisplayModeVote() {
        assertEquals(
            SYSTEM_AUTO_DISPLAY_MODE_ID,
            normalizePreferredDisplayModeId(SYSTEM_AUTO_DISPLAY_MODE_ID, modes),
        )
    }

    @Test
    fun supportedManualPreference_keepsRequestedMode() {
        assertEquals(2, normalizePreferredDisplayModeId(2, modes))
    }

    @Test
    fun staleManualPreference_fallsBackToSystemAuto() {
        assertEquals(
            SYSTEM_AUTO_DISPLAY_MODE_ID,
            normalizePreferredDisplayModeId(99, modes),
        )
    }

    @Test
    fun displayModeLabel_containsRateAndResolution() {
        assertEquals("120 Hz · 1080 × 2400", displayModePreferenceLabel(modes.last()))
    }
}
