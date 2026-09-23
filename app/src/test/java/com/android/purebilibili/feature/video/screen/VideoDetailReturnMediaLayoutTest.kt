package com.android.purebilibili.feature.video.screen

import androidx.compose.ui.geometry.Rect
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailReturnMediaLayoutTest {
    @Test
    fun stackedCardUsesMeasuredCoverGeometry() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = Rect(100f, 200f, 500f, 700f),
            sourceCoverBounds = Rect(100f, 200f, 500f, 440f),
            sourceLayout = VideoCardSourceLayout.STACKED,
        )

        assertTrue(layout.canRender)
        assertEquals(240f, layout.coverHeightPx)
        assertEquals(260f, layout.infoHeightPx)
    }

    @Test
    fun horizontalCardUsesMeasuredFullCardHeight() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = Rect(40f, 100f, 840f, 360f),
            sourceCoverBounds = Rect(40f, 100f, 320f, 300f),
            sourceLayout = VideoCardSourceLayout.SIDE_BY_SIDE,
        )

        assertTrue(layout.canRender)
        assertEquals(260f, layout.infoHeightPx)
        assertEquals(280f, layout.coverWidthPx)
    }

    @Test
    fun invalidBoundsDoNotProduceReturnGeometry() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = null,
            sourceCoverBounds = null,
        )

        assertFalse(layout.canRender)
    }

    @Test
    fun entryCoverMatchesManualStartViewportBelowStatusBar() {
        for (topInset in listOf(0, 72)) {
            val frame = resolveVideoDetailReturnMediaLayoutFrame(
                containerWidthPx = 1088,
                containerHeightPx = 612 + topInset,
                landingLayout = null,
                handoffProgress = 0f,
                contentTopInsetPx = topInset,
            )
            assertEquals(0, frame.offsetXPx)
            assertEquals(topInset, frame.offsetYPx)
            assertEquals(1088, frame.widthPx)
            assertEquals(612, frame.heightPx)
        }
    }

    @Test
    fun statusBarInsetFadesOutOfReturnGeometryWithoutChangingCardLanding() {
        val landing = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1088f,
            sourceBounds = Rect(0f, 0f, 544f, 420f),
            sourceCoverBounds = Rect(0f, 0f, 544f, 306f),
            sourceLayout = VideoCardSourceLayout.STACKED,
        )
        fun frame(progress: Float, inset: Int) = resolveVideoDetailReturnMediaLayoutFrame(
            containerWidthPx = 1088,
            containerHeightPx = 684,
            landingLayout = landing,
            handoffProgress = progress,
            contentTopInsetPx = inset,
        )
        assertEquals(72, frame(0f, 72).offsetYPx)
        assertEquals(612, frame(0f, 72).heightPx)
        assertEquals(36, frame(0.5f, 72).offsetYPx)
        assertEquals(frame(1f, 0), frame(1f, 72))
    }

    @Test
    fun coverInsetCannotProduceEmptyOrNegativeMediaSize() {
        for (inset in listOf(-20, 1000)) {
            val frame = resolveVideoDetailReturnMediaLayoutFrame(
                containerWidthPx = 1088,
                containerHeightPx = 100,
                landingLayout = null,
                handoffProgress = 0f,
                contentTopInsetPx = inset,
            )
            assertTrue(frame.heightPx >= 1)
            assertEquals(100, frame.offsetYPx + frame.heightPx)
        }
    }

    @Test
    fun mediaFrameInterpolatesToMeasuredCover() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = Rect(0f, 0f, 500f, 300f),
            sourceCoverBounds = Rect(0f, 0f, 200f, 180f),
            sourceLayout = VideoCardSourceLayout.SIDE_BY_SIDE,
        )
        val frame = resolveVideoDetailReturnMediaLayoutFrame(
            containerWidthPx = 1000,
            containerHeightPx = 600,
            landingLayout = layout,
            handoffProgress = 1f,
        )

        assertEquals(400, frame.widthPx)
        // Side-by-side media overdraws one source pixel below the landing edge. The outer
        // shared-bounds clip removes the excess and prevents the black player layer peeking out.
        assertEquals(362, frame.heightPx)
    }

    @Test
    fun mediaFrameUsesCurrentInverseYSoCoverTracksTheClip() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = Rect(0f, 0f, 500f, 300f),
            sourceCoverBounds = Rect(0f, 0f, 500f, 240f),
            sourceLayout = VideoCardSourceLayout.STACKED,
        )
        val frozen = resolveVideoDetailReturnMediaLayoutFrame(
            containerWidthPx = 1000,
            containerHeightPx = 600,
            landingLayout = layout,
            handoffProgress = 1f,
        )
        val clipTracked = resolveVideoDetailReturnMediaLayoutFrame(
            containerWidthPx = 1000,
            containerHeightPx = 600,
            landingLayout = layout,
            handoffProgress = 1f,
            inverseScaleX = 2f,
            inverseScaleY = 2.4f,
        )

        assertEquals(1000, frozen.widthPx)
        assertEquals(480, frozen.heightPx)
        assertEquals(1000, clipTracked.widthPx)
        assertEquals(576, clipTracked.heightPx)
    }
}
