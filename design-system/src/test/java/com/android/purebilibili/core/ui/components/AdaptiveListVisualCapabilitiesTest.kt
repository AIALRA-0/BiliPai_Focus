package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveListVisualCapabilitiesTest {

    @Test
    fun `material3 capabilities preserve explicit chevron and roomier metrics`() {
        val capabilities = resolveAdaptiveListVisualCapabilities(AppUiStyle.MATERIAL3)

        assertTrue(capabilities.showExplicitActionChevron)
        assertEquals(40, capabilities.componentSpec.iconContainerSizeDp)
        assertEquals(18, capabilities.rowSpec.insideHorizontalPaddingDp)
        assertEquals(48, capabilities.rowSpec.minTouchTargetHeightDp)
    }

    @Test
    fun `miuix capabilities use native action affordance and dense metrics`() {
        val capabilities = resolveAdaptiveListVisualCapabilities(AppUiStyle.MIUIX)

        assertFalse(capabilities.showExplicitActionChevron)
        assertEquals(38, capabilities.componentSpec.iconContainerSizeDp)
        assertEquals(16, capabilities.rowSpec.insideHorizontalPaddingDp)
        assertEquals(48, capabilities.rowSpec.minTouchTargetHeightDp)
    }
}
