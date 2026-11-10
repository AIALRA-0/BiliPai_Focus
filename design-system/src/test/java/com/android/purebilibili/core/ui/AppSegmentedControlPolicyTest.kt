package com.android.purebilibili.core.ui

import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.ui.components.shouldUseOpaqueMiuixTabBackdrop
import com.android.purebilibili.core.ui.components.shouldUseCompactMiuixTabRow
import com.android.purebilibili.core.ui.components.resolveMeasuredTabMinWidth
import com.android.purebilibili.core.ui.components.resolveCompactMiuixTabRowWidth
import com.android.purebilibili.core.ui.components.resolveAppMiuixTabContentColor
import com.android.purebilibili.core.ui.components.resolveAppMiuixTabTrackColor
import com.android.purebilibili.core.ui.components.resolveEqualMiuixNonGlassTabItemWidth
import com.android.purebilibili.core.ui.components.resolveMiuixTabMinWidth
import com.android.purebilibili.core.ui.components.resolvePiliPlusScrollableUnderlineMinWidth
import com.android.purebilibili.core.ui.components.resolveMiuixNonGlassContentTabItemWidths
import com.android.purebilibili.core.ui.components.shouldEqualizeMiuixNonGlassTabItems
import com.android.purebilibili.core.ui.components.shouldStretchMiuixNonGlassTabRowToTrack
import com.android.purebilibili.core.ui.components.MIUIX_NON_GLASS_TAB_ITEM_SPACING_DP
import com.android.purebilibili.core.ui.components.MiuixNonGlassTabItemWidthMode
import com.android.purebilibili.core.ui.components.resolveAppTabSlotGeometry
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSegmentedControlPolicyTest {
    @Test
    fun `fixed slots keep exact widths while scrollable`() {
        val geometry = resolveAppTabSlotGeometry(5, 60.dp, 8.dp, 180.dp, true)
        assertEquals(180.dp, geometry.containerWidth)
        assertEquals(60.dp, geometry.itemWidth)
        assertEquals(8.dp, geometry.horizontalPadding)
    }

    @Test
    fun `fixed slots use requested outer width when parent permits`() {
        val geometry = resolveAppTabSlotGeometry(3, 60.dp, 8.dp, 400.dp, false)
        assertEquals(196.dp, geometry.containerWidth)
        assertEquals(60.dp, geometry.itemWidth)
        assertEquals(8.dp, geometry.horizontalPadding)
    }

    @Test
    fun `non scrollable fixed slots divide available width after padding`() {
        val geometry = resolveAppTabSlotGeometry(3, 60.dp, 6.dp, 132.dp, false)
        assertEquals(132.dp, geometry.containerWidth)
        assertEquals(40.dp, geometry.itemWidth)
        assertEquals(6.dp, geometry.horizontalPadding)
    }

    @Test
    fun `fixed slots cannot overflow a parent narrower than its padding`() {
        val geometry = resolveAppTabSlotGeometry(2, 60.dp, 8.dp, 10.dp, false)
        assertEquals(10.dp, geometry.containerWidth)
        assertEquals(0.dp, geometry.itemWidth)
        assertEquals(5.dp, geometry.horizontalPadding)
    }

    @Test
    fun `empty fixed slots occupy no space`() {
        val geometry = resolveAppTabSlotGeometry(0, 60.dp, 8.dp, 400.dp, false)
        assertEquals(0.dp, geometry.containerWidth)
        assertEquals(0.dp, geometry.itemWidth)
        assertEquals(0.dp, geometry.horizontalPadding)
    }

    @Test
    fun nativeOutlinedItemsNeedOpaqueFillOnlyInsideImmersiveChrome() {
        assertTrue(shouldUseOpaqueMiuixTabBackdrop(nonGlassMiuix = true, immersiveTopChrome = true))
        assertFalse(shouldUseOpaqueMiuixTabBackdrop(nonGlassMiuix = true, immersiveTopChrome = false))
        assertFalse(shouldUseOpaqueMiuixTabBackdrop(nonGlassMiuix = false, immersiveTopChrome = true))
    }


    @Test
    fun `non glass Miuix removes outer dock while keeping readable labels`() {
        val track = Color(0xFF303030)
        val inactive = Color(0xFF8A8A8A)
        val readable = Color(0xFFF2F2F2)

        assertEquals(Color.Transparent, resolveAppMiuixTabTrackColor(true, track))
        assertEquals(track, resolveAppMiuixTabTrackColor(false, track))
        assertEquals(readable, resolveAppMiuixTabContentColor(true, inactive, readable))
        assertEquals(inactive, resolveAppMiuixTabContentColor(false, inactive, readable))

    }

    @Test
    fun `non glass tabs keep native geometry and grow for accessible text`() {
        assertEquals(RoundedControlVisualGeometry(42.dp, 12.dp),
            resolveMiuixNonGlassControlGeometry(false, 22.dp))
        assertEquals(RoundedControlVisualGeometry(36.dp, 10.dp),
            resolveMiuixNonGlassControlGeometry(true, 20.dp))
        assertEquals(RoundedControlVisualGeometry(64.dp, 12.dp),
            resolveMiuixNonGlassControlGeometry(false, 48.dp))
    }


    @Test
    fun `compact two option rows do not consume viewport`() {
        assertEquals(144.dp, resolveCompactMiuixTabRowWidth(400.dp, 72.dp, 2, false))
        assertEquals(400.dp, resolveCompactMiuixTabRowWidth(400.dp, 72.dp, 2, true))
        assertEquals(400.dp, resolveCompactMiuixTabRowWidth(400.dp, 72.dp, 3, false))
    }

    @Test
    fun `tab minimum follows measured text rather than character count`() {
        assertEquals(56.dp, resolveMeasuredTabMinWidth(48.dp, listOf(32.dp, 20.dp)))
        assertEquals(114.dp, resolveMeasuredTabMinWidth(48.dp, listOf(90.dp, 20.dp)))
        assertEquals(72.dp, resolveMeasuredTabMinWidth(72.dp, listOf(20.dp)))
        assertEquals(48.dp, resolveMeasuredTabMinWidth(48.dp, emptyList()))
        assertEquals(106.dp, resolveMeasuredTabMinWidth(52.dp, listOf(32.dp, 90.dp), horizontalPadding = 8.dp))
        assertEquals(52.dp, resolveMeasuredTabMinWidth(52.dp, listOf(32.dp), horizontalPadding = 8.dp))
    }

    @Test
    fun `Miuix non glass equal tabs follow the longest measured label`() {
        assertEquals(
            88.dp,
            resolveEqualMiuixNonGlassTabItemWidth(
                longestLabelWidth = 72.dp,
                minTabWidth = AppChromeSizeTokens.MinimumTouchTarget,
            ),
        )
        assertEquals(
            64.dp,
            resolveEqualMiuixNonGlassTabItemWidth(
                longestLabelWidth = 20.dp,
                minTabWidth = 64.dp,
            ),
        )
    }

    @Test
    fun `Miuix non glass content tabs size each item from its own label`() {
        assertEquals(
            listOf(48.dp, 75.dp, 320.dp),
            resolveMiuixNonGlassContentTabItemWidths(
                labelWidths = listOf(20.dp, 51.dp, 400.dp),
                minTabWidth = 48.dp,
            ),
        )
    }

    @Test
    fun `long collection titles do not inflate short content sized tabs`() {
        val sharedWidth = resolveMeasuredTabMinWidth(
            requestedMinWidth = 48.dp,
            labelWidths = listOf(32.dp, 32.dp, 176.dp),
        )
        assertTrue(sharedWidth > 56.dp)
        val contentMinWidth = resolveMiuixTabMinWidth(
            requestedMinWidth = 48.dp,
            sharedMinWidth = sharedWidth,
            contentSizedItems = true,
        )
        assertEquals(
            listOf(56.dp, 56.dp, 200.dp),
            resolveMiuixNonGlassContentTabItemWidths(
                labelWidths = listOf(32.dp, 32.dp, 176.dp),
                minTabWidth = contentMinWidth,
            ),
        )
        assertEquals(sharedWidth, resolveMiuixTabMinWidth(48.dp, sharedWidth, false))
        assertEquals(72.dp, resolveMiuixTabMinWidth(72.dp, sharedWidth, true))
        assertEquals(0.dp, resolvePiliPlusScrollableUnderlineMinWidth())
    }

    @Test
    fun `Miuix non glass content tabs can keep long labels complete`() {
        assertEquals(
            listOf(48.dp, 424.dp),
            resolveMiuixNonGlassContentTabItemWidths(
                labelWidths = listOf(20.dp, 400.dp),
                minTabWidth = 48.dp,
                maxTabWidth = androidx.compose.ui.unit.Dp.Infinity,
            ),
        )
    }

    @Test
    fun `equal longest label mode is isolated to Miuix non glass tabs`() {
        assertTrue(
            shouldEqualizeMiuixNonGlassTabItems(
                widthMode = MiuixNonGlassTabItemWidthMode.EQUAL_TO_LONGEST_LABEL,
                isMiuixNonGlass = true,
                optionCount = 2,
            )
        )
        assertFalse(
            shouldEqualizeMiuixNonGlassTabItems(
                widthMode = MiuixNonGlassTabItemWidthMode.EQUAL_TO_LONGEST_LABEL,
                isMiuixNonGlass = false,
                optionCount = 2,
            )
        )
        assertFalse(
            shouldEqualizeMiuixNonGlassTabItems(
                widthMode = MiuixNonGlassTabItemWidthMode.CONTENT,
                isMiuixNonGlass = true,
                optionCount = 2,
            )
        )
    }

    @Test
    fun `non glass Miuix page tabs stay content sized except compact two option tracks`() {
        assertTrue(
            shouldStretchMiuixNonGlassTabRowToTrack(
                compact = true,
                scrollable = false,
                optionCount = 2,
            )
        )
        assertFalse(
            shouldStretchMiuixNonGlassTabRowToTrack(
                compact = false,
                scrollable = false,
                optionCount = 3,
            )
        )
        assertFalse(
            shouldStretchMiuixNonGlassTabRowToTrack(
                compact = false,
                scrollable = true,
                optionCount = 6,
            )
        )
        assertEquals(9, MIUIX_NON_GLASS_TAB_ITEM_SPACING_DP)
    }

    @Test
    fun `only non scrollable two option Miuix rows use compact width`() {
        assertTrue(shouldUseCompactMiuixTabRow(2, scrollable = false, compactWhenTwoOptions = true))
        assertFalse(shouldUseCompactMiuixTabRow(2, scrollable = true, compactWhenTwoOptions = true))
        assertFalse(shouldUseCompactMiuixTabRow(3, scrollable = false, compactWhenTwoOptions = true))
        assertFalse(shouldUseCompactMiuixTabRow(2, scrollable = false, compactWhenTwoOptions = false))
    }

    @Test
    fun `material3 exposes material segmented capabilities`() {
        val policy = resolveAppSegmentedControlPolicy(AppUiStyle.MATERIAL3)

        assertTrue(policy.usesEmphasizedTitle)
        assertTrue(policy.usesMaterialFallback)
        assertTrue(policy.usesMaterialColorTokens)
        assertFalse(policy.usesNativeTabRow)
    }

    @Test
    fun `miuix exposes native tab row capability`() {
        val policy = resolveAppSegmentedControlPolicy(AppUiStyle.MIUIX)

        assertTrue(policy.usesEmphasizedTitle)
        assertTrue(policy.usesMaterialFallback)
        assertTrue(policy.usesNativeTabRow)
        assertFalse(policy.usesMaterialColorTokens)
    }

    @Test
    fun `segmented policy exposes semantic corners without a shared visual height`() {
        val material = resolveAppSegmentedControlPolicy(AppUiStyle.MATERIAL3)
        val miuix = resolveAppSegmentedControlPolicy(AppUiStyle.MIUIX)
        assertEquals(10.8.dp, material.preferredCornerRadius)
        assertEquals(16.dp, miuix.preferredCornerRadius)
    }

    @Test
    fun `oversized semantic corner is clamped instead of enlarging the control`() {
        val geometry = resolveRoundedControlVisualGeometry(
            preferredCornerRadius = 14.4.dp,
            nativeMinimumHeight = 40.dp,
        )

        assertEquals(48.dp, geometry.height)
        assertEquals(14.4.dp, geometry.cornerRadius)
        assertTrue(geometry.cornerRadius <= geometry.height * 0.3f + 0.1.dp)
    }

    @Test
    fun `native minimum wins when semantic corner already fits`() {
        val geometry = resolveRoundedControlVisualGeometry(
            preferredCornerRadius = 12.dp,
            nativeMinimumHeight = 42.dp,
        )

        assertEquals(42.dp, geometry.height)
        assertEquals(12.dp, geometry.cornerRadius)
    }

}
