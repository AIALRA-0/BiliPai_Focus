package com.android.purebilibili.core.ui

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import com.android.purebilibili.core.util.AppHingeFeature
import com.android.purebilibili.core.util.AppHingeOrientation
import com.android.purebilibili.core.util.resolveHingeSafeContentRegions
import kotlin.test.Test
import kotlin.test.assertEquals

class AppHingeSplitGeometryTest {
    @Test
    fun `zero width fold reserves clearance at its physical position`() {
        assertEquals(
            listOf(IntRect(0, 0, 484, 800), IntRect(516, 0, 1000, 800)),
            resolveHingeSafeContentRegions(1000, 800, listOf(vertical(500, 500)), clearancePx = 16),
        )
    }

    @Test
    fun `off center hinge is independent of the requested split ratio`() {
        assertEquals(
            listOf(IntRect(0, 0, 404, 800), IntRect(456, 0, 1000, 800)),
            resolveHingeSafeContentRegions(1000, 800, listOf(vertical(420, 440)), clearancePx = 16),
        )
    }

    @Test
    fun `parent offsets are subtracted from window hinge coordinates`() {
        assertEquals(
            listOf(IntRect(0, 0, 284, 500), IntRect(332, 0, 800, 500)),
            resolveHingeSafeContentRegions(
                800, 500, listOf(vertical(400, 416)), IntOffset(100, 80), 16,
            ),
        )
    }

    @Test
    fun `changing UI density changes clearance but never rescales the hinge bounds`() {
        assertEquals(
            listOf(IntRect(0, 0, 476, 800), IntRect(524, 0, 1000, 800)),
            resolveHingeSafeContentRegions(1000, 800, listOf(vertical(500, 500)), clearancePx = 24),
        )
    }

    @Test
    fun `zero height tabletop fold reserves both edges`() {
        val hinge = AppHingeFeature(AppHingeOrientation.Horizontal, IntRect(0, 300, 1000, 300), true, false, false)
        assertEquals(
            listOf(IntRect(0, 0, 1000, 284), IntRect(0, 316, 1000, 800)),
            resolveHingeSafeContentRegions(1000, 800, listOf(hinge), clearancePx = 16),
        )
    }

    private fun vertical(left: Int, right: Int) = AppHingeFeature(
        AppHingeOrientation.Vertical, IntRect(left, 0, right, 1000), true, false, false,
    )
}
