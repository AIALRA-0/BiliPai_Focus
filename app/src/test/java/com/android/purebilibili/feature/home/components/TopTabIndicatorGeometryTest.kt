package com.android.purebilibili.feature.home.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class TopTabIndicatorGeometryTest {

    @Test
    fun `liquid capsule width interpolates between adjacent labels`() {
        assertEquals(
            54f,
            resolveTopTabInterpolatedIndicatorWidthDp(
                position = 0.5f,
                itemWidthDp = 100f,
                horizontalGapDp = 2f,
                contentWidthsDp = listOf(30f, 70f),
            ),
            0.01f,
        )
    }

    @Test
    fun `icon only top tab uses a compact side indicator`() {
        assertEquals(24.dp, resolveIconOnlyTopTabIndicatorWidth())
    }

    @Test
    fun `top tab panel offset cannot travel outward past dock edges`() {
        assertEquals(0f, resolveTopTabEdgeAwarePanelOffsetPx(4f, 4, 6f), 0.01f)
        assertEquals(0f, resolveTopTabEdgeAwarePanelOffsetPx(0f, 4, -6f), 0.01f)
        assertEquals(-6f, resolveTopTabEdgeAwarePanelOffsetPx(4f, 4, -6f), 0.01f)
        assertEquals(6f, resolveTopTabEdgeAwarePanelOffsetPx(0f, 4, 6f), 0.01f)
    }

    @Test
    fun `top tab outward panel offset fades during final quarter slot`() {
        assertEquals(3f, resolveTopTabEdgeAwarePanelOffsetPx(3.875f, 4, 6f), 0.01f)
        assertEquals(6f, resolveTopTabEdgeAwarePanelOffsetPx(2f, 4, 6f), 0.01f)
    }

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
    fun `top tab row horizontal padding is zero for text-only docked style`() {
        assertEquals(
            0f,
            resolveTopTabRowHorizontalPaddingDp(
                isFloatingStyle = false,
                labelMode = 2
            ),
            0.01f
        )
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
    fun `top dock shell lens matches floating bottom bar full strength`() {
        assertEquals(1f, TOP_DOCK_SHELL_LENS_INTENSITY, 0.01f)
    }

    @Test
    fun `top tab dock indicator leaves compact vertical breathing space with outer chrome`() {
        assertEquals(
            2f,
            resolveTopTabDockIndicatorHorizontalGapDp(hasOuterChromeSurface = true),
            0.01f
        )
        assertEquals(
            3f,
            resolveTopTabDockIndicatorVerticalGapDp(hasOuterChromeSurface = true),
            0.01f
        )
    }

    @Test
    fun `reused liquid glass top indicator keeps a narrow edge gap`() {
        assertEquals(
            1f,
            resolveTopTabDockIndicatorHorizontalGapDp(
                hasOuterChromeSurface = true,
                isLiquidGlassReuseEnabled = true
            ),
            0.01f
        )
    }

    @Test
    fun `top tab dock indicator uses the same compact gap without outer dock`() {
        assertEquals(
            2f,
            resolveTopTabDockIndicatorHorizontalGapDp(hasOuterChromeSurface = false),
            0.01f
        )
        assertEquals(
            3f,
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
            rowHeightDp = 36f,
            verticalGapDp = verticalGap,
            minHeightDp = 30f
        )

        assertEquals(92f, width, 0.01f)
        assertEquals(30f, height, 0.01f)
    }

    @Test
    fun `top tab dock indicator keeps compact rounded rectangle height with min floor`() {
        assertEquals(
            30f,
            resolveTopTabDockIndicatorHeightDp(
                rowHeightDp = 36f,
                verticalGapDp = 3f,
                minHeightDp = 30f,
                indicatorWidthDp = 54f
            ),
            0.01f
        )
    }

    @Test
    fun `top tab dock indicator translation starts after inner gap`() {
        val horizontalGap = resolveTopTabDockIndicatorHorizontalGapDp(
            hasOuterChromeSurface = true
        )

        assertEquals(
            34f,
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

    @Test
    fun `md3 top tab underline centers directly below label content`() {
        assertEquals(
            14f,
            resolveMd3TopTabUnderlineCenterOffsetDp(showIcon = false, showText = true),
            0.01f
        )
        assertEquals(
            26f,
            resolveMd3TopTabUnderlineCenterOffsetDp(showIcon = true, showText = true),
            0.01f
        )
        assertEquals(
            13f,
            resolveMd3TopTabUnderlineCenterOffsetDp(showIcon = true, showText = false),
            0.01f
        )
    }
}
