package com.android.purebilibili.feature.search

import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchChromePolicyTest {

    @Test
    fun `md3 preset should use taller search chrome and filled action`() {
        val spec = resolveSearchChromeVisualSpec(UiPreset.MD3)

        assertEquals(48, spec.inputHeightDp)
        assertEquals(28, spec.inputCornerRadiusDp)
        assertTrue(spec.useFilledSearchAction)
        assertEquals(20, spec.suggestionContainerCornerRadiusDp)
    }

    @Test
    fun `ios preset should preserve compact capsule search chrome`() {
        val spec = resolveSearchChromeVisualSpec(UiPreset.IOS)

        assertEquals(42, spec.inputHeightDp)
        assertEquals(50, spec.inputCornerRadiusDp)
        assertFalse(spec.useFilledSearchAction)
        assertEquals(12, spec.suggestionContainerCornerRadiusDp)
    }

    @Test
    fun `miuix variant should use denser rounded search chrome`() {
        val spec = resolveSearchChromeVisualSpec(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertEquals(46, spec.inputHeightDp)
        assertEquals(23, spec.inputCornerRadiusDp)
        assertTrue(spec.useFilledSearchAction)
        assertEquals(18, spec.suggestionContainerCornerRadiusDp)
    }
}
