package com.android.purebilibili.feature.home.components

import androidx.compose.ui.unit.dp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FloatingBottomBarGeometryTest {
    @Test
    fun navigationIndicatorGeometryUsesTheSameAxesAndInsetContour() {
        val shellThickness = 64f
        val slotExtent = 48f
        val thickness = resolveFloatingDockIndicatorHeightDp(
            requestedHeightDp = 56f,
            tabWidthDp = slotExtent,
            geometryMode = FloatingBottomBarGeometryMode.TopNavigation,
            shellHeightDp = shellThickness,
        )
        assertEquals(56f, thickness)
        assertEquals(4f, (shellThickness - thickness) / 2f)
        assertEquals(24f, resolveFloatingDockIndicatorCornerRadiusDp(shellThickness, thickness, slotExtent))
        assertEquals(28f, resolveFloatingDockIndicatorCornerRadiusDp(shellThickness, thickness, 80f))
        val geometry = resolveFloatingDockIndicatorGeometry(
            shellThickness, thickness, FloatingBottomBarGeometryMode.TopNavigation,
        )
        assertEquals(78f / 56f, geometry.pressedScale, 0.0001f)
    }

    @Test
    fun chromaticBandFollowsExpansionAndReturnsThroughTheSameSizes() {
        val scales = listOf(1f, 1.15f, 1.25f, 78f / 56f)
        val bands = scales.map { resolveFloatingDockChromaticGapBand(56f, 64f, it) }
        assertEquals(0f, bands.first())
        assertTrue(bands.zipWithNext().all { (before, after) -> after >= before })
        assertTrue(bands.last() > 0f)
        assertEquals(bands.reversed(), scales.reversed().map {
            resolveFloatingDockChromaticGapBand(56f, 64f, it)
        })
    }

    @Test
    fun nativeForegroundStaysVisibleWhileGlassCaptureOwnsCoveredContent() {
        for (coverage in listOf(-0.2f, 0f, 0.5f, 1f, 1.2f)) {
            assertEquals(1f, resolveFloatingDockBaseContentAlpha(isLiquidGlass = false, coverage = coverage))
        }
        assertEquals(1f, resolveFloatingDockBaseContentAlpha(isLiquidGlass = true, coverage = 0f))
        assertEquals(0.5f, resolveFloatingDockBaseContentAlpha(isLiquidGlass = true, coverage = 0.5f))
        assertEquals(0f, resolveFloatingDockBaseContentAlpha(isLiquidGlass = true, coverage = 1f))
    }


    @Test
    fun `top glass minimum slots keep horizontal capsules across content modes`() {
        // Compact text, docked icon+text, floating icon+text and icon-only shells.
        for (shell in listOf(40.dp, 48.dp, 52.dp)) {
            val indicatorHeight = resolveBiliPaiBottomBarIndicatorHeight(shell)
            for (contentWidth in listOf(24.dp, 48.dp, 88.dp, 160.dp)) {
                val slot = resolveHomeTopDockMinimumSlotWidth(shell, contentWidth, true)
                val fittedHeight = resolveFloatingDockIndicatorHeightDp(
                    indicatorHeight.value, slot.value,
                    FloatingBottomBarGeometryMode.TopNavigation, shell.value,
                )
                assertTrue(slot >= contentWidth)
                assertTrue(slot.value / fittedHeight >= FLOATING_DOCK_MIN_INDICATOR_ASPECT - 0.001f)
                assertEquals(indicatorHeight.value, fittedHeight, 0.001f)
            }
        }
    }

    @Test
    fun `top native minimum slots preserve content sizing`() {
        for (shell in listOf(40.dp, 48.dp, 52.dp)) {
            for (contentWidth in listOf(24.dp, 48.dp, 88.dp)) {
                assertEquals(contentWidth, resolveHomeTopDockMinimumSlotWidth(shell, contentWidth, false))
            }
        }
    }

    @Test
    fun `compact dock reuses bottom dock inset and press geometry`() {
        for (shellHeight in listOf(40f, 48f, 56f, 64f)) {
            val requestedHeight = resolveBiliPaiBottomBarIndicatorHeight(shellHeight.dp).value
            val compactHeight = resolveFloatingDockIndicatorHeightDp(
                requestedHeight, 80f, FloatingBottomBarGeometryMode.CompactDock, shellHeight,
            )
            val bottomHeight = resolveFloatingDockIndicatorHeightDp(
                requestedHeight, 80f, FloatingBottomBarGeometryMode.Dock, shellHeight,
            )
            assertEquals(shellHeight * 56f / 64f, compactHeight, 0.001f)
            assertEquals(bottomHeight, compactHeight, 0.001f)
            assertEquals(
                resolveFloatingDockIndicatorGeometry(shellHeight, bottomHeight, FloatingBottomBarGeometryMode.Dock),
                resolveFloatingDockIndicatorGeometry(shellHeight, compactHeight, FloatingBottomBarGeometryMode.CompactDock),
            )
        }
    }

    @Test
    fun `browsing a date rail never relocates selection onto a visible neighbour`() {
        val selected = 2f
        for (scroll in listOf(0f, 100f, 250f, 500f)) {
            val visual = resolveFloatingDockViewportIndicatorPosition(
                position = selected,
                tabWidthPx = 100f,
                tabsCount = 10,
                indicatorWidthPx = 80f,
                scrollOffsetPx = scroll,
                viewportWidthPx = 308f,
                horizontalPaddingPx = 4f,
                clampToViewport = false,
            )
            assertEquals(selected, visual)
            val offset = resolveFloatingDockIndicatorOffsetPx(visual, 100f, 10, 80f)
            assertEquals(210f, offset)
            assertEquals(-offset, resolveFloatingDockClippedContentTranslationPx(visual, 100f, 10, 80f))
        }
    }

    @Test
    fun `content shell trims endpoint whitespace without changing label spacing`() {
        for (widths in listOf(listOf(56f, 56f, 56f), listOf(56f, 120f, 88f))) {
            val slot = 160f
            val padding = 4f
            val totalWidth = slot * widths.size + padding * 2f
            val leading = resolveFloatingDockContentShellInsetPx(slot, widths.first())
            val trailing = resolveFloatingDockContentShellInsetPx(slot, widths.last())
            val first = padding + resolveFloatingDockIndicatorOffsetPx(0f, slot, 3, widths.first())
            val last = padding + resolveFloatingDockIndicatorOffsetPx(2f, slot, 3, widths.last())
            assertEquals(padding, first - leading, 0.001f)
            assertEquals(padding, totalWidth - trailing - last - widths.last(), 0.001f)
            val centers = widths.mapIndexed { index, width ->
                val offset = resolveFloatingDockIndicatorOffsetPx(index.toFloat(), slot, 3, width)
                assertEquals(0f, resolveFloatingDockIndicatorContentAlignmentPx(
                    index.toFloat(), slot, 3, width,
                ), 0.001f)
                padding + offset + width / 2f
            }
            centers.zipWithNext().forEach { (before, after) ->
                assertEquals(slot, after - before, 0.001f)
            }
        }
    }

    @Test
    fun `full slot capsules do not trim the shell`() {
        assertEquals(0f, resolveFloatingDockContentShellInsetPx(80f, 80f))
        assertEquals(0f, resolveFloatingDockContentShellInsetPx(80f, 120f))
        assertEquals(0f, resolveFloatingDockContentShellInsetPx(0f, 56f))
    }

    @Test
    fun `content capsule grows and shrinks continuously between labels`() {
        val widths = listOf(56f, 120f, 72f)
        fun width(position: Float) = resolveFloatingDockContentIndicatorWidthPx(position, widths, 160f)
        assertEquals(56f, width(0f))
        assertEquals(88f, width(0.5f))
        assertEquals(120f, width(1f))
        assertEquals(96f, width(1.5f))
        assertEquals(72f, width(2f))
        assertEquals(56f, width(-1f))
        assertEquals(72f, width(3f))
        assertEquals(80f, resolveFloatingDockContentIndicatorWidthPx(1f, widths, 80f))
        assertEquals(160f, resolveFloatingDockContentIndicatorWidthPx(1f, emptyList(), 160f))
    }

    @Test
    fun `scrolling rail keeps indicator inside visible dock with end padding`() {
        for (scroll in listOf(0f, 75f, 200f, 500f)) {
            for (position in listOf(0f, 0.5f, 2f, 4.5f, 7f)) {
                val visual = resolveFloatingDockViewportIndicatorPosition(
                    position = position,
                    tabWidthPx = 100f,
                    tabsCount = 8,
                    indicatorWidthPx = 80f,
                    scrollOffsetPx = scroll,
                    viewportWidthPx = 308f,
                    horizontalPaddingPx = 4f,
                )
                val screenLeft = 4f + resolveFloatingDockIndicatorOffsetPx(
                    visual, 100f, 8, 80f,
                ) - scroll
                assertTrue(screenLeft >= 4f - 0.001f)
                assertTrue(screenLeft + 80f <= 304f + 0.001f)
            }
        }
    }

    @Test
    fun `viewport clamp preserves visible motion and follows rail at either edge`() {
        fun visual(position: Float, scroll: Float) = resolveFloatingDockViewportIndicatorPosition(
            position, 100f, 8, 80f, scroll, 308f, 4f,
        )
        assertEquals(3f, visual(3f, 200f), 0.001f)
        assertEquals(1.9f, visual(0f, 200f), 0.001f)
        assertEquals(4.1f, visual(7f, 200f), 0.001f)
        assertTrue(visual(7f, 210f) > visual(7f, 200f))
    }

    @Test
    fun `chromatic gap stays outside the track at each indicator size`() {
        for ((indicatorHeight, shellHeight, pressedHeight) in listOf(
            Triple(56f, 64f, 78f),
            Triple(35f, 40f, 48.75f),
            Triple(28f, 32f, 39f),
        )) {
            assertEquals(0f, resolveFloatingDockChromaticGapBand(indicatorHeight, shellHeight, 1f))
            val scale = pressedHeight / indicatorHeight
            val band = resolveFloatingDockChromaticGapBand(indicatorHeight, shellHeight, scale)
            assertTrue(band > 0f)
            // The two colored bands end exactly at the track boundaries, leaving
            // the label and central glass area outside the dispersion mask.
            assertEquals(shellHeight, (indicatorHeight - 2f * band) * scale, 0.001f)
            assertTrue(resolveFloatingDockChromaticGapBand(indicatorHeight, shellHeight, scale, 1.05f) < band)
        }
        assertEquals(0f, resolveFloatingDockChromaticGapBand(56f, 64f, 0f))
        assertEquals(0f, resolveFloatingDockChromaticGapBand(56f, 64f, Float.NaN))
    }

    @Test
    fun `drag autoscroll is stationary in center and proportional at edges`() {
        assertEquals(0f, resolveDockDragEdgeScrollFraction(150f, 300f, 60f))
        assertEquals(-0.5f, resolveDockDragEdgeScrollFraction(30f, 300f, 60f), 0.001f)
        assertEquals(0.5f, resolveDockDragEdgeScrollFraction(270f, 300f, 60f), 0.001f)
        assertEquals(-1f, resolveDockDragEdgeScrollFraction(-20f, 300f, 60f))
        assertEquals(1f, resolveDockDragEdgeScrollFraction(320f, 300f, 60f))
    }

    @Test
    fun `drag autoscroll rejects unmeasured and invalid viewport geometry`() {
        assertEquals(0f, resolveDockDragEdgeScrollFraction(0f, 0f, 60f))
        assertEquals(0f, resolveDockDragEdgeScrollFraction(0f, 300f, 0f))
        assertEquals(0f, resolveDockDragEdgeScrollFraction(Float.NaN, 300f, 60f))
    }

    @Test
    fun `home dock restores original 56 to 78 press bloom ratio`() {
        assertEquals(78f / 56f, FloatingBottomBarPressedScale, 0.0001f)
    }

    @Test
    fun `home top label modes preserve equal shell insets without width flattening`() {
        // Cover both top shell styles and all label modes under constrained widths.
        for (mode in listOf(0, 1, 2)) {
            for (floating in listOf(false, true)) {
                val shell = resolveHomeTopDockShellHeight(floating)
                for (container in listOf(240.dp, 393.dp)) {
                    val width = resolveHomeTopTabFloatingDockWidth(container, 5, mode, shell)
                    val slot = resolveFloatingDockSlotWidthPx(width.value, 8f, 5)
                    val height = resolveFloatingDockIndicatorHeightDp(
                        shell.value - 8f, slot, FloatingBottomBarGeometryMode.TopNavigation, shell.value,
                    )
                    assertEquals(4f, (shell.value - height) / 2f, 0.001f, "label mode $mode")
                }
            }
        }
    }

    @Test
    fun `dynamic top navigation fills its shell while comment controls stay compact`() {
        assertEquals(38f, resolveFloatingDockIndicatorHeightDp(
            38f, 70f, FloatingBottomBarGeometryMode.TopNavigation, 50f,
        ), 0.001f)
        assertEquals(30f, resolveFloatingDockIndicatorHeightDp(
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
            assertEquals(minOf(30f, shell - 6f), height, 0.001f)
            assertTrue(slot / height >= 1.6f)
        }
        assertEquals(36f, resolveFloatingDockIndicatorHeightDp(
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
    fun `click selection and external fractional follow keep optics active without a pointer press`() {
        for ((position, target) in listOf(0.5f to 1f, 1.5f to 1.5f, 2.75f to 3f)) {
            val motion = resolveFloatingDockSelectionMotionProgress(position, target)
            assertEquals(1f, resolveFloatingDockRefractionProgress(
                pressProgress = 0f,
                tapPressRefractionEnabled = false,
                isDragging = false,
                isPagerScrolling = false,
                selectionMotionProgress = motion,
            ))
        }
        assertEquals(0f, resolveFloatingDockSelectionMotionProgress(3f, 3f))
        assertEquals(0f, resolveFloatingDockSelectionMotionProgress(3.005f, 3f))
        assertEquals(0f, resolveFloatingDockSelectionMotionProgress(Float.NaN, 1f))
        // Reversing a selection reuses current geometry; it does not need a new press.
        assertEquals(
            resolveFloatingDockSelectionMotionProgress(0.5f, 1f),
            resolveFloatingDockSelectionMotionProgress(0.5f, 0f),
        )
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
    fun `segmented resting capsules stay oval with shared shell relative bloom`() {
        for (width in listOf(40f, 48f, 56f, 64f, 108f)) {
            val height = resolveFloatingDockIndicatorHeightDp(
                requestedHeightDp = 52f,
                tabWidthDp = width,
                geometryMode = FloatingBottomBarGeometryMode.Segmented,
            )
            assertEquals(resolveSegmentedControlIndicatorHeightDp(width, 52f), height, 0.001f)
            assertTrue(width / height >= 1.6f)
            val pressed = resolveFloatingDockIndicatorGeometry(
                shellHeightDp = 56f,
                indicatorHeightDp = height,
                geometryMode = FloatingBottomBarGeometryMode.Segmented,
            )
            assertEquals(56f * 78f / 64f, pressed.pressedHeightDp, 0.001f)
            assertTrue(pressed.pressedHeightDp > 56f)
            val overflow = resolveFloatingDockCaptureInsets(
                shellHeightDp = 56f,
                requestedIndicatorHeightDp = 52f,
                indicatorWidthDp = width,
                geometryMode = FloatingBottomBarGeometryMode.Segmented,
            )
            assertTrue(overflow.verticalDp >= (pressed.pressedHeightDp - 56f) / 2f)
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

    @Test
    fun `adaptive scroll shell keeps endpoint inset constant while approaching content edge`() {
        // A 60px capsule is centered inside a 100px slot: the extra 20px is
        // outside the visible shell until scrolling carries that content edge away.
        for (distance in listOf(0f, 4f, 10f, 20f)) {
            val trim = resolveFloatingDockScrollableShellInsetPx(100f, 60f, distance)
            assertEquals(20f - distance, trim, 0.001f)
            assertEquals(0f, 20f - distance - trim, 0.001f)
        }
        assertEquals(0f, resolveFloatingDockScrollableShellInsetPx(100f, 60f, 40f))
        assertEquals(0f, resolveFloatingDockScrollableShellInsetPx(100f, 100f, 0f))
        assertEquals(0f, resolveFloatingDockScrollableShellInsetPx(100f, 160f, 0f))
    }

}
