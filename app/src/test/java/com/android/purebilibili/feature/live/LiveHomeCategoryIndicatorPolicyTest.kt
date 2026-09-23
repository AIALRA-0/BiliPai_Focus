package com.android.purebilibili.feature.live

import com.android.purebilibili.core.ui.CompactCapsuleChromeSpec
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveHomeCategoryIndicatorPolicyTest {

    @Test
    fun `recommend category resolves to first indicator position`() {
        val index = resolveLiveHomeCategorySelectedIndex(
            selectedAreaId = 0,
            areaIds = listOf(2, 3, 6)
        )

        assertEquals(0, index)
    }

    @Test
    fun `selected area resolves after recommend item`() {
        val index = resolveLiveHomeCategorySelectedIndex(
            selectedAreaId = 6,
            areaIds = listOf(2, 3, 6)
        )

        assertEquals(3, index)
    }

    @Test
    fun `unknown selected area falls back to recommend`() {
        val index = resolveLiveHomeCategorySelectedIndex(
            selectedAreaId = 99,
            areaIds = listOf(2, 3, 6)
        )

        assertEquals(0, index)
    }

    @Test
    fun `live home category control follows each chrome density`() {
        val ios = resolveLiveHomeCategorySegmentedControlSpec(
            compactChrome(primaryHeightDp = 44, compactChipHeightDp = 32),
        )
        val md3 = resolveLiveHomeCategorySegmentedControlSpec(
            compactChrome(primaryHeightDp = 56, compactChipHeightDp = 28),
        )
        val miuix = resolveLiveHomeCategorySegmentedControlSpec(
            compactChrome(primaryHeightDp = 48, compactChipHeightDp = 28),
        )

        assertEquals(44, ios.heightDp)
        assertEquals(39, ios.indicatorHeightDp)
        assertEquals(56, md3.heightDp)
        assertEquals(49, md3.indicatorHeightDp)
        assertEquals(48, miuix.heightDp)
        assertEquals(42, miuix.indicatorHeightDp)
        listOf(ios, md3, miuix).forEach { spec ->
            assertEquals(82, spec.itemWidthDp)
            assertEquals(14, spec.labelFontSizeSp)
            assertEquals(4, spec.containerHorizontalPaddingDp)
            assertEquals(4, spec.containerVerticalPaddingDp)
            assertEquals(20, spec.edgeBufferDp)
        }
    }

    @Test
    fun `all tags parent category uses fixed width so labels are not compressed`() {
        val spec = resolveLiveAreaParentSegmentedControlSpec(
            compactChrome(primaryHeightDp = 56, compactChipHeightDp = 28),
        )

        assertEquals(112, spec.itemWidthDp)
        assertEquals(56, spec.heightDp)
        assertEquals(49, spec.indicatorHeightDp)
        assertEquals(16, spec.labelFontSizeSp)
        assertEquals(4, spec.containerHorizontalPaddingDp)
        assertEquals(4, spec.containerVerticalPaddingDp)
        assertEquals(20, spec.edgeBufferDp)
    }

    @Test
    fun `follow scroll keeps visible indicator in place`() {
        val target = resolveLiveHomeCategoryFollowScrollTarget(
            indicatorPosition = 2f,
            itemWidthPx = 100f,
            itemCount = 8,
            viewportWidthPx = 320f,
            currentScrollPx = 0f,
            maxScrollPx = 500f,
            edgeBufferPx = 12f
        )

        assertEquals(0, target)
    }

    @Test
    fun `follow scroll moves right while indicator approaches hidden item`() {
        val target = resolveLiveHomeCategoryFollowScrollTarget(
            indicatorPosition = 4.2f,
            itemWidthPx = 100f,
            itemCount = 8,
            viewportWidthPx = 300f,
            currentScrollPx = 0f,
            maxScrollPx = 500f,
            edgeBufferPx = 20f
        )

        assertEquals(240, target)
    }

    @Test
    fun `follow scroll moves left while indicator returns toward hidden item`() {
        val target = resolveLiveHomeCategoryFollowScrollTarget(
            indicatorPosition = 1f,
            itemWidthPx = 100f,
            itemCount = 8,
            viewportWidthPx = 300f,
            currentScrollPx = 250f,
            maxScrollPx = 500f,
            edgeBufferPx = 20f
        )

        assertEquals(80, target)
    }

    private fun compactChrome(
        primaryHeightDp: Int,
        compactChipHeightDp: Int,
    ) = CompactCapsuleChromeSpec(
        primaryHeightDp = primaryHeightDp,
        secondaryButtonSizeDp = 48,
        chipHeightDp = 32,
        compactChipHeightDp = compactChipHeightDp,
        primaryCornerRadiusDp = 16,
        secondaryButtonCornerRadiusDp = 16,
        chipCornerRadiusDp = 16,
        compactChipCornerRadiusDp = 14,
        iconSizeDp = 20,
        smallIconSizeDp = 16,
        inputHorizontalPaddingDp = 12,
        chipHorizontalPaddingDp = 12,
        compactChipHorizontalPaddingDp = 10,
        standardGapDp = 8,
    )
}
