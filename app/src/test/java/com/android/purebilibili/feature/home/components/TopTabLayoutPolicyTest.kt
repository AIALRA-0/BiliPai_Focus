package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.store.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabLayoutPolicyTest {
    @Test
    fun `legacy top tabs divide the available width across all five items`() {
        assertEquals(
            72f,
            resolveFixedHomeTopTabItemWidthDp(
                containerWidthDp = 360f,
                categoryCount = 5,
            ),
            0.001f,
        )
    }

    @Test
    fun `visible slot count should stay in compact range`() {
        assertEquals(1, resolveTopTabVisibleSlots(1))
        assertEquals(3, resolveTopTabVisibleSlots(3))
        assertEquals(4, resolveTopTabVisibleSlots(4))
        assertEquals(5, resolveTopTabVisibleSlots(5, longestLabelLength = 6))
        assertEquals(5, resolveTopTabVisibleSlots(6, longestLabelLength = 2))
        assertEquals(4, resolveTopTabVisibleSlots(5, longestLabelLength = 9))
        assertEquals(4, resolveTopTabVisibleSlots(8, longestLabelLength = 10))
        assertEquals(5, resolveTopTabVisibleSlots(8, longestLabelLength = 2))
    }

    @Test
    fun `floating style should enforce wider min width to avoid clipping`() {
        assertEquals(72f, resolveTopTabItemWidthDp(260f, 5, isFloatingStyle = true), 0.001f)
    }

    @Test
    fun `docked style should keep a denser minimum width`() {
        assertEquals(64f, resolveTopTabItemWidthDp(260f, 5, isFloatingStyle = false), 0.001f)
    }

    @Test
    fun `wide containers should use proportional width`() {
        assertEquals(100f, resolveTopTabItemWidthDp(500f, 5, isFloatingStyle = true), 0.001f)
    }

    @Test
    fun `ios top tab action shares centered slot with visible categories`() {
        assertEquals(1, resolveTopTabVisibleCategorySlots(1, longestLabelLength = 2))
        assertEquals(3, resolveTopTabVisibleCategorySlots(3, longestLabelLength = 2))
        assertEquals(5, resolveTopTabVisibleCategorySlots(5, longestLabelLength = 6))
        assertEquals(150f, resolveTopTabActionSlotWidthDp(600f, 3, longestLabelLength = 2), 0.001f)
        assertEquals(100f, resolveTopTabActionSlotWidthDp(600f, 5, longestLabelLength = 6), 0.001f)
        assertEquals(100f, resolveTopTabItemWidthDp(500f, 5, isFloatingStyle = false), 0.001f)
    }

    @Test
    fun textOnlyTabsKeepRoomForTwoChineseCharactersOnNarrowScreens() {
        // 64dp floor keeps two CJK glyphs after compact side padding instead of pure "...".
        assertEquals(
            64f,
            resolveMd3TopTabItemWidthDp(
                containerWidthDp = 248f,
                visibleSlots = 5,
                labelMode = 2
            ),
            0.001f
        )
    }

    @Test
    fun `md3 top tabs use compact scrollable item widths instead of fixed four slots`() {
        assertEquals(3, resolveMd3TopTabVisibleSlots())
        assertEquals(106.666f, resolveMd3TopTabItemWidthDp(containerWidthDp = 320f), 0.001f)
        assertEquals(120f, resolveMd3TopTabItemWidthDp(containerWidthDp = 360f), 0.001f)
        assertEquals(213.333f, resolveMd3TopTabItemWidthDp(containerWidthDp = 640f), 0.001f)
        // Five slots use the full phone width while keeping labels readable.
        assertEquals(72f, resolveMd3TopTabItemWidthDp(containerWidthDp = 360f, visibleSlots = 5), 0.001f)
    }

    @Test
    fun `icon and text tabs reserve enough width for Chinese labels`() {
        assertEquals(80f, resolveTopTabWrapItemWidthDp(labelMode = 0, isFloatingStyle = false), 0.001f)
        assertEquals(84f, resolveTopTabWrapItemWidthDp(labelMode = 0, isFloatingStyle = true), 0.001f)
        assertEquals(72f, resolveTopTabWrapItemWidthDp(labelMode = 2, isFloatingStyle = true), 0.001f)
        assertEquals(
            80f,
            resolveMd3TopTabItemWidthDp(
                containerWidthDp = 360f,
                visibleSlots = 5,
                labelMode = 0
            ),
            0.001f
        )
        assertEquals(
            80f,
            resolveIosTopTabItemWidthDp(
                containerWidthDp = 360f,
                categoryCount = 5,
                labelMode = 0
            ),
            0.001f
        )
    }

    @Test
    fun `five text tabs fit the phone width without pure ellipsis`() {
        val itemWidth = resolveMd3TopTabItemWidthDp(
            containerWidthDp = 360f,
            visibleSlots = 5,
            labelMode = 2
        )
        assertEquals(72f, itemWidth, 0.001f)
        // Content budget after 3dp outer + 4dp content padding each side.
        assertTrue("text room should fit two CJK glyphs", itemWidth - 14f >= 30f)
        assertTrue(
            "five text tabs should fit the viewport",
            itemWidth * 5f <= 360f + 0.001f
        )
    }

    @Test
    fun `md3 top tabs center sparse categories in three slot viewport`() {
        val itemWidth = resolveMd3TopTabItemWidthDp(containerWidthDp = 360f)

        assertEquals(60f, resolveMd3TopTabContentPaddingDp(360f, itemWidth, categoryCount = 2), 0.001f)
        assertEquals(120f, resolveMd3TopTabContentPaddingDp(360f, itemWidth, categoryCount = 1), 0.001f)
        assertEquals(0f, resolveMd3TopTabContentPaddingDp(360f, itemWidth, categoryCount = 3), 0.001f)
    }

    @Test
    fun `md3 and miuix multi-tab rows lead align on phones and center on tablets`() {
        val itemWidth = resolveMd3TopTabItemWidthDp(containerWidthDp = 400f, visibleSlots = 5)

        // 400/5 = 80, within the text-only floor/ceiling (64–88).
        assertEquals(80f, itemWidth, 0.001f)
        // Phone rows remain lead-aligned when the tab group is dense.
        assertEquals(
            0f,
            resolveMd3TopTabContentPaddingDp(
                containerWidthDp = 400f,
                itemWidthDp = itemWidth,
                categoryCount = 5,
                labelMode = 2
            ),
            0.001f
        )
        assertEquals(
            0f,
            resolveMd3TopTabContentPaddingDp(
                containerWidthDp = 400f,
                itemWidthDp = itemWidth,
                categoryCount = 5,
                labelMode = 0
            ),
            0.001f
        )
        // Sparse 1–2 tabs still center residual so a lone tab is not glued left
        // 2 × 120 on 360 → leftover 120 → padding 60
        assertEquals(
            60f,
            resolveMd3TopTabContentPaddingDp(
                containerWidthDp = 360f,
                itemWidthDp = 120f,
                categoryCount = 2,
                labelMode = 0
            ),
            0.001f
        )
        // 5 × 88 on 1000 → leftover 560 → centered padding 280.
        assertEquals(
            280f,
            resolveMd3TopTabContentPaddingDp(
                containerWidthDp = 1000f,
                itemWidthDp = 88f,
                categoryCount = 5,
                labelMode = 2
            ),
            0.001f
        )
    }

    @Test
    fun `md3 indicator translation at index zero sits at leading edge when padding is zero`() {
        // position 0, no content padding: indicator left = (item - indicator) / 2 (slot center)
        assertEquals(
            2f,
            resolveMd3TopTabIndicatorTranslationPx(
                absolutePagerPosition = 0f,
                itemWidthPx = 72f,
                rowScrollOffsetPx = 0f,
                indicatorWidthPx = 68f, // item - 2*2dp gap
                contentPaddingPx = 0f
            ),
            0.001f
        )
        // With leftover centered (old bug): first indicator jumps right by padding
        assertEquals(
            22f,
            resolveMd3TopTabIndicatorTranslationPx(
                absolutePagerPosition = 0f,
                itemWidthPx = 72f,
                rowScrollOffsetPx = 0f,
                indicatorWidthPx = 68f,
                contentPaddingPx = 20f
            ),
            0.001f
        )
    }

    @Test
    fun `md3 top tabs show at most five tabs for every label mode`() {
        listOf(0, 1, 2).forEach { labelMode ->
            assertEquals(
                5,
                resolveMd3TopTabLayoutVisibleSlots(
                    categoryCount = 6,
                    labelMode = labelMode,
                    showPartitionAction = false
                )
            )
            assertEquals(
                5,
                resolveMd3TopTabLayoutVisibleSlots(
                    categoryCount = 5,
                    labelMode = labelMode,
                    showPartitionAction = false
                )
            )
        }
        assertEquals(
            4,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 4,
                labelMode = 2,
                showPartitionAction = false
            )
        )
    }

    @Test
    fun `md3 top tabs fit five inline tabs including partition within phone width`() {
        assertEquals(
            5,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 5,
                labelMode = 2,
                showPartitionAction = false
            )
        )
        listOf(0, 1, 2).forEach { labelMode ->
            val visibleSlots = resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 5,
                labelMode = labelMode,
                showPartitionAction = false,
                containerWidthDp = 360f
            )
            val itemWidth = resolveMd3TopTabItemWidthDp(
                containerWidthDp = 360f,
                visibleSlots = visibleSlots,
                labelMode = labelMode
            )
            assertTrue(
                "visible tabs must fit within 360dp for labelMode=$labelMode, got ${itemWidth * visibleSlots}",
                itemWidth * visibleSlots <= 360f + 0.001f
            )
            assertTrue(
                "item width must keep complete chrome for labelMode=$labelMode",
                itemWidth + 0.001f >= resolveMd3TopTabMinItemWidthDp(labelMode)
            )
        }
    }

    @Test
    fun `ios top tabs cap legacy over-limit counts at five slots`() {
        listOf(0, 1, 2).forEach { labelMode ->
            assertEquals(
                5,
                resolveIosTopTabLayoutVisibleSlots(
                    categoryCount = 6,
                    labelMode = labelMode
                )
            )
            val itemWidth = resolveIosTopTabItemWidthDp(
                containerWidthDp = 360f,
                categoryCount = 6,
                labelMode = labelMode
            )
            assertTrue(
                "labelMode=$labelMode itemWidth=$itemWidth",
                itemWidth >= resolveMd3TopTabMinItemWidthDp(labelMode)
            )
        }
    }

    @Test
    fun `ios icon-only and text-only tabs fit five inline tabs within phone width`() {
        listOf(1, 2).forEach { labelMode ->
            assertEquals(
                5,
                resolveIosTopTabLayoutVisibleSlots(
                    categoryCount = 5,
                    labelMode = labelMode
                )
            )
            val itemWidth = resolveIosTopTabItemWidthDp(
                containerWidthDp = 360f,
                categoryCount = 5,
                labelMode = labelMode
            )
            assertTrue(
                "five ios tabs must fit within 360dp for labelMode=$labelMode, got ${itemWidth * 5f}",
                itemWidth * 5f + 4f <= 360f + 0.001f
            )
        }
    }

    @Test
    fun `md3 top tabs cap expanded custom tabs at five visible slots`() {
        assertEquals(
            5,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 8,
                labelMode = 0,
                showPartitionAction = false
            )
        )
        assertEquals(
            5,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 8,
                labelMode = 2,
                showPartitionAction = false
            )
        )
    }

    @Test
    fun `md3 top tabs become scrollable instead of truncating labels at large font scale`() {
        assertEquals(
            4,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 6,
                labelMode = 2,
                showPartitionAction = false,
                fontScale = 1.3f
            )
        )
        assertEquals(
            5,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 6,
                labelMode = 2,
                showPartitionAction = false,
                fontScale = 1.15f
            )
        )
    }

    @Test
    fun `md3 top tabs keep compact scrollable slots for external partition action`() {
        assertEquals(
            3,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 5,
                labelMode = 2,
                showPartitionAction = true
            )
        )
    }

    @Test
    fun `floating dock should wrap width to tab content instead of full bleed`() {
        assertTrue(
            shouldWrapTopTabDockWidth(
                isFloatingStyle = true,
                hasOuterChromeSurface = true,
                edgeToEdge = false
            )
        )
        assertTrue(
            shouldWrapTopTabDockWidth(
                isFloatingStyle = true,
                hasOuterChromeSurface = false,
                edgeToEdge = false
            )
        )
        assertFalse(
            shouldWrapTopTabDockWidth(
                isFloatingStyle = false,
                hasOuterChromeSurface = false,
                edgeToEdge = false
            )
        )
        assertFalse(
            shouldWrapTopTabDockWidth(
                isFloatingStyle = true,
                hasOuterChromeSurface = true,
                edgeToEdge = true
            )
        )
    }

    @Test
    fun `wrap and floating docks use the floating bottom bar end inset`() {
        assertEquals(4f, resolveTopTabDockEndInsetDp(wrapContent = true, isFloatingStyle = true), 0.001f)
        assertEquals(4f, resolveTopTabDockEndInsetDp(wrapContent = false, isFloatingStyle = true), 0.001f)
        assertEquals(0f, resolveTopTabDockEndInsetDp(wrapContent = false, isFloatingStyle = false), 0.001f)
    }

    @Test
    fun `first and last liquid capsules match the floating bottom bar dock padding`() {
        val endInset = resolveTopTabDockEndInsetDp(wrapContent = true, isFloatingStyle = true)
        val indicatorGap = resolveTopTabDockIndicatorHorizontalGapDp(hasOuterChromeSurface = true)
        // FloatingBottomBar uses 4dp content padding. The 2dp indicator gap is the
        // remaining slot inset, so the first capsule starts 6dp from the glass edge.
        assertEquals(4f, endInset, 0.001f)
        assertEquals(2f, indicatorGap, 0.001f)
        assertEquals(6f, endInset + indicatorGap, 0.001f)
        assertEquals(
            6f,
            resolveTopTabDockIndicatorOffsetPx(
                slotTranslationPx = endInset,
                horizontalGapPx = indicatorGap
            ),
            0.001f
        )
    }

    @Test
    fun `wrap dock width follows preferred item width times tab count`() {
        val endInset = resolveTopTabDockEndInsetDp(wrapContent = true, isFloatingStyle = true)
        assertEquals(4f, endInset, 0.001f)
        // Icon + text floating: 84 × 5 + 4 × 2 = 428, fits in 440
        assertEquals(84f, resolveTopTabWrapItemWidthDp(labelMode = 0, isFloatingStyle = true), 0.001f)
        assertEquals(
            428f,
            resolveTopTabDockWrapWidthDp(
                itemWidthDp = 84f,
                categoryCount = 5,
                maxWidthDp = 440f,
                contentPaddingHorizontalDp = endInset
            ),
            0.001f
        )
        // Icon only: 56 × 5 + 8 = 288
        assertEquals(56f, resolveTopTabWrapItemWidthDp(labelMode = 1, isFloatingStyle = true), 0.001f)
        assertEquals(
            288f,
            resolveTopTabDockWrapWidthDp(
                itemWidthDp = 56f,
                categoryCount = 5,
                maxWidthDp = 440f,
                contentPaddingHorizontalDp = endInset
            ),
            0.001f
        )
        // Text only: 72 × 5 + 8 = 368
        assertEquals(72f, resolveTopTabWrapItemWidthDp(labelMode = 2, isFloatingStyle = true), 0.001f)
        assertEquals(
            368f,
            resolveTopTabDockWrapWidthDp(
                itemWidthDp = 72f,
                categoryCount = 5,
                maxWidthDp = 440f,
                contentPaddingHorizontalDp = endInset
            ),
            0.001f
        )
        // Overflow clamps to max so small phones still fill
        assertEquals(
            300f,
            resolveTopTabDockWrapWidthDp(
                itemWidthDp = 84f,
                categoryCount = 5,
                maxWidthDp = 300f
            ),
            0.001f
        )
    }

    @Test
    fun `wrapped dock can stay centered for every label mode and count`() {
        val containerWidths = listOf(320f, 360f, 440f)

        listOf(0, 1, 2).forEach { labelMode ->
            (1..SettingsManager.MAX_TOP_TABS).forEach { categoryCount ->
                containerWidths.forEach { containerWidth ->
                    val itemWidth = resolveTopTabWrapItemWidthDp(
                        labelMode = labelMode,
                        isFloatingStyle = true
                    )
                    val dockWidth = resolveTopTabDockWrapWidthDp(
                        itemWidthDp = itemWidth,
                        categoryCount = categoryCount,
                        maxWidthDp = containerWidth,
                        contentPaddingHorizontalDp = resolveTopTabDockEndInsetDp(
                            wrapContent = true,
                            isFloatingStyle = true
                        )
                    )
                    val leadingSpace = (containerWidth - dockWidth) / 2f
                    val trailingSpace = containerWidth - dockWidth - leadingSpace

                    assertTrue(
                        "dock must fit: mode=$labelMode count=$categoryCount width=$containerWidth",
                        dockWidth <= containerWidth + 0.001f
                    )
                    assertEquals(
                        "center margins: mode=$labelMode count=$categoryCount width=$containerWidth",
                        leadingSpace,
                        trailingSpace,
                        0.001f
                    )
                }
            }
        }
    }

    @Test
    fun `wrap dock item width uses preferred when pack fits otherwise falls back`() {
        val preferred = resolveTopTabWrapItemWidthDp(labelMode = 0, isFloatingStyle = true)
        assertEquals(
            preferred,
            resolveTopTabDockItemWidthDp(
                maxWidthDp = 440f,
                categoryCount = 5,
                labelMode = 0,
                isFloatingStyle = true,
                wrapContent = true,
                fillItemWidthDp = 72f
            ),
            0.001f
        )
        // Too narrow for preferred pack → use fill width
        assertEquals(
            60f,
            resolveTopTabDockItemWidthDp(
                maxWidthDp = 300f,
                categoryCount = 5,
                labelMode = 0,
                isFloatingStyle = true,
                wrapContent = true,
                fillItemWidthDp = 60f
            ),
            0.001f
        )
        // wrapContent off always uses fill
        assertEquals(
            72f,
            resolveTopTabDockItemWidthDp(
                maxWidthDp = 440f,
                categoryCount = 5,
                labelMode = 0,
                isFloatingStyle = true,
                wrapContent = false,
                fillItemWidthDp = 72f
            ),
            0.001f
        )
    }

    @Test
    fun `ios top tabs reserve enough height for icon label modes`() {
        // Stacked icon+text needs the same taller track reserved by HomeTopPresetStyle.
        assertEquals(40f, resolveIosTopTabRowHeight(isFloatingStyle = true, labelMode = 2).value, 0.001f)
        assertEquals(40f, resolveIosTopTabRowHeight(isFloatingStyle = true, labelMode = 1).value, 0.001f)
        assertEquals(60f, resolveIosTopTabRowHeight(isFloatingStyle = true, labelMode = 0).value, 0.001f)
        assertEquals(56f, resolveIosTopTabRowHeight(isFloatingStyle = false, labelMode = 0).value, 0.001f)
    }

    @Test
    fun `top tab item content policy keeps icon plus text inside the compact background`() {
        assertEquals(30f, resolveTopTabContentMinHeightDp(labelMode = 0), 0.001f)
        assertEquals(30f, resolveTopTabContentMinHeightDp(labelMode = 1), 0.001f)
        assertEquals(30f, resolveTopTabContentMinHeightDp(labelMode = 2), 0.001f)
        assertEquals(5f, resolveTopTabContentVerticalPaddingDp(labelMode = 0), 0.001f)
        assertEquals(5f, resolveTopTabContentVerticalPaddingDp(labelMode = 1), 0.001f)
        assertEquals(5f, resolveTopTabContentVerticalPaddingDp(labelMode = 2), 0.001f)
    }

    @Test
    fun `narrow phones keep complete labels by reducing visible slots per mode`() {
        assertEquals(
            4,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 5,
                labelMode = 0,
                showPartitionAction = false,
                containerWidthDp = 320f
            )
        )
        assertEquals(
            5,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 5,
                labelMode = 1,
                showPartitionAction = false,
                containerWidthDp = 320f
            )
        )
        assertEquals(
            5,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 5,
                labelMode = 2,
                showPartitionAction = false,
                containerWidthDp = 320f
            )
        )
        val iconTextWidth = resolveMd3TopTabItemWidthDp(
            containerWidthDp = 320f,
            visibleSlots = 4,
            labelMode = 0
        )
        assertTrue(iconTextWidth >= resolveMd3TopTabMinItemWidthDp(0))
        assertTrue(iconTextWidth * 4f <= 320f + 0.001f)
    }

    @Test
    fun `md3 top tabs keep every category in scroll order`() {
        assertEquals(
            listOf(0, 1, 2, 3, 4),
            resolveMd3VisibleTabIndices(totalCount = 5, selectedIndex = 0)
        )
        assertEquals(
            listOf(0, 1, 2, 3, 4),
            resolveMd3VisibleTabIndices(totalCount = 5, selectedIndex = 4)
        )
        assertEquals(
            4,
            resolveMd3SelectedVisibleIndex(
                visibleIndices = listOf(0, 1, 2, 3, 4),
                selectedIndex = 4
            )
        )
    }
}
