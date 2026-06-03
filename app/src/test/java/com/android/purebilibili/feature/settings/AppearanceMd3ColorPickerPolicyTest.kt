package com.android.purebilibili.feature.settings

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppearanceMd3ColorPickerPolicyTest {

    @Test
    fun md3ColorPickerSliderLayout_reservesSpaceForThumbOverflow() {
        val layout = resolveMd3ColorPickerSliderLayout()

        assertEquals(28.dp, layout.trackHeight)
        assertEquals(14.dp, layout.thumbRadius)
        assertTrue(layout.horizontalPadding >= layout.thumbRadius)
        assertTrue(layout.frameHeight > layout.trackHeight)
    }

    @Test
    fun md3ColorPickerSelectionHaptic_isRateLimitedDuringDrag() {
        assertTrue(
            shouldEmitMd3ColorPickerSelectionHaptic(
                lastFeedbackAtMs = 0L,
                nowMs = 10L
            )
        )
        assertFalse(
            shouldEmitMd3ColorPickerSelectionHaptic(
                lastFeedbackAtMs = 1_000L,
                nowMs = 1_050L
            )
        )
        assertTrue(
            shouldEmitMd3ColorPickerSelectionHaptic(
                lastFeedbackAtMs = 1_000L,
                nowMs = 1_080L
            )
        )
    }
}
