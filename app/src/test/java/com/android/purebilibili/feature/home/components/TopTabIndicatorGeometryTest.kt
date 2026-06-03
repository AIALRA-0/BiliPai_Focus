package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TopTabIndicatorGeometryTest {

    @Test
    fun `indicator width follows ratio when within bounds`() {
        val width = resolveTopTabIndicatorWidthPx(
            itemWidthPx = 100f,
            widthRatio = 0.78f,
            minWidthPx = 48f,
            horizontalInsetPx = 8f
        )

        assertEquals(78f, width, 0.01f)
    }

    @Test
    fun `indicator width uses minimum width on narrow tabs`() {
        val width = resolveTopTabIndicatorWidthPx(
            itemWidthPx = 54f,
            widthRatio = 0.78f,
            minWidthPx = 48f,
            horizontalInsetPx = 8f
        )

        assertEquals(48f, width, 0.01f)
    }

    @Test
    fun `indicator width respects max width from inset`() {
        val width = resolveTopTabIndicatorWidthPx(
            itemWidthPx = 200f,
            widthRatio = 0.95f,
            minWidthPx = 48f,
            horizontalInsetPx = 16f
        )

        assertEquals(184f, width, 0.01f)
    }

    @Test
    fun `ios top indicator fills the full slot when using bottom bar ratio`() {
        val width = resolveTopTabIndicatorWidthPx(
            itemWidthPx = 72f,
            widthRatio = 1.34f,
            minWidthPx = 90f,
            horizontalInsetPx = 0f
        )

        assertEquals(72f, width, 0.01f)
    }

    @Test
    fun `floating top indicator width is capped on narrow tabs`() {
        val width = resolveLiquidIndicatorWidthPx(
            itemWidthPx = 72f,
            widthMultiplier = 1.42f,
            minWidthPx = 104f,
            maxWidthPx = 136f,
            maxWidthToItemRatio = 1.42f
        )

        assertEquals(102.24f, width, 0.01f)
    }

    @Test
    fun `floating top indicator keeps design width on regular tabs`() {
        val width = resolveLiquidIndicatorWidthPx(
            itemWidthPx = 100f,
            widthMultiplier = 1.42f,
            minWidthPx = 104f,
            maxWidthPx = 136f,
            maxWidthToItemRatio = 1.42f
        )

        assertEquals(136f, width, 0.01f)
    }

    @Test
    fun `floating top indicator ignores oversized minimum on very narrow tabs`() {
        val width = resolveLiquidIndicatorWidthPx(
            itemWidthPx = 60f,
            widthMultiplier = 1.42f,
            minWidthPx = 104f,
            maxWidthPx = 136f,
            maxWidthToItemRatio = 1.42f
        )

        assertEquals(85.2f, width, 0.01f)
    }

    @Test
    fun `floating indicator start padding applies left bias`() {
        val startPadding = resolveFloatingIndicatorStartPaddingPx(
            baseInsetPx = 20f,
            leftBiasPx = 4f
        )

        assertEquals(16f, startPadding, 0.01f)
    }

    @Test
    fun `floating indicator start padding never goes negative`() {
        val startPadding = resolveFloatingIndicatorStartPaddingPx(
            baseInsetPx = 2f,
            leftBiasPx = 4f
        )

        assertEquals(0f, startPadding, 0.01f)
    }

    @Test
    fun `top tab row horizontal padding is zero for floating style`() {
        assertEquals(0f, resolveTopTabRowHorizontalPaddingDp(isFloatingStyle = true), 0.01f)
    }

    @Test
    fun `top tab row horizontal padding keeps legacy spacing for non floating style`() {
        assertEquals(4f, resolveTopTabRowHorizontalPaddingDp(isFloatingStyle = false), 0.01f)
    }

    @Test
    fun `top tab row horizontal padding is zero in edge to edge mode`() {
        assertEquals(
            0f,
            resolveTopTabRowHorizontalPaddingDp(
                isFloatingStyle = false,
                edgeToEdge = true
            ),
            0.01f
        )
    }

    @Test
    fun `floating top tab no longer reserves inner dock spacing`() {
        assertEquals(0f, resolveTopTabRowHorizontalPaddingDp(isFloatingStyle = true), 0.01f)
    }

    @Test
    fun `top tab dock indicator uses flatter geometry with outer chrome`() {
        assertEquals(
            3f,
            resolveTopTabDockIndicatorHorizontalGapDp(hasOuterChromeSurface = true),
            0.01f
        )
        assertEquals(
            10f,
            resolveTopTabDockIndicatorVerticalGapDp(hasOuterChromeSurface = true),
            0.01f
        )
    }

    @Test
    fun `top tab dock indicator preserves legacy gap without outer dock`() {
        assertEquals(
            3f,
            resolveTopTabDockIndicatorHorizontalGapDp(hasOuterChromeSurface = false),
            0.01f
        )
        assertEquals(
            4f,
            resolveTopTabDockIndicatorVerticalGapDp(hasOuterChromeSurface = false),
            0.01f
        )
    }

    @Test
    fun `top tab dock indicator leaves gap inside each slot`() {
        val horizontalGap = resolveTopTabDockIndicatorHorizontalGapDp(
            hasOuterChromeSurface = true
        )
        val verticalGap = resolveTopTabDockIndicatorVerticalGapDp(
            hasOuterChromeSurface = true
        )
        val width = resolveTopTabDockIndicatorWidthDp(
            itemWidthDp = 96f,
            horizontalGapDp = horizontalGap
        )
        val height = resolveTopTabDockIndicatorHeightDp(
            rowHeightDp = 56f,
            verticalGapDp = verticalGap,
            minHeightDp = 2f
        )

        assertEquals(90f, width, 0.01f)
        assertEquals(36f, height, 0.01f)
    }

    @Test
    fun `top tab dock indicator translation starts after inner gap`() {
        val horizontalGap = resolveTopTabDockIndicatorHorizontalGapDp(
            hasOuterChromeSurface = true
        )

        assertEquals(
            35f,
            resolveTopTabDockIndicatorOffsetPx(
                slotTranslationPx = 32f,
                horizontalGapPx = horizontalGap
            ),
            0.01f
        )
    }

    @Test
    fun `md3 top tab row stays vertically centered inside outer chrome`() {
        assertEquals(
            0f,
            resolveMd3TopTabRowVerticalTranslationDp(
                skinPlainStyle = false,
                hasOuterChromeSurface = true
            ),
            0.01f
        )
        assertEquals(
            -4f,
            resolveMd3TopTabRowVerticalTranslationDp(
                skinPlainStyle = false,
                hasOuterChromeSurface = false
            ),
            0.01f
        )
        assertEquals(
            0f,
            resolveMd3TopTabRowVerticalTranslationDp(
                skinPlainStyle = true,
                hasOuterChromeSurface = false
            ),
            0.01f
        )
    }
}
