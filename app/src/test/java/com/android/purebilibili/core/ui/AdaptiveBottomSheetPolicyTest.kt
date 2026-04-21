package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.UiPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveBottomSheetPolicyTest {

    @Test
    fun `md3 preset should use material drag handle and larger corner radius`() {
        val spec = resolveAdaptiveBottomSheetVisualSpec(UiPreset.MD3)

        assertEquals(28, spec.cornerRadiusDp)
        assertTrue(spec.useMaterialDragHandle)
    }

    @Test
    fun `ios preset should preserve compact sheet chrome`() {
        val spec = resolveAdaptiveBottomSheetVisualSpec(UiPreset.IOS)

        assertEquals(14, spec.cornerRadiusDp)
        assertFalse(spec.useMaterialDragHandle)
    }

    @Test
    fun `ios preset should use softer sheet motion`() {
        val spec = resolveAdaptiveBottomSheetMotionSpec(UiPreset.IOS)

        assertEquals(240, spec.scrimEnterDurationMillis)
        assertEquals(180, spec.scrimExitDurationMillis)
        assertEquals(240, spec.contentEnterFadeDurationMillis)
        assertEquals(160, spec.contentExitFadeDurationMillis)
    }

    @Test
    fun `md3 preset should keep sheet dismiss faster than enter`() {
        val spec = resolveAdaptiveBottomSheetMotionSpec(UiPreset.MD3)

        assertTrue(spec.scrimExitDurationMillis < spec.scrimEnterDurationMillis)
        assertTrue(spec.contentExitFadeDurationMillis < spec.contentEnterFadeDurationMillis)
    }
}
