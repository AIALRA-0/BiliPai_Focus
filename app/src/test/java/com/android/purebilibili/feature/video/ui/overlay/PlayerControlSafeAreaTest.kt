package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerControlSafeAreaTest {
    @Test
    fun horizontalCutoutIsMirroredWithoutCarryingVerticalInsets() {
        val density = Density(2f)
        for ((left, right) in listOf(0 to 0, 96 to 0, 0 to 96, 48 to 96)) {
            val insets = SymmetricPlayerHorizontalInsets(WindowInsets(left, 30, right, 20))
            for (direction in LayoutDirection.entries) {
                assertEquals(maxOf(left, right), insets.getLeft(density, direction))
                assertEquals(maxOf(left, right), insets.getRight(density, direction))
                assertEquals(0, insets.getTop(density))
                assertEquals(0, insets.getBottom(density))
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun sameInsetsInstanceTracksCutoutChangesAfterRotation() {
        val density = Density(1f)
        val cutout = MutableWindowInsets(WindowInsets(80, 0, 0, 0))
        val insets = SymmetricPlayerHorizontalInsets(cutout)
        assertEquals(80, insets.getRight(density, LayoutDirection.Ltr))
        cutout.insets = WindowInsets(0, 0, 64, 0)
        assertEquals(64, insets.getLeft(density, LayoutDirection.Ltr))
        assertEquals(64, insets.getRight(density, LayoutDirection.Ltr))
        cutout.insets = WindowInsets(0, 0, 0, 0)
        assertEquals(0, insets.getLeft(density, LayoutDirection.Ltr))
        assertEquals(0, insets.getRight(density, LayoutDirection.Ltr))
    }
}
