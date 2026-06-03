package com.android.purebilibili.feature.live

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
    fun `live home category control fits four visible items with matching indicator width`() {
        val spec = resolveLiveHomeCategorySegmentedControlSpec()

        assertEquals(82, spec.itemWidthDp)
        assertEquals(44, spec.heightDp)
        assertEquals(30, spec.indicatorHeightDp)
        assertEquals(14, spec.labelFontSizeSp)
        assertEquals(4, spec.containerHorizontalPaddingDp)
        assertEquals(4, spec.containerVerticalPaddingDp)
    }

    @Test
    fun `all tags parent category uses fixed width so labels are not compressed`() {
        val spec = resolveLiveAreaParentSegmentedControlSpec()

        assertEquals(112, spec.itemWidthDp)
        assertEquals(44, spec.heightDp)
        assertEquals(30, spec.indicatorHeightDp)
        assertEquals(16, spec.labelFontSizeSp)
        assertEquals(4, spec.containerHorizontalPaddingDp)
        assertEquals(4, spec.containerVerticalPaddingDp)
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
}
