package com.android.purebilibili.feature.home.components

import androidx.compose.ui.unit.dp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FloatingBottomBarGeometryTest {

    @Test
    fun `home dock restores original 56 to 78 press bloom ratio`() {
        assertEquals(78f / 56f, FloatingBottomBarPressedScale, 0.0001f)
    }

    @Test
    fun `home top icon text and combined modes keep fuller flat indicators`() {
        // 0: icon + label, 1: icon only, 2: label only. Use the actual dock width policy.
        for (mode in listOf(0, 1, 2)) {
            val width = resolveHomeTopTabFloatingDockWidth(393.dp, 5, mode)
            val slot = resolveFloatingDockSlotWidthPx(width.value, 4f, 5)
            val height = resolveFloatingDockIndicatorHeightDp(
                52f, slot, FloatingBottomBarGeometryMode.TopNavigation, 56f,
            )
            val previous = resolveFloatingDockIndicatorHeightDp(
                52f, slot, FloatingBottomBarGeometryMode.Segmented, 56f,
            )
            assertTrue(height >= previous, "label mode $mode")
            assertEquals(minOf(52f, slot / 1.35f), height, 0.001f)
            assertTrue(slot / height >= 1.35f - 0.001f)
        }
    }

    @Test
    fun `dynamic top navigation fills its shell while comment controls stay compact`() {
        assertEquals(46f, resolveFloatingDockIndicatorHeightDp(
            38f, 70f, FloatingBottomBarGeometryMode.TopNavigation, 50f,
        ), 0.001f)
        assertEquals(36f, resolveFloatingDockIndicatorHeightDp(
            30f, 66f, FloatingBottomBarGeometryMode.Segmented, 40f,
        ), 0.001f)
    }

    @Test
    fun `segmented resting inset matches home without sacrificing flat aspect`() {
        for ((shell, slot) in listOf(40f to 66f, 44f to 72f, 56f to 90f)) {
            val height = resolveFloatingDockIndicatorHeightDp(
                requestedHeightDp = 30f,
                tabWidthDp = slot,
                geometryMode = FloatingBottomBarGeometryMode.Segmented,
                shellHeightDp = shell,
            )
            assertEquals(2f, (shell - height) / 2, 0.001f)
            assertTrue(slot / height >= 1.6f)
        }
        assertEquals(30f, resolveFloatingDockIndicatorHeightDp(
            30f, 48f, FloatingBottomBarGeometryMode.Segmented, 40f,
        ), 0.001f)
        // Dock 模式也留静止边距（56dp 壳 → 56 × 4/64 = 3.5dp/侧 → 49dp 指示器）。
        assertEquals(49f, resolveFloatingDockIndicatorHeightDp(
            52f, 75f, FloatingBottomBarGeometryMode.Dock, 56f,
        ), 0.001f)
    }

    @Test
    fun `short controls retain the five slot home drag reference width`() {
        for (count in listOf(2, 3, 5)) {
            val slot = resolveFloatingDockSlotWidthPx(64f * count + 8f, 4f, count)
            assertEquals(328f, resolveFloatingDockDragReferenceWidthPx(slot, 4f))
        }
        assertEquals(1f, resolveFloatingDockDragReferenceWidthPx(0f, 0f))
    }

    @Test
    fun `custom padding keeps all slots inside the padded content band`() {
        for (padding in listOf(0f, 4f, 12f, 24f)) {
            val slot = resolveFloatingDockSlotWidthPx(360f, padding, 5)
            assertEquals(360f - padding * 2, slot * 5, 0.001f)
            val last = padding + resolveFloatingDockIndicatorOffsetPx(4f, slot, 5, slot)
            assertEquals(360f - padding, last + slot, 0.001f)
        }
        assertEquals(0f, resolveFloatingDockSlotWidthPx(20f, 12f, 5))
        assertEquals(72f, resolveFloatingDockSlotWidthPx(360f, -4f, 5))
    }

    @Test
    fun `tap refraction opt out preserves drag and pager effects`() {
        assertEquals(0.6f, resolveFloatingDockRefractionProgress(0.6f, true, false, false))
        assertEquals(0f, resolveFloatingDockRefractionProgress(0.6f, false, false, false))
        assertEquals(0.6f, resolveFloatingDockRefractionProgress(0.6f, false, true, false))
        assertEquals(0.6f, resolveFloatingDockRefractionProgress(0.6f, false, false, true))
        assertEquals(0f, resolveFloatingDockRefractionProgress(0f, true, false, false))
    }

    @Test
    fun `segmented geometry flattens narrow slots without changing dock geometry`() {
        for (width in listOf(40f, 48f, 56f, 64f, 108f)) {
            val height = resolveFloatingDockIndicatorHeightDp(
                requestedHeightDp = 52f,
                tabWidthDp = width,
                geometryMode = FloatingBottomBarGeometryMode.Segmented,
            )
            assertEquals(minOf(52f, width / 1.6f), height, 0.001f)
            assertTrue(width / height >= 1.6f - 0.001f)
            assertEquals(0f, resolveFloatingDockIndicatorOffsetPx(0f, width, 3, width))
            assertEquals(width * 2, resolveFloatingDockIndicatorOffsetPx(2f, width, 3, width))
        }
        assertEquals(52f, resolveFloatingDockIndicatorHeightDp(52f, 56f))
        assertEquals(52f, resolveFloatingDockIndicatorHeightDp(
            52f, 108f, FloatingBottomBarGeometryMode.Segmented,
        ))
    }

    @Test
    fun `upstream effect padding combines 24dp lens and 16dp press bloom`() {
        assertEquals(
            40f,
            resolveFloatingDockEffectPaddingDp(
                refractionAmountDp = MIUIX_UPSTREAM_DOCK_SHELL_LENS_DP,
                pressBloomDp = MIUIX_UPSTREAM_DOCK_PRESS_BLOOM_DP,
            ),
            0.001f,
        )
    }

    @Test
    fun `upstream bottom dock keeps the 56dp resting indicator in a 78dp slot`() {
        assertEquals(
            56f,
            resolveFloatingDockIndicatorHeightDp(requestedHeightDp = 56f, tabWidthDp = 78f),
        )
        assertTrue(78f / 56f >= FLOATING_DOCK_MIN_INDICATOR_ASPECT)
    }

    @Test
    fun `narrow dynamic slots flatten the indicator instead of drawing a circle`() {
        val height = resolveFloatingDockIndicatorHeightDp(
            requestedHeightDp = 52f,
            tabWidthDp = 40f,
        )
        assertEquals(40f / FLOATING_DOCK_MIN_INDICATOR_ASPECT, height, 0.001f)
        assertTrue(height < 52f)
        assertTrue(40f / height >= FLOATING_DOCK_MIN_INDICATOR_ASPECT - 0.001f)
    }

    @Test
    fun `slots wider than the pill keep the requested indicator height`() {
        assertEquals(
            52f,
            resolveFloatingDockIndicatorHeightDp(
                requestedHeightDp = 52f,
                tabWidthDp = 56f,
            ),
            0.001f,
        )
        assertEquals(
            52f,
            resolveFloatingDockIndicatorHeightDp(
                requestedHeightDp = 52f,
                tabWidthDp = 64f,
            ),
            0.001f,
        )
    }

    @Test
    fun `search-compressed liquid indicator scales both axes proportionally`() {
        val referenceWidth = 69f
        val compressedWidth = 64.2f
        val height = resolveFloatingDockIndicatorHeightDp(
            requestedHeightDp = 56f,
            tabWidthDp = compressedWidth,
            geometryMode = FloatingBottomBarGeometryMode.Dock,
            shellHeightDp = 64f,
            proportionalReferenceWidthDp = referenceWidth,
        )

        assertEquals(compressedWidth / referenceWidth, height / 56f, 0.001f)
        assertTrue(height < 56f)
    }

    @Test
    fun `captured content counters horizontal indicator stretch`() {
        assertEquals(
            1f,
            resolveFloatingDockCapturedContentHorizontalScale(
                itemScale = 1.2f,
                indicatorScaleX = 1.8f,
                indicatorScaleY = 1.5f,
            ),
            0.001f,
        )
        assertEquals(
            1.2f,
            resolveFloatingDockCapturedContentHorizontalScale(
                itemScale = 1.2f,
                indicatorScaleX = 1.5f,
                indicatorScaleY = 1.5f,
            ),
            0.001f,
        )
    }

    @Test
    fun `velocity deformation follows HyperIsland on both axes and directions`() {
        val forwardX = resolveFloatingDockIndicatorLayerScaleX(1.4f, 8f)
        val backwardX = resolveFloatingDockIndicatorLayerScaleX(1.4f, -8f)
        val forwardY = resolveFloatingDockIndicatorLayerScaleY(1.4f, 8f)
        val backwardY = resolveFloatingDockIndicatorLayerScaleY(1.4f, -8f)

        assertTrue(forwardX > 1.4f)
        assertTrue(backwardX < 1.4f)
        assertTrue(forwardY < 1.4f)
        assertTrue(backwardY > 1.4f)
    }

    @Test
    fun `wider search-mode indicator stays inside first and last dock edges`() {
        assertEquals(
            0f,
            resolveFloatingDockIndicatorOffsetPx(
                position = 0f,
                tabWidthPx = 54.2f,
                tabsCount = 5,
                indicatorWidthPx = 69f,
            ),
            0.001f,
        )
        assertEquals(
            202f,
            resolveFloatingDockIndicatorOffsetPx(
                position = 4f,
                tabWidthPx = 54.2f,
                tabsCount = 5,
                indicatorWidthPx = 69f,
            ),
            0.001f,
        )
        assertEquals(
            7.4f,
            resolveFloatingDockIndicatorContentAlignmentPx(
                position = 0f,
                tabWidthPx = 54.2f,
                tabsCount = 5,
                indicatorWidthPx = 69f,
            ),
            0.001f,
        )
        assertEquals(
            -7.4f,
            resolveFloatingDockIndicatorContentAlignmentPx(
                position = 4f,
                tabWidthPx = 54.2f,
                tabsCount = 5,
                indicatorWidthPx = 69f,
            ),
            0.001f,
        )
        assertEquals(
            0f,
            resolveFloatingDockClippedContentTranslationPx(
                position = 0f,
                tabWidthPx = 54.2f,
                tabsCount = 5,
                indicatorWidthPx = 69f,
            ),
            0.001f,
        )
        assertEquals(
            -(54.2f * 5f - 69f),
            resolveFloatingDockClippedContentTranslationPx(
                position = 4f,
                tabWidthPx = 54.2f,
                tabsCount = 5,
                indicatorWidthPx = 69f,
            ),
            0.001f,
        )
    }

    @Test
    fun `drag is rejected in the predictive-back edge bands`() {
        assertFalse(
            shouldAcceptFloatingDockDragAtWindowX(
                windowX = 10f,
                screenWidthPx = 1080f,
                leftInsetPx = 72f,
                rightInsetPx = 72f,
            )
        )
        assertFalse(
            shouldAcceptFloatingDockDragAtWindowX(
                windowX = 1070f,
                screenWidthPx = 1080f,
                leftInsetPx = 72f,
                rightInsetPx = 72f,
            )
        )
        assertTrue(
            shouldAcceptFloatingDockDragAtWindowX(
                windowX = 540f,
                screenWidthPx = 1080f,
                leftInsetPx = 72f,
                rightInsetPx = 72f,
            )
        )
    }

    @Test
    fun `short search and top docks scale lens so refraction cannot meet in the middle`() {
        assertEquals(1f, resolveFloatingDockGeometryScale(shellHeightDp = 64f))
        assertEquals(40f / 64f, resolveFloatingDockGeometryScale(40f), 0.001f)
        assertEquals(36f / 64f, resolveFloatingDockGeometryScale(36f), 0.001f)
        val shortLens = MIUIX_UPSTREAM_DOCK_SHELL_LENS_DP *
            resolveFloatingDockGeometryScale(36f)
        assertTrue(shortLens * 2f < 36f)
    }

    @Test
    fun `interactive highlight radius covers long capsules without shrinking compact docks`() {
        assertEquals(
            64f * 1.2f,
            resolveDockInteractiveHighlightRadiusPx(
                shellMinDimensionPx = 64f,
                tabWidthPx = 70f,
            ),
            0.001f,
        )
        assertEquals(
            200f,
            resolveDockInteractiveHighlightRadiusPx(
                shellMinDimensionPx = 40f,
                tabWidthPx = 400f,
            ),
            0.001f,
        )
    }

    @Test
    fun `pill highlight rim stays 1dp on home docks and thickens on long indicators`() {
        assertEquals(
            1f,
            resolveDockPillHighlightWidthDp(
                indicatorWidthDp = 70f,
                indicatorHeightDp = 56f,
            ),
            0.001f,
        )
        assertEquals(
            2f,
            resolveDockPillHighlightWidthDp(
                indicatorWidthDp = 400f,
                indicatorHeightDp = 40f,
            ),
            0.001f,
        )
    }

    @Test
    fun `compact docks reuse bottom-bar highlight motion at scaled size`() {
        assertEquals(24f, resolveCompactDockLensDp(64f), 0.001f)
        assertEquals(16f, resolveCompactDockPressBloomDp(64f), 0.001f)
        assertEquals(10f, resolveCompactDockIndicatorLensHeightDp(64f), 0.001f)
        assertEquals(14f, resolveCompactDockIndicatorLensAmountDp(64f), 0.001f)
        assertEquals(8f, resolveCompactDockInnerShadowRadiusDp(64f), 0.001f)
        assertEquals(1.2f, resolveCompactDockTabPressScale(64f), 0.001f)

        val compact = 40f
        val intensity = 40f / 64f
        assertEquals(24f * intensity, resolveCompactDockLensDp(compact), 0.001f)
        assertEquals(16f * intensity, resolveCompactDockPressBloomDp(compact), 0.001f)
        assertEquals(10f * intensity, resolveCompactDockIndicatorLensHeightDp(compact), 0.001f)
        assertTrue(resolveCompactDockLensDp(compact) * 2f < compact)
        assertTrue(resolveCompactDockTabPressScale(compact) < 1.2f)
        assertTrue(resolveCompactDockTabPressScale(compact) > 1f)
    }

    @Test
    fun `home rest indicator keeps the HyperIsland 4dp per 64dp shell vertical inset`() {
        assertEquals(
            4f,
            resolveFloatingDockRestIndicatorVerticalInsetDp(shellHeightDp = 64f),
            0.001f,
        )
        // 56dp 壳按同一比例得到 3.5dp。原先这里期望 0f：指示器与壳同高、静止边距为 0。
        assertEquals(
            3.5f,
            resolveFloatingDockRestIndicatorVerticalInsetDp(shellHeightDp = 56f),
            0.001f,
        )
    }

    @Test
    fun `tight shell-height constraints do not steal rest inset for press overflow`() {
        val overflowPx = 7
        assertFalse(
            shouldReserveFloatingDockScaleOverflow(
                incomingMaxHeightPx = 64,
                shellHeightPx = 64,
                overflowPx = overflowPx,
            )
        )
        assertTrue(
            shouldReserveFloatingDockScaleOverflow(
                incomingMaxHeightPx = 64 + overflowPx * 2,
                shellHeightPx = 64,
                overflowPx = overflowPx,
            )
        )
        assertTrue(
            shouldReserveFloatingDockScaleOverflow(
                incomingMaxHeightPx = androidx.compose.ui.unit.Constraints.Infinity,
                shellHeightPx = 64,
                overflowPx = overflowPx,
            )
        )
    }

    @Test
    fun `pressed indicator overflow is reserved so compact docks are not clipped`() {
        val homeOverflow = resolveCompactDockScaleOverflowDp(
            shellHeightDp = 64f,
            indicatorHeightDp = 48f,
        )
        val compactOverflow = resolveCompactDockScaleOverflowDp(
            shellHeightDp = 40f,
            indicatorHeightDp = 30f,
        )
        assertEquals((78f - 64f) / 2f, homeOverflow, 0.001f)
        assertEquals((40f * 78f / 56f - 40f) / 2f, compactOverflow, 0.001f)
        assertTrue(compactOverflow > 0f)
    }

    @Test
    fun `narrow fitted indicator still grows beyond the dock while dragging`() {
        val fittedHeight = resolveFloatingDockIndicatorHeightDp(
            requestedHeightDp = 39f,
            tabWidthDp = 30f,
        )
        val geometry = com.android.purebilibili.core.ui.resolveMatchedLiquidIndicatorGeometry(
            dockHeightDp = 44f,
            indicatorHeightDp = fittedHeight,
        )

        assertTrue(fittedHeight < 39f)
        assertTrue(geometry.pressedHeightDp > geometry.dockHeightDp)
    }

    @Test
    fun `wide edge indicator capture covers drag scale and velocity stretch`() {
        val indicatorWidth = 470f
        val captureInsets = resolveFloatingDockCaptureInsets(
            shellHeightDp = 44f,
            requestedIndicatorHeightDp = 39f,
            indicatorWidthDp = indicatorWidth,
        )
        val geometry = com.android.purebilibili.core.ui.resolveMatchedLiquidIndicatorGeometry(
            dockHeightDp = 44f,
            indicatorHeightDp = 39f,
        )
        val restingScaleOverflow = indicatorWidth * (geometry.pressedScale - 1f) / 2f

        assertTrue(captureInsets.horizontalDp > restingScaleOverflow)
        assertTrue(captureInsets.verticalDp > 0f)
        assertTrue(captureInsets.horizontalDp > captureInsets.verticalDp)
    }

    @Test
    fun `system gesture inset never shrinks below the fallback edge`() {
        assertEquals(24f, resolveFloatingDockDragEdgeInsetPx(systemInsetPx = 0f, fallbackPx = 24f))
        assertEquals(80f, resolveFloatingDockDragEdgeInsetPx(systemInsetPx = 80f, fallbackPx = 24f))
    }
}
