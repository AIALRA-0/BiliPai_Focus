package com.android.purebilibili.feature.home

import com.android.purebilibili.core.util.WindowWidthSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeFeedPinchZoomPolicyTest {

    @Test
    fun resolveHomeFeedPinchColumnBounds_compactScreen() {
        val bounds = resolveHomeFeedPinchColumnBounds(
            widthSizeClass = WindowWidthSizeClass.Compact,
            contentWidthDp = 412,
        )
        assertEquals(1..4, bounds)
    }

    @Test
    fun resolveHomeFeedPinchColumnBounds_narrowCompactScreen() {
        val bounds = resolveHomeFeedPinchColumnBounds(
            widthSizeClass = WindowWidthSizeClass.Compact,
            contentWidthDp = 340,
        )
        assertEquals(1..3, bounds)
    }

    @Test
    fun resolveHomeFeedPinchColumnBounds_expandedTablet() {
        val bounds = resolveHomeFeedPinchColumnBounds(
            widthSizeClass = WindowWidthSizeClass.Expanded,
            contentWidthDp = 1000,
        )
        assertEquals(2..6, bounds)
    }

    @Test
    fun resolveHomeFeedPinchColumnBounds_singleColumnMode() {
        val bounds = resolveHomeFeedPinchColumnBounds(
            widthSizeClass = WindowWidthSizeClass.Compact,
            contentWidthDp = 412,
            displayMode = 1,
        )
        assertEquals(1..1, bounds)
    }

    @Test
    fun calculatePinchStepColumns_zoomInDecreasesColumns() {
        val bounds = 1..4
        val (nextColumns, resetZoom) = calculatePinchStepColumns(
            currentColumns = 2,
            cumulativeZoom = 1.25f,
            bounds = bounds,
        )
        assertEquals(1, nextColumns)
        assertEquals(1.0f, resetZoom)
    }

    @Test
    fun calculatePinchStepColumns_zoomOutIncreasesColumns() {
        val bounds = 1..4
        val (nextColumns, resetZoom) = calculatePinchStepColumns(
            currentColumns = 2,
            cumulativeZoom = 0.80f,
            bounds = bounds,
        )
        assertEquals(3, nextColumns)
        assertEquals(1.0f, resetZoom)
    }

    @Test
    fun calculatePinchStepColumns_subThresholdRetainsState() {
        val bounds = 1..4
        val (nextColumns, retainedZoom) = calculatePinchStepColumns(
            currentColumns = 2,
            cumulativeZoom = 1.10f,
            bounds = bounds,
        )
        assertEquals(2, nextColumns)
        assertEquals(1.10f, retainedZoom)
    }

    @Test
    fun calculatePinchStepColumns_respectsUpperAndLowerBounds() {
        val bounds = 1..4
        val (atMin, _) = calculatePinchStepColumns(
            currentColumns = 1,
            cumulativeZoom = 1.30f,
            bounds = bounds,
        )
        assertEquals(1, atMin)

        val (atMax, _) = calculatePinchStepColumns(
            currentColumns = 4,
            cumulativeZoom = 0.75f,
            bounds = bounds,
        )
        assertEquals(4, atMax)
    }

    @Test
    fun calculatePinchVisualScale_normalZoomFollowsDirectly() {
        val bounds = 1..4
        // 处于 2 列，两指撑开 (放大视图)
        val zoomIn = calculatePinchVisualScale(
            cumulativeZoom = 1.15f,
            currentColumns = 2,
            bounds = bounds,
        )
        assertEquals(1.15f, zoomIn)

        // 处于 2 列，两指捏合 (缩小视图)
        val zoomOut = calculatePinchVisualScale(
            cumulativeZoom = 0.88f,
            currentColumns = 2,
            bounds = bounds,
        )
        assertEquals(0.88f, zoomOut)
    }

    @Test
    fun calculatePinchVisualScale_boundaryAppliesRubberBanding() {
        val bounds = 1..4
        // 已达最小列数 1（卡片最大），向外撑开时施加阻尼，视觉比例不会线性飙升
        val dampedIn = calculatePinchVisualScale(
            cumulativeZoom = 1.40f,
            currentColumns = 1,
            bounds = bounds,
        )
        // 1.0 + (1.4 - 1.0) * 0.25 = 1.10
        assertEquals(1.10f, dampedIn)

        // 已达最大列数 4（网格最紧凑），向内捏合时施加阻尼
        val dampedOut = calculatePinchVisualScale(
            cumulativeZoom = 0.60f,
            currentColumns = 4,
            bounds = bounds,
        )
        // 1.0 - (1.0 - 0.60) * 0.25 = 0.90
        assertEquals(0.90f, dampedOut)
    }
}
