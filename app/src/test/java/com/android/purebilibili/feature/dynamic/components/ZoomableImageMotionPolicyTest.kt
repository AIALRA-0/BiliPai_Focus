package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class ZoomableImageMotionPolicyTest {
    @Test
    fun wideImage_canPanHorizontallyButNotIntoVerticalBlankSpace() {
        val bounds = resolveZoomableImagePanBounds(IntSize(1000, 500), IntSize(400, 800), 2f)
        assertEquals(Offset(200f, 0f), bounds)
        assertEquals(Offset(200f, 0f), clampZoomableImageOffset(Offset(500f, 100f), bounds))
    }

    @Test
    fun longImageAtWidthFill_onlyScrollsVertically() {
        assertEquals(
            Offset(0f, 3600f),
            resolveZoomableImagePanBounds(IntSize(1000, 20000), IntSize(400, 800), 10f),
        )
    }

    @Test
    fun missingImageAndFitScale_haveNoPanRange() {
        assertEquals(Offset.Zero, resolveZoomableImagePanBounds(IntSize.Zero, IntSize(400, 800), 2f))
        assertEquals(Offset.Zero, resolveZoomableImagePanBounds(IntSize(1000, 500), IntSize(400, 800), 1f))
    }

    @Test
    fun outwardVelocityAtEdge_preservesMotionAlongOtherAxis() {
        assertEquals(
            Offset(0f, 300f),
            resolveZoomableImageFlingVelocity(
                Offset(500f, 300f), Offset(100f, 0f), Offset(100f, 200f), 1f,
            ),
        )
        assertEquals(
            Offset(500f, 0f),
            resolveZoomableImageFlingVelocity(
                Offset(500f, -300f), Offset(-100f, -200f), Offset(100f, 200f), 1f,
            ),
        )
    }

    @Test
    fun flingIgnoresSlowOrInvalidMotionAndCapsFastMotionInDp() {
        assertEquals(
            Offset.Zero,
            resolveZoomableImageFlingVelocity(
                Offset(100f, Float.NaN), Offset.Zero, Offset(100f, 200f), 2f,
            ),
        )
        assertEquals(
            Offset(8000f, -8000f),
            resolveZoomableImageFlingVelocity(
                Offset(20000f, -20000f), Offset.Zero, Offset(100f, 200f), 2f,
            ),
        )
    }
}
