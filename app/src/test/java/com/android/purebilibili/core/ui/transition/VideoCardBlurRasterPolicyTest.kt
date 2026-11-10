package com.android.purebilibili.core.ui.transition

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoCardBlurRasterPolicyTest {
    @Test
    fun nearClearUsesOriginalResolutionAndStrongBlurUsesQuarterPixelArea() {
        for (radius in listOf(0f, 0.5f, 2f, 4f, 6f, Float.NaN)) {
            assertEquals(1f, resolveVideoCardBlurRasterScale(radius))
        }
        val scale = resolveVideoCardBlurRasterScale(33f)
        assertEquals(IntSize(540, 1200), resolveVideoCardBlurRasterSize(Size(1080f, 2400f), scale))
        assertEquals(IntSize(541, 1201), resolveVideoCardBlurRasterSize(Size(1081f, 2401f), scale))
    }

    @Test
    fun rasterSwitchPreservesScreenGeometryAtEveryCardPivot() {
        for (pivot in listOf(Offset.Zero, Offset(270f, 600f), Offset(810f, 1800f))) {
            for (contentScale in listOf(1f, 0.94f, 0.9f)) {
                for (point in listOf(Offset.Zero, Offset(540f, 1200f), Offset(1080f, 2400f))) {
                    for (rasterScale in listOf(1f, 0.5f)) {
                        val localPivot = pivot * rasterScale
                        val screenPoint = (point * rasterScale - localPivot) *
                            (contentScale / rasterScale) + localPivot +
                            resolveVideoCardBlurRasterTranslation(pivot, rasterScale)
                        val expected = (point - pivot) * contentScale + pivot
                        assertEquals(expected.x, screenPoint.x, 0.001f)
                        assertEquals(expected.y, screenPoint.y, 0.001f)
                    }
                }
            }
        }
    }

    @Test
    fun blurStrengthIsEqualInScreenPixelsAfterRasterCompensation() {
        for (radius in listOf(0f, 0.5f, 4f, 6f, 8f, 24f, 33f)) {
            val scale = resolveVideoCardBlurRasterScale(radius)
            assertEquals(radius, (radius * scale) / scale)
        }
    }
}
